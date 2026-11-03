package com.xc.mobi4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 把 MOBI 的 HTML 正文转换成可读的纯文本。
 *
 * <p>只做轻量处理：去掉 head/script/style，把块级标签和 br 转成换行，
 * 解码常见 HTML 实体，压缩空白。不依赖任何第三方库。</p>
 */
final class HtmlTextExtractor {

    private static final Map<String, String> ENTITIES = new HashMap<>();

    static {
        ENTITIES.put("amp", "&");
        ENTITIES.put("lt", "<");
        ENTITIES.put("gt", ">");
        ENTITIES.put("quot", "\"");
        ENTITIES.put("apos", "'");
        ENTITIES.put("nbsp", "\u00A0");
        ENTITIES.put("ensp", " ");
        ENTITIES.put("emsp", " ");
        ENTITIES.put("thinsp", " ");
        ENTITIES.put("shy", "");
        ENTITIES.put("copy", "\u00A9");
        ENTITIES.put("reg", "\u00AE");
        ENTITIES.put("trade", "\u2122");
        ENTITIES.put("hellip", "\u2026");
        ENTITIES.put("mdash", "\u2014");
        ENTITIES.put("ndash", "\u2013");
        ENTITIES.put("lsquo", "\u2018");
        ENTITIES.put("rsquo", "\u2019");
        ENTITIES.put("ldquo", "\u201C");
        ENTITIES.put("rdquo", "\u201D");
        ENTITIES.put("laquo", "\u00AB");
        ENTITIES.put("raquo", "\u00BB");
        ENTITIES.put("bull", "\u2022");
        ENTITIES.put("middot", "\u00B7");
        ENTITIES.put("dagger", "\u2020");
        ENTITIES.put("Dagger", "\u2021");
        ENTITIES.put("deg", "\u00B0");
        ENTITIES.put("plusmn", "\u00B1");
        ENTITIES.put("times", "\u00D7");
        ENTITIES.put("divide", "\u00F7");
        ENTITIES.put("frac12", "\u00BD");
        ENTITIES.put("frac14", "\u00BC");
        ENTITIES.put("frac34", "\u00BE");
        ENTITIES.put("sup2", "\u00B2");
        ENTITIES.put("sup3", "\u00B3");
        ENTITIES.put("micro", "\u00B5");
        ENTITIES.put("para", "\u00B6");
        ENTITIES.put("sect", "\u00A7");
        ENTITIES.put("euro", "\u20AC");
        ENTITIES.put("pound", "\u00A3");
        ENTITIES.put("yen", "\u00A5");
        ENTITIES.put("cent", "\u00A2");
        ENTITIES.put("eacute", "\u00E9");
        ENTITIES.put("egrave", "\u00E8");
        ENTITIES.put("agrave", "\u00E0");
        ENTITIES.put("ccedil", "\u00E7");
        ENTITIES.put("uuml", "\u00FC");
        ENTITIES.put("ouml", "\u00F6");
        ENTITIES.put("auml", "\u00E4");
        ENTITIES.put("szlig", "\u00DF");
        ENTITIES.put("ntilde", "\u00F1");
        ENTITIES.put("iexcl", "\u00A1");
        ENTITIES.put("iquest", "\u00BF");
    }

    private HtmlTextExtractor() {
    }

    static String toPlainText(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        String s = html;
        s = s.replaceAll("(?is)<(script|style|head)\\b[^>]*>.*?</\\1\\s*>", "\n");
        s = s.replaceAll("(?is)<!--.*?-->", "");
        s = s.replaceAll("(?i)<\\s*(br|hr)\\b[^>]*>", "\n");
        s = s.replaceAll("(?i)<\\s*mbp:pagebreak\\s*/?\\s*>", "\n\n");
        s = s.replaceAll("(?i)</\\s*(p|div|h[1-6]|li|tr|blockquote|section|article|table|ul|ol|dl|dt|dd"
                + "|pre|figure|figcaption|header|footer|aside|nav)\\s*>", "\n");
        s = s.replaceAll("(?i)</\\s*(td|th)\\s*>", " ");
        s = s.replaceAll("(?s)<[^>]*>", "");
        s = unescapeEntities(s);

        s = s.replace('\u0000', ' ').replace('\u00A0', ' ');
        s = s.replace("\r\n", "\n").replace('\r', '\n');

        String[] rawLines = s.split("\n", -1);
        List<String> lines = new ArrayList<>(rawLines.length);
        boolean previousBlank = true;
        for (String rawLine : rawLines) {
            String line = rawLine.replaceAll("[\\t\\x0B\\f ]+", " ").trim();
            if (line.isEmpty()) {
                if (!previousBlank) {
                    lines.add("");
                    previousBlank = true;
                }
            } else {
                lines.add(line);
                previousBlank = false;
            }
        }
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return String.join("\n", lines);
    }

    static String unescapeEntities(String text) {
        int amp = text.indexOf('&');
        if (amp < 0) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        out.append(text, 0, amp);
        int i = amp;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c != '&') {
                out.append(c);
                i++;
                continue;
            }
            int semicolon = text.indexOf(';', i + 1);
            if (semicolon < 0 || semicolon - i > 12) {
                out.append(c);
                i++;
                continue;
            }
            String entity = text.substring(i + 1, semicolon);
            String replacement = resolveEntity(entity);
            if (replacement == null) {
                out.append(c);
                i++;
            } else {
                out.append(replacement);
                i = semicolon + 1;
            }
        }
        return out.toString();
    }

    private static String resolveEntity(String entity) {
        if (entity.isEmpty()) {
            return null;
        }
        if (entity.charAt(0) == '#') {
            try {
                int codePoint;
                if (entity.length() > 1 && (entity.charAt(1) == 'x' || entity.charAt(1) == 'X')) {
                    codePoint = Integer.parseInt(entity.substring(2), 16);
                } else {
                    codePoint = Integer.parseInt(entity.substring(1));
                }
                if (codePoint > 0 && codePoint <= 0x10FFFF
                        && !(codePoint >= 0xD800 && codePoint <= 0xDFFF)) {
                    return new String(Character.toChars(codePoint));
                }
            } catch (NumberFormatException ignored) {
                return null;
            }
            return null;
        }
        return ENTITIES.get(entity);
    }
}
