package com.xc.mobi4j;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MOBI 元数据：标题、作者、出版社、简介、ISBN、封面等，以及文件的技术信息。
 */
public final class MobiMetadata {

    private final String title;
    private final List<String> authors;
    private final String publisher;
    private final String imprint;
    private final String description;
    private final String isbn;
    private final List<String> subjects;
    private final String publishDate;
    private final String contributor;
    private final String rights;
    private final String source;
    private final String asin;
    private final String bookType;
    private final String language;
    private final String creatorSoftware;

    private final Charset charset;
    private final int textEncoding;
    private final int compression;
    private final int encryptionType;
    private final int mobiType;
    private final int fileVersion;
    private final int locale;

    private final int recordCount;
    private final int textRecordCount;
    private final int textLength;
    private final long fileSize;
    private final int kf8Boundary;

    private final MobiImage coverImage;
    private final MobiImage thumbnailImage;
    private final List<ExthEntry> exthEntries;

    private MobiMetadata(Builder b) {
        this.title = b.title;
        this.authors = List.copyOf(b.authors);
        this.publisher = b.publisher;
        this.imprint = b.imprint;
        this.description = b.description;
        this.isbn = b.isbn;
        this.subjects = List.copyOf(b.subjects);
        this.publishDate = b.publishDate;
        this.contributor = b.contributor;
        this.rights = b.rights;
        this.source = b.source;
        this.asin = b.asin;
        this.bookType = b.bookType;
        this.language = b.language;
        this.creatorSoftware = b.creatorSoftware;
        this.charset = b.charset;
        this.textEncoding = b.textEncoding;
        this.compression = b.compression;
        this.encryptionType = b.encryptionType;
        this.mobiType = b.mobiType;
        this.fileVersion = b.fileVersion;
        this.locale = b.locale;
        this.recordCount = b.recordCount;
        this.textRecordCount = b.textRecordCount;
        this.textLength = b.textLength;
        this.fileSize = b.fileSize;
        this.kf8Boundary = b.kf8Boundary;
        this.coverImage = b.coverImage;
        this.thumbnailImage = b.thumbnailImage;
        this.exthEntries = List.copyOf(b.exthEntries);
    }

    static Builder builder() {
        return new Builder();
    }

    /** 书名。MOBI 头的全名字段为空时退回 PDB 数据库名。 */
    public String getTitle() {
        return title;
    }

