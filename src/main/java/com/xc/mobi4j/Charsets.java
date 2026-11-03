package com.xc.mobi4j;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 把 MOBI 头里的 code page 编号映射成 Java 字符集。
 */
final class Charsets {

    private Charsets() {
    }

    static Charset forMobiEncoding(int codePage) {
        switch (codePage) {
            case 65001:
                return StandardCharsets.UTF_8;
            case 1200:
                return StandardCharsets.UTF_16LE;
            case 1201:
                return StandardCharsets.UTF_16BE;
            case 932:
                return Charset.forName("Shift_JIS");
            case 936:
                return Charset.forName("GBK");
            case 949:
                return Charset.forName("EUC-KR");
            case 950:
                return Charset.forName("Big5");
            case 1250:
            case 1251:
            case 1252:
            case 1253:
            case 1254:
            case 1255:
            case 1256:
            case 1257:
            case 1258:
                return Charset.forName("windows-" + codePage);
            default:
                try {
                    return Charset.forName("windows-" + codePage);
                } catch (RuntimeException ignored) {
                    return Charset.forName("windows-1252");
                }
        }
    }
}
