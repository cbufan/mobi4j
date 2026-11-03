package com.xc.mobi4j;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TrailingDataTest {

    @Test
    void noFlagsReturnsSameData() {
        byte[] data = "hello".getBytes(StandardCharsets.ISO_8859_1);
        assertArrayEquals(data, TrailingData.trim(data, 0));
    }

    @Test
    void stripsOneBackwardEncodedEntry() {
        // bit 1：<3 字节数据><长度 4>，长度包含自身
        byte[] data = concat("hello".getBytes(StandardCharsets.ISO_8859_1), new byte[]{1, 2, 3, (byte) 0x84});
        assertArrayEquals("hello".getBytes(StandardCharsets.ISO_8859_1), TrailingData.trim(data, 0x02));
    }

    @Test
    void stripsMultipleEntriesFromOutsideIn() {
        // bit 1 在最内层，bit 2 在外层
        byte[] data = concat(
                "hello".getBytes(StandardCharsets.ISO_8859_1),
                new byte[]{1, 2, 3, (byte) 0x84},       // bit 1 条目
                new byte[]{9, 9, (byte) 0x83});         // bit 2 条目
        assertArrayEquals("hello".getBytes(StandardCharsets.ISO_8859_1), TrailingData.trim(data, 0x06));
    }

    @Test
    void stripsMultibyteOverlap() {
        // bit 0：3 个重叠字节 + 计数/标志字节 0x03
        byte[] data = concat("hello".getBytes(StandardCharsets.ISO_8859_1),
                new byte[]{(byte) 0xE4, (byte) 0xB8, (byte) 0xAD, 0x03});
        assertArrayEquals("hello".getBytes(StandardCharsets.ISO_8859_1), TrailingData.trim(data, 0x01));
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.write(part, 0, part.length);
        }
        return out.toByteArray();
    }
}
