<#
.SYNOPSIS
    Excel コードドキュメントジェネレーター(Ganan)
.DESCRIPTION
    コードパーサー(Sharon)で解析したXMLを指定されたExcelテンプレートに展開し、
    コードドキュメントを生成します。
.NOTES
    This project was released under the MIT License.
#>

using module '.\DocumentTaskServer.psm1'

using namespace System.Collections.Generic
using namespace System.Linq

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
    [string] $OutputDirectory,

    # 並列実行数
    [Parameter(Mandatory=$false)]
    [Int] $ParallelCount = 2
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

    # タスクサーバー
    [List[DocumentTaskServer]] $listServers =
        (New-Object -TypeName 'System.Collections.Generic.List[DocumentTaskServer]')

    # 削除キュー
    hidden [Queue[DocumentTaskServer]] $deleteQueue =
        (New-Object -TypeName 'System.Collections.Generic.Queue[DocumentTaskServer]')

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
                -ArgumentList @("$($global:messages.E005003) Template:$($script:Path)"))
        }

        # テンプレートチェック
        if (![System.IO.File]::Exists($this.template)) {
            throw (New-Object -TypeName 'System.ArgumentException' `
                -ArgumentList @("$($global:messages.E005001) Template:$($script:Template)"))
        }

        # 出力先ディレクトリチェック
        if (![System.IO.Directory]::Exists($this.outputDir)) {
            throw (New-Object -TypeName 'System.ArgumentException' `
                -ArgumentList @("$($global:messages.E005002) OutputDirectory:$($script:OutputDirectory)"))
        }

        Write-Debug "[GananApplication] end."
    }

    [void] GenerateDocuments() {
        Write-Debug "[GenerateDocuments] begin..."
        [IEnumerator[string]] $enumFilelist = [System.IO.File]::ReadLines($this.filelist, [System.Text.Encoding]::UTF8).GetEnumerator()

        for ($i = 0; $i -lt $script:ParallelCount; $i++) {
            # インスタンスを作成
            [DocumentTaskServer] $instance = [DocumentTaskServer]::new()
            $this.listServers.Add($instance)

            # テンプレートの初期化指示
            $instance.InitializeTemplate($this.template)
        }

        do {
            $this.CheckIdlingServer($enumFilelist)
        } while ($this.UpdateServer())

        Write-Debug "[GenerateDocuments] end."
    }

    hidden [void] CheckIdlingServer([IEnumerator[string]] $enumFilelist) {
        $signals = [Enumerable]::ToArray(
            [Enumerable]::Select($this.listServers, [Func[DocumentTaskServer, System.Threading.WaitHandle]]{
                param([DocumentTaskServer] $server)
                return $server.signal
            }))

        $active = [System.Threading.WaitHandle]::WaitAny($signals, 0)
        if ($active -ne [System.Threading.WaitHandle]::WaitTimeout) {
            # 次のスケジューリングを行う
            $this.ScheduleNext($enumFilelist, $active)
        }
    }

    hidden [bool] ScheduleNext([IEnumerator[string]] $enumFilelist, [Int] $index) {
        [bool] $result = $enumFilelist.MoveNext()

        if ($result) {
            # 次の要素を取得
            $json = ($enumFilelist.Current | ConvertFrom-Json)

            # 出力ファイル名生成
            if ($json.packagePath -ne "") {
                $dirPackage = [System.IO.Path]::Combine($this.outputDir, $json.packagePath.Replace('.', [System.IO.Path]::DirectorySeparatorChar))

                if (-not (Test-Path -Path "$dirPackage" -PathType 'Container')) {
                    # 存在しないので、ディレクトリを作成する
                    Write-Debug "[ScheduleNext] Creating directory... dir:$dirPackage"

                    New-Item -Path "$dirPackage" -ItemType Directory
                }

                $fileDocument = [System.IO.Path]::Combine($dirPackage, "$($json.fileName).xlsx")
            }
            else {
                $fileDocument = [System.IO.Path]::Combine($this.outputDir, "$($json.fileName).xlsx")
            }

            Write-Debug "[ScheduleNext] Generating xml:$($json.xmlFile), doc:$fileDocument"
            $this.listServers[$index].GenerateDocument($json.xmlFile, $fileDocument)
        }
        else {
            # タスクの完了
            Write-Debug "[ScheduleNext] Already tasks that generate the documents was finished."
            $this.listServers[$index].EndTaskServer()
        }

        return $result
    }

    hidden [bool] UpdateServer() {
        [DocumentTaskServer] $current = $null

        foreach ($current in $this.listServers) {
            if (-not $current.PollingCommand()) {
                # 無効になったので、削除対象
                $this.deleteQueue.Enqueue($current)
            }
        }

        while ($this.deleteQueue.TryDequeue([ref]$current)) {
            $this.listServers.Remove($current)
        }

        return ($this.listServers.Count -gt 0)
    }

}

$app = [GananApplication]::new()
$app.GenerateDocuments()
