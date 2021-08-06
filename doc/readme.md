# コードドキュメントジェネレーター
これはコードからコードドキュメント(詳細設計書)を生成するツールです。
次のプログラムで構成されています。

1. Sharon.jar (Kotlin)
    - Javaソースコードを解析し、コメント(Javadoc)を抽出します。
    抽出した情報は独自形式のXMLとして出力し、Gananの入力情報となります。
2. ganan.ps1 (PowerShell)
    - ExcelのテンプレートファイルとSharonの出力したXMLを読み込み、
    情報を埋め込んでExcelコードドキュメントを生成します。

特に整形を担うgananはスクリプトファイルとなっています。
テンプレートカスタマイズだけでは意図したフォーマットを実現できない場合などでも、
整形処理がスクリプトであることで、整形処理自体を直接カスタマイズすることが容易となっています。

### License
This project was released under the MIT Lincense.

```
Copyright 2021 webmaster@love2hina.net

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```

## 記載ルール

### Javaソースコード
パラメーターや外部インターフェース仕様については、Javadoc(`/***/`)を認識します。
処理部については、行コメントについては3重スラッシュ(`///`)を用います。
ブロックコメントは(`/*/`)で開始します。

処理部の記述で特別な意味を持つものは、以下の通りです。
- \#(シャープ)
    - ロジック記述に項目番号を振ることを示します。
- \=(等号)
    - 左辺に右辺の値を設定することを示します。

また、処理部では、条件分岐等はプログラム構造から読み取ります。
その為、if文やswitch文、for文などは条件部は仮置きで入力します。

以下に詳細設計工程でのJavaソースコードの例を示します。

```java
/**
 * メソッド.
 * 
 * @param name パラメーター名
 * @return リターンコード
 * @throws IOException
 */
public int method(String name) throws IOException {

    /// # 初期処理
    /// 内部変数の初期化を行う。
    // この行の通常コメントはコードドキュメントには出力されません。
    /*
     * このブロックコメントもコードドキュメントには出力されません。
     * なお、詳細設計工程では実処理は記述しません。
     */
    /// [内部変数]カウンター = 0
    /// [内部変数]リターンコード = 正常

    /// # パラメーターチェック
    /// 入力パラメータ―のチェックを行う。

    /// [引数]パラメーター名が null の場合
    if (true) {
        /// ここのコメントは、if文の条件コメントとしては扱われません。
    }
    /// [引数]パラメーター名が 空文字 の場合
    else if (false) {
        /// ここのコメントも、else if文の条件コメントとしては扱われません。
    }
    /// その他の場合
    else {
        /// この場所のコメントは、The Java Language Specification上、
        /// ブロックステートメント内のコメントであり、if elseステートメントに対するコメントとは解釈されません。
    }
}
```

### Excel

#### 制御文
A列に鍵括弧で括り、\#で始まる文字列(例:`{#sheet class}`)を制御文と呼びます。
制御文は実際のコードドキュメントには出力されません。
該当行全てが無いものとして扱われます。

|制御文|パラメーター|概要|例|
|:-----|:-----------|:---|:-|
|sheet ||このシートの作成区分を指定します。||
|      |once|固定で1度のみ追加するシートです。表紙などに用います。|`{#sheet once}`|
|      |file|ファイル単位で1度のみ追加するシートです。|`{#sheet file}`|
|      |type|型定義毎に追加するシートです。|`{#sheet type}`|
|      |class|クラス定義毎に追加するシートです。|`{#sheet class}`|
|      |field|フィールド定義毎に追加するシートです。|`{#sheet filed}`|
|      |method|メソッド定義毎に追加するシートです。|`{#sheet method}`|
|begin |-|指定された要素の繰り返し処理を開始します。||
|      |fields|フィールド定義を繰り返します。|`{#begin fields}`|
|      |codes|ロジック記述を開始します。|`{#begin codes}`|
|end   |-|指定された要素の繰り返し処理を終了します。||

#### 変数
任意のセルに$で始まる文字列(例:`{$name}`)は変数展開されます。
この部分は指定された変数に該当する値に置き換えられてコードドキュメントに出力されます。

|スコープ|変数|概要|指定例|出力例|
|:------|:---|:---|:-----|:-----|
|クラス  |modifier|修飾子|`{$modifier}`|`public`|
|       |name|クラス名|`{$name}`|`ClassName`|
