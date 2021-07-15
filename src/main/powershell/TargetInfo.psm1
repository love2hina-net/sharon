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


    ClassTargetInfo([XPathNavigator]$node) {

        $this.comment = $node.Evaluate('comment/text()')
        $this.modifier = $node.Evaluate('@modifier')
        $this.name = $node.Evaluate('@name')
        $this.fullname = $node.Evaluate('@fullname')

    }

}
