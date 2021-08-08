
# 制御文構造保持
class ControlHolder {

    # 内包する制御文
    [ControlHolder[]] $controls = @()

    # ネスト制御文種別
    [string] $token = ''

    [void] Open([string]$token) {
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

            $this.token = ''
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

    [void] Output([ref]$templateCursor, $sheetDocument, [ref]$documentCursor, $targe) {
        # 処理なし
    }

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

    [void] Output([ref]$templateCursor, $sheetDocument, [ref]$documentCursor, $target) {

        $nodes = $target.node.Evaluate('block/node()')

        foreach ($node in $nodes) {
            # TODO: 登場する要素によって出力を分ける
            switch ($node.Name) {
                'comment' {}
                'condition' {}
            }
        }
    }

}

# 説明制御文
class DescriptionControl : ControlStatement {

    DescriptionControl($match, $cell) : base($match, $cell) {
        $this.Open('description')
    }

    [void] Output([ref]$templateCursor, $sheetDocument, [ref]$documentCursor, $target) {
        # TODO
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

    [void] Output([ref]$templateCursor, $sheetDocument, [ref]$documentCursor, $target) {
        # 列挙対象の取得
        $collections = (Invoke-Expression ('$target.' + "$($this.target)"))

        foreach ($i in $collections) {
            # 繰り返しテンプレートの挿入
            [void] $sheetDocument.Rows("$($documentCursor.Value + 2):$($documentCursor.Value + $this.length - 1)").Copy()
            [void] $sheetDocument.Rows("$($documentCursor.Value + 1)").Insert($global:const.xlShiftDown)

            # 置換処理
            $this.translateLines($sheetDocument, $documentCursor, $i)
        }
    }

    [void] translateLines($sheetDocument, [ref]$documentCursor, $target) {

        if ($this.length -ge 3) {
            $rangeLine = $sheetDocument.Range(
                $sheetDocument.Cells($documentCursor.Value + 1, 1),
                $sheetDocument.Cells($documentCursor.Value + $this.length - 2, 1))

            $regex = [System.Text.RegularExpressions.Regex]'\{\$(\w+)\}'
            [System.Text.RegularExpressions.MatchEvaluator] $replacer = {
                param([System.Text.RegularExpressions.Match]$match)
                # 置き換え
                return (Invoke-Expression ('$target.' + "$($match.Groups[1])"))
            }

            # 置き換え処理
            # 行のループ
            foreach ($cell in $rangeLine) {
                # 列のループ
                do {
                    $text = $cell.Text
                    $replaced = $regex.Replace($text, $replacer)
                    if ($text -ne $replaced) {
                        $cell.Value = $replaced
                    }

                    # Ctrl + → と同等の処理で列挙高速化
                    $cell = $cell.End($global:const.xlToRight)
                } while ($cell.Column -le $global:config.searchColumns)
            }

            # 出力行数
            $documentCursor.Value += $this.length - 2
        }
    }

}
