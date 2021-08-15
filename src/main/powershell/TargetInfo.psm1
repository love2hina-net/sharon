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
    # 戻り値型
    [string] $return
    # 名前
    [string] $name

    MethodTargetInfo([XPathNavigator]$node) : base($node) {

        $this.comment = $node.Evaluate('comment/text()')
        $this.modifier = $node.Evaluate('@modifier')
        $this.return = $node.Evaluate('@return')
        $this.name = $node.Evaluate('@name')
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
            $this.number = $docWriter.incrementParagraph($title)
            $this.description = $title
        }
        else {
            $this.description = $node.Evaluate('text()')
        }
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
        $this.description = $node.Evaluate('comment/text()')
    }

}
