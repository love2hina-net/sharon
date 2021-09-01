using module ".\TargetInfo.psm1"

using namespace System.Xml.XPath

class TargetEnumerator : System.Collections.IEnumerable, System.Collections.IEnumerator {

    # 対象ノード
    hidden [XPathNavigator] $_node = $null
    # クエリ
    hidden [string] $_query = $null
    #
    hidden [Func[XPathNavigator, TargetInfo]] $_generator
    # 初期化状態
    hidden [bool] $_initial = $false
    # クエリ結果
    hidden [XPathNodeIterator] $_iterator = $null
    # 現在のノード
    hidden [object] $_current = $null

    TargetEnumerator() {}

    TargetEnumerator([XPathNavigator] $node, [string] $query, [Func[XPathNavigator, TargetInfo]] $generator) {
        $this._node = $node
        $this._query = $query
        $this._generator = $generator
        $this._ReEvaluate()
    }

    hidden [void] _ReEvaluate() {
        $this._initial = $true
        $this._iterator = $this._node.Evaluate($this._query)
        $this._current = $null
    }

    [System.Collections.IEnumerator] GetEnumerator() {
        return $this
    }

    [object] get_Current() {
        return $this._current
    }

    [bool] MoveNext() {
        $available = if ($null -ne $this._iterator) { $this._iterator.MoveNext() }
        else { $false }

        $this._current = if ($available) { $this._generator.Invoke($this._iterator.Current) }
        else { $null }

        $this._initial = $false
        return $available
    }

    [void] Reset() {
        Write-Debug '-> Reset'
        if (!$this._initial) {
            $this._ReEvaluate()
        }
    }

}

class RootTargetEnumerator : TargetEnumerator {

    RootTargetEnumerator([object] $parent, [string] $query, [Func[XPathNavigator, TargetInfo]] $generator)
     : base($this._GetParentNode($parent), $this._GetQuery($parent, $query), $generator) {}

    hidden [XPathNavigator] _GetParentNode([object] $parent) {
        $info = $parent -as [TargetInfo]
        $xpath = $parent -as [XPathNavigator]

        if ($null -ne $xpath) { return $xpath }
        elseif ($null -ne $info) { return $info.node }
        else {
            throw (New-Object -TypeName 'System.ArgumentException' -ArgumentList ($global:messages.E004001))
        }
    }

    hidden [string] _GetQuery([object] $parent, [string] $query) {
        $info = $parent -as [TargetInfo]
        $xpath = $parent -as [XPathNavigator]

        if ($null -ne $xpath) { return "//$query" }
        elseif ($null -ne $info) { return $query }
        else {
            throw (New-Object -TypeName 'System.ArgumentException' -ArgumentList ($global:messages.E004001))
        }
    }

}

class ConditionTargetEnumerator : TargetEnumerator {

    # 項目番号
    [int] $index = 0
    # 段落番号
    [string] $number

    ConditionTargetEnumerator([XPathNavigator]$node, [string]$number)
     : base($node, 'case', [Func[XPathNavigator, TargetInfo]]{
        param([XPathNavigator] $node)
        return [ConditionTargetInfo]::new($node, $this.number, ++$this.index)
    }) {
        $this.number = $number
    }

}
