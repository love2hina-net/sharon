using module '.\TargetInfo.psm1'
using module '.\DocumentWriter.psm1'
using module '.\ControlStatement.psm1'

using namespace System.Collections.Generic
using namespace System.Text.RegularExpressions
using namespace System.Linq
using namespace System.Xml.XPath

# ドキュメント生成
class DocumentGenerator {

    # テンプレートフォーマット情報
    $format = @{ entries = @() }
    # Excel Application
    $excel = (New-Object -ComObject 'Excel.Application')
    # テンプレートワークブック
    $bookTemplate
    # 生成ドキュメントワークブック
    $bookDocument

    # XMLドキュメント
    hidden [System.Xml.XmlDocument] $xml
    # XPath
    hidden [XPathNavigator] $xpath

    [void] GenerateDocument([string]$fileXml, [string]$fileTemplate) {

        # コード解析XMLファイルを開く
        $this.xml = [System.Xml.XmlDocument](Get-Content $fileXml)
        $this.xpath = $this.xml.CreateNavigator()

        # テンプレートを開く
        $this.bookTemplate = $this.excel.Workbooks.Open($fileTemplate, 0, $true)
        $this.ParseTemplate()

        # ドキュメントを生成する
        $this.bookDocument = $this.excel.Workbooks.Add($global:const.xlWorksheet)
        $this.MakeDocument()

        $this.bookTemplate.Close($false)
        #$this.excel.Quit()
        $this.excel = $null
    }

    # 制御文解析
    [void] ParseTemplate() {
        Write-Debug '[ParseTemplate] begin template parsing...'

        [SheetFormat] $parSheetFmt = $null # 親シート

        foreach ($sheet in $this.bookTemplate.WorkSheets) {
            if ($sheet.Type -eq $global:const.xlWorksheet) {
                # 通常のシートの場合
                $curSheetFmt = [SheetFormat]::new($sheet.Name)
                Write-Debug "[ParseTemplate] parsing sheet: $($curSheetFmt.name)"

                # 制御文スタック
                $stackControl = (New-Object -TypeName 'System.Collections.Generic.Stack[ControlHolder]')
                $stackControl.Push($curSheetFmt)

                # A列のセルを走査
                foreach ($cell in $sheet.Range("A1:A$($global:config.searchLines)").Cells) {
                    [Match] $match = [Regex]::Match($cell.Text, '\{#(\w+)(?:\s+(\S+))*\}')
                    if ($match.Success) {
                        Write-Verbose "[ParseTemplate] found control statement: $($match.Value)"

                        # パラメーター展開
                        [string[]] $params = @()
                        foreach ($i in $match.Groups) {
                            $_values = [Enumerable]::Select($i.Captures, [Func[Capture, string]] {
                                param([Capture] $capture)
                                return $capture.Value
                            })
                            $params += [Enumerable]::ToArray($_values)
                        }

                        # 制御文
                        $control = $null
                        switch ($params[1]) {
                            'sheet' {
                                $control = [SheetControl]::new($params, $cell)
                                # このシートの種別を記録
                                $curSheetFmt.type = $control.type
                            }
                            'begin' {
                                # 第2パラメーターによって分岐
                                switch ($params[2]) {
                                    'codes' {
                                        $control = [CodesControl]::new($params, $cell)
                                    }
                                    'description' {
                                        $control = [DescriptionControl]::new($params, $cell)
                                    }
                                    'assignment' {
                                        $control = [AssignmentControl]::new($params, $cell)
                                    }
                                    'condition' {
                                        $control = [ConditionControl]::new($params, $cell)
                                    }
                                    default {
                                        $control = [IterationControl]::new($params, $cell)
                                    }
                                }
                            }
                            'end' {
                                $last = $stackControl.Pop()
                                $last.Close($params, $cell)
                            }
                        }
                        if ($null -ne $control) {
                            ($stackControl.Peek()).controls += $control
                            if ($control.IsNested()) {
                                $stackControl.Push($control)
                            }

                            Write-Verbose "[ParseTemplate] add control statement: $($control.command), Line: $($control.row)"
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
                    throw (New-Object -TypeName 'System.InvalidOperationException' -ArgumentList ($global:messages.E001001))
                }
            }
        }

        Write-Debug '[ParseTemplate] end template parsing.'
    }

    [void] MakeDocument() {

        $this.EnumEntries($null, $this.format.entries)
    }

    [void] EnumEntries($parent, $entries) {

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
                    $targetCursor = [TargetEnumerable]::new()
                }
                'file' {
                    # TODO: ファイル情報のみ
                    $targetCursor = [TargetEnumerable]::new()
                }
                'class' {
                    # クラス情報
                    $targetCursor = [RootTargetEnumerable]::new($parent, 'class', [Func[XPathNavigator, TargetInfo]]{
                        param([XPathNavigator] $node)
                        return [ClassTargetInfo]::new($node)
                    })
                }
                'method' {
                    # メソッド
                    $targetCursor = [RootTargetEnumerable]::new($parent, 'method', [Func[XPathNavigator, TargetInfo]]{
                        param([XPathNavigator] $node)
                        return [MethodTargetInfo]::new($node)
                    })
                }
            }

            foreach ($target in $targetCursor) {
                $docWriter = [DocumentWriter]::new($this.bookTemplate, $this.bookDocument, $entry.name)

                foreach ($control in $entry.controls) {
                    # 制御文出力処理
                    $control.Output($docWriter, $target)
                }

                # ファイナライズ
                $docWriter.Finalize($target)

                # 子シートの出力
                $this.EnumEntries($target, $entry.entries)
            }
        }
    }

}
