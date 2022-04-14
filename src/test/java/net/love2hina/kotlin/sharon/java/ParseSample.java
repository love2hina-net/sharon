package net.love2hina.kotlin.sharon.java;

import java.io.IOException;

public class ParseSample {

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

}
