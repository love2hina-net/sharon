using module "./TargetInfo.psm1"

using namespace System.Xml.XPath

class TargetEnumerator : System.Collections.IEnumerable, System.Collections.IEnumerator {

    hidden [object] $_current
    
    [System.Collections.IEnumerator] GetEnumerator() {
        return $this
    }

    [object] get_Current() {
        return $this._current
    }

    [bool] MoveNext() {
        return $false
    }

    [void] Reset() {}

}

class ClassTargetEnumerator : TargetEnumerator {

    hidden [XPathNodeIterator] $_query

    ClassTargetEnumerator([XPathNodeIterator]$query) {
        $this._query = $query
    }

    [bool] MoveNext() {
        $available = $this._query.MoveNext()
        if ($available) {
            $this._current = [ClassTargetInfo]::new($this._query.Current)
        }
        else {
            $this._current = $null
        }
        return $available
    }

}
