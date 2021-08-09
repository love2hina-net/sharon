
# トランザクション情報
class TransactionRange {
    # 行
    [long] $row
    # 行数
    [long] $length
}

# コードドキュメント出力
class DocumentWriter {

    # 出力先ワークシート
    [object] $sheetDocument

    # テンプレート側で出力した行位置
    [long] $lineTemplate = 0

    # 生成ドキュメント側で出力した行位置
    [long] $lineDocument = 0

    # トランザクションスタック
    [System.Collections.Generic.Stack[TransactionRange]] $stackTransaction =
        (New-Object -TypeName 'System.Collections.Generic.Stack[TransactionRange]')

    # 変数値Regex
    [System.Text.RegularExpressions.Regex] $regexVarExp = [System.Text.RegularExpressions.Regex]'\{\$(\w+)\}'

    # 変数値置換子
    [System.Text.RegularExpressions.MatchEvaluator] $evalVarReplacer = {
        param([System.Text.RegularExpressions.Match] $match)
        # 置き換え
        return (Invoke-Expression ('$target.' + "$($match.Groups[1])"))
    }

    DocumentWriter($bookTemplate, $bookDocument, [string] $templateName) {
        # コピー元のテンプレートを取得
        $sheetTemplate = $bookTemplate.WorkSheets.Item($templateName)
        # コピー
        [void]$sheetTemplate.Copy([System.Reflection.Missing]::Value, $bookDocument.WorkSheets.Item($bookDocument.WorkSheets.Count))
        # コピーしたシートの参照を取得
        $this.sheetDocument = $bookDocument.WorkSheets.Item($bookDocument.WorkSheets.Count)
    }

    # トランザクションの開始
    [void] beginTransaction([long] $lineStart, [long] $lineLength) {
        [TransactionRange] $range = $null

        # 開始チェック
        if ($lineLength -lt 1) {
            throw (New-Object -TypeName 'System.ArgumentException' -ArgumentList ("行数が不正です。指定行数:$lineLength"))
        }
        if ($lineStart -lt $this.lineTemplate) {
            throw (New-Object -TypeName 'System.ArgumentException' `
                -ArgumentList ("既に出力が確定している行に対して、出力を開始できません。指定開始行:$lineStart, 確定行:$($this.lineTemplate)"))
        }
        if ($this.stackTransaction.TryPeek([ref]$range)) {
            if ($lineStart -lt $range.row) {
                throw (New-Object -TypeName 'System.ArgumentException' `
                    -ArgumentList ("既に開始されているトランザクション範囲外の行に対して、出力を開始できません。指定開始行:$lineStart, 開始行:$($range.row)"))
            }
            if (($lineStart + $lineLength) -gt ($range.row + $range.length)) {
                throw (New-Object -TypeName 'System.ArgumentException' `
                    -ArgumentList ('既に開始されているトランザクション範囲外の行に対して、出力を開始できません。' `
                        + "指定終了行:$($lineStart + $lineLength - 1), 終了行:$($range.row + $range.length - 1)"))
            }
        }

        $range = [TransactionRange]::new()
        $range.row = $lineStart
        $range.length = $lineLength
        $this.stackTransaction.Push($range)
    }

    # コミット
    [void] commitTransaction() {
        [TransactionRange] $range = $this.stackTransaction.Pop()

        # 空となった場合
        if ($this.stackTransaction.Count -eq 0) {
            # 確定させる
            $this.outputSkip($range.row + $range.length - 1)
        }
    }

    # トランザクション中のテンプレート行位置を取得
    # .PARAM lineStart
    #   テンプレート側行位置
    # .RETURN
    #   $sheetDocument上での該当行位置
    [long] getTemplateRowOnDocument([long] $lineStart) {
        [long] $offset = 0

        $offset = $lineStart - $this.lineTemplate - 1
        if ($offset -lt 0) {
            throw (New-Object -TypeName 'System.ArgumentException' `
                -ArgumentList ("既に出力が確定している行には操作できません。指定開始行:$lineStart, 確定行:$($this.lineTemplate)"))
        }

        return $this.lineDocument + $offset + 1
    }

    # トランザクション中のテンプレート行位置を取得
    [long] getTemplateRowOnDocument() {
        [TransactionRange] $range = $null

        return if ($this.stackTransaction.TryPeek([ref]$range)) {
            $this.getTemplateRowOnDocument($range.row)
        }
        else {
            $this.getTemplateRowOnDocument($this.lineDocument + 1)
        }
    }

    # 挿入
    # .PARAM lineStart
    #   テンプレート側行位置
    # .PARAM lineLength
    #   行数
    # .PARAM target
    #   出力置換対象
    [void] append([long] $lineStart, [long] $lineLength, $target) {

        if ($lineLength -le 0) {
            throw (New-Object -TypeName 'System.ArgumentException' `
                -ArgumentList ("出力指定行数が1未満です。指定行数:$lineLength"))
        }

        [long] $lineDocumentStart = $this.getTemplateRowOnDocument($lineStart)

        # 挿入
        [void] $this.sheetDocument.Rows("$($lineDocumentStart):$($lineDocumentStart + $lineLength - 1)").Copy()
        [void] $this.sheetDocument.Rows("$($this.lineDocument + 1)").Insert($global:const.xlShiftDown)

        # 出力置換処理
        $this._translateLines($lineLength, $target)
    }

    # 出力置換処理
    hidden [void] _translateLines([long] $lineLength, $target) {

        if ($lineLength -le 0) {
            throw (New-Object -TypeName 'System.ArgumentException' `
                -ArgumentList ("出力指定行数が1未満です。指定行数:$lineLength"))
        }

        $rangeLine = $this.sheetDocument.Range(
            $this.sheetDocument.Cells($this.lineDocument + 1, 1),
            $this.sheetDocument.Cells($this.lineDocument + $lineLength, 1))

        # 置き換え処理
        # 行のループ
        foreach ($cell in $rangeLine) {
            # 列のループ
            do {
                $text = $cell.Text
                $replaced = $this.regexVarExp.Replace($text, $this.evalVarReplacer)
                if ($text -ne $replaced) {
                    $cell.Value = $replaced
                }

                # Ctrl + → と同等の処理で列挙高速化
                $cell = $cell.End($global:const.xlToRight)
            } while ($cell.Column -le $global:config.searchColumns)
        }

        # 出力行数分、加算
        $this.lineDocument += $lineLength
    }

    # テンプレートのスルー出力
    [void] outputPassThrough([long] $lineOutput, $target) {
        $lines = $lineOutput - $this.lineTemplate

        if ($lines -ge 1) {
            $this._translateLines($lines, $target)

            # テンプレート出力位置
            $this.lineTemplate += $lines
        }
    }

    # テンプレートのスキップ出力
    [void] outputSkip([long] $lineOutput) {
        $lines = $lineOutput - $this.lineTemplate

        if ($lines -ge 1) {
            $rangeLine = $this.sheetDocument.Rows("$($this.lineDocument + 1):$($this.lineDocument + $lines)")

            # 出力ドキュメント側の削除
            [void]$rangeLine.Delete()

            # テンプレート側の行位置加算
            $this.lineTemplate += $lines
        }
    }

}
