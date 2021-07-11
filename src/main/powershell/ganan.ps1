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
[CmdletBinding()]
param()

$config = @{}
# 対象となる行数
$config.searchLines = 1028

$const = @{}
# xlWorksheet定数
$const.xlWorksheet = -4167

$format = @{ entries = @() }

function parseTemplate($bookTemplate) {
    Write-Verbose "[parseTemplate] begin template parsing..."

    $parSheetFmt = $null # 親シート

    foreach ($sheet in $bookTemplate.WorkSheets) {
        if ($sheet.Type -eq $const.xlWorksheet) {
            # 通常のシートの場合
            $curSheetFmt = @{ name = $sheet.Name; entries = @(); controls = @() }
            Write-Output "[parseTemplate] parsing sheet: $($curSheetFmt.name)"

            # A列のセルを走査
            foreach ($cell in $sheet.Range("A1:A$($config.searchLines)").Cells) {
                if ($cell.Text -match '\{#(\w+)(?:\s+(\S+))*\}') {
                    # 制御文
                    $control = @{ cmd = $Matches[1]; row = $cell.Row }
                    switch ($control.cmd) {
                        'sheet'
                        {
                            $control.type = $Matches[2]
                            # このシートの種別を記録
                            $curSheetFmt.type = $control.type
                        }
                    }
                    $curSheetFmt.controls += $control

                    Write-Verbose "[parseTemplate] found control statement: $($control.cmd), Line: $($control.row)"
                }
            }

            # 階層構造の作成
            switch -regex ($curSheetFmt.type) {
                'once|file' {
                    # テンプレートルートに追加
                    $format.entries += $curSheetFmt
                    # 親シートはなし
                    $parSheetFmt = $null
                }
                'type|class' {
                    # テンプレートルートに追加
                    $format.entries += $curSheetFmt
                    # 親シートを設定
                    $parSheetFmt = $curSheetFmt
                }
                'field|method' {
                    # 親シートがあるかどうか
                    if ($null -eq $parSheetFmt) {
                        # テンプレートルートに追加
                        $format.entries += $curSheetFmt
                    }
                    else {
                        # 親シートに追加
                        $parSheetFmt.entries += $curSheetFmt
                    }
                }
            }
        }
    }

    Write-Verbose "[parseTemplate] end template parsing."
}

function makeDocument($bookTemplate, $bookDocument) {

    enumEntries $bookTemplate $bookDocument $format.entries
}

function enumEntries($bookTemplate, $bookDocument, $entries) {

    foreach ($entry in $entries) {
        $sheetTemplate = $bookTemplate.WorkSheets.Item($entry.name)
        [void]$sheetTemplate.Copy([System.Reflection.Missing]::Value, $bookDocument.WorkSheets.Item($bookDocument.WorkSheets.Count))
        $sheetDocument = $bookDocument.WorkSheets.Item($bookDocument.WorkSheets.Count)
        $lineTemplate = 0 # テンプレート側で出力した行位置
        $lineDocument = 0 # 生成ドキュメント側で出力した行位置

        foreach ($control in $entry.controls) {
            # 制御文までを出力
            translateLines $sheetTemplate ([ref]$lineTemplate) ($control.row - 1) $sheetDocument ([ref]$lineDocument)

            # 生成ドキュメント側制御文削除
            [void]$sheetDocument.Rows("$($lineDocument + 1)").Delete()

            # テンプレート側行位置制御
            $lineTemplate = $control.row
        }

        # 残りを出力
        translateLines $sheetTemplate ([ref]$lineTemplate) $config.searchLines $sheetDocument ([ref]$lineDocument)
    }
}

function translateLines($sheetTemplate, [ref]$templateCursor, $templateEnd, $sheetDocument, [ref]$documentCursor) {

    $lines = $templateEnd - $templateCursor.Value

    if ($lines -ge 1) {
        $rangeFrom = $sheetTemplate.Rows("$($templateCursor.Value + 1):$templateEnd")
        $rangeTo = $sheetDocument.Range("A$($documentCursor.Value + 1)")

        # TODO: 置き換え処理

        # 出力行数
        $templateCursor.Value += $lines
        $documentCursor.Value += $lines
    }
}

$excel = New-Object -ComObject "Excel.Application"
$excel.Visible = $true

$bookTemplate = $excel.Workbooks.Open('D:\ServerFolders\Repository\Sharon\template\test.xlsm', 0, $true)
parseTemplate $bookTemplate

$bookDocument = $excel.Workbooks.Add($const.xlWorksheet)
makeDocument $bookTemplate $bookDocument

$bookTemplate.Close($false)
#$excel.Quit()
$excel = $null
