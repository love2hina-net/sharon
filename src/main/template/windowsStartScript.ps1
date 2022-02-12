<#
.SYNOPSIS
    コードドキュメントジェネレーター(${applicationName})
.DESCRIPTION
    コードパーサー(${applicationName})を起動し、解析情報ファイル(XML)を出力します。
.NOTES
    This project was released under the MIT License.
#>

param(
    # 解析対象となるJavaソースコードを指定します。
    # ディレクトリや.javaファイル自体を直接指定出来ます。
    [Parameter(Mandatory=\$true)]
    [string[]] \$Path,

    # XMLの出力先ディレクトリを指定します。
    [Parameter(Mandatory=\$true)]
    [string] \$OutputDirectory
)

# コードページの変更 UTF-8(65001)
[void](chcp 65001)

\$dirName = \$PSScriptRoot
\$appNameBase = \$MyInvocation.MyCommand.Name
\$appHome = (Join-Path -Path "\$dirName" -ChildPath '${appHomeRelativePath}' -Resolve)

\$defaultJvmOpts = '${defaultJvmOpts}'

if (\$env:JAVA_HOME -ne '') {
    \$javaHome = [Environment]::ExpandEnvironmentVariables(\$env:JAVA_HOME)
    \$javaExe = (Join-Path -Path "\$javaHome" -ChildPath 'bin\\java.exe')
    if (-not (Test-Path -Path "\$javaExe")) {
        throw "ERROR: JAVA_HOME is set to an invalid directory: \$env:JAVA_HOME"
    }
}
else {
    \$javaExe = 'java.exe'
    [void](. "\$javaExe" -version)
    if (-not \$?) {
        throw 'ERROR: JAVA_HOME is not set and no "java" command could be found in your PATH.'
    }
}

# ${applicationName}の実行
. "\$javaExe" \$defaultJvmOpts \$env:JAVA_OPTS <% if ( appNameSystemProperty ) { %>"-D${appNameSystemProperty}=\$appNameBase"<% } %> `
    -classpath "${classpath}" `
    <% if ( mainClassName.startsWith('--module ') ) { %>--module-path "${modulePath}" <% } %>'${mainClassName}' '--outdir' \$OutputDirectory '--' \$Path
