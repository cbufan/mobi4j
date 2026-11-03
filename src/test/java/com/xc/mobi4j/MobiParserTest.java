package com.xc.mobi4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobiParserTest {

    @Test
    void parsesMetadataContentAndImages() throws Exception {
        MobiBook book = MobiParser.parse(new TestMobiBuilder().build());
        MobiMetadata metadata = book.getMetadata();

        assertEquals("测试书名", metadata.getTitle());
        assertEquals(List.of("张三"), metadata.getAuthors());
        assertEquals("测试出版社", metadata.getPublisher());
        assertEquals("一本用于测试的书", metadata.getDescription());
        assertEquals("9780000000001", metadata.getIsbn());
        assertEquals(List.of("小说"), metadata.getSubjects());
        assertEquals("zh", metadata.getLanguage());
        assertEquals("kindlegen (Linux) 2.9", metadata.getCreatorSoftware());
        assertEquals(2, metadata.getCompression());
        assertEquals("PalmDOC", metadata.getCompressionName());
        assertEquals(65001, metadata.getTextEncoding());
        assertFalse(metadata.isEncrypted());
        assertEquals(4, metadata.getRecordCount());
        assertEquals(2, metadata.getTextRecordCount());
        assertEquals(11, metadata.getExthEntries().size());
        assertTrue(metadata.getExthValues(ExthTags.AUTHOR).contains("张三"));

        assertTrue(book.getWarnings().isEmpty());
        assertTrue(book.isContentAvailable());
        assertTrue(book.getHtml().contains("<h1>第一章</h1>"));
        assertTrue(book.getText().contains("第一章"));
        assertTrue(book.getText().contains("你好，世界！"));
        assertTrue(book.getText().contains("Hello world & more."));
        assertFalse(book.getText().contains("<p>"));

        assertEquals(1, book.getImages().size());
        MobiImage cover = metadata.getCoverImage();
        assertNotNull(cover);
        assertEquals(3, cover.getRecordIndex());
        assertEquals("jpeg", cover.getFormat());
        assertEquals("jpg", cover.getExtension());
        assertEquals("image/jpeg", cover.getMimeType());
        assertEquals(cover, metadata.getThumbnailImage());
    }

    @Test
    void stripsTrailingEntriesFromTextRecords() throws Exception {
        TestMobiBuilder builder = new TestMobiBuilder();
        builder.withTrailingData = true;
        MobiBook book = MobiParser.parse(builder.build());

        assertTrue(book.isContentAvailable());
        assertTrue(book.getText().contains("第一章"));
        assertTrue(book.getText().contains("你好，世界！"));
        assertFalse(book.getText().contains("\u0011"));
        assertFalse(book.getText().contains("\u0083"));
    }

    @Test
    void parsesUncompressedText() throws Exception {
        TestMobiBuilder builder = new TestMobiBuilder();
        builder.compression = 1;
        MobiBook book = MobiParser.parse(builder.build());

        assertEquals(1, book.getMetadata().getCompression());
        assertEquals("none", book.getMetadata().getCompressionName());
        assertTrue(book.getText().contains("你好，世界！"));
    }

    @Test
    void parsesPlainPalmDocWithoutMobiHeader() throws Exception {
        TestMobiBuilder builder = new TestMobiBuilder();
        builder.includeMobiHeader = false;
        builder.title = "PlainDoc";
        builder.html = "<html><body><p>Hello plain PalmDOC.</p></body></html>";
        MobiBook book = MobiParser.parse(builder.build());

        assertEquals("PlainDoc", book.getMetadata().getTitle());
        assertEquals(0, book.getMetadata().getMobiType());
        assertTrue(book.getWarnings().stream().anyMatch(w -> w.contains("MOBI 头")));
        assertTrue(book.getText().contains("Hello plain PalmDOC."));
    }

    @Test
    void parsesFromFile(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("book.mobi");
        Files.write(file, new TestMobiBuilder().build());

        MobiBook book = MobiParser.parse(file);
        assertEquals("测试书名", book.getMetadata().getTitle());
        assertTrue(book.getText().contains("第一章"));

        Path textFile = directory.resolve("out.txt");
        book.writeText(textFile);
        assertEquals(book.getText(), Files.readString(textFile));
    }

    @Test
    void metadataOnlySkipsContent() throws Exception {
        MobiMetadata metadata = MobiParser.readMetadata(new TestMobiBuilder().build());
        assertEquals("测试书名", metadata.getTitle());
        assertNotNull(metadata.getCoverImage());
    }

    @Test
    void reportsDrmAndLeavesContentEmpty() throws Exception {
        TestMobiBuilder builder = new TestMobiBuilder();
        builder.encryptionType = 2;
        MobiBook book = MobiParser.parse(builder.build());

        assertTrue(book.getMetadata().isEncrypted());
        assertFalse(book.isContentAvailable());
        assertTrue(book.getWarnings().stream().anyMatch(w -> w.contains("DRM")));
        assertEquals("测试书名", book.getMetadata().getTitle());
    }

    @Test
    void reportsUnsupportedHuffCdic() throws Exception {
        TestMobiBuilder builder = new TestMobiBuilder();
        builder.compression = 17480;
        MobiBook book = MobiParser.parse(builder.build());

        assertFalse(book.isContentAvailable());
        assertTrue(book.getWarnings().stream().anyMatch(w -> w.contains("HUFF/CDIC")));
    }

    @Test
    void rejectsNonPdbData() {
        assertThrows(MobiException.class, () -> MobiParser.parse(new byte[10]));
        assertThrows(MobiException.class, () -> MobiParser.parse(new byte[200]));
    }
}
