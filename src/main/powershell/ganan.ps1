<#
.SYNOPSIS
    Excel コードドキュメントジェネレーター(Ganan)
.DESCRIPTION
    コードパーサー(Sharon)で解析したXMLを指定されたExcelテンプレートに展開し、
    コードドキュメントを生成します。
.NOTES
    This project was released under the MIT Lincense.
#>

[CmdletBinding()]
param(
    # Sharonで出力したXMLファイルを指定します。
    # ディレクトリを指定した場合、配下のXMLを対象とします。
    [Parameter(Mandatory=$true)]
    [string[]] $Path,

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

    # テンプレートファイル名
    [string] $template

    # 出力先ディレクトリ
    [string] $outputDir

    # ファイルリスト
    [string[]] $files = @()

    GananApplication() {
        Write-Debug "[GananApplication] begin..."

        $this.template = [System.IO.Path]::GetFullPath($script:Template)
        $this.outputDir = [System.IO.Path]::GetFullPath($script:OutputDirectory)

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

        # 生成対象XMLファイル列挙
        $list = [System.Collections.Generic.List[string]]::new()
        foreach ($i in $script:Path) {
            $fullpath = [System.IO.Path]::GetFullPath($i)

            if ([System.IO.File]::Exists($fullpath)) {
                [void]$list.Add($fullpath)
            }
            elseif ([System.IO.Directory]::Exists($fullpath)) {
                # ディレクトリ内のXMLファイルを対象とする
                [void]$list.AddRange([System.IO.Directory]::EnumerateFiles($fullpath, '*.xml'))
            }
            else {
                throw (New-Object -TypeName 'System.ArgumentException' `
                    -ArgumentList ("$($global:messages.E005003) Path:$i"))
            }
        }

        $this.files = $list.ToArray()

        Write-Debug "[GananApplication] end."
    }

    [void] GenerateDocuments() {
        Write-Debug "[GenerateDocuments] begin..."

        $jobs = $this.files | ForEach-Object {
            $fileTemplate = $this.template
            $fileXml = $_
            $fileDocument = [System.IO.Path]::Combine($this.outputDir, [System.IO.Path]::GetFileNameWithoutExtension($_) + ".xlsx")

            Start-ThreadJob {
                # 出力設定を引き継ぐ
                $ConfirmPreference = $using:ConfirmPreference
                $DebugPreference = $using:DebugPreference
                $VerbosePreference = $using:VerbosePreference
                $WarningPreference = $using:WarningPreference
                $ErrorActionPreference = $using:ErrorActionPreference

                & (Join-Path -Path $using:PSScriptRoot -ChildPath '.\DocumentGenerator.ps1' -Resolve) -FileTemplate "$using:fileTemplate" -FileXml "$using:fileXml" -FileDocument "$using:fileDocument"
            }
        }

        do {
            Receive-Job -Job $jobs
        } while ($null -eq (Wait-Job -Job $jobs -Timeout 1))
        Receive-Job -Job $jobs

        Write-Debug "[GenerateDocuments] end."
    }

}

$app = [GananApplication]::new()
$app.GenerateDocuments()
