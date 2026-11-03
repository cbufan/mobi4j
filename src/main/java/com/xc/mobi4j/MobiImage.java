package com.xc.mobi4j;

import java.util.Arrays;

/**
 * 从 MOBI 中提取出来的一张图片（封面、插图等）。
 */
public final class MobiImage {

    private final int recordIndex;
    private final String format;
    private final byte[] data;

    MobiImage(int recordIndex, String format, byte[] data) {
        this.recordIndex = recordIndex;
        this.format = format;
        this.data = data;
    }

    /** 图片所在的 PDB 记录号。 */
    public int getRecordIndex() {
        return recordIndex;
    }

    /** 图片格式：jpeg、png、gif、bmp 之一。 */
    public String getFormat() {
        return format;
    }

    /** 文件扩展名，例如 jpg。 */
    public String getExtension() {
        return "jpeg".equals(format) ? "jpg" : format;
    }

    public String getMimeType() {
        return switch (format) {
            case "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            default -> "application/octet-stream";
        };
    }

    /** 图片字节（副本）。 */
    public byte[] getData() {
        return data.clone();
    }

    /** 图片字节数。 */
    public int getSize() {
        return data.length;
    }

    /** 通过文件头魔数识别图片格式，无法识别时返回 null。 */
    static String sniff(byte[] data) {
        if (data.length >= 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8
                && (data[2] & 0xFF) == 0xFF) {
            return "jpeg";
        }
        if (data.length >= 8 && (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N'
                && data[3] == 'G' && data[4] == 0x0D && data[5] == 0x0A && data[6] == 0x1A
                && data[7] == 0x0A) {
            return "png";
        }
        if (data.length >= 6 && data[0] == 'G' && data[1] == 'I' && data[2] == 'F'
                && data[3] == '8' && (data[4] == '7' || data[4] == '9') && data[5] == 'a') {
            return "gif";
        }
        if (data.length >= 2 && data[0] == 'B' && data[1] == 'M') {
            return "bmp";
        }
        return null;
    }

    @Override
    public String toString() {
        return "MobiImage{record=" + recordIndex + ", format=" + format + ", size=" + data.length + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MobiImage other)) {
            return false;
        }
        return recordIndex == other.recordIndex && format.equals(other.format) && Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * recordIndex + format.hashCode()) + Arrays.hashCode(data);
    }
}
