package com.xc.mobi4j;

import java.nio.charset.StandardCharsets;

/**
 * 大端序读取工具。MOBI / PalmDB 的所有多字节整数都是大端序。
 */
final class Bytes {

    private Bytes() {
    }

    static int u8(byte[] data, int offset) {
        return data[offset] & 0xFF;
    }

    static int u16(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    /** 读取无符号 32 位整数，用 long 保存以免溢出。 */
    static long u32(byte[] data, int offset) {
        return ((long) (data[offset] & 0xFF) << 24)
                | ((long) (data[offset + 1] & 0xFF) << 16)
                | ((long) (data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    static int i32(byte[] data, int offset) {
        return (int) u32(data, offset);
    }

    /** 越界时返回默认值的 u16。 */
    static int u16Or(byte[] data, int offset, int defaultValue) {
        return offset >= 0 && offset + 2 <= data.length ? u16(data, offset) : defaultValue;
    }

    /** 越界时返回默认值的 i32。 */
    static int i32Or(byte[] data, int offset, int defaultValue) {
        return offset >= 0 && offset + 4 <= data.length ? i32(data, offset) : defaultValue;
    }

    static boolean startsWith(byte[] data, int offset, String ascii) {
        if (offset < 0 || offset + ascii.length() > data.length) {
            return false;
        }
        for (int i = 0; i < ascii.length(); i++) {
            if ((data[offset + i] & 0xFF) != (ascii.charAt(i) & 0xFF)) {
                return false;
            }
        }
        return true;
    }

    static String ascii(byte[] data, int offset, int length) {
        return new String(data, offset, length, StandardCharsets.ISO_8859_1);
    }

    /** 去掉结尾的 NUL 和空白后解码。 */
    static String decodeString(byte[] data, int offset, int length, java.nio.charset.Charset charset) {
        int end = Math.min(data.length, offset + length);
        while (end > offset && (data[end - 1] == 0 || data[end - 1] == ' ')) {
            end--;
        }
        return new String(data, offset, end - offset, charset).strip();
    }
}
