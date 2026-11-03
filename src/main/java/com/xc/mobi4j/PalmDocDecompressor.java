package com.xc.mobi4j;

/**
 * PalmDOC LZ77 解压。
 *
 * <pre>
 * 0x00          输出一个 0 字节
 * 0x01-0x08     后面 N 个字节原样输出
 * 0x09-0x7F     输出该字节本身
 * 0x80-0xBF     双字节回溯引用：距离 = ((c &amp; 0x3F) &lt;&lt; 5) | (c2 &gt;&gt; 3)，
 *               长度 = (c2 &amp; 0x07) + 3（可能重叠，需要逐字节复制）
 * 0xC0-0xFF     输出空格 + (c ^ 0x80)
 * </pre>
 */
final class PalmDocDecompressor {

    private PalmDocDecompressor() {
    }

    static byte[] decompress(byte[] input, int maxOutput) {
        if (input.length == 0 || maxOutput <= 0) {
            return new byte[0];
        }
        OutputBuffer output = new OutputBuffer(Math.min(maxOutput, input.length * 4 + 32));
        int i = 0;

        while (i < input.length && output.size() < maxOutput) {
            int c = input[i++] & 0xFF;

            if (c == 0x00) {
                output.add(0);
            } else if (c <= 0x08) {
                int count = Math.min(c, input.length - i);
                output.add(input, i, count);
                i += count;
            } else if (c <= 0x7F) {
                output.add(c);
            } else if (c <= 0xBF) {
                if (i >= input.length) {
                    break;
                }
                int c2 = input[i++] & 0xFF;
                int distance = ((c & 0x3F) << 5) | (c2 >>> 3);
                int length = (c2 & 0x07) + 3;
                if (distance == 0 || distance > output.size()) {
                    continue;
                }
                for (int n = 0; n < length && output.size() < maxOutput; n++) {
                    output.add(output.get(output.size() - distance));
                }
            } else {
                output.add(' ');
                output.add(c ^ 0x80);
            }
        }
        return output.toByteArray();
    }

    private static final class OutputBuffer {
        private byte[] buffer;
        private int size;

        OutputBuffer(int initialCapacity) {
            this.buffer = new byte[Math.max(64, initialCapacity)];
        }

        int size() {
            return size;
        }

        void add(int value) {
            ensure(1);
            buffer[size++] = (byte) value;
        }

        void add(byte[] source, int offset, int length) {
            ensure(length);
            System.arraycopy(source, offset, buffer, size, length);
            size += length;
        }

        byte get(int index) {
            return buffer[index];
        }

        byte[] toByteArray() {
            return java.util.Arrays.copyOf(buffer, size);
        }

        private void ensure(int extra) {
            int required = size + extra;
            if (required <= buffer.length) {
                return;
            }
            int capacity = Math.max(required, buffer.length + (buffer.length >> 1) + 16);
            buffer = java.util.Arrays.copyOf(buffer, capacity);
        }
    }
}
