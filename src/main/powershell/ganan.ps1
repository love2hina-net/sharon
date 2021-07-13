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
[CmdletBinding()]
param()

$global:config = @{}
# 対象となる行数
$global:config.searchLines = 1028

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
        Write-Verbose "[parseTemplate] begin template parsing..."

        $parSheetFmt = $null # 親シート

        foreach ($sheet in $this.bookTemplate.WorkSheets) {
            if ($sheet.Type -eq $global:const.xlWorksheet) {
                # 通常のシートの場合
                $curSheetFmt = @{ name = $sheet.Name; entries = @(); controls = @() }
                Write-Output "[parseTemplate] parsing sheet: $($curSheetFmt.name)"

                # A列のセルを走査
                foreach ($cell in $sheet.Range("A1:A$($global:config.searchLines)").Cells) {
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
            }
        }

        Write-Verbose "[parseTemplate] end template parsing."
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
    
    
            $sheetTemplate = $this.bookTemplate.WorkSheets.Item($entry.name)
            [void]$sheetTemplate.Copy([System.Reflection.Missing]::Value, $this.bookDocument.WorkSheets.Item($this.bookDocument.WorkSheets.Count))
            $sheetDocument = $this.bookDocument.WorkSheets.Item($this.bookDocument.WorkSheets.Count)
            $lineTemplate = 0 # テンプレート側で出力した行位置
            $lineDocument = 0 # 生成ドキュメント側で出力した行位置
    
            foreach ($control in $entry.controls) {
                # 制御文までを出力
                $this.translateLines($sheetTemplate, ([ref]$lineTemplate), ($control.row - 1), $sheetDocument, ([ref]$lineDocument))
    
                # 生成ドキュメント側制御文削除
                [void]$sheetDocument.Rows("$($lineDocument + 1)").Delete()
    
                # テンプレート側行位置制御
                $lineTemplate = $control.row
            }
    
            # 残りを出力
            $this.translateLines($sheetTemplate, ([ref]$lineTemplate), $global:config.searchLines, $sheetDocument, ([ref]$lineDocument))
        }
    }

    [void] translateLines($sheetTemplate, [ref]$templateCursor, $templateEnd, $sheetDocument, [ref]$documentCursor) {

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

}

$app = [GananApplication]::new()
$app.test()
