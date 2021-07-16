<#
.SYNOPSIS
    Excel コードドキュメントジェネレーター
.DESCRIPTION
    コードパーサー(Sharon)で解析したXMLを指定されたExcelテンプレートに展開し、
    コードドキュメントを生成します。
.NOTES
    Code released under the MIT Lincense.

    Copyright 2021 webmaster@love2hina.net

    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
    to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
    and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
    WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#>
using module "./TargetEnumerator.psm1"
using module "./ControlStatement.psm1"

[CmdletBinding()]
param()

$global:config = @{}
# 対象となる行数
$global:config.searchLines = 128
# 対象となる列数
$global:config.searchColumns = 64

$global:const = @{}
# xlWorksheet定数
$global:const.xlWorksheet = -4167

class GananApplication {
    # テンプレートフォーマット情報
    $format = @{ entries = @() }
    # Excel Application
    $excel = (New-Object -ComObject "Excel.Application")
    # テンプレートワークブック
    $bookTemplate
    # 生成ドキュメントワークブック
    $bookDocument
    #
    [System.Xml.XmlDocument] $xml
    [System.Xml.XPath.XPathNavigator] $xpath

    [void] test() {
        $this.excel.Visible = $true

        $this.xml = [System.Xml.XmlDocument](Get-Content 'D:\ServerFolders\Repository\Sharon\test.xml')
        $this.xpath = $this.xml.CreateNavigator()

        $this.bookTemplate = $this.excel.Workbooks.Open('D:\ServerFolders\Repository\Sharon\template\test.xlsm', 0, $true)
        $this.parseTemplate()

        $this.bookDocument = $this.excel.Workbooks.Add($global:const.xlWorksheet)
        $this.makeDocument()

        $this.bookTemplate.Close($false)
        #$this.excel.Quit()
        $this.excel = $null
    }

    [void] parseTemplate() {
        Write-Debug "[parseTemplate] begin template parsing..."

        [SheetFormat] $parSheetFmt = $null # 親シート

        foreach ($sheet in $this.bookTemplate.WorkSheets) {
            if ($sheet.Type -eq $global:const.xlWorksheet) {
                # 通常のシートの場合
                $curSheetFmt = [SheetFormat]::new($sheet.Name)
                Write-Debug "[parseTemplate] parsing sheet: $($curSheetFmt.name)"

                # 制御文スタック
                $stackControl = (New-Object -TypeName 'System.Collections.Generic.Stack[ControlHolder]')
                $stackControl.Push($curSheetFmt)

                # A列のセルを走査
                foreach ($cell in $sheet.Range("A1:A$($global:config.searchLines)").Cells) {
                    if ($cell.Text -match '\{#(\w+)(?:\s+(\S+))*\}') {
                        Write-Verbose "[parseTemplate] found control statement: $($Matches[0])"

                        # 制御文
                        $control = $null
                        switch ($Matches[1]) {
                            'sheet' {
                                $control = [SheetControl]::new($Matches, $cell)
                                # このシートの種別を記録
                                $curSheetFmt.type = $control.type
                            }
                            'begin' {
                                $control = [IterationControl]::new($Matches, $cell)
                            }
                            'end' {
                                $last = $stackControl.Pop()
                                $last.Close($Matches, $cell)
                            }
                        }
                        if ($null -ne $control) {
                            ($stackControl.Peek()).controls += $control
                            if ($control.IsNested()) {
                                $stackControl.Push($control)
                            }

                            Write-Verbose "[parseTemplate] add control statement: $($control.command), Line: $($control.row)"
                        }
                    }
                }

                # 階層構造の作成
                switch -regex ($curSheetFmt.type) {
                    'once|file' {
                        # テンプレートルートに追加
                        $this.format.entries += $curSheetFmt
                        # 親シートはなし
                        $parSheetFmt = $null
                    }
                    'type|class' {
                        # テンプレートルートに追加
                        $this.format.entries += $curSheetFmt
                        # 親シートを設定
                        $parSheetFmt = $curSheetFmt
                    }
                    'field|method' {
                        # 親シートがあるかどうか
                        if ($null -eq $parSheetFmt) {
                            # テンプレートルートに追加
                            $this.format.entries += $curSheetFmt
                        }
                        else {
                            # 親シートに追加
                            $parSheetFmt.entries += $curSheetFmt
                        }
                    }
                }

                # スタックチェック
                if ($stackControl.Pop() -ne $curSheetFmt) {
                    throw (New-Object -TypeName 'System.InvalidOperationException' -ArgumentList ('制御文が正しく閉じられていません。'))
                }
            }
        }

        Write-Debug "[parseTemplate] end template parsing."
    }

    [void] makeDocument() {

        $this.enumEntries($this.format.entries)
    }

    [void] enumEntries($entries) {

        foreach ($entry in $entries) {
            # データ投影
            $targetCursor = $null

            switch ($entry.type) {
                'once' {
                    # なし
                    $targetCursor = [TargetEnumerator]::new()
                }
                'file' {
                    # TODO: ファイル情報のみ
                    $targetCursor = [TargetEnumerator]::new()
                }
                'class' {
                    # クラス情報
                    $targetCursor = [ClassTargetEnumerator]::new($this.xpath.Select('//class'))
                }
            }

            foreach ($target in $targetCursor) {
                $sheetTemplate = $this.bookTemplate.WorkSheets.Item($entry.name)
                [void]$sheetTemplate.Copy([System.Reflection.Missing]::Value, $this.bookDocument.WorkSheets.Item($this.bookDocument.WorkSheets.Count))
                $sheetDocument = $this.bookDocument.WorkSheets.Item($this.bookDocument.WorkSheets.Count)
                $lineTemplate = 0 # テンプレート側で出力した行位置
                $lineDocument = 0 # 生成ドキュメント側で出力した行位置

                foreach ($control in $entry.controls) {
                    # 制御文までを出力
                    $this.translateLines(([ref]$lineTemplate), ($control.row - 1), $sheetDocument, ([ref]$lineDocument), $target)

                    # 生成ドキュメント側制御文削除
                    [void]$sheetDocument.Rows("$($lineDocument + 1)").Delete()

                    # テンプレート側行位置制御
                    $lineTemplate = $control.row
                }

                # 残りを出力
                $this.translateLines(([ref]$lineTemplate), $global:config.searchLines, $sheetDocument, ([ref]$lineDocument), $target)
            }
        }
    }

    [void] translateLines([ref]$templateCursor, $templateEnd, $sheetDocument, [ref]$documentCursor, $target) {

        $lines = $templateEnd - $templateCursor.Value

        if ($lines -ge 1) {
            $rangeLine = $sheetDocument.Range(
                $sheetDocument.Cells($documentCursor.Value + 1, 1),
                $sheetDocument.Cells($documentCursor.Value + $lines, 1))

            $regex = [System.Text.RegularExpressions.Regex]'\{\$(\w+)\}'
            [System.Text.RegularExpressions.MatchEvaluator]$replacer = {
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
                    $cell = $cell.End(-4161)
                } while ($cell.Column -le $global:config.searchColumns)
            }

            # 出力行数
            $templateCursor.Value += $lines
            $documentCursor.Value += $lines
        }
    }

}

$app = [GananApplication]::new()
$app.test()
