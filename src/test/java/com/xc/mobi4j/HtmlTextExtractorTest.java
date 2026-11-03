package com.xc.mobi4j;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlTextExtractorTest {

    @Test
    void convertsBlocksAndEntities() {
        String html = "<html><head><title>x</title><style>p{}</style></head><body>"
                + "<h1>标题</h1><p>第一段 &amp; <b>加粗</b></p><p>第二段<br/>换行</p>"
                + "<script>alert(1)</script></body></html>";
        String text = HtmlTextExtractor.toPlainText(html);
        assertTrue(text.contains("标题"));
        assertTrue(text.contains("第一段 & 加粗"));
        assertTrue(text.contains("第二段"));
        assertTrue(text.contains("换行"));
        assertFalse(text.contains("alert"));
        assertFalse(text.contains("<"));
        assertFalse(text.contains("p{}"));
    }

    @Test
    void decodesNumericEntities() {
        assertEquals("A中😀", HtmlTextExtractor.unescapeEntities("&#65;&#x4E2D;&#128512;"));
    }

    @Test
    void keepsUnknownEntities() {
        assertEquals("&unknown;", HtmlTextExtractor.unescapeEntities("&unknown;"));
    }

    @Test
    void collapsesWhitespaceAndBlankLines() {
        String text = HtmlTextExtractor.toPlainText("<p>a   b</p><p></p><p></p><p>c</p>");
        assertEquals("a b\n\nc", text);
    }
}