    /** 作者列表（EXTH 100 可以出现多次）。 */
    public List<String> getAuthors() {
        return authors;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getImprint() {
        return imprint;
    }

    /** 内容简介，可能是带少量 HTML 标签的原始文本。 */
    public String getDescription() {
        return description;
    }

    public String getIsbn() {
        return isbn;
    }

    /** 主题/分类列表（EXTH 105 可以出现多次）。 */
    public List<String> getSubjects() {
        return subjects;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public String getContributor() {
        return contributor;
    }

    public String getRights() {
        return rights;
    }

    public String getSource() {
        return source;
    }

    /** Amazon ASIN（EXTH 113 或 504）。 */
    public String getAsin() {
        return asin;
    }

    /** book-type，例如 comic、children。 */
    public String getBookType() {
        return bookType;
    }

    /** 语言代码，例如 zh、en、ja。优先取 EXTH 524，否则由 locale 推导。 */
    public String getLanguage() {
        return language;
    }

    /** 生成该文件的软件，例如 "kindlegen 2.9"。 */
    public String getCreatorSoftware() {
        return creatorSoftware;
    }

    /** 正文使用的字符集。 */
    public Charset getCharset() {
        return charset;
    }

    /** MOBI 头里声明的 code page，例如 65001 (UTF-8)、1252。 */
    public int getTextEncoding() {
        return textEncoding;
    }

    /** 压缩方式原始值：1=不压缩，2=PalmDOC，17480=HUFF/CDIC。 */
    public int getCompression() {
        return compression;
    }

    /** 压缩方式的可读名称。 */
    public String getCompressionName() {
        return switch (compression) {
            case 1 -> "none";
            case 2 -> "PalmDOC";
            case 17480 -> "HUFF/CDIC";
            default -> "unknown(" + compression + ')';
        };
    }

    /** 0=未加密，1/2=DRM 加密。 */
    public int getEncryptionType() {
        return encryptionType;
    }

    public boolean isEncrypted() {
        return encryptionType != 0;
    }

    /** MOBI 类型：2=普通电子书，248=KF8(AZW3)，257=新闻等。 */
    public int getMobiType() {
        return mobiType;
    }

    /** MOBI 文件格式版本。 */
    public int getFileVersion() {
        return fileVersion;
    }

    /** Windows LCID，例如 0x0804 表示简体中文。 */
    public int getLocale() {
        return locale;
    }

    /** PDB 记录总数。 */
    public int getRecordCount() {
        return recordCount;
    }

    /** 正文使用的记录数。 */
    public int getTextRecordCount() {
        return textRecordCount;
    }

    /** 解压后的正文字节数（解码前）。 */
    public int getTextLength() {
        return textLength;
    }

    /** 文件字节数。 */
    public long getFileSize() {
        return fileSize;
    }

    /** 双格式 MOBI（KF7+KF8）中 KF8 部分的起始记录号，没有时返回 -1。 */
    public int getKf8Boundary() {
        return kf8Boundary;
    }

    /** 封面图片，没有时返回 null。 */
    public MobiImage getCoverImage() {
        return coverImage;
    }

    /** 缩略图，没有时返回 null。 */
    public MobiImage getThumbnailImage() {
        return thumbnailImage;
    }

    /** 所有 EXTH 记录（按文件中的顺序）。 */
    public List<ExthEntry> getExthEntries() {
        return exthEntries;
    }

    /** 某个 EXTH 类型的全部字符串值。 */
    public List<String> getExthValues(int type) {
        List<String> values = new ArrayList<>();
        for (ExthEntry entry : exthEntries) {
            if (entry.getType() == type) {
                values.add(entry.asString(charset));
            }
        }
        return Collections.unmodifiableList(values);
    }

    /** 某个 EXTH 类型的第一个字符串值，没有时返回 null。 */
    public String getExthValue(int type) {
        for (ExthEntry entry : exthEntries) {
            if (entry.getType() == type) {
                return entry.asString(charset);
            }
        }
        return null;
    }

    static String languageFromLocale(int locale) {
        return switch (locale & 0xFF) {
            case 0x01 -> "ar";
            case 0x02 -> "bg";
            case 0x04 -> "zh";
            case 0x05 -> "cs";
            case 0x06 -> "da";
            case 0x07 -> "de";
            case 0x08 -> "el";
            case 0x09 -> "en";
            case 0x0A -> "es";
            case 0x0B -> "fi";
            case 0x0C -> "fr";
            case 0x0D -> "he";
            case 0x0E -> "hu";
            case 0x0F -> "is";
            case 0x10 -> "it";
            case 0x11 -> "ja";
            case 0x12 -> "ko";
            case 0x13 -> "nl";
            case 0x14 -> "no";
            case 0x15 -> "pl";
            case 0x16 -> "pt";
            case 0x18 -> "ro";
            case 0x19 -> "ru";
            case 0x1A -> "hr";
            case 0x1B -> "sk";
            case 0x1D -> "sv";
            case 0x1E -> "th";
            case 0x1F -> "tr";
            case 0x20 -> "ur";
            case 0x21 -> "id";
            case 0x22 -> "uk";
            case 0x24 -> "sl";
            case 0x25 -> "et";
            case 0x26 -> "lv";
            case 0x27 -> "lt";
            case 0x29 -> "fa";
            case 0x2A -> "vi";
            case 0x2B -> "hy";
            case 0x2D -> "eu";
            case 0x39 -> "hi";
            case 0x3E -> "ms";
            default -> null;
        };
    }

    static final class Builder {
        String title;
        final List<String> authors = new ArrayList<>();
        String publisher;
        String imprint;
        String description;
        String isbn;
        final List<String> subjects = new ArrayList<>();
        String publishDate;
        String contributor;
        String rights;
        String source;
        String asin;
        String bookType;
        String language;
        String creatorSoftware;
        Charset charset;
        int textEncoding;
        int compression;
        int encryptionType;
        int mobiType;
        int fileVersion;
        int locale;
        int recordCount;
        int textRecordCount;
        int textLength;
        long fileSize;
        int kf8Boundary = -1;
        MobiImage coverImage;
        MobiImage thumbnailImage;
        final List<ExthEntry> exthEntries = new ArrayList<>();

        MobiMetadata build() {
            return new MobiMetadata(this);
        }
    }
}
