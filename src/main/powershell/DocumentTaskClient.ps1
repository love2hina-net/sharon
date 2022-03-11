using module './CommonModule.psm1'
using module './DocumentTaskCommand.psm1'
using module './DocumentGenerator.psm1'

using namespace System.IO
using namespace System.IO.Pipes

[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)]
    [string] $PipeName
)

# 出力設定を引き継ぐ
$ConfirmPreference = $using:ConfirmPreference
$DebugPreference = $using:DebugPreference
$VerbosePreference = $using:VerbosePreference
$WarningPreference = $using:WarningPreference
$ErrorActionPreference = $using:ErrorActionPreference
if ($ErrorActionPreference -eq 'Break') {
    $ErrorActionPreference = 'Stop'
}

# メッセージ定義の読み込み
& (Join-Path -Path $using:PSScriptRoot -ChildPath '.\Messages.ps1' -Resolve)
Import-LocalizedData -BindingVariable 'messages' -FileName 'Messages'

class DocumentTaskClient {

    # パイプ名
    [string] $pipeName

    # パイプクライアント
    [NamedPipeClientStream] $pipeClient

    # メッセージ待機フラグ
    [bool] $waitMessage

    # ドキュメントジェネレーター
    [DocumentGenerator] $generator

    DocumentTaskClient() {
        # パイプ名の設定
        $this.pipeName = $script:PipeName
        # パイプの作成
        Write-Debug "[$($this.pipeName)][DocumentTaskClient] Start a pipe client..."
        $this.pipeClient = (New-Object -TypeName 'System.IO.Pipes.NamedPipeClientStream' `
            -ArgumentList @('.', $this.pipeName, [PipeDirection]::InOut))

        # サーバーへの接続
        Write-Debug "[$($this.pipeName)][DocumentTaskClient] Connect to a server..."
        $this.pipeClient.Connect()

        Write-Debug "[$($this.pipeName)][DocumentTaskClient] Init a client was completed."
    }

    [void] Main() {
        Write-Debug "[$($this.pipeName)][DocumentTaskClient] Wait for messages..."

        [System.Text.UTF8Encoding] $utf8 = (New-Object -TypeName 'System.Text.UTF8Encoding' -ArgumentList @($false, $true))
        [StreamReader] $streamReader = (New-Object -TypeName 'System.IO.StreamReader' -ArgumentList @($this.pipeClient, $utf8, $true, -1, $true))
        [StreamWriter] $streamWriter = (New-Object -TypeName 'System.IO.StreamWriter' -ArgumentList @($this.pipeClient, $utf8, -1, $true))
        $streamWriter.AutoFlush = $true

        Use-Disposable @($streamReader, $streamWriter, $this.pipeClient) {
            $this.waitMessage = $true
            do {
                $message = $streamReader.ReadLine()
                Write-Debug "[$($this.pipeName)][DocumentTaskClient] Recieve a message... msg:$message"

                [bool] $handled = [TaskCommand]::ReceiveServerRequest($message, $this, $streamWriter)
                if (-not $handled) {
                    # 処理されなかったコマンド
                    Write-Warning "[$($this.pipeName)][DocumentTaskClient] The message was not procceded. msg:$message"
                    $streamWriter.WriteLine('error')
                }
            } while ($this.waitMessage)

            [void](Start-Sleep -Seconds 1.0)
        }

        Write-Debug "[$($this.pipeName)][DocumentTaskClient] End message waiting."
    }

}

$client = [DocumentTaskClient]::new()
$client.Main()

# 強制的にリリース(Excelプロセスが残り続けるため)
[System.GC]::Collect()
