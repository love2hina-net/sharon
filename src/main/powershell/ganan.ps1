<#
.SYNOPSIS
    Excel コードドキュメントジェネレーター(Ganan)
.DESCRIPTION
    コードパーサー(Sharon)で解析したXMLを指定されたExcelテンプレートに展開し、
    コードドキュメントを生成します。
.NOTES
    This project was released under the MIT Lincense.
#>
using module '.\TargetInfo.psm1'
using module '.\TargetEnumerator.psm1'
using module '.\DocumentWriter.psm1'
using module '.\ControlStatement.psm1'

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
# xlShiftDown定数
$global:const.xlShiftDown = -4121
# xlToRight定数
$global:const.xlToRight = -4161

class GananApplication {
    # テンプレートフォーマット情報
    $format = @{ entries = @() }
    # Excel Application
    $excel = (New-Object -ComObject 'Excel.Application')
    # テンプレートワークブック
    $bookTemplate
    # 生成ドキュメントワークブック
    $bookDocument
    # XMLドキュメント
    [System.Xml.XmlDocument] $xml
    # XPath
    [System.Xml.XPath.XPathNavigator] $xpath

    [void] test() {
        $this.excel.Visible = $true

        $projectRoot = (Convert-Path "$PSScriptRoot\\..\\..\\..")

        $this.xml = [System.Xml.XmlDocument](Get-Content "$projectRoot\\test.xml")
        $this.xpath = $this.xml.CreateNavigator()

        $this.bookTemplate = $this.excel.Workbooks.Open("$projectRoot\\template\\test.xlsm", 0, $true)
        $this.parseTemplate()

        $this.bookDocument = $this.excel.Workbooks.Add($global:const.xlWorksheet)
        $this.makeDocument()

        $this.bookTemplate.Close($false)
        #$this.excel.Quit()
        $this.excel = $null
    }

    # 制御文解析
    [void] parseTemplate() {
        Write-Debug '[parseTemplate] begin template parsing...'

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
                                # 第2パラメーターによって分岐
                                switch ($Matches[2]) {
                                    'codes' {
                                        $control = [CodesControl]::new($Matches, $cell)
                                    }
                                    'description' {
                                        $control = [DescriptionControl]::new($Matches, $cell)
                                    }
                                    'condition' {
                                        $control = [ConditionControl]::new($Matches, $cell)
                                    }
                                    default {
                                        $control = [IterationControl]::new($Matches, $cell)
                                    }
                                }
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

        Write-Debug '[parseTemplate] end template parsing.'
    }

    [void] makeDocument() {

        $this.enumEntries($null, $this.format.entries)
    }

    [void] enumEntries($parent, $entries) {

        if ($null -eq $parent) {
            # ルートを親要素とする
            $parent = $this.xpath
        }

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
                    $targetCursor = [ClassTargetEnumerator]::new($parent)
                }
                'method' {
                    # メソッド
                    $targetCursor = [MethodTargetEnumerator]::new($parent)
                }
            }

            foreach ($target in $targetCursor) {
                $docWriter = [DocumentWriter]::new($this.bookTemplate, $this.bookDocument, $entry.name)

                foreach ($control in $entry.controls) {
                    # 制御文までを出力
                    $docWriter.outputPassThrough(($control.row - 1), $target)

                    # 制御文出力処理
                    $control.Output($docWriter, $target)
                }

                # 残りを出力
                $docWriter.outputPassThrough($global:config.searchLines, $target)

                # 子シートの出力
                $this.enumEntries($target, $entry.entries)
            }
        }
    }

}

$app = [GananApplication]::new()
$app.test()
