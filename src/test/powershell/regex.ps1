using namespace System.Collections.Generic
using namespace System.Text.RegularExpressions
using namespace System.Linq

[Match] $match = [Regex]::Match('{#begin condition header:2 footer:1}', '\{#(\w+)(?:\s+(\S+))*\}')
$captures = [Enumerable]::SelectMany($match.Groups, [Func[Group, IEnumerable[string]]] {
    param([Group] $g)
    return [Enumerable]::Select($g.Captures, [Func[Capture, string]] {
        param([Capture] $c)
        return [string] $c.Value
    })
})

foreach ($capture in $captures) {
    Write-Debug $capture
}

# https://github.com/PowerShell/PowerShell/issues/7651
