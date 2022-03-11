using namespace System.Linq
using namespace System.Xml.XPath

#region 対象情報

class TargetInfo {

    # ノード
    [XPathNavigator] $node

    # 変換用
    hidden static [System.Reflection.MethodInfo] $funcCast = [System.Linq.Enumerable].GetMethod('Cast').MakeGenericMethod([XPathNavigator])
    hidden static [Func[XPathNavigator, string]] $funcSelectValue = {
        param([XPathNavigator] $node)
        return $node.Value
    }

    TargetInfo([XPathNavigator] $node) {
        $this.node = $node
    }

    static [string[]] EvaluateStringArray([XPathNavigator] $node, [string] $query) {
        return [Enumerable]::ToArray(
            [Enumerable]::Select([TargetInfo]::funcCast.Invoke($null, @(, $node.Evaluate($query))), [TargetInfo]::funcSelectValue))
    }

}

class FileTargetInfo : TargetInfo {

    # 言語
    [string] $language
    # ファイル
    [string] $src
    # パッケージ宣言
    [string] $package
    # インポート
    [string[]] $imports

    # クラス
    [TargetEnumerable] $classes

    FileTargetInfo([XPathNavigator] $node) : base($node) {

        $this.language = $node.Evaluate('@language')
        $this.src = $node.Evaluate('@src')
        $this.package = $node.Evaluate('package/@package')
        $this.imports = [TargetInfo]::EvaluateStringArray($node, 'import/@package')

        $this.classes = [TargetEnumerable]::new($node, 'class', [Func[XPathNavigator, TargetInfo]]{
            param([XPathNavigator] $_node)
            return [ClassTargetInfo]::new($_node)
        })
    }

}

class ClassTargetInfo : TargetInfo {

    # 修飾子
    [string] $modifier
    # 名前
    [string] $name
    # 完全名
    [string] $fullname
    # 継承
    [string[]] $extends
    # インターフェース
    [string[]] $implements

    # since(Javadoc)
    [string] $since
    # deprecated(Javadoc)
    [string] $deprecated
    # serial(Javadoc)
    [string] $serial
    # version(Javadoc)
    [string] $version
    # author(Javadoc)
    [string[]] $author

    # 説明
    [string] $description

    # 型引数
    [TargetEnumerable] $typeParameters
    # フィールド
    [TargetEnumerable] $fields
    # メソッド
    [TargetEnumerable] $methods

    ClassTargetInfo([XPathNavigator] $node) : base($node) {

        $this.modifier = $node.Evaluate('@modifier')
        $this.name = $node.Evaluate('@name')
        $this.fullname = $node.Evaluate('@fullname')

        $this.extends = [TargetInfo]::EvaluateStringArray($node, 'extends/@name')
        $this.implements = [TargetInfo]::EvaluateStringArray($node, 'implements/@name')

        $this.since = $node.Evaluate('javadoc/@since')
        $this.deprecated = $node.Evaluate('javadoc/@deprecated')
        $this.serial = $node.Evaluate('javadoc/@serial')
        $this.version = $node.Evaluate('javadoc/@version')

        $this.author = [TargetInfo]::EvaluateStringArray($node, 'javadoc/author/text()')

        $this.description = $node.Evaluate('description/text()')

        $this.typeParameters = [TargetEnumerable]::new($node, 'typeParameter', [Func[XPathNavigator, TargetInfo]]{
            param([XPathNavigator] $_node)
            return [TypeParameterTargetInfo]::new($_node)
        })
        $this.fields = [TargetEnumerable]::new($node, 'field', [Func[XPathNavigator, TargetInfo]]{
            param([XPathNavigator] $_node)
            return [FieldTargetInfo]::new($_node)
        })
        $this.methods = [TargetEnumerable]::new($node, 'method', [Func[XPathNavigator, TargetInfo]]{
            param([XPathNavigator] $_node)
            return [MethodTargetInfo]::new($_node)
        })
    }

}

class EnumTargetInfo : TargetInfo {

    # 修飾子
    [string] $modifier
    # 名前
    [string] $name
    # 完全名
    [string] $fullname
    # インターフェース
    [string[]] $implements

    # since(Javadoc)
    [string] $since
    # deprecated(Javadoc)
    [string] $deprecated
    # serial(Javadoc)
    [string] $serial
    # version(Javadoc)
    [string] $version
    # author(Javadoc)
    [string[]] $author

