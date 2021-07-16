
# 制御文構造保持
class ControlHolder {

    # 内包する制御文
    [ControlHolder[]] $controls = @()

    [bool] IsNested() {
        return $false
    }

    [void] Close($match, $cell) {
        throw (New-Object -TypeName 'System.InvalidOperationException' -ArgumentList ('ネストしない制御文に対するCloseは無効です。'))
    }

}

# シート書式
class SheetFormat : ControlHolder {

    # シート名
    [string] $name

    # 登場区分
    [string] $type

    # 子エントリ
    [SheetFormat[]] $entries = @()

    SheetFormat([string] $name) {
        $this.name = $name
    }

}

# 制御文
class ControlStatement : ControlHolder {

    # 制御文コマンド
    [string] $command

    # 行
    [long] $row
    # 行数
    [long] $length = 1

    ControlStatement($match, $cell) {
        $this.command = $match[1]
        $this.row = $cell.Row
    }

}

# シート出力指定
class SheetControl : ControlStatement {

    # 登場区分
    [string] $type

    SheetControl($match, $cell) : base($match, $cell) {
        $this.type = $match[2]
    }

}

# 繰り返し
class IterationControl : ControlStatement {

    # 対象
    [string] $target

    IterationControl($match, $cell) : base($match, $cell) {
        $this.target = $match[2]
    }

    [bool] IsNested() {
        return $true
    }

    [void] Close($match, $cell) {
        if ($match[2] -ne $this.target) {
            throw (New-Object -TypeName 'System.InvalidOperationException' `
                -ArgumentList ("制御文の組み合わせが正しくありません。指定: $($match[2]), 想定: $($this.target)"))
        }

        $this.length = $cell.Row - $this.row + 1
    }

}
