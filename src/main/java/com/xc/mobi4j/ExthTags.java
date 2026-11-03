package com.xc.mobi4j;

/**
 * 常用的 EXTH 记录编号。
 */
public final class ExthTags {

    public static final int DRM_SERVER_ID = 1;
    public static final int DRM_COMMERCE_ID = 2;
    public static final int DRM_EBOOKBASE_BOOK_ID = 3;

    public static final int AUTHOR = 100;
    public static final int PUBLISHER = 101;
    public static final int IMPRINT = 102;
    public static final int DESCRIPTION = 103;
    public static final int ISBN = 104;
    public static final int SUBJECT = 105;
    public static final int PUBLISHING_DATE = 106;
    public static final int REVIEW = 107;
    public static final int CONTRIBUTOR = 108;
    public static final int RIGHTS = 109;
    public static final int SUBJECT_CODE = 110;
    public static final int TYPE = 111;
    public static final int SOURCE = 112;
    public static final int ASIN = 113;
    public static final int VERSION_NUMBER = 114;
    public static final int SAMPLE = 115;
    public static final int START_READING = 116;
    public static final int ADULT = 117;
    public static final int RETAIL_PRICE = 118;
    public static final int RETAIL_PRICE_CURRENCY = 119;
    public static final int KF8_BOUNDARY = 121;
    public static final int FIXED_LAYOUT = 122;
    public static final int BOOK_TYPE = 123;
    public static final int ORIENTATION_LOCK = 124;
    public static final int RESOURCE_COUNT = 125;
    public static final int ORIGINAL_RESOLUTION = 126;
    public static final int ZERO_GUTTER = 127;
    public static final int ZERO_MARGIN = 128;
    public static final int METADATA_RESOURCE_URI = 129;
    public static final int DICTIONARY_SHORT_NAME = 200;
    public static final int COVER_OFFSET = 201;
    public static final int THUMBNAIL_OFFSET = 202;
    public static final int HAS_FAKE_COVER = 203;
    public static final int CREATOR_SOFTWARE = 204;
    public static final int CREATOR_MAJOR_VERSION = 205;
    public static final int CREATOR_MINOR_VERSION = 206;
    public static final int CREATOR_BUILD_NUMBER = 207;
    public static final int WATERMARK = 208;
    public static final int TAMPER_PROOF_KEYS = 209;
    public static final int FONT_SIGNATURE = 300;
    public static final int CLIPPING_LIMIT = 401;
    public static final int PUBLISHER_LIMIT = 402;
    public static final int TTS_FLAG = 404;
    public static final int RENTAL_FLAG = 405;
    public static final int RENTAL_EXPIRATION = 406;
    public static final int CDE_TYPE = 501;
    public static final int LAST_UPDATE_TIME = 502;
    public static final int UPDATED_TITLE = 503;
    public static final int ASIN_2 = 504;
    public static final int LANGUAGE = 524;
    public static final int WRITING_MODE = 525;

    private ExthTags() {
    }

    /** 返回字段的可读名称，未知类型返回 "unknown"。 */
    public static String name(int type) {
        return switch (type) {
            case 1 -> "drm-server-id";
            case 2 -> "drm-commerce-id";
            case 3 -> "drm-ebookbase-book-id";
            case AUTHOR -> "author";
            case PUBLISHER -> "publisher";
            case IMPRINT -> "imprint";
            case DESCRIPTION -> "description";
            case ISBN -> "isbn";
            case SUBJECT -> "subject";
            case PUBLISHING_DATE -> "publishing-date";
            case REVIEW -> "review";
            case CONTRIBUTOR -> "contributor";
            case RIGHTS -> "rights";
            case SUBJECT_CODE -> "subject-code";
            case TYPE -> "type";
            case SOURCE -> "source";
            case ASIN, ASIN_2 -> "asin";
            case VERSION_NUMBER -> "version-number";
            case SAMPLE -> "sample";
            case START_READING -> "start-reading";
            case ADULT -> "adult";
            case RETAIL_PRICE -> "retail-price";
            case RETAIL_PRICE_CURRENCY -> "retail-price-currency";
            case KF8_BOUNDARY -> "kf8-boundary";
            case FIXED_LAYOUT -> "fixed-layout";
            case BOOK_TYPE -> "book-type";
            case ORIENTATION_LOCK -> "orientation-lock";
            case RESOURCE_COUNT -> "resource-count";
            case ORIGINAL_RESOLUTION -> "original-resolution";
            case ZERO_GUTTER -> "zero-gutter";
            case ZERO_MARGIN -> "zero-margin";
            case METADATA_RESOURCE_URI -> "metadata-resource-uri";
            case DICTIONARY_SHORT_NAME -> "dictionary-short-name";
            case COVER_OFFSET -> "cover-offset";
            case THUMBNAIL_OFFSET -> "thumbnail-offset";
            case HAS_FAKE_COVER -> "has-fake-cover";
            case CREATOR_SOFTWARE -> "creator-software";
            case CREATOR_MAJOR_VERSION -> "creator-major-version";
            case CREATOR_MINOR_VERSION -> "creator-minor-version";
            case CREATOR_BUILD_NUMBER -> "creator-build-number";
            case WATERMARK -> "watermark";
            case TAMPER_PROOF_KEYS -> "tamper-proof-keys";
            case FONT_SIGNATURE -> "font-signature";
            case CLIPPING_LIMIT -> "clipping-limit";
            case PUBLISHER_LIMIT -> "publisher-limit";
            case TTS_FLAG -> "tts-flag";
            case RENTAL_FLAG -> "rental-flag";
            case RENTAL_EXPIRATION -> "rental-expiration";
            case CDE_TYPE -> "cde-type";
            case LAST_UPDATE_TIME -> "last-update-time";
            case UPDATED_TITLE -> "updated-title";
            case LANGUAGE -> "language";
            case WRITING_MODE -> "writing-mode";
            default -> "unknown";
        };
    }
}
