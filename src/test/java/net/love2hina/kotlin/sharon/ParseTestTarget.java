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
public class ParseTestTarget {

    /** フィールド */
    private int fieldInt = 0;

    /**
     * メソッド.
     *
     * @return リターンコード
     */
    public int method() {
        System.out.println("Hello, world!");

        final Path path = Path.of("file://localhost");

        return 0;
    }

}
