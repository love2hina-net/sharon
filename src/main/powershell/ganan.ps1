<#
.SYNOPSIS
    Excel コードドキュメントジェネレーター(Ganan)
.DESCRIPTION
    コードパーサー(Sharon)で解析したXMLを指定されたExcelテンプレートに展開し、
    コードドキュメントを生成します。
.NOTES
    This project was released under the MIT License.
#>

[CmdletBinding()]
param(
    # Sharonで出力したfilelist.jsonを指定します。
    # ディレクトリを指定した場合、配下のfilelist.jsonを対象とします。
    [Parameter(Mandatory=$true)]
    [string] $Path,

    # Excelテンプレートファイルを指定します。
    [Parameter(Mandatory=$true)]
    [string] $Template,

    # 出力先ディレクトリを指定します。
    [Parameter(Mandatory=$true)]
    [string] $OutputDirectory
)

# メッセージ定義の読み込み
& (Join-Path -Path $PSScriptRoot -ChildPath '.\Messages.ps1' -Resolve)
Import-LocalizedData -BindingVariable 'messages' -FileName 'Messages'

class GananApplication {

    # ファイルリスト
    [string] $filelist

    # テンプレートファイル名
    [string] $template

    # 出力先ディレクトリ
    [string] $outputDir

    GananApplication() {
        Write-Debug "[GananApplication] begin..."

        $this.filelist = [System.IO.Path]::GetFullPath($script:Path)
        $this.template = [System.IO.Path]::GetFullPath($script:Template)
        $this.outputDir = [System.IO.Path]::GetFullPath($script:OutputDirectory)

        # ファイルリストチェック
        if ([System.IO.Directory]::Exists($this.filelist)) {
            # ディレクトリのため、ファイル名補完
            $this.filelist = [System.IO.Path]::Combine($this.filelist, 'filelist.json')
        }
        if (![System.IO.File]::Exists($this.filelist)) {
            throw (New-Object -TypeName 'System.ArgumentException' `
                -ArgumentList ("$($global:messages.E005003) Template:$($script:Path)"))
        }

        # テンプレートチェック
        if (![System.IO.File]::Exists($this.template)) {
            throw (New-Object -TypeName 'System.ArgumentException' `
                -ArgumentList ("$($global:messages.E005001) Template:$($script:Template)"))
        }

        # 出力先ディレクトリチェック
        if (![System.IO.Directory]::Exists($this.outputDir)) {
            throw (New-Object -TypeName 'System.ArgumentException' `
                -ArgumentList ("$($global:messages.E005002) OutputDirectory:$($script:OutputDirectory)"))
        }

        Write-Debug "[GananApplication] end."
    }

    [void] GenerateDocuments() {
        Write-Debug "[GenerateDocuments] begin..."

        $pathDocGen = (Join-Path -Path $PSScriptRoot -ChildPath '.\DocumentGenerator.ps1' -Resolve)
        $fileTemplate = $this.template
        $dirOutput = $this.outputDir

        [System.IO.File]::ReadLines($this.filelist, [System.Text.Encoding]::UTF8) | `
            ConvertFrom-Json | ForEach-Object -Parallel {
                $json = $_

                # 出力設定を引き継ぐ
                $ConfirmPreference = $using:ConfirmPreference
                $DebugPreference = $using:DebugPreference
                $VerbosePreference = $using:VerbosePreference
                $WarningPreference = $using:WarningPreference
                $ErrorActionPreference = $using:ErrorActionPreference

                # 出力ファイル名生成
                if ($json.packagePath -ne "") {
                    $dirPackage = [System.IO.Path]::Combine($using:dirOutput, $json.packagePath.Replace('.', [System.IO.Path]::DirectorySeparatorChar))

                    if (-not (Test-Path -Path "$dirPackage" -PathType 'Container')) {
                        # 存在しないので、ディレクトリを作成する
                        Write-Debug "[GenerateDocuments] Creating directory... dir:$dirPackage"

                        New-Item -Path "$dirPackage" -ItemType Directory
                    }

                    $fileDocument = [System.IO.Path]::Combine($dirPackage, "$($json.fileName).xlsx")
                }
                else {
                    $fileDocument = [System.IO.Path]::Combine($using:dirOutput, "$($json.fileName).xlsx")
                }

                Write-Debug "[GenerateDocuments] Generating xml:$($json.xmlFile) doc:$fileDocument"
                & "$using:pathDocGen" -FileTemplate "$using:fileTemplate" -FileXml "$($json.xmlFile)" -FileDocument "$fileDocument"
            }

        Write-Debug "[GenerateDocuments] end."
    }

}

$app = [GananApplication]::new()
$app.GenerateDocuments()
