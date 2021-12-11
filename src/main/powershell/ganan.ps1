<#
.SYNOPSIS
    Excel コードドキュメントジェネレーター(Ganan)
.DESCRIPTION
    コードパーサー(Sharon)で解析したXMLを指定されたExcelテンプレートに展開し、
    コードドキュメントを生成します。
.NOTES
    This project was released under the MIT Lincense.
#>
using module '.\DocumentGenerator.psm1'

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

# メッセージ定義の読み込み
& (Join-Path -Path $PSScriptRoot -ChildPath '.\Messages.ps1' -Resolve)
Import-LocalizedData -BindingVariable 'messages' -FileName 'Messages'

class GananApplication {

    [void] Test() {

        $projectRoot = (Convert-Path "$PSScriptRoot\\..\\..\\..")

        $generator = [DocumentGenerator]::new()
        $generator.excel.Visible = $true

        $generator.GenerateDocument("$projectRoot\\test.xml", "$projectRoot\\template\\test.xlsm")
    }

}

$app = [GananApplication]::new()
$app.Test()
