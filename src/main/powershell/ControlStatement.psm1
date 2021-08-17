﻿using module '.\TargetInfo.psm1'
using module '.\TargetEnumerator.psm1'
using module '.\DocumentWriter.psm1'

using namespace System.Collections.Generic

# 制御文構造保持
class ControlHolder {

    # 内包する制御文
    [ControlHolder[]] $controls = @()

    # ネスト制御文種別
    [string] $token = ''

    [void] Open([string] $token) {
        if ($this.IsNested()) {
            throw (New-Object -TypeName 'System.InvalidOperationException' -ArgumentList ('既にOpenされている制御文に重複してOpenは実行できません。'))
        }

        $this.token = $token
    }

    [void] Close([string[]] $params, $cell) {
        if ($this.IsNested()) {
            # クローズトークンチェック
            if ($params[2] -ne $this.token) {
                throw (New-Object -TypeName 'System.InvalidOperationException' `
                    -ArgumentList ("制御文の組み合わせが正しくありません。指定: $($params[2]), 想定: $($this.token)"))
            }
        }
        else {
            throw (New-Object -TypeName 'System.InvalidOperationException' -ArgumentList ('ネストしない制御文に対するCloseは無効です。'))
        }
    }

    [bool] IsNested() {
        return ($this.token -ne '')
    }

}

# シート書式
class SheetFormat : ControlHolder {

    # シート名
    [string] $name

    # 登場区分
    [string] $type

    # 子エントリ
    [SheetFormat[]] $entries = @()

    SheetFormat([string] $name) {
        $this.name = $name
    }

}

# 制御文
class ControlStatement : ControlHolder {

    # 制御文コマンド
    [string] $command

    # 行
    [long] $row
    # 行数
    [long] $length = 1

    # ヘッダー行数
    [long] $headerLength = 0
    # フッター行数
    [long] $footerLength = 0

    ControlStatement([string[]] $params, $cell) {
        $this.command = $params[1]
        $this.row = $cell.Row

        # 拡張パラメーター
        for ($i = 2; $i -lt $params.Length; ++$i) {
            if ($params[$i] -match '^(\w+):(\d+)$') {
                switch ($Matches[1]) {
                    'header' { $this.headerLength = [long]$Matches[2] }
                    'footer' { $this.footerLength = [long]$Matches[2] }
                }
            }
        }
    }

    [void] Close([string[]] $params, $cell) {
        # 基底処理呼び出し
        ([ControlHolder]$this).Close($params, $cell)

        # 行数算出
        $this.length = $cell.Row - $this.row + 1
        if ($this.headerLength + $this.footerLength -gt $this.length) {
            throw (New-Object -TypeName 'System.InvalidOperationException' `
                -ArgumentList ('ヘッダー／フッター行数が定義済みのブロック行数を超えています。'))
        }
    }

    [void] Output([DocumentWriter] $docWriter, $target) {
        $this._BeginTransaction($docWriter, $target)
        $this._CommitTransaction($docWriter)
    }

    #region ステートメントユーティリティー

    # トランザクションの開始
    hidden [void] _BeginTransaction([DocumentWriter] $docWriter, $target) {
        $docWriter.BeginTransaction($this.row, $this.length, $target)
    }

    # トランザクションのコミット
    hidden [void] _CommitTransaction([DocumentWriter] $docWriter) {
        $docWriter.CommitTransaction()
    }

    hidden [void] _AppendHeader([DocumentWriter] $docWriter, $target) {
        if ($this.headerLength -gt 0) {
            $docWriter.Append($this.row + 1, $this.headerLength, $target)
        }
    }

    # 挿入
    hidden [void] _AppendBody([DocumentWriter] $docWriter, $target) {
        if ($this.IsNested()) {
            # 開始と終了を除く
            $appendLength = $this.length - $this.headerLength - $this.footerLength - 2
            if ($appendLength -gt 0) {
                $docWriter.Append($this.row + $this.headerLength + 1, $appendLength, $target)
            }
        }
        else {
            # 出力するものがない
            throw (New-Object -TypeName 'System.InvalidOperationException' -ArgumentList ('ネストしない制御文に対する出力は無効です。'))
        }
    }

    hidden [void] _AppendFooter([DocumentWriter] $docWriter, $target) {
        if ($this.footerLength -gt 0) {
            $docWriter.Append(
                $this.row + $this.length - $this.footerLength - 1,
                $this.footerLength,
                $target)
        }
    }

    #endregion

}

# シート出力指定制御文
class SheetControl : ControlStatement {

    # 登場区分
    [string] $type

    SheetControl([string[]] $params, $cell) : base($params, $cell) {
        $this.type = $params[2]
    }

}

# コード制御文
class CodesControl : ControlStatement {

    # ロジック説明制御文
    [DescriptionControl] $descCtrl
    # 代入式制御文
    [AssignmentControl] $assignCtrl
    # 条件制御文
    [ConditionControl] $condCtrl

    CodesControl([string[]] $params, $cell) : base($params, $cell) {
        $this.Open('codes')
    }

