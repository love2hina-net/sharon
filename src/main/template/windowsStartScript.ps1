<#
.SYNOPSIS
    the document generation tool(${applicationName})
.DESCRIPTION
    the code parser(${applicationName}) launching script.
.NOTES
    This project was released under the MIT License.
#>

# change code page UTF-8(65001)
[void](chcp 65001)

\$dirName = \$PSScriptRoot
\$appNameBase = \$MyInvocation.MyCommand.Name
\$appHome = (Join-Path -Path "\$dirName" -ChildPath '${appHomeRelativePath}' -Resolve)

\$defaultJvmOpts = '${defaultJvmOpts}'

if (\$env:JAVA_HOME -ne '') {
    \$javaHome = [Environment]::ExpandEnvironmentVariables(\$env:JAVA_HOME)
    \$javaExe = (Join-Path -Path "\$javaHome" -ChildPath 'bin\\java.exe')
    if (-not (Test-Path -Path "\$javaExe")) {
        Write-Error "ERROR: JAVA_HOME is set to an invalid directory: \$env:JAVA_HOME"
        Write-Error ''
        Write-Error 'Please set the JAVA_HOME variable in your environment to match the'
        Write-Error 'location of your Java installation.'
        throw 'FAILED'
    }
}
else {
    \$javaExe = 'java.exe'
    [void](. "\$javaExe" -version)
    if (-not \$?) {
        Write-Error 'ERROR: JAVA_HOME is not set and no "java" command could be found in your PATH.'
        Write-Error ''
        Write-Error 'Please set the JAVA_HOME variable in your environment to match the'
        Write-Error 'location of your Java installation.'
        throw 'FAILED'
    }
}

# Execute ${applicationName}
. "\$javaExe" \$defaultJvmOpts \$env:JAVA_OPTS <% if ( appNameSystemProperty ) { %>"-D${appNameSystemProperty}=\$appNameBase"<% } %> `
    -classpath "${classpath}" `
    <% if ( mainClassName.startsWith('--module ') ) { %>--module-path "${modulePath}" <% } %>'${mainClassName}' \$args
