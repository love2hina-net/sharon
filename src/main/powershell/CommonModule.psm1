using namespace System.Linq
using namespace System.Text.RegularExpressions

function Use-Disposable(
    [Parameter(Mandatory=$true)]
    [System.IDisposable[]] $InputObjects,

    [Parameter(Mandatory=$true)]
    [scriptblock] $ScriptBlock)
{
    try {
        . $ScriptBlock
    }
    finally {
        foreach ($i in $InputObjects) {
            if ($i -is [System.IDisposable]) {
                $i.Dispose()
            }
        }
    }
}