    # 説明
    [string] $description

    # エントリー
    [TargetEnumerable] $entries
    # フィールド
    [TargetEnumerable] $fields
    # メソッド
    [TargetEnumerable] $methods

    EnumTargetInfo([XPathNavigator] $node) : base($node) {

        $this.modifier = $node.Evaluate('@modifier')
        $this.name = $node.Evaluate('@name')
        $this.fullname = $node.Evaluate('@fullname')

        $this.implements = [TargetInfo]::EvaluateStringArray($node, 'implements/@name')

        $this.since = $node.Evaluate('javadoc/@since')
        $this.deprecated = $node.Evaluate('javadoc/@deprecated')
        $this.serial = $node.Evaluate('javadoc/@serial')
        $this.version = $node.Evaluate('javadoc/@version')

        $this.author = [TargetInfo]::EvaluateStringArray($node, 'javadoc/author/text()')

        $this.description = $node.Evaluate('description/text()')

        $this.entries = [TargetEnumerable]::new($node, 'entry', [Func[XPathNavigator, TargetInfo]]{
            param([XPathNavigator] $_node)
            return [EnumEntryTargetInfo]::new($_node)
        })
        $this.fields = [TargetEnumerable]::new($node, 'field', [Func[XPathNavigator, TargetInfo]]{
            param([XPathNavigator] $_node)
            return [FieldTargetInfo]::new($_node)
        })
        $this.methods = [TargetEnumerable]::new($node, 'method', [Func[XPathNavigator, TargetInfo]]{
            param([XPathNavigator] $_node)
            return [MethodTargetInfo]::new($_node)
        })
    }

}

class EnumEntryTargetInfo : TargetInfo {

    # 名前
    [string] $name

    # since(Javadoc)
    [string] $since
    # deprecated(Javadoc)
    [string] $deprecated
    # serial(Javadoc)
    [string] $serial

    # 説明
    [string] $description

    # 引数
    [TargetEnumerable] $arguments

    EnumEntryTargetInfo([XPathNavigator] $node) : base($node) {

        $this.name = $node.Evaluate('@name')

        $this.since = $node.Evaluate('javadoc/@since')
        $this.deprecated = $node.Evaluate('javadoc/@deprecated')
        $this.serial = $node.Evaluate('javadoc/@serial')

        $this.description = $node.Evaluate('description/text()')

        $this.arguments = [TargetEnumerable]::new($node, 'argument', [Func[XPathNavigator, TargetInfo]]{
            param([XPathNavigator] $_node)
            return [ArgumentTargetInfo]::new($_node)
        })
    }
}

class ArgumentTargetInfo : TargetInfo {

    # 型
    [string] $type
    # 値
    [string] $value

    ArgumentTargetInfo([XPathNavigator] $node) : base($node) {

        $this.type = $node.Evaluate('@type')
        $this.value = $node.Evaluate('text()')
    }
}

class FieldTargetInfo : TargetInfo {

    # コメント
    [string] $comment
    # 修飾子
    [string] $modifier
    # 型名
    [string] $type
    # 名前
    [string] $name
    # 初期値
    [string] $value

    FieldTargetInfo([XPathNavigator] $node) : base($node) {

        $this.comment = $node.Evaluate('comment/text()')
        $this.modifier = $node.Evaluate('@modifier')
        $this.type = $node.Evaluate('@type')
        $this.name = $node.Evaluate('@name')
        $this.value = $node.Evaluate('@value')
    }

}

class MethodTargetInfo : TargetInfo {

    # コメント
    [string] $comment
    # 修飾子
    [string] $modifier
    # 名前
    [string] $name
    # 定義
    [string] $definition

    # 型引数
    [TargetEnumerable] $typeParameters
    # 引数
    [TargetEnumerable] $parameters
    # 戻り値
    [TargetEnumerable] $return
    # 例外
    [TargetEnumerable] $throws

