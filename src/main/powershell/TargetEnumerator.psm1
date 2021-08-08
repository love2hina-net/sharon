using module ".\TargetInfo.psm1"

using namespace System.Xml.XPath

class TargetEnumerator : System.Collections.IEnumerable, System.Collections.IEnumerator {

    # クエリ結果
    hidden [XPathNodeIterator] $_query
    # 現在のノード
    hidden [object] $_current

    TargetEnumerator() {
        $this._query = $null
    }

    TargetEnumerator([object]$parent, [string]$tag) {
        $info = $parent -as [TargetInfo]
        $xpath = $parent -as [XPathNavigator]

        if ($null -ne $xpath) {
            # ルート
            $this._query = $xpath.Evaluate("//$tag")
        }
        elseif ($null -ne $info) {
            # 子要素
            $this._query = $info.node.Evaluate($tag)
        }
        else {
            throw (New-Object -TypeName 'System.ArgumentException' -ArgumentList ("検索起点が不明です。"))
        }
    }

    [TargetInfo] CreateInfo([XPathNavigator]$node) {
        return $null
    }

    [System.Collections.IEnumerator] GetEnumerator() {
        return $this
    }

    [object] get_Current() {
        return $this._current
    }

    [bool] MoveNext() {
        $available = if ($null -ne $this._query) { $this._query.MoveNext() }
        else { $false }

        $this._current = if ($available) { $this.CreateInfo($this._query.Current) }
        else { $null }

        return $available
    }

    [void] Reset() {}

}

class ClassTargetEnumerator : TargetEnumerator {

    ClassTargetEnumerator([object]$parent) : base($parent, 'class') {}

    [TargetInfo] CreateInfo([XPathNavigator]$node) {
        return [ClassTargetInfo]::new($node)
    }

}

class MethodTargetEnumerator : TargetEnumerator {

    MethodTargetEnumerator([object]$parent) : base($parent, 'method') {}

    [TargetInfo] CreateInfo([XPathNavigator]$node) {
        return [MethodTargetInfo]::new($node)
    }

}
