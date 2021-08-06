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
