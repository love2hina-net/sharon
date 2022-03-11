using module './CommonModule.psm1'
using module './DocumentTaskCommand.psm1'

using namespace System.IO
using namespace System.IO.Pipes

class DocumentTaskServer {

    # 有効フラグ
    [bool] $enabled = $true
    # 処理中シグナル
    [System.Threading.AutoResetEvent] $signal =
        (New-Object -TypeName 'System.Threading.AutoResetEvent' -ArgumentList @($true))

    # タスク
    hidden [System.Threading.Tasks.Task[string]] $task = $null
    # 実行中コマンド
    hidden [TaskCommand] $taskCommand = $null

    # パイプ名
    [string] $pipeName
    # パイプサーバー
    [NamedPipeServerStream] $pipeServer

    # ライター
    hidden [StreamWriter] $streamWriter
    # リーダー
    hidden [StreamReader] $streamReader

    # クライアントジョブ
    [object] $clientJob

    DocumentTaskServer() {
        # パイプ名の作成
        $this.pipeName = "ganan.task.$([System.Guid]::NewGuid().ToString())"
        # パイプの作成
        Write-Debug "[$($this.pipeName)][DocumentTaskServer] Start a pipe server..."
        $this.pipeServer = (New-Object -TypeName 'System.IO.Pipes.NamedPipeServerStream' `
            -ArgumentList @($this.pipeName, [PipeDirection]::InOut, 1))

        # クライアントの開始
        $this.clientJob = (Start-Job -WorkingDirectory $PSScriptRoot `
            -FilePath (Join-Path -Path $PSScriptRoot -ChildPath 'DocumentTaskClient.ps1' -Resolve) -ArgumentList @($this.pipeName))

        # クライアントの接続待ち
        Write-Debug "[$($this.pipeName)][DocumentTaskServer] Wait for a client..."
        $this.WaitForAsync($this.pipeServer.WaitForConnectionAsync())

        Write-Debug "[$($this.pipeName)][DocumentTaskServer] Init a server was completed."

        # リーダー／ライターの準備
        [System.Text.UTF8Encoding] $utf8 = (New-Object -TypeName 'System.Text.UTF8Encoding' -ArgumentList @($false, $true))
        $this.streamWriter = (New-Object -TypeName 'System.IO.StreamWriter' -ArgumentList @($this.pipeServer, $utf8, -1, $true))
        $this.streamReader = (New-Object -TypeName 'System.IO.StreamReader' -ArgumentList @($this.pipeServer, $utf8, $true, -1, $true))
        $this.streamWriter.AutoFlush = $true
    }

    hidden [void] WaitForAsync([System.Threading.Tasks.Task] $task) {
        do {
            Receive-Job -Job $this.clientJob
        } while (-not $task.Wait(500))
    }

    [void] InitializeTemplate([string] $fileTemplate) {
        $this.SendCommand([InitTaskCommand]::new(), @($fileTemplate))
    }

    [void] EndTaskServer() {
        $this.SendCommand([ExitTaskCommand]::new(), @())
    }

    [void] GenerateDocument([string] $fileXml, [string] $fileDocument) {
        $this.SendCommand([GenerateTaskCommand]::new(), @($fileXml, $fileDocument))
    }

    [void] SendCommand([TaskCommand] $cmd, [string[]] $params) {

        if (($this.enabled -eq $false) -or ($null -ne $this.task) -or ($null -ne $this.taskCommand)) {
            throw (New-Object -TypeName 'System.InvalidOperationException' -ArgumentList @($global:messages.E006001))
        }

        $this.signal.Reset()

        # 送信電文
        Write-Debug "[$($this.pipeName)][DocumentTaskServer] Send $($cmd.command) message..."
        $cmd.SendClentRequest($params, $this, $this.streamWriter)
        # 応答電文の受信をタスクに設定
        $this.task = $this.streamReader.ReadLineAsync()
        $this.taskCommand = $cmd
    }

    [bool] PollingCommand() {
        [bool] $result = $false

        if ($null -ne $this.task) {
            if ($this.task.IsCompleted) {
                $this.taskCommand.ReceiveClientResponse($this.task.Result, $this)

                # 処理完了
                $this.task = $null
                $this.taskCommand = $null
                if ($this.enabled -eq $true) {
                    $this.signal.Set()
                }
            }
        }

        if ($null -ne $this.clientJob) {
            # クライアントジョブの状態を更新
            $resultJob = (Wait-Job -Job $this.clientJob -Timeout 1)
            Receive-Job -Job $this.clientJob

            if ($null -ne $resultJob) {
                Write-Debug "Reason:$($this.clientJob.ChildJobs[0].JobStateInfo.Reason)"

                $this.clientJob = $null
                # 破棄
                Write-Debug "[$($this.pipeName)][DocumentTaskServer] Closing a connection..."
                $this.streamWriter.Dispose()
                $this.streamReader.Dispose()
                $this.pipeServer.Dispose()
            }
            else {
                $result = $true
            }
        }

        return $result
    }

}
