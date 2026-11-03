package com.xc.mobi4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MOBI / AZW / PRC 解析器。
 *
 * <pre>{@code
 * MobiBook book = MobiParser.parse(Path.of("book.mobi"));
 * System.out.println(book.getMetadata().getTitle());
 * System.out.println(book.getMetadata().getAuthors());
 * System.out.println(book.getText());
 * }</pre>
 *
 * <p>支持 PalmDOC 压缩（compression=1/2）的 MOBI7 与 KF8 文件，能读取 EXTH 元数据、
 * 正文 HTML/纯文本以及内嵌图片。受 DRM 保护的文件仍可读取元数据，但正文不可用；
 * HUFF/CDIC 压缩（compression=17480）目前不解析正文。两种情况都会记录在
 * {@link MobiBook#getWarnings()} 中。</p>
 */
public final class MobiParser {

    private MobiParser() {
    }

    /** 解析文件，读取元数据和正文。 */
    public static MobiBook parse(Path file) throws IOException {
        return parse(Files.readAllBytes(file), false);
    }

    /** 解析内存中的文件内容。 */
    public static MobiBook parse(byte[] data) throws IOException {
        return parse(data, false);
    }

    public static MobiBook parse(InputStream input) throws IOException {
        return parse(input.readAllBytes(), false);
    }

    /** 只读取元数据（不解析正文，速度更快）。 */
    public static MobiMetadata readMetadata(Path file) throws IOException {
        return parse(Files.readAllBytes(file), true).getMetadata();
    }

    public static MobiMetadata readMetadata(byte[] data) throws IOException {
        return parse(data, true).getMetadata();
    }

    private static MobiBook parse(byte[] data, boolean metadataOnly) throws IOException {
        PalmDatabase database = PalmDatabase.parse(data);
        MobiHeader header = MobiHeader.parse(database.record(0), database.recordCount());
        Charset charset = Charsets.forMobiEncoding(header.textEncoding);

        List<String> warnings = new ArrayList<>();
        if (!header.hasMobiHeader) {
            warnings.add("未找到 MOBI 头，按纯 PalmDOC 处理，只能获得数据库名和正文");
        }
        if (header.encryptionType != 0) {
            warnings.add("文件受 DRM 保护（encryption=" + header.encryptionType + "），只能读取元数据");
        }
        boolean supportedCompression = header.compression == 1 || header.compression == 2;
        if (!supportedCompression) {
            warnings.add("暂不支持压缩方式 " + header.compression
                    + (header.compression == 17480 ? " (HUFF/CDIC)" : "") + "，无法读取正文");
        }

        List<MobiImage> images = extractImages(database, header);
        MobiImage cover = findImage(header, images, ExthTags.COVER_OFFSET);
        MobiImage thumbnail = findImage(header, images, ExthTags.THUMBNAIL_OFFSET);

        String html = "";
        if (!metadataOnly && header.encryptionType == 0 && supportedCompression) {
            html = new String(readRawText(database, header), charset);
        }
        String text = HtmlTextExtractor.toPlainText(html);

        MobiMetadata metadata = buildMetadata(database, header, charset, cover, thumbnail, data.length);
        return new MobiBook(metadata, html, text, images, warnings);
    }

    private static MobiMetadata buildMetadata(PalmDatabase database, MobiHeader header, Charset charset,
                                               MobiImage cover, MobiImage thumbnail, long fileSize) {
        MobiMetadata.Builder builder = MobiMetadata.builder();

        String title = header.fullName;
        if (title == null || title.isBlank()) {
            title = firstExthString(header, ExthTags.UPDATED_TITLE, charset);
        }
        if (title == null || title.isBlank()) {
            title = database.name();
        }
        builder.title = title == null || title.isBlank() ? "(未命名)" : title;

        for (String author : exthStrings(header, ExthTags.AUTHOR, charset)) {
            if (!author.isBlank()) {
                builder.authors.add(author);
            }
        }
        builder.publisher = firstExthString(header, ExthTags.PUBLISHER, charset);
        builder.imprint = firstExthString(header, ExthTags.IMPRINT, charset);
        builder.description = firstExthString(header, ExthTags.DESCRIPTION, charset);
        builder.isbn = firstExthString(header, ExthTags.ISBN, charset);
        for (String subject : exthStrings(header, ExthTags.SUBJECT, charset)) {
            if (!subject.isBlank()) {
                builder.subjects.add(subject);
            }
        }
        builder.publishDate = firstExthString(header, ExthTags.PUBLISHING_DATE, charset);
        builder.contributor = firstExthString(header, ExthTags.CONTRIBUTOR, charset);
        builder.rights = firstExthString(header, ExthTags.RIGHTS, charset);
        builder.source = firstExthString(header, ExthTags.SOURCE, charset);

        String asin = firstExthString(header, ExthTags.ASIN, charset);
        if (asin == null || asin.isBlank()) {
            asin = firstExthString(header, ExthTags.ASIN_2, charset);
        }
        builder.asin = asin;
        builder.bookType = firstExthString(header, ExthTags.BOOK_TYPE, charset);

        String language = firstExthString(header, ExthTags.LANGUAGE, charset);
        if (language == null || language.isBlank()) {
            language = MobiMetadata.languageFromLocale(header.locale);
        }
        builder.language = language;

        builder.creatorSoftware = creatorSoftware(header);
        builder.charset = charset;
        builder.textEncoding = header.textEncoding;
        builder.compression = header.compression;
        builder.encryptionType = header.encryptionType;
        builder.mobiType = header.mobiType;
        builder.fileVersion = header.fileVersion;
        builder.locale = header.locale;
        builder.recordCount = database.recordCount();
        builder.textRecordCount = header.textRecordCount;
        builder.textLength = header.textLength;
        builder.fileSize = fileSize;
        builder.kf8Boundary = header.kf8Boundary;
        builder.coverImage = cover;
        builder.thumbnailImage = thumbnail;
        builder.exthEntries.addAll(header.exth);
        return builder.build();
    }

    private static String creatorSoftware(MobiHeader header) {
        String name = null;
        for (ExthEntry entry : header.exth) {
            if (entry.getType() == ExthTags.CREATOR_SOFTWARE) {
                name = switch (entry.asInt()) {
                    case 1 -> "mobigen";
                    case 2 -> "Mobipocket Creator";
                    case 200 -> "kindlegen (Windows)";
                    case 201 -> "kindlegen (Linux)";
                    case 202 -> "kindlegen (Mac)";
                    default -> null;
                };
                break;
            }
        }
        if (name == null) {
            return null;
        }

        int major = exthInt(header, ExthTags.CREATOR_MAJOR_VERSION, 0);
        int minor = exthInt(header, ExthTags.CREATOR_MINOR_VERSION, 0);
        int build = exthInt(header, ExthTags.CREATOR_BUILD_NUMBER, 0);
        if (major == 0 && minor == 0 && build == 0) {
            return name;
        }
        StringBuilder version = new StringBuilder(name).append(' ');
        version.append(major).append('.').append(minor);
        if (build != 0) {
            version.append('.').append(build);
        }
        return version.toString();
    }

    private static List<String> exthStrings(MobiHeader header, int type, Charset charset) {
        List<String> values = new ArrayList<>();
        for (ExthEntry entry : header.exth) {
            if (entry.getType() == type) {
                values.add(entry.asString(charset));
            }
        }
        return values;
    }

    private static String firstExthString(MobiHeader header, int type, Charset charset) {
        for (ExthEntry entry : header.exth) {
            if (entry.getType() == type) {
                String value = entry.asString(charset);
                return value.isBlank() ? null : value;
            }
        }
        return null;
    }

    private static int exthInt(MobiHeader header, int type, int defaultValue) {
        for (ExthEntry entry : header.exth) {
            if (entry.getType() == type) {
                return entry.asInt();
            }
        }
        return defaultValue;
    }

    /**
     * 按 PalmDOC 头中的记录数拼接并解压正文。只取 textLength 字节，因为最后一条记录
     * 可能带有 FLIS/FCIS 等附加数据。
     */
    private static byte[] readRawText(PalmDatabase database, MobiHeader header) {
        int expected = header.textLength;
        boolean hasExpected = expected > 0 && expected <= 256 * 1024 * 1024;
        int limit = hasExpected ? expected : Integer.MAX_VALUE;

        ByteArrayOutputStream output = new ByteArrayOutputStream(
                hasExpected ? Math.min(expected, 8 * 1024 * 1024) : 8192);
        int start = Math.max(1, header.firstContentRecord);
        int count = header.textRecordCount > 0
                ? header.textRecordCount
                : Math.max(0, database.recordCount() - start);
        int total = 0;

        for (int i = 0; i < count && total < limit; i++) {
            int recordIndex = start + i;
            if (recordIndex >= database.recordCount()) {
                break;
            }
            byte[] trimmed = TrailingData.trim(database.record(recordIndex), header.extraDataFlags);
            byte[] plain;
            if (header.compression == 2) {
                plain = PalmDocDecompressor.decompress(trimmed, limit - total);
            } else {
                plain = trimmed;
                if (plain.length > limit - total) {
                    plain = Arrays.copyOf(plain, limit - total);
                }
            }
            output.write(plain, 0, plain.length);
            total += plain.length;
        }

        byte[] raw = output.toByteArray();
        return hasExpected && raw.length > expected ? Arrays.copyOf(raw, expected) : raw;
    }

    /**
     * 图片从 firstImageIndex 开始，每条记录一张。逐条用文件头魔数识别，
     * 不依赖记录类型。
     */
    private static List<MobiImage> extractImages(PalmDatabase database, MobiHeader header) {
        if (header.firstImageIndex < 1 || header.firstImageIndex >= database.recordCount()) {
            return List.of();
        }
        List<MobiImage> images = new ArrayList<>();
        for (int i = header.firstImageIndex; i < database.recordCount(); i++) {
            byte[] record = database.record(i);
            String format = MobiImage.sniff(record);
            if (format != null) {
                images.add(new MobiImage(i, format, record));
            }
        }
        return images;
    }

    /** EXTH 201/202 存的是相对 firstImageIndex 的偏移。 */
    private static MobiImage findImage(MobiHeader header, List<MobiImage> images, int exthType) {
        int offset = exthInt(header, exthType, -1);
        if (offset < 0 || images.isEmpty()) {
            return null;
        }
        if (header.firstImageIndex < 0) {
            return offset < images.size() ? images.get(offset) : null;
        }
        int recordIndex = header.firstImageIndex + offset;
        for (MobiImage image : images) {
            if (image.getRecordIndex() == recordIndex) {
                return image;
            }
        }
        return null;
    }
}
