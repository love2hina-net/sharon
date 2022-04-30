package net.love2hina.kotlin.sharon.kotlin

import java.nio.file.Path

/**
 * テストクラス.
 *
 * <p>
 * For example:<br>
 * <pre>{@code Window win = new Window(parent);
 * win.show();}</pre>
 * </p>
 *
 * @author webmaster@love2hina.net
 * @since 0.1.0
 * @version 0.2.0
 */
class ParseTestClassTarget
    /**
     * コンストラクタ.
     *
     * @param parent 親
     */
    constructor(val parent: String): Any(), java.io.Serializable {

    /** フィールド */
    private var fieldInt = 0

    init {
        /// # 初期化
        /// 初期化処理を行う
        fieldInt = 2
    }

    fun <Ty> method(name: String, amount: Ty): Int {
        /// # 出力
        println("Hello, world.")

        /// # パスの設定
        val path = Path.of("file://localhost")

        /// # なにか
        /// 記述文
        /// 変数A = "365"
        /// 変数B = "FG"

        /*/
         * ブロックコメント
         *
         * あいう
         * 変数C = 54
         * かきく
         */

        /// # 条件判断
        /// if
        if (true) {
            /// # 1つめの条件を満たす場合
        }
        /// else if
        else if (false) {
            /// # 2つめの条件を満たす場合
        }
        /// else
        else {
            /// # その他の場合
        }

        /// # when文パターン1
        when (fieldInt) {
            /// 条件1
            (0) -> {
                /// # 条件1の処理
            }
            /// 条件2
            (1) -> {
                /// # 条件2の処理
            }
        }

        /// # when文パターン2
        when {
            /// 条件1
            (fieldInt == 0) -> {
                /// # 条件1の処理
            }
            (path.toString() == "file://anything") -> {
                /// # 条件2の処理
            }
        }

        /// # リターン
        return 0
    }

}
