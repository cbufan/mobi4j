package com.xc.mobi4j;

import java.util.Arrays;

/**
 * 处理每条正文记录末尾的“trailing entries”。
 *
 * <p>MOBI 头的 Extra Data Flags 表示正文记录末尾附加了哪些数据（多字节字符重叠、
 * 索引信息等）。这些字节不属于正文，解压前必须去掉。除 bit 0 外的条目格式为
 * <code>数据 + 反向编码的长度</code>，长度包含长度字段自身。</p>
 */
final class TrailingData {

    private TrailingData() {
    }

    static byte[] trim(byte[] data, int flags) {
        if (flags == 0 || data.length == 0) {
            return data;
        }
        int end = data.length;

        // 条目按 bit 顺序从正文侧向记录末尾排列，bit 0 最靠近正文。
        // 因此从最高位开始，从记录末尾向前剥离。
        for (int bit = 15; bit >= 1; bit--) {
            if ((flags & (1 << bit)) == 0) {
                continue;
            }
            int[] encoded = readBackwardLength(data, end - 1);
            if (encoded == null) {
                break;
            }
            int remove = Math.max(encoded[0], encoded[1]);
            if (remove <= 0 || remove > end) {
                break;
            }
            end -= remove;
        }

        if ((flags & 1) != 0 && end > 0) {
            // 多字节字符重叠：最后一个字节的低 2 位是重叠字节数 N，
            // 整个条目为 N 个字节 + 1 个计数/标志字节。
            int count = (data[end - 1] & 0x03) + 1;
            if (count <= end) {
                end -= count;
            }
        }

        return end == data.length ? data : Arrays.copyOf(data, end);
    }

    /**
     * 从 index 开始向前读取反向编码的变长整数。
     *
     * @return {值, 消耗的字节数}；编码非法时返回 null
     */
    private static int[] readBackwardLength(byte[] data, int index) {
        int value = 0;
        int shift = 0;
        int position = index;
        while (position >= 0 && shift < 35) {
            int b = data[position--] & 0xFF;
            value |= (b & 0x7F) << shift;
            shift += 7;
            if ((b & 0x80) != 0) {
                int consumed = index - position;
                return new int[]{value, consumed};
            }
        }
        return null;
    }
}
