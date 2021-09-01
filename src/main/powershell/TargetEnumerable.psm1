using module ".\TargetInfo.psm1"

using namespace System.Xml.XPath

class TargetEnumerable : System.Collections.IEnumerable {

    # 対象ノード
    hidden [XPathNavigator] $_node = $null
    # クエリ
    hidden [string] $_query = $null
    # 結果生成
    hidden [Func[XPathNavigator, TargetInfo]] $_generator = $null

    TargetEnumerable() {}

    TargetEnumerable([XPathNavigator] $node, [string] $query, [Func[XPathNavigator, TargetInfo]] $generator) {
        $this._node = $node
        $this._query = $query
        $this._generator = $generator
    }

    hidden [XPathNodeIterator] _Evaluate() {
        if ($null -ne $this._node) {
            return $this._node.Evaluate($this._query)
        }
        else {
            return $null
        }
    }

    [System.Collections.IEnumerator] GetEnumerator() {
        return [TargetEnumerator]::new($this, $this._generator)
    }

}

class TargetEnumerator : System.Collections.IEnumerator {

    # 親のEnumerable
    hidden [TargetEnumerable] $_enumerable
    # 結果生成
    hidden [Func[XPathNavigator, TargetInfo]] $_generator
    # クエリ結果
    hidden [XPathNodeIterator] $_iterator = $null
    # 現在のノード
    hidden [object] $_current = $null

    TargetEnumerator([TargetEnumerable] $parent, [Func[XPathNavigator, TargetInfo]] $generator) {
        $this._enumerable = $parent
        $this._generator = $generator
        $this._Initialize()
    }

    hidden [void] _Initialize() {
        $this._iterator = $this._enumerable._Evaluate()
        $this._current = $null
    }

    [object] get_Current() {
        return $this._current
    }

    [bool] MoveNext() {
        $available = if ($null -ne $this._iterator) { $this._iterator.MoveNext() }
        else { $false }

        $this._current = if ($available) { $this._generator.Invoke($this._iterator.Current) }
        else { $null }

        return $available
    }

    [void] Reset() {
        $this._Initialize()
    }

}

class RootTargetEnumerable : TargetEnumerable {

    RootTargetEnumerable([object] $parent, [string] $query, [Func[XPathNavigator, TargetInfo]] $generator)
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

class ConditionTargetEnumerable : TargetEnumerable {

    # 段落番号
    [string] $number

    ConditionTargetEnumerable([XPathNavigator]$node, [string]$number) : base($node, 'case', $null) {
        $this.number = $number
    }

    [System.Collections.IEnumerator] GetEnumerator() {
        return [ConditionTargetEnumerator]::new($this, $this.number)
    }

}

class ConditionTargetEnumerator : TargetEnumerator {

    # 項目番号
    [int] $index
    # 段落番号
    [string] $number

    ConditionTargetEnumerator([ConditionTargetEnumerable] $parent, [string]$number)
     : base($parent, [Func[XPathNavigator, TargetInfo]]{
            param([XPathNavigator] $node)
            return [ConditionTargetInfo]::new($node, $this.number, ++$this.index)
        }) {
        $this.number = $number
    }

    hidden [void] _Initialize() {
        $this._iterator = $this._enumerable._Evaluate()
        $this._current = $null
        $this.index = 0
    }

}
