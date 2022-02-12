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

    # ミューテックス
    [System.Threading.Mutex] $mutex =
        (New-Object -TypeName 'System.Threading.Mutex' -ArgumentList ($false, 'Global\sharon.doc.mutex'))

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
        if ($match.Groups[1].Value -eq '_') {
            return Invoke-Expression '$target'
        }
        else {
            return Invoke-Expression ('$target.' + "$($match.Groups[1].Value)")
        }
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

    DocumentWriter($bookTemplate, $bookDocument, [string] $templateName, $target) {
        # コピー元のテンプレートを取得
        $sheetTemplate = $bookTemplate.WorkSheets.Item($templateName)
        # コピー
        [void]$sheetTemplate.Copy([System.Reflection.Missing]::Value, $bookDocument.WorkSheets.Item($bookDocument.WorkSheets.Count))
        # コピーしたシートの参照を取得
        $this.sheetDocument = $bookDocument.WorkSheets.Item($bookDocument.WorkSheets.Count)

        # シートの名前
        $text = $this.sheetDocument.Name
        $replaced = $this.regexVarExp.Replace($text, $this.evalVarReplacer)
        if ($text -ne $replaced) {
            $this.sheetDocument.Name = $replaced.Trim()
        }

        # 項番に初期値を設定
        $this.PushParagraph()
    }

    #region テンプレート出力

    # トランザクションの開始
    [void] BeginTransaction([long] $lineStart, [long] $lineLength, $target) {
        [TransactionRange] $range = $null

        # 開始チェック
        if ($lineLength -lt 1) {
            throw (New-Object -TypeName 'System.ArgumentException' -ArgumentList ("$($global:messages.E003001) lineLength:$lineLength"))
        }
        if ($lineStart -lt $this.lineTemplate) {
            throw (New-Object -TypeName 'System.ArgumentException' `
                -ArgumentList ("$($global:messages.E003002) specified line:$lineStart, decitioned line:$($this.lineTemplate)"))
        }
        if ($this.stackTransaction.TryPeek([ref]$range)) {
            if ($lineStart -lt $range.row) {
                throw (New-Object -TypeName 'System.ArgumentException' `
                    -ArgumentList ("$($global:messages.E003003) specified begin:$lineStart, transaction begin:$($range.row)"))
            }
            if (($lineStart + $lineLength) -gt ($range.row + $range.length)) {
                throw (New-Object -TypeName 'System.ArgumentException' `
                    -ArgumentList ("$($global:messages.E003003) specified end:$($lineStart + $lineLength - 1), transaction end:$($range.row + $range.length - 1)"))
            }
        }
        else {
            # 初回時は直前行までを出力確定する
            $this._OutputPassThrough($lineStart - 1, $target)
        }

        $range = [TransactionRange]::new()
        $range.row = $lineStart
        $range.length = $lineLength
        $this.stackTransaction.Push($range)
    }

    # コミット
    [void] CommitTransaction() {
        [TransactionRange] $range = $this.stackTransaction.Pop()

        # 空となった場合
        if ($this.stackTransaction.Count -eq 0) {
            # 確定させる
            $this._OutputSkip($range.row + $range.length - 1)
        }
    }

    # トランザクション中のテンプレート行位置を取得
    # .PARAM lineStart
    #   テンプレート側行位置
    # .RETURN
    #   $sheetDocument上での該当行位置
    [long] GetTemplateRowOnDocument([long] $lineStart) {
        [long] $offset = 0

        $offset = $lineStart - $this.lineTemplate - 1
        if ($offset -lt 0) {
            throw (New-Object -TypeName 'System.ArgumentException' `
                -ArgumentList ("$($global:messages.E003004) specified line:$lineStart, decitioned line:$($this.lineTemplate)"))
        }

        return $this.lineDocument + $offset + 1
    }

    # トランザクション中のテンプレート行位置を取得
    [long] GetTemplateRowOnDocument() {
        [TransactionRange] $range = $null

        return if ($this.stackTransaction.TryPeek([ref]$range)) {
            $this.GetTemplateRowOnDocument($range.row)
        }
        else {
            $this.GetTemplateRowOnDocument($this.lineDocument + 1)
        }
    }

    # 挿入
    # .PARAM lineStart
    #   テンプレート側行位置
    # .PARAM lineLength
    #   行数
    # .PARAM shiftStep
    #   シフトステップ
    # .PARAM shiftLimit
    #   シフト上限
    # .PARAM shiftColumn
    #   シフト開始列
    # .PARAM shiftWidth
    #   シフト列幅
    # .PARAM target
    #   出力置換対象
    [void] Append([long] $lineStart, [long] $lineLength,
        [long] $shiftStep, [long] $shiftLimit, [long] $shiftColumn, [long] $shiftWidth,
        $target) {

        if ($lineLength -le 0) {
            throw (New-Object -TypeName 'System.ArgumentException' `
                -ArgumentList ("$($global:messages.E003005) lineLength:$lineLength"))
        }

        [long] $lineDocumentStart = $this.GetTemplateRowOnDocument($lineStart)

        # クリップボードのコンフリクト防止
        $this.mutex.WaitOne()
        try {
            # 挿入
            [void] $this.sheetDocument.Rows("$($lineDocumentStart):$($lineDocumentStart + $lineLength - 1)").Copy()
            [void] $this.sheetDocument.Rows("$($this.lineDocument + 1)").Insert($global:const.xlShiftDown)
        }
        finally {
            $this.mutex.ReleaseMutex()
        }

        # シフト処理
        if ($shiftStep -gt 0) {
            [long] $offset = ($this.stackParagraph.Count - 1) * $shiftStep
            if ($shiftLimit -gt 0) {
                $offset = [System.Math]::Min($offset, $shiftLimit)
            }

            if ($offset -gt 0) {
                [object] $shiftSrcRange = $null
                [object] $shiftDestRange = $null

                if (($shiftColumn -gt 0) -and ($shiftWidth -gt 0)) {
                    # 一部のシフト
                    $shiftSrcRange = $this.sheetDocument.Range(
                        $this.sheetDocument.Cells($this.lineDocument + 1, $shiftColumn),
                        $this.sheetDocument.Cells($this.lineDocument + $lineLength, $shiftColumn + $shiftWidth - 1))
                    $shiftDestRange = $this.sheetDocument.Range(
                        $this.sheetDocument.Cells($this.lineDocument + 1, $shiftColumn + $offset),
                        $this.sheetDocument.Cells($this.lineDocument + $lineLength, $shiftColumn + $shiftWidth + $offset - 1))
                }
                else {
                    # 行全体のシフト
                    $shiftSrcRange = $this.sheetDocument.Range(
                        $this.sheetDocument.Cells($this.lineDocument + 1, 1),
                        $this.sheetDocument.Cells($this.lineDocument + $lineLength, $global:config.searchColumns))
                    $shiftDestRange = $this.sheetDocument.Range(
                        $this.sheetDocument.Cells($this.lineDocument + 1, $offset + 1),
                        $this.sheetDocument.Cells($this.lineDocument + $lineLength, $offset + $global:config.searchColumns))
                }

                $shiftSrcRange.Cut($shiftDestRange)
            }
        }

        # 出力置換処理
        $this._TranslateLines($lineLength, $target)
    }

    # 出力置換処理
    hidden [void] _TranslateLines([long] $lineLength, $target) {

        if ($lineLength -le 0) {
            throw (New-Object -TypeName 'System.ArgumentException' `
                -ArgumentList ("$($global:messages.E003005) lineLength:$lineLength"))
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
                    $cell.Value = $replaced.Trim()

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
    hidden [void] _OutputPassThrough([long] $lineOutput, $target) {
        $lines = $lineOutput - $this.lineTemplate

        if ($lines -ge 1) {
            $this._TranslateLines($lines, $target)

            # テンプレート出力位置
            $this.lineTemplate += $lines
        }
    }

    # テンプレートのスキップ出力
    hidden [void] _OutputSkip([long] $lineOutput) {
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
    [void] Finalize($target) {

        # 残りを出力
        $this._OutputPassThrough($global:config.searchLines, $target)

        # 遅延置換処理
        foreach ($cell in $this.listLateReplace) {
            $text = $cell.Text
            $replaced = $this.regexLateRep.Replace($text, $this.evalLateReplacer)
            if ($text -ne $replaced) {
                $cell.Value = $replaced.Trim()
            }
        }

        $this.listLateReplace.Clear()
    }

    #endregion

    #region 段落番号

    [string] GetCurrentParagraphNumber() {
        [System.Text.StringBuilder] $strNumber = [System.Text.StringBuilder]''

        foreach ($i in $this.stackParagraph) {
            if ($strNumber.Length -gt 0) {
                $strNumber.Insert(0, '.')
            }
            $strNumber.Insert(0, $i)
        }

        return $strNumber.ToString()
    }

    [string] IncrementParagraph([string] $title) {
        [int] $number = $this.stackParagraph.Pop()
        return $this._PushParagraph((++$number), $title)
    }

    [void] PushParagraph() {
        $this.stackParagraph.Push(0)
    }

    hidden [string] _PushParagraph([int] $number, [string] $title) {
        $this.stackParagraph.Push($number)
        [string] $result = $this.GetCurrentParagraphNumber()
        # 項目の登録
        $this.dictionaryParagraph.Add($result, $title)
        return $result
    }

    [void] PopParagraph() {
        $this.stackParagraph.Pop()
    }

    #endregion

}
