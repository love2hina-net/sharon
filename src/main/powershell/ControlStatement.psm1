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

    [void] Close($match, $cell) {
        if ($this.IsNested()) {
            # クローズトークンチェック
            if ($match[2] -ne $this.token) {
                throw (New-Object -TypeName 'System.InvalidOperationException' `
                    -ArgumentList ("制御文の組み合わせが正しくありません。指定: $($match[2]), 想定: $($this.token)"))
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

    ControlStatement($match, $cell) {
        $this.command = $match[1]
        $this.row = $cell.Row
    }

    [void] Close($match, $cell) {
        # 基底処理呼び出し
        ([ControlHolder]$this).Close($match, $cell)

        # 行数算出
        $this.length = $cell.Row - $this.row + 1
    }

    [void] Output([DocumentWriter] $docWriter, $target) {
        $this.beginTransaction($docWriter)
        $this.commitTransaction($docWriter)
    }

    #region ステートメントユーティリティー

    # トランザクションの開始
    hidden [void] beginTransaction([DocumentWriter] $docWriter) {
        $docWriter.beginTransaction($this.row, $this.length)
    }

    # トランザクションのコミット
    hidden [void] commitTransaction([DocumentWriter] $docWriter) {
        $docWriter.commitTransaction()
    }

    # 挿入
    hidden [void] append([DocumentWriter] $docWriter, $target) {
        if ($this.IsNested()) {
            # 開始と終了を除く
            $docWriter.append($this.row + 1, $this.length - 2, $target)
        }
        else {
            # 出力するものがない
            throw (New-Object -TypeName 'System.InvalidOperationException' -ArgumentList ('ネストしない制御文に対する出力は無効です。'))
        }
    }

    #endregion

}

# シート出力指定制御文
class SheetControl : ControlStatement {

    # 登場区分
    [string] $type

    SheetControl($match, $cell) : base($match, $cell) {
        $this.type = $match[2]
    }

}

# コード制御文
class CodesControl : ControlStatement {

    # ロジック説明制御文
    [DescriptionControl] $descCtrl

    CodesControl($match, $cell) : base($match, $cell) {
        $this.Open('codes')
    }

    [void] Close($match, $cell) {
        # 基底処理呼び出し
        ([ControlStatement]$this).Close($match, $cell)

        # 定義の割り当て
        foreach ($control in $this.controls) {
            switch ($control) {
                { $_ -is [DescriptionControl] } {
                    $this.descCtrl = $control
                }
            }
        }
    }

    # TODO
    [long] Output([long] $lineTemplate, $sheetDocument, [ref] $documentCursor, $target) {

        $nodes = $target.node.Evaluate('block/node()')

        foreach ($node in $nodes) {
            # TODO: 登場する要素によって出力を分ける
            switch ($node.Name) {
                'comment' {}
                'condition' {}
            }
        }

        return ([ControlStatement]$this).Output($lineTemplate, $sheetDocument, $documentCursor, $target)
    }

}

# 説明制御文
class DescriptionControl : ControlStatement {

    DescriptionControl($match, $cell) : base($match, $cell) {
        $this.Open('description')
    }

    [long] Output([long] $lineTemplate, $sheetDocument, [ref] $documentCursor, $target) {
        # TODO
        return 0
    }

}

# 繰り返し制御文
class IterationControl : ControlStatement {

    # 対象
    [string] $target

    IterationControl($match, $cell) : base($match, $cell) {
        $this.target = $match[2]
        # Openする
        $this.Open($this.target)
    }

    [void] Output([DocumentWriter] $docWriter, $target) {
        $this.beginTransaction($docWriter)

        # 列挙対象の取得
        $collections = (Invoke-Expression ('$target.' + "$($this.target)"))

        foreach ($i in $collections) {
            # 繰り返し出力する
            $this.append($docWriter, $i)
        }

        $this.commitTransaction($docWriter)
    }

}
