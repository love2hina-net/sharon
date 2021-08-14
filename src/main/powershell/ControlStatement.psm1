using module '.\TargetInfo.psm1'
using module '.\TargetEnumerator.psm1'
using module '.\DocumentWriter.psm1'

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
        $this._beginTransaction($docWriter)
        $this._commitTransaction($docWriter)
    }

    #region ステートメントユーティリティー

    # トランザクションの開始
    hidden [void] _beginTransaction([DocumentWriter] $docWriter) {
        $docWriter.beginTransaction($this.row, $this.length)
    }

    # トランザクションのコミット
    hidden [void] _commitTransaction([DocumentWriter] $docWriter) {
        $docWriter.commitTransaction()
    }

    hidden [void] _appendHeader([DocumentWriter] $docWriter, $target) {
        if ($this.headerLength -gt 0) {
            $docWriter.append($this.row + 1, $this.headerLength, $target)
        }
    }

    # 挿入
    hidden [void] _appendBody([DocumentWriter] $docWriter, $target) {
        if ($this.IsNested()) {
            # 開始と終了を除く
            $appendLength = $this.length - $this.headerLength - $this.footerLength - 2
            if ($appendLength -gt 0) {
                $docWriter.append($this.row + $this.headerLength + 1, $appendLength, $target)
            }
        }
        else {
            # 出力するものがない
            throw (New-Object -TypeName 'System.InvalidOperationException' -ArgumentList ('ネストしない制御文に対する出力は無効です。'))
        }
    }

    hidden [void] _appendFooter([DocumentWriter] $docWriter, $target) {
        if ($this.footerLength -gt 0) {
            $docWriter.append(
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
        if ($null -eq $this.condCtrl) {
            throw (New-Object -TypeName 'System.InvalidOperationException' `
                -ArgumentList ('コード制御文(codes)中に条件制御文(condition)が未定義です。'))
        }
    }

    [void] Output([DocumentWriter] $docWriter, $target) {
        $this._beginTransaction($docWriter)

        $nodes = $target.node.Evaluate('code/node()')

        foreach ($node in $nodes) {
            # 登場する要素によって出力を分ける
            switch ($node.Name) {
                'comment' {
                    # 処理記述
                    $this.descCtrl.Output($docWriter, ([DescriptionTargetInfo]::new($node, $docWriter)))
                }
                'condition' {
                    $paraNumber = $docWriter.getCurrentParagraphNumber()
                    # 条件表
                    $cases = [ConditionTargetEnumerator]::new($node, $paraNumber)
                    $this.condCtrl.Output($docWriter, $cases)
                    # 記述部
                    $docWriter.pushParagraph()
                    # TargetEnumeratorはリセットできないので、作り直し
                    $cases = [ConditionTargetEnumerator]::new($node, $paraNumber)
                    foreach ($case in $cases) {
                        # 再帰呼び出し
                        $this.Output($docWriter, $case)
                    }
                    $docWriter.popParagraph()
                }
            }
        }

        $this._commitTransaction($docWriter)
    }

}

# 説明制御文
class DescriptionControl : ControlStatement {

    DescriptionControl([string[]] $params, $cell) : base($params, $cell) {
        $this.Open('description')
    }

    [void] Output([DocumentWriter] $docWriter, $target) {
        $this._beginTransaction($docWriter)

        # 単純出力
        $this._appendHeader($docWriter, $target)
        $this._appendBody($docWriter, $target)
        $this._appendFooter($docWriter, $target)

        $this._commitTransaction($docWriter)
    }

}

# 条件制御文
class ConditionControl : ControlStatement {

    ConditionControl([string[]] $params, $cell) : base($params, $cell) {
        $this.Open('condition')
    }

    [void] Output([DocumentWriter] $docWriter, $target) {
        $this._beginTransaction($docWriter)

        $this._appendHeader($docWriter, $target)

        # 条件を繰り返す
        foreach ($i in $target) {
            $this._appendBody($docWriter, $i)
        }

        $this._appendFooter($docWriter, $target)

        $this._commitTransaction($docWriter)
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
        $this._beginTransaction($docWriter)

        $this._appendHeader($docWriter, $target)

        # 列挙対象の取得
        $collections = (Invoke-Expression ('$target.' + "$($this.target)"))
        foreach ($i in $collections) {
            # 繰り返し出力する
            $this._appendBody($docWriter, $i)
        }

        $this._appendFooter($docWriter, $target)

        $this._commitTransaction($docWriter)
    }

}
