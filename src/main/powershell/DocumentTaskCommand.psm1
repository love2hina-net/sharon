using module './DocumentGenerator.psm1'

using namespace System.Linq
using namespace System.Text.RegularExpressions

class TaskCommand {

    # コマンド
    [string] $command

    # コマンドマップ
    static [System.Collections.Generic.Dictionary[string, TaskCommand]] $dictionaryCommand =
        (New-Object -TypeName 'System.Collections.Generic.Dictionary[string, TaskCommand]')

    # 分割正規表現
    hidden static [Regex] $regexCommand = [Regex]'([^\x1f]+)[\x1f]?'

    TaskCommand([string] $cmd) {
        $this.command = $cmd
    }

    [string] BuildTaskCommand([string[]] $params) {
        [System.Text.StringBuilder] $builder = (New-Object -TypeName 'System.Text.StringBuilder')

        $builder.Append($this.command)
        foreach ($i in $params) {
            $builder.Append("`u{1f}")
            $builder.Append($i)
        }

        return $builder.ToString()
    }

    static [string[]] ParseTaskCommand([string] $message) {
        [MatchCollection] $token = [TaskCommand]::regexCommand.Matches($message)
        [string[]] $result = [Enumerable]::ToArray(
            [Enumerable]::Select($token, [Func[Match, string]] {
                param([Match] $match)
                return $match.Groups[1].Value
            }))

        if ($result.Count -eq 0) {
            return @($null)
        }
        else {
            return $result
        }
    }

    static [void] Register([TaskCommand] $cmd) {
        [TaskCommand]::dictionaryCommand.Add($cmd.command, $cmd)
    }

    #region サーバー側電文処理

    # 送信電文処理
    [void] SendClentRequest([string[]] $params, $server, [System.IO.StreamWriter] $writerRequest) {
        $writerRequest.WriteLine($this.BuildTaskCommand($params))
    }

    [void] ReceiveClientResponse([string] $message, $server) {
        $this.ReceiveClientResponse([TaskCommand]::ParseTaskCommand($message), $server)
    }

    # クライアントからの応答電文受信処理
    [void] ReceiveClientResponse([string[]] $tokens, $server) {
    }
    #endregion

    #region クライアント側電文処理

    # サーバーからの送信電文受信処理
    static [bool] ReceiveServerRequest([string] $message, $client, [System.IO.StreamWriter] $writerResponse) {
        $tokens = [TaskCommand]::ParseTaskCommand($message)
        [TaskCommand] $instance = $null

        if ([TaskCommand]::dictionaryCommand.TryGetValue($tokens[0], [ref]$instance)) {
            $instance.ReceiveServerRequest($tokens, $client, $writerResponse)
            return $true
        }
        else {
            return $false
        }
    }

    # サーバーからの送信電文受信処理
    [void] ReceiveServerRequest([string[]] $tokens, $client, [System.IO.StreamWriter] $writerResponse) {
    }
    #endregion

}

class InitTaskCommand : TaskCommand {

    InitTaskCommand() : base('init') {}

    [void] ReceiveClientResponse([string[]] $tokens, $server) {
        Write-Debug "[$($server.pipeName)][ExitTaskCommand] A client was initialized."
    }

    [void] ReceiveServerRequest([string[]] $tokens, $client, [System.IO.StreamWriter] $writerResponse) {
        # 初期化処理
        Write-Debug "[$($client.pipeName)][InitTaskCommand] Load a template... template:$($tokens[1])"
        $client.generator = [DocumentGenerator]::new($tokens[1])

        Write-Debug "[$($client.pipeName)][InitTaskCommand] Load a template was completed."
        $writerResponse.WriteLine('ok')
    }

}

class ExitTaskCommand : TaskCommand {

    ExitTaskCommand() : base('exit') {}

    [void] SendClentRequest([string[]] $params, $server, [System.IO.StreamWriter] $writerRequest) {
        ([TaskCommand]$this).SendClentRequest($params, $server, $writerRequest)

        # サーバーの新規電文送信を抑止する
        $server.enabled = $false
    }

    [void] ReceiveClientResponse([string[]] $tokens, $server) {
        Write-Debug "[$($server.pipeName)][ExitTaskCommand] A client was finished."
    }

    [void] ReceiveServerRequest([string[]] $tokens, $client, [System.IO.StreamWriter] $writerResponse) {
        # 終了処理
        Write-Debug "[$($client.pipeName)][ExitTaskCommand] Dispose the Excel instance..."
        if ($null -ne $client.generator) {
            $client.generator.Dispose()
            $client.generator = $null
        }

        Write-Debug "[$($client.pipeName)][ExitTaskCommand] Turn on the signal that ending wait for messages..."
        $client.waitMessage = $false

        $writerResponse.WriteLine('ok')
    }

}

class GenerateTaskCommand : TaskCommand {

    GenerateTaskCommand() : base('generate') {}

    [void] ReceiveClientResponse([string[]] $tokens, $server) {
        Write-Debug "[$($server.pipeName)][GenerateTaskCommand] Generate a document was done."
    }

    [void] ReceiveServerRequest([string[]] $tokens, $client, [System.IO.StreamWriter] $writerResponse) {
        # 生成処理
        Write-Debug "[$($client.pipeName)][GenerateTaskCommand] Generate a document... xml:$($tokens[1]), doc:$($tokens[2])"
        if ($null -eq $client.generator) {
            throw (New-Object -TypeName 'System.InvalidOperationException' -ArgumentList @($global:messages.E007001))
        }

        $client.generator.GenerateDocument($tokens[1], $tokens[2])

        $writerResponse.WriteLine('ok')
    }

}

# 登録
[TaskCommand]::Register([InitTaskCommand]::new())
[TaskCommand]::Register([ExitTaskCommand]::new())
[TaskCommand]::Register([GenerateTaskCommand]::new())
