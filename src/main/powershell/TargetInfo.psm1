using namespace System.Xml.XPath

class ClassTargetInfo {

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

    ClassTargetInfo([XPathNavigator]$node) {

        $this.comment = $node.Evaluate('comment/text()')
        $this.modifier = $node.Evaluate('@modifier')
        $this.name = $node.Evaluate('@name')
        $this.fullname = $node.Evaluate('@fullname')

        $this.fields = @()
        foreach ($i in $node.Evaluate('field')) {
            $this.fields += [FieldTargetInfo]::new($i)
        }
    }

}

class FieldTargetInfo {

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

    FieldTargetInfo([XPathNavigator]$node) {

        $this.comment = $node.Evaluate('comment/text()')
        $this.modifier = $node.Evaluate('@modifier')
        $this.type = $node.Evaluate('@type')
        $this.name = $node.Evaluate('@name')
        $this.value = $node.Evaluate('@value')
    }

}