    MethodTargetInfo([XPathNavigator] $node) : base($node) {

        $this.comment = $node.Evaluate('description/text()')
        $this.modifier = $node.Evaluate('@modifier')
        $this.name = $node.Evaluate('@name')
        $this.definition = $node.Evaluate('definition/text()')

        $this.typeParameters = [TargetEnumerable]::new($node, 'typeParameter', [Func[XPathNavigator, TargetInfo]]{
            param([XPathNavigator] $_node)
            return [TypeParameterTargetInfo]::new($_node)
        })
        $this.parameters = [TargetEnumerable]::new($node, 'parameter', [Func[XPathNavigator, TargetInfo]]{
            param([XPathNavigator] $_node)
            return [ParameterTargetInfo]::new($_node)
        })
        $this.return = [TargetEnumerable]::new($node, 'return', [Func[XPathNavigator, TargetInfo]]{
            param([XPathNavigator] $_node)
            return [ReturnTargetInfo]::new($_node)
        })
        $this.throws = [TargetEnumerable]::new($node, 'throws', [Func[XPathNavigator, TargetInfo]]{
            param([XPathNavigator] $_node)
            return [ThrowsTargetInfo]::new($_node)
        })
    }

}

class TypeParameterTargetInfo : TargetInfo {

    # コメント
    [string] $comment
    # 型名
    [string] $type

    TypeParameterTargetInfo([XPathNavigator] $node) : base($node) {

        $this.comment = $node.Evaluate('text()')
        $this.type = $node.Evaluate('@type')
    }

}

class ParameterTargetInfo : TargetInfo {

    # コメント
    [string] $comment
    # 修飾子
    [string] $modifier
    # 型名
    [string] $type
    # 名前
    [string] $name

    ParameterTargetInfo([XPathNavigator] $node) : base($node) {

        $this.comment = $node.Evaluate('text()')
        $this.modifier = $node.Evaluate('@modifier')
        $this.type = $node.Evaluate('@type')
        $this.name = $node.Evaluate('@name')
    }

}

class ReturnTargetInfo : TargetInfo {

    # コメント
    [string] $comment
    # 型名
    [string] $type

    ReturnTargetInfo([XPathNavigator] $node) : base($node) {

        $this.comment = $node.Evaluate('text()')
        $this.type = $node.Evaluate('@type')
    }

}

class ThrowsTargetInfo : TargetInfo {

    # コメント
    [string] $comment
    # 型名
    [string] $type

    ThrowsTargetInfo([XPathNavigator] $node) : base($node) {

        $this.comment = $node.Evaluate('text()')
        $this.type = $node.Evaluate('@type')
    }

}

class DescriptionTargetInfo : TargetInfo {

    # 段落番号
    [string] $number
    # 記述
    [string] $description

    DescriptionTargetInfo([XPathNavigator] $node, $docWriter) : base($node) {

        [string] $desc = $node.Evaluate('text()')
        if ($desc -match '^#\s?(\S+)') {
            # 段落番号を設定する
            $title = $Matches[1]
            $this.number = $docWriter.IncrementParagraph($title)
            $this.description = $title
        }
        else {
            $this.description = $node.Evaluate('text()')
        }
    }

}

class AssignmentTargetInfo : TargetInfo {

    # 項目番号
    [string] $index
    # 変数名
    [string] $var
    # 設定値
    [string] $value

    AssignmentTargetInfo([XPathNavigator] $node) : base($node) {

        $this.var = $node.Evaluate('@var')
        $this.value = $node.Evaluate('@value')
    }

}

class ConditionTargetInfo : TargetInfo {

    # 項目番号
    [string] $index
    # 段落番号
    [string] $number
    # 段落名
    [string] $title
    # 記述
    [string] $description

    ConditionTargetInfo([XPathNavigator] $node, [string] $number, [int] $index) : base($node) {

        $this.index = $index
        $this.number = [string]::Format('{0}.{1}', $number, $index)
        $this.title = '{*paragraph ' + $this.number + '}'
        $this.description = $node.Evaluate('description/text()')
    }

}

#endregion

#region 列挙子

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
            throw (New-Object -TypeName 'System.ArgumentException' -ArgumentList @($global:messages.E004001))
        }
    }

    hidden [string] _GetQuery([object] $parent, [string] $query) {
        $info = $parent -as [TargetInfo]
        $xpath = $parent -as [XPathNavigator]

        if ($null -ne $xpath) { return "//$query" }
        elseif ($null -ne $info) { return $query }
        else {
            throw (New-Object -TypeName 'System.ArgumentException' -ArgumentList @($global:messages.E004001))
        }
    }

}

class ConditionTargetEnumerable : TargetEnumerable {

    # 段落番号
    [string] $number

    ConditionTargetEnumerable([XPathNavigator] $node, [string] $number) : base($node, 'case', $null) {
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

    ConditionTargetEnumerator([ConditionTargetEnumerable] $parent, [string] $number)
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

#endregion
