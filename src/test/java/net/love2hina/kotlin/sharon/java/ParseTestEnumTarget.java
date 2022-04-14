package net.love2hina.kotlin.sharon.java;

/**
 * テスト列挙型.
 *
 * @author webmaster@love2hina.net
 * @since 0.2.0
 * @version 0.2.0
 */
public enum ParseTestEnumTarget {
    /** 通常定義 */
    NORMAL,
    /** コンストラクタ定義 */
    CONSTRUCTOR("value");

    public final String value;

    ParseTestEnumTarget() {
        value = null;
    }
    ParseTestEnumTarget(String v) {
        value = v;
    }
}