    [void] Close([string[]] $params, $cell) {
        # 基底処理呼び出し
        ([ControlStatement]$this).Close($params, $cell)

        # 定義の割り当て
        foreach ($control in $this.controls) {
            switch ($control) {
                { $_ -is [DescriptionControl] } {
                    $this.descCtrl = $control
                }
                { $_ -is [AssignmentControl] } {
                    $this.assignCtrl = $control
                }
                { $_ -is [ConditionControl] } {
                    $this.condCtrl = $control
                }
            }
        }

        # 定義チェック
        if ($null -eq $this.descCtrl) {
            throw (New-Object -TypeName 'System.InvalidOperationException' `
                -ArgumentList ('コード制御文(codes)中に記述制御文(description)が未定義です。'))
        }
        if ($null -eq $this.assignCtrl) {
            throw (New-Object -TypeName 'System.InvalidOperationException' `
                -ArgumentList ('コード制御文(codes)中に代入式制御文(assignment)が未定義です。'))
        }
        if ($null -eq $this.condCtrl) {
            throw (New-Object -TypeName 'System.InvalidOperationException' `
                -ArgumentList ('コード制御文(codes)中に条件制御文(condition)が未定義です。'))
        }
    }

    [void] Output([DocumentWriter] $docWriter, $target) {
        $this._BeginTransaction($docWriter, $target)

        $nodes = $target.node.Evaluate('code/node()')

        # 代入式を纏める
        [List[AssignmentTargetInfo]] $listAssignment =
            (New-Object -TypeName 'System.Collections.Generic.LinkedList[AssignmentTargetInfo]')

        foreach ($node in $nodes) {
            # 登場する要素によって出力を分ける
            switch ($node.Name) {
                'description' {
                    $this._OutputAssign($docWriter, $listAssignment)

                    # 処理記述
                    $this.descCtrl.Output($docWriter, ([DescriptionTargetInfo]::new($node, $docWriter)))
                }
                'assignment' {
                    # 代入式
                    $listAssignment.Add([AssignmentTargetInfo]::new($node))
                }
                'condition' {
                    $this._OutputAssign($docWriter, $listAssignment)

                    $paraNumber = $docWriter.GetCurrentParagraphNumber()
                    # 条件表
                    $cases = [ConditionTargetEnumerator]::new($node, $paraNumber)
                    $this.condCtrl.Output($docWriter, $cases)
                    # 記述部
                    $docWriter.PushParagraph()
                    # TargetEnumeratorはリセットできないので、作り直し
                    $cases = [ConditionTargetEnumerator]::new($node, $paraNumber)
                    foreach ($case in $cases) {
                        # 再帰呼び出し
                        $this.Output($docWriter, $case)
                    }
                    $docWriter.PopParagraph()
                }
            }
        }

        # 未出力分を処理
        $this._OutputAssign($docWriter, $listAssignment)

        $this._CommitTransaction($docWriter)
    }

    hidden [void] _OutputAssign([DocumentWriter] $docWriter, [List[AssignmentTargetInfo]] $listAssignment) {

        if ($listAssignment.Count -gt 0) {
            # 条件表の出力
            $this.assignCtrl.Output($docWriter, $listAssignment)
        }
        $listAssignment.Clear()
    }

}

# 説明制御文
class DescriptionControl : ControlStatement {

    DescriptionControl([string[]] $params, $cell) : base($params, $cell) {
        $this.Open('description')
    }

    [void] Output([DocumentWriter] $docWriter, $target) {
        $this._BeginTransaction($docWriter, $target)

        # 単純出力
        $this._AppendHeader($docWriter, $target)
        $this._AppendBody($docWriter, $target)
        $this._AppendFooter($docWriter, $target)

        $this._CommitTransaction($docWriter)
    }

}

# 代入式制御文
class AssignmentControl : ControlStatement {

    AssignmentControl([string[]] $params, $cell) : base($params, $cell) {
        $this.Open('assignment')
    }

    [void] Output([DocumentWriter] $docWriter, $target) {
        $this._BeginTransaction($docWriter, $target)

        $this._AppendHeader($docWriter, $target)

        [int] $index = 0

        # 条件を繰り返す
        foreach ($i in $target) {
            $i.index = ++$index
            $this._AppendBody($docWriter, $i)
        }

        $this._AppendFooter($docWriter, $target)

        $this._CommitTransaction($docWriter)
    }

}

# 条件制御文
class ConditionControl : ControlStatement {

    ConditionControl([string[]] $params, $cell) : base($params, $cell) {
        $this.Open('condition')
    }

    [void] Output([DocumentWriter] $docWriter, $target) {
        $this._BeginTransaction($docWriter, $target)

        $this._AppendHeader($docWriter, $target)

        # 条件を繰り返す
        foreach ($i in $target) {
            $this._AppendBody($docWriter, $i)
        }

        $this._AppendFooter($docWriter, $target)

        $this._CommitTransaction($docWriter)
    }

}

# 繰り返し制御文
class IterationControl : ControlStatement {

    # 対象
    [string] $target

    IterationControl([string[]] $params, $cell) : base($params, $cell) {
        $this.target = $params[2]
        # Openする
        $this.Open($this.target)
    }

    [void] Output([DocumentWriter] $docWriter, $target) {
        $this._BeginTransaction($docWriter, $target)

        $this._AppendHeader($docWriter, $target)

        # 列挙対象の取得
        $collections = (Invoke-Expression ('$target.' + "$($this.target)"))
        foreach ($i in $collections) {
            # 繰り返し出力する
            $this._AppendBody($docWriter, $i)
        }

        $this._AppendFooter($docWriter, $target)

        $this._CommitTransaction($docWriter)
    }

}
