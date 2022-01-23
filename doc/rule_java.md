## 記載ルール
### Javaソースコード
パラメーターや外部インターフェース仕様については、Javadoc(`/**`)を認識します。
処理部については、行コメントについては3重スラッシュ(`///`)を用い、
ブロックコメントは(`/*/`)で開始します。

処理部の記述で特別な意味を持つものは、以下の通りです。
- \#(シャープ)
    - 先頭に付与することで、ロジック記述に段落番号を振ることを示します。番号は自動的に採番されます。`#`の後ろにはスペースが必要です。
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
     * 詳細設計工程ではこのように実処理は記述しません。
     */
    /// [内部変数]カウンター = 0
    /// [内部変数]リターンコード = 正常

    /// # パラメーターチェック
    /// 入力パラメータ―のチェックを行う。

    /// [引数]パラメーター名が null の場合
    if (true) {
        // 条件は仮置きすればよいです。
    }
    /// [引数]パラメーター名が 空文字 の場合
    else if (false) {
    }
    /// その他の場合
    else {
    }

    /// # リターンする
    return 0;
}
```

次によく見かけるコメント記述ですが、悪い例を示します。
```java
public int badExsample(String name) {

    /// # パラメーターチェック
    if (name == null) {
        /// [引数]パラメーター名が null の場合
        // 上記指定は正しくなく、if文の条件コメントとしては扱われません。
    } else if (name.isEmpty()) {
        /// [引数]パラメーター名が 空文字 の場合
        // ここのコメントも、else if文の条件コメントとしては扱われません。

        /// その他の場合
    } else {
        // 上のその他の場合もコメント位置が正しくありません。
    }
}
```
Sharonコードパーサーは比較的厳密にJava言語仕様(The Java Language Specification)に従ってパースします。
上記の例はJava言語仕様上、条件分岐に対するコメントとしてみなされないため、正しく解釈されません。