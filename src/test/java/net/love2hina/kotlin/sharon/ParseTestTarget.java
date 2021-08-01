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
     * @return リターンコード
     */
    @MyAnnotation
    public int method(String name) {
        /// # 出力
        System.out.println("Hello, world!");

        /// # パスの設定
        final Path path = Path.of("file://localhost");

        /// # なにか
        /// 記述文

        /*/
         * ブロックコメント
         */

        /// if
        if (true) {

        }
        /// else if
        else if (false) {

        }
        /// else
        else {

        }

        String str = "";

        switch (str) {
            /// 文字列
            case "":
            case "ABC":
                break;
            /// その他
            default:
                throw new RuntimeException();
        }

        return 0;
    }

}
