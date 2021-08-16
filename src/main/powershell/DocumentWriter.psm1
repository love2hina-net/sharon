using namespace System.Collections.Generic
using namespace System.Text.RegularExpressions

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
    [Stack[TransactionRange]] $stackTransaction =
        (New-Object -TypeName 'System.Collections.Generic.Stack[TransactionRange]')

    # 段落番号
    [Stack[Int32]] $stackParagraph =
        (New-Object -TypeName 'System.Collections.Generic.Stack[Int32]')

    # 段落構成
    [Dictionary[string, string]] $dictionaryParagraph =
        (New-Object -TypeName 'System.Collections.Generic.Dictionary[string, string]')

    # 変数値Regex
    [Regex] $regexVarExp = [Regex]'\{\$(\w+)\}'

    # 変数値置換子
    [MatchEvaluator] $evalVarReplacer = {
        param([Match] $match)
        # 置き換え
        return (Invoke-Expression ('$target.' + "$($match.Groups[1].Value)"))
    }

    # 遅延置換Regex
    [Regex] $regexLateRep = [Regex]'\{\*(\w+)(?:\s+(\S+))*\}'

    # 遅延置換子
    [MatchEvaluator] $evalLateReplacer = {
        param([Match] $match)
        [string] $result = ''
        switch ($match.Groups[1].Value) {
            'paragraph' {
                # 段落タイトル
                $result = $this.dictionaryParagraph[$match.Groups[2].Value]
            }
        }
        return $result
    }

    # 遅延置換リスト
    [List[object]] $listLateReplace =
        (New-Object -TypeName 'System.Collections.Generic.LinkedList[object]')

    DocumentWriter($bookTemplate, $bookDocument, [string] $templateName) {
        # コピー元のテンプレートを取得
        $sheetTemplate = $bookTemplate.WorkSheets.Item($templateName)
        # コピー
        [void]$sheetTemplate.Copy([System.Reflection.Missing]::Value, $bookDocument.WorkSheets.Item($bookDocument.WorkSheets.Count))
        # コピーしたシートの参照を取得
        $this.sheetDocument = $bookDocument.WorkSheets.Item($bookDocument.WorkSheets.Count)

        # 項番に初期値を設定
        $this.pushParagraph()
    }

    #region テンプレート出力

    # トランザクションの開始
    [void] beginTransaction([long] $lineStart, [long] $lineLength, $target) {
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
        else {
            # 初回時は直前行までを出力確定する
            $this._outputPassThrough($lineStart - 1, $target)
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
            $this._outputSkip($range.row + $range.length - 1)
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

                    # 遅延置換が必要か
                    if ($this.regexLateRep.IsMatch($replaced)) {
                        $this.listLateReplace.Add($cell)
                    }
                }

                # 次のセル
                $next = $cell.Item(1, 2)
                if ($next.Text -ne '') {
                    # 次のセルに値がある(連続)
                    $cell = $next
                }
                else {
                    # Ctrl + → と同等の処理で列挙高速化
                    $cell = $cell.End($global:const.xlToRight)
                }
            } while ($cell.Column -le $global:config.searchColumns)
        }

        # 出力行数分、加算
        $this.lineDocument += $lineLength
    }

    # テンプレートのスルー出力
    hidden [void] _outputPassThrough([long] $lineOutput, $target) {
        $lines = $lineOutput - $this.lineTemplate

        if ($lines -ge 1) {
            $this._translateLines($lines, $target)

            # テンプレート出力位置
            $this.lineTemplate += $lines
        }
    }

    # テンプレートのスキップ出力
    hidden [void] _outputSkip([long] $lineOutput) {
        $lines = $lineOutput - $this.lineTemplate

        if ($lines -ge 1) {
            $rangeLine = $this.sheetDocument.Rows("$($this.lineDocument + 1):$($this.lineDocument + $lines)")

            # 出力ドキュメント側の削除
            [void]$rangeLine.Delete()

            # テンプレート側の行位置加算
            $this.lineTemplate += $lines
        }
    }

    # ファイナライズ
    [void] finalize($target) {

        # 残りを出力
        $this._outputPassThrough($global:config.searchLines, $target)

        # 遅延置換処理
        foreach ($cell in $this.listLateReplace) {
            $text = $cell.Text
            $replaced = $this.regexLateRep.Replace($text, $this.evalLateReplacer)
            if ($text -ne $replaced) {
                $cell.Value = $replaced
            }
        }

        $this.listLateReplace.Clear()
    }

    #endregion

    #region 段落番号

    [string] getCurrentParagraphNumber() {
        [System.Text.StringBuilder] $strNumber = [System.Text.StringBuilder]''

        foreach ($i in $this.stackParagraph) {
            if ($strNumber.Length -gt 0) {
                $strNumber.Insert(0, '.')
            }
            $strNumber.Insert(0, $i)
        }

        return $strNumber.ToString()
    }

    [string] incrementParagraph([string] $title) {
        [int] $number = $this.stackParagraph.Pop()
        return $this._pushParagraph((++$number), $title)
    }

    [void] pushParagraph() {
        $this.stackParagraph.Push(0)
    }

    hidden [string] _pushParagraph([int] $number, [string] $title) {
        $this.stackParagraph.Push($number)
        [string] $result = $this.getCurrentParagraphNumber()
        # 項目の登録
        $this.dictionaryParagraph.Add($result, $title)
        return $result
    }

    [void] popParagraph() {
        $this.stackParagraph.Pop()
    }

    #endregion

}
