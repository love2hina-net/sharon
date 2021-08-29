using namespace System.Xml.XPath

class TargetInfo {

    # ノード
    [XPathNavigator] $node

    TargetInfo([XPathNavigator]$node) {
        $this.node = $node
    }

}

class ClassTargetInfo : TargetInfo {

    # コメント
    [string] $comment
    # 修飾子
    [string] $modifier
    # 名前
    [string] $name
    # 完全名
    [string] $fullname

    # フィールド
    [FieldTargetInfo[]] $fields

    # メソッド
    [MethodTargetInfo[]] $methods

    ClassTargetInfo([XPathNavigator]$node) : base($node) {

        $this.comment = $node.Evaluate('comment/text()')
        $this.modifier = $node.Evaluate('@modifier')
        $this.name = $node.Evaluate('@name')
        $this.fullname = $node.Evaluate('@fullname')

        $this.fields = @()
        foreach ($i in $node.Evaluate('field')) {
            $this.fields += [FieldTargetInfo]::new($i)
        }

        $this.methods = @()
        foreach ($i in $node.Evaluate('method')) {
            $this.methods += [MethodTargetInfo]::new($i)
        }
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

    FieldTargetInfo([XPathNavigator]$node) : base($node) {

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
    [TypeParameterTargetInfo[]] $typeParameters = @()
    # 引数
    [ParameterTargetInfo[]] $parameters = @()
    # 戻り値
    [ReturnTargetInfo[]] $return = @()
    # 例外
    [ThrowsTargetInfo[]] $throws = @()

    MethodTargetInfo([XPathNavigator]$node) : base($node) {

        $this.comment = $node.Evaluate('description/text()')
        $this.modifier = $node.Evaluate('@modifier')
        $this.name = $node.Evaluate('@name')
        $this.definition = $node.Evaluate('definition/text()')

        foreach ($i in $node.Evaluate('typeParameter')) {
            $this.typeParameters += [TypeParameterTargetInfo]::new($i)
        }
        foreach ($i in $node.Evaluate('parameter')) {
            $this.parameters += [ParameterTargetInfo]::new($i)
        }
        foreach ($i in $node.Evaluate('return')) {
            $this.return += [ReturnTargetInfo]::new($i)
        }
        foreach ($i in $node.Evaluate('throws')) {
            $this.throws += [ThrowsTargetInfo]::new($i)
        }
    }

}

class TypeParameterTargetInfo: TargetInfo {

    # コメント
    [string] $comment
    # 型名
    [string] $type

    TypeParameterTargetInfo([XPathNavigator]$node) : base($node) {

        $this.comment = $node.Evaluate('text()')
        $this.type = $node.Evaluate('@type')
    }

}

class ParameterTargetInfo: TargetInfo {

    # コメント
    [string] $comment
    # 修飾子
    [string] $modifier
    # 型名
    [string] $type
    # 名前
    [string] $name

    ParameterTargetInfo([XPathNavigator]$node) : base($node) {

        $this.comment = $node.Evaluate('text()')
        $this.modifier = $node.Evaluate('@modifier')
        $this.type = $node.Evaluate('@type')
        $this.name = $node.Evaluate('@name')
    }

}

class ReturnTargetInfo: TargetInfo {

    # コメント
    [string] $comment
    # 型名
    [string] $type

    ReturnTargetInfo([XPathNavigator]$node) : base($node) {

        $this.comment = $node.Evaluate('text()')
        $this.type = $node.Evaluate('@type')
    }

}

class ThrowsTargetInfo: TargetInfo {

    # コメント
    [string] $comment
    # 型名
    [string] $type

    ThrowsTargetInfo([XPathNavigator]$node) : base($node) {

        $this.comment = $node.Evaluate('text()')
        $this.type = $node.Evaluate('@type')
    }

}

class DescriptionTargetInfo : TargetInfo {

    # 段落番号
    [string] $number
    # 記述
    [string] $description

    DescriptionTargetInfo([XPathNavigator]$node, $docWriter) : base($node) {

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

    AssignmentTargetInfo([XPathNavigator]$node) : base($node) {

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

    ConditionTargetInfo([XPathNavigator]$node, [string]$number, [int]$index) : base($node) {

        $this.index = $index
        $this.number = [string]::Format('{0}.{1}', $number, $index)
        $this.title = '{*paragraph ' + $this.number + '}'
        $this.description = $node.Evaluate('description/text()')
    }

}
