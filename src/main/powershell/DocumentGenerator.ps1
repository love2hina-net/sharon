using module '.\DocumentGenerator.psm1'

[CmdletBinding()]
param(
    [string] $FileTemplate,
    [string] $FileXml,
    [string] $FileDocument
)

# メッセージ定義の読み込み
& (Join-Path -Path $PSScriptRoot -ChildPath '.\Messages.ps1' -Resolve)
Import-LocalizedData -BindingVariable 'messages' -FileName 'Messages'

Write-Debug "[DocumentGenerator] $FileTemplate, $FileXml, $FileDocument"

$generator = [DocumentGenerator]::new($FileTemplate)
$generator.GenerateDocument($FileXml, $FileDocument)
$generator.Dispose()
$generator = $null

# 強制的にリリース(Excelプロセスが残り続けるため)
[System.GC]::Collect()
