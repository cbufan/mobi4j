package com.xc.mobi4j;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 一条 EXTH 元数据记录。EXTH 用数字 type 标识字段（作者、出版社、封面偏移等），
 * 具体编号见 {@link ExthTags}。
 */
public final class ExthEntry {

    private final int type;
    private final byte[] data;

    ExthEntry(int type, byte[] data) {
        this.type = type;
        this.data = data;
    }

    /** EXTH 记录类型，例如 100 表示作者。 */
    public int getType() {
        return type;
    }

    /** 原始字节（副本）。字符串字段通常是 UTF-8/CP1252，数字字段是 4 字节大端整数。 */
    public byte[] getData() {
        return data.clone();
    }

    /** 数据长度（字节）。 */
    public int getLength() {
        return data.length;
    }

    /** 按 UTF-8 解码为字符串。 */
    public String asString() {
        return asString(StandardCharsets.UTF_8);
    }

    /** 按指定字符集解码为字符串。 */
    public String asString(Charset charset) {
        return Bytes.decodeString(data, 0, data.length, charset);
    }

    /** 按 4 字节大端有符号整数读取；长度不足时返回较小的整数。 */
    public int asInt() {
        if (data.length >= 4) {
            return Bytes.i32(data, 0);
        }
        if (data.length == 2) {
            return Bytes.u16(data, 0);
        }
        if (data.length == 1) {
            return data[0] & 0xFF;
        }
        return 0;
    }

    /** 按 4/8 字节大端整数读取。 */
    public long asLong() {
        if (data.length >= 8) {
            return (Bytes.u32(data, 0) << 32) | Bytes.u32(data, 4);
        }
        if (data.length >= 4) {
            return Bytes.u32(data, 0);
        }
        if (data.length == 2) {
            return Bytes.u16(data, 0);
        }
        if (data.length == 1) {
            return data[0] & 0xFF;
        }
        return 0L;
    }

    @Override
    public String toString() {
        return "ExthEntry{type=" + type + " (" + ExthTags.name(type) + "), length=" + data.length + '}';
    }
}
