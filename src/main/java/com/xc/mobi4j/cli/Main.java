package com.xc.mobi4j.cli;

import com.xc.mobi4j.ExthEntry;
import com.xc.mobi4j.ExthTags;
import com.xc.mobi4j.MobiBook;
import com.xc.mobi4j.MobiImage;
import com.xc.mobi4j.MobiMetadata;
import com.xc.mobi4j.MobiParser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * 命令行入口：
 *
 * <pre>
 * java -jar mobi4j.jar book.mobi
 * java -jar mobi4j.jar book.mobi --text
 * java -jar mobi4j.jar book.mobi --out ./output
 * java -jar mobi4j.jar book.mobi --json
 * </pre>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            int code = run(args);
            if (code != 0) {
                System.exit(code);
            }
        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            System.exit(1);
        }
    }

    private static int run(String[] args) throws IOException {
        if (args.length == 0) {
            usage();
            return 2;
        }

        Path input = null;
        Path outDir = null;
        boolean metadataOnly = false;
        boolean textOnly = false;
        boolean htmlOnly = false;
        boolean json = false;
        boolean showExth = false;
        Integer limit = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-h", "--help" -> {
                    usage();
                    return 0;
                }
                case "-o", "--out" -> outDir = Path.of(requireValue(args, ++i, arg));
                case "--metadata" -> metadataOnly = true;
                case "--text" -> textOnly = true;
                case "--html" -> htmlOnly = true;
                case "--json" -> json = true;
                case "--exth" -> showExth = true;
                case "--limit" -> {
                    String value = requireValue(args, ++i, arg);
                    try {
                        limit = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("--limit 需要正整数: " + value);
                    }
                    if (limit <= 0) {
                        throw new IllegalArgumentException("--limit 需要正整数: " + value);
                    }
                }
                default -> {
                    if (arg.startsWith("-")) {
                        throw new IllegalArgumentException("未知选项: " + arg);
                    }
                    if (input != null) {
                        throw new IllegalArgumentException("只能指定一个输入文件");
                    }
                    input = Path.of(arg);
                }
            }
        }

        if (input == null) {
            usage();
            return 2;
        }
        if (!Files.isRegularFile(input)) {
            throw new IllegalArgumentException("文件不存在: " + input);
        }

        MobiBook book = MobiParser.parse(input);
        for (String warning : book.getWarnings()) {
            System.err.println("[警告] " + warning);
        }

        if (json) {
            System.out.println(toJson(book, input));
            return 0;
        }

        if (outDir != null) {
            export(book, input, outDir);
        }

        boolean anyPrintMode = metadataOnly || textOnly || htmlOnly;
        if (!anyPrintMode) {
            printMetadata(book.getMetadata(), input);
            if (showExth) {
                printExth(book.getMetadata());
            }
            printPreview(book, limit == null ? 2000 : limit);
        } else {
            if (metadataOnly) {
                printMetadata(book.getMetadata(), input);
                if (showExth) {
                    printExth(book.getMetadata());
                }
            }
            if (textOnly) {
                printContent(book.getText(), limit);
            }
            if (htmlOnly) {
                printContent(book.getHtml(), limit);
            }
        }
        return 0;
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException(option + " 缺少参数");
        }
        return args[index];
    }

    private static void export(MobiBook book, Path input, Path outDir) throws IOException {
        Files.createDirectories(outDir);
        String base = stripExtension(input.getFileName().toString()).replaceAll("[\\\\/:*?\"<>|]", "_");

        if (book.isContentAvailable()) {
            Path textFile = outDir.resolve(base + ".txt");
            Path htmlFile = outDir.resolve(base + ".html");
            book.writeText(textFile);
            book.writeHtml(htmlFile);
            System.out.println("已导出正文: " + textFile.toAbsolutePath());
            System.out.println("已导出 HTML: " + htmlFile.toAbsolutePath());
        } else {
            System.out.println("正文不可用，跳过正文导出");
        }

        if (!book.getImages().isEmpty()) {
            Path imageDir = outDir.resolve("images");
            Files.createDirectories(imageDir);
            int index = 1;
            for (MobiImage image : book.getImages()) {
                Path file = imageDir.resolve(String.format(Locale.ROOT, "image-%03d.%s", index++, image.getExtension()));
                Files.write(file, image.getData());
            }
            System.out.println("已导出图片: " + imageDir.toAbsolutePath() + "（" + book.getImages().size() + " 张）");
        }
    }

    private static void printMetadata(MobiMetadata metadata, Path input) {
        System.out.println("===== 文件 =====");
        printField("路径", input.toAbsolutePath().toString());
        printField("标题", metadata.getTitle());
        printField("作者", join(metadata.getAuthors()));
        printField("出版社", metadata.getPublisher());
        printField("出版日期", metadata.getPublishDate());
        printField("语言", metadata.getLanguage());
        printField("ISBN", metadata.getIsbn());
        printField("主题", join(metadata.getSubjects()));
        printField("ASIN", metadata.getAsin());
        printField("书籍类型", metadata.getBookType());
        printField("版权", metadata.getRights());
        printField("来源", metadata.getSource());
        printField("贡献者", metadata.getContributor());
        printField("生成工具", metadata.getCreatorSoftware());
        printField("简介", metadata.getDescription());

        System.out.println();
        System.out.println("===== 技术信息 =====");
        printField("格式", "MOBI type=" + metadata.getMobiType() + ", version=" + metadata.getFileVersion());
        printField("压缩", metadata.getCompressionName());
        printField("字符集", metadata.getCharset().name() + " (" + metadata.getTextEncoding() + ")");
        printField("加密", metadata.isEncrypted() ? "是（DRM）" : "否");
        printField("记录数", metadata.getRecordCount() + "（正文 " + metadata.getTextRecordCount() + " 条）");
        printField("正文长度", metadata.getTextLength() + " 字节");
        printField("文件大小", formatSize(metadata.getFileSize()));
        if (metadata.getKf8Boundary() >= 0) {
            printField("KF8 边界", "record " + metadata.getKf8Boundary());
        }
        MobiImage cover = metadata.getCoverImage();
        if (cover != null) {
            printField("封面", cover.getFormat() + ", " + formatSize(cover.getSize())
                    + "（record " + cover.getRecordIndex() + "）");
        }
        printField("EXTH", metadata.getExthEntries().size() + " 条");
    }

    private static void printExth(MobiMetadata metadata) {
        System.out.println();
        System.out.println("===== EXTH 记录 =====");
        for (ExthEntry entry : metadata.getExthEntries()) {
            System.out.printf(Locale.ROOT, "%4d  %-24s %s%n",
                    entry.getType(), ExthTags.name(entry.getType()),
                    displayValue(entry, metadata.getCharset()));
        }
    }

    /** 二进制字段（如 4 字节整数）解码后通常是控制字符，改用十六进制展示。 */
    private static String displayValue(ExthEntry entry, Charset charset) {
        String value = entry.asString(charset);
        if (value.chars().anyMatch(c -> c < 0x20 && c != '\t')) {
            return "0x" + hex(entry.getData());
        }
        return value;
    }

    private static void printPreview(MobiBook book, int limit) {
        if (!book.isContentAvailable()) {
            System.out.println();
            System.out.println("（正文不可用，原因见上方警告）");
            return;
        }
        String text = book.getText();
        System.out.println();
        System.out.println("===== 正文预览 =====");
        boolean truncated = text.length() > limit;
        System.out.println(truncated ? text.substring(0, limit) : text);
        if (truncated) {
            System.out.println();
            System.out.println("...（已截断，共 " + text.length() + " 字符。使用 --text 输出全文，"
                    + "或 --out <目录> 导出文件）");
        }
    }

    private static void printContent(String content, Integer limit) {
        if (content.isEmpty()) {
            System.err.println("（内容为空，原因见上方警告）");
            return;
        }
        if (limit != null && content.length() > limit) {
            System.out.println(content.substring(0, limit));
            System.err.println("（已截断至 " + limit + " 字符，共 " + content.length() + " 字符）");
        } else {
            System.out.println(content);
        }
    }

    private static void printField(String name, String value) {
        if (value != null && !value.isEmpty()) {
            System.out.printf(Locale.ROOT, "%-9s: %s%n", name, value);
        }
    }

    private static String join(List<String> values) {
        return values.isEmpty() ? null : String.join(", ", values);
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String hex(byte[] data) {
        StringBuilder sb = new StringBuilder(Math.min(data.length * 2, 64));
        for (int i = 0; i < data.length && i < 32; i++) {
            sb.append(String.format(Locale.ROOT, "%02x", data[i] & 0xFF));
        }
        if (data.length > 32) {
            sb.append("...");
        }
        return sb.toString();
    }

    private static String toJson(MobiBook book, Path input) {
        MobiMetadata m = book.getMetadata();
        StringBuilder sb = new StringBuilder(4096);
        sb.append("{\n");
        field(sb, "path", input.toAbsolutePath().toString(), true);
        field(sb, "title", m.getTitle(), true);
        array(sb, "authors", m.getAuthors(), true);
        field(sb, "publisher", m.getPublisher(), true);
        field(sb, "imprint", m.getImprint(), true);
        field(sb, "description", m.getDescription(), true);
        field(sb, "isbn", m.getIsbn(), true);
        array(sb, "subjects", m.getSubjects(), true);
        field(sb, "publishDate", m.getPublishDate(), true);
        field(sb, "contributor", m.getContributor(), true);
        field(sb, "rights", m.getRights(), true);
        field(sb, "source", m.getSource(), true);
        field(sb, "asin", m.getAsin(), true);
        field(sb, "bookType", m.getBookType(), true);
        field(sb, "language", m.getLanguage(), true);
        field(sb, "creatorSoftware", m.getCreatorSoftware(), true);
        field(sb, "compression", m.getCompressionName(), true);
        number(sb, "compressionId", m.getCompression(), true);
        field(sb, "charset", m.getCharset().name(), true);
        number(sb, "textEncoding", m.getTextEncoding(), true);
        bool(sb, "encrypted", m.isEncrypted(), true);
        number(sb, "mobiType", m.getMobiType(), true);
        number(sb, "fileVersion", m.getFileVersion(), true);
        field(sb, "languageLocale", String.format(Locale.ROOT, "0x%04X", m.getLocale()), true);
        number(sb, "recordCount", m.getRecordCount(), true);
        number(sb, "textRecordCount", m.getTextRecordCount(), true);
        number(sb, "textLength", m.getTextLength(), true);
        number(sb, "fileSize", m.getFileSize(), true);
        number(sb, "kf8Boundary", m.getKf8Boundary(), true);
        number(sb, "imageCount", book.getImages().size(), true);
        bool(sb, "contentAvailable", book.isContentAvailable(), true);

        sb.append("  \"cover\": ");
        if (m.getCoverImage() == null) {
            sb.append("null,\n");
        } else {
            MobiImage cover = m.getCoverImage();
            sb.append("{\"record\": ").append(cover.getRecordIndex())
                    .append(", \"format\": ").append(quote(cover.getFormat()))
                    .append(", \"size\": ").append(cover.getSize()).append("},\n");
        }

        sb.append("  \"warnings\": [");
        for (int i = 0; i < book.getWarnings().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(quote(book.getWarnings().get(i)));
        }
        sb.append("],\n");

        sb.append("  \"exth\": [\n");
        List<ExthEntry> entries = m.getExthEntries();
        for (int i = 0; i < entries.size(); i++) {
            ExthEntry entry = entries.get(i);
            sb.append("    {\"type\": ").append(entry.getType())
                    .append(", \"name\": ").append(quote(ExthTags.name(entry.getType())))
                    .append(", \"value\": ").append(quote(displayValue(entry, m.getCharset())))
                    .append(", \"length\": ").append(entry.getLength()).append('}');
            sb.append(i + 1 < entries.size() ? ",\n" : "\n");
        }
        sb.append("  ]\n");
        sb.append('}');
        return sb.toString();
    }

    private static void field(StringBuilder sb, String name, String value, boolean comma) {
        sb.append("  ").append(quote(name)).append(": ")
                .append(value == null ? "null" : quote(value))
                .append(comma ? ",\n" : "\n");
    }

    private static void number(StringBuilder sb, String name, long value, boolean comma) {
        sb.append("  ").append(quote(name)).append(": ").append(value)
                .append(comma ? ",\n" : "\n");
    }

    private static void bool(StringBuilder sb, String name, boolean value, boolean comma) {
        sb.append("  ").append(quote(name)).append(": ").append(value)
                .append(comma ? ",\n" : "\n");
    }

    private static void array(StringBuilder sb, String name, List<String> values, boolean comma) {
        sb.append("  ").append(quote(name)).append(": [");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(quote(values.get(i)));
        }
        sb.append(']').append(comma ? ",\n" : "\n");
    }

    private static String quote(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 2);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    private static void usage() {
        String text = """
                用法:
                  java -jar mobi4j.jar <book.mobi> [选项]

                选项:
                  -o, --out <目录>   导出正文 (.txt/.html) 和图片到目录
                      --metadata     只打印元数据
                      --text         打印纯文本正文
                      --html         打印原始 HTML 正文
                      --json         以 JSON 输出元数据和 EXTH
                      --exth         额外打印全部 EXTH 记录
                      --limit <n>    限制输出的字符数
                  -h, --help         显示帮助

                不指定输出选项时，默认打印元数据和正文预览（前 2000 字符）。
                """;
        System.out.print(text);
    }
}
