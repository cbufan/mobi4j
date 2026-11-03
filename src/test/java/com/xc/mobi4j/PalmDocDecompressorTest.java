package com.xc.mobi4j;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PalmDocDecompressorTest {

    @Test
    void decodesLiteralsAndRuns() {
        byte[] input = {0x00, 0x05, 'a', 'b', 'c', 'd', 'e', 'X', 0x01, (byte) 0xE4};
        byte[] expected = {0x00, 'a', 'b', 'c', 'd', 'e', 'X', (byte) 0xE4};
        assertArrayEquals(expected, PalmDocDecompressor.decompress(input, 100));
    }

    @Test
    void decodesBackReference() {
        // "abc" + (距离 3, 长度 3) -> "abcabc"
        byte[] input = {'a', 'b', 'c', (byte) 0x80, 0x18};
        assertArrayEquals("abcabc".getBytes(StandardCharsets.ISO_8859_1),
                PalmDocDecompressor.decompress(input, 100));
    }

    @Test
    void decodesOverlappingBackReference() {
        // "a" + (距离 1, 长度 5) -> "aaaaaa"
        byte[] input = {'a', (byte) 0x80, 0x0A};
        assertEquals("aaaaaa", new String(PalmDocDecompressor.decompress(input, 100),
                StandardCharsets.ISO_8859_1));
    }

    @Test
    void decodesSpaceEncoding() {
        // 0xC1 表示 空格 + (0xC1 ^ 0x80) = 'A'
        byte[] input = {(byte) 0xC1};
        assertEquals(" A", new String(PalmDocDecompressor.decompress(input, 100),
                StandardCharsets.ISO_8859_1));
    }

    @Test
    void respectsOutputLimit() {
        byte[] input = "hello world".getBytes(StandardCharsets.ISO_8859_1);
        assertEquals(5, PalmDocDecompressor.decompress(input, 5).length);
    }
}
