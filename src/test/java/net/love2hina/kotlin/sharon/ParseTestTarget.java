package net.love2hina.kotlin.sharon;

import java.nio.file.Path;

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
 * @version 1.0.0
 */
public class ParseTestTarget extends Object implements java.io.Serializable {

    /** フィールド */
    private int fieldInt = 0, fieldInt2 = 2;

    public enum MyEnum {

    }

    protected @interface MyAnnotation {

    }

    /**
     * メソッド.
     *
     * @param <Ty> 価格の型
     * @param this インスタンス
     * @param name 名前
     * @param amount 価格
     * @return リターンコード
     * @throws Exception 例外
     */
    @MyAnnotation
    public <Ty> int method(ParseTestTarget this, final String name, Ty amount) throws Exception {
        /// # 出力
        System.out.println("Hello, world!");

        /// # パスの設定
        final Path path = Path.of("file://localhost");

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

        String str = "";

        /// # switch文
        switch (str) {
            /// 文字列
            case "":
            case "ABC":
                /// # 文字列の処理
                break;
            /// 小文字
            case "abc":
            {
                /// # ブロック内
                break;
            }
            /// その他
            default:
                /// # その他
                throw new RuntimeException();
        }

        return 0;
    }

}
