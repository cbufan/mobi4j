package com.xc.mobi4j;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PDB record 0 中的 PalmDOC 头和 MOBI 头。
 *
 * <p>字段偏移来自 MobileRead 的 MOBI 格式说明。所有偏移都相对 record 0 起始位置；
 * MOBI 头从第 16 字节开始。</p>
 */
final class MobiHeader {

    static final int MOBI_HEADER_START = 16;
    static final int EXTH_FLAG = 0x40;

    int compression;
    int textLength;
    int textRecordCount;
    int recordSize;
    int encryptionType;

    boolean hasMobiHeader;
    int mobiHeaderLength;
    int mobiType;
    int textEncoding = 1252;
    int fileVersion;
    int locale;
    String fullName;

    int firstImageIndex = -1;
    int firstContentRecord;
    int huffmanRecordOffset;
    int huffmanRecordCount;
    int exthFlags;
    int extraDataFlags;
    int kf8Boundary = -1;

    List<ExthEntry> exth = Collections.emptyList();

    static MobiHeader parse(byte[] record0, int totalRecords) throws MobiException {
        if (record0.length < 16) {
            throw new MobiException("PalmDOC 头不完整（record 0 只有 " + record0.length + " 字节）");
        }

        MobiHeader header = new MobiHeader();
        header.compression = Bytes.u16(record0, 0);
        header.textLength = (int) Bytes.u32(record0, 4);
        header.textRecordCount = Bytes.u16(record0, 8);
        header.recordSize = Bytes.u16(record0, 10);
        header.encryptionType = Bytes.u16(record0, 12);

        header.hasMobiHeader = record0.length >= 24 && Bytes.startsWith(record0, MOBI_HEADER_START, "MOBI");
        if (!header.hasMobiHeader) {
            return header;
        }

        int declaredLength = Bytes.i32(record0, 20);
        int maxLength = record0.length - MOBI_HEADER_START;
        if (declaredLength < 24) {
            declaredLength = Math.min(24, maxLength);
        }
        if (declaredLength > maxLength) {
            declaredLength = maxLength;
        }
        header.mobiHeaderLength = declaredLength;
        int headerEnd = MOBI_HEADER_START + declaredLength;

        header.mobiType = Bytes.i32Or(record0, 24, 0);
        header.textEncoding = Bytes.i32Or(record0, 28, 1252);
        header.fileVersion = Bytes.i32Or(record0, 36, 0);
        header.locale = headerEnd >= 96 ? Bytes.i32Or(record0, 92, 0) : 0;
        header.firstImageIndex = headerEnd >= 112 ? Bytes.i32Or(record0, 108, -1) : -1;
        header.huffmanRecordOffset = headerEnd >= 120 ? Bytes.i32Or(record0, 112, 0) : 0;
        header.huffmanRecordCount = headerEnd >= 120 ? Bytes.i32Or(record0, 116, 0) : 0;
        header.exthFlags = headerEnd >= 132 ? Bytes.i32Or(record0, 128, 0) : 0;
        if (headerEnd >= 244) {
            header.extraDataFlags = Bytes.i32Or(record0, 240, 0);
        }
        header.firstContentRecord = headerEnd >= 194 ? Bytes.u16Or(record0, 192, 0) : 0;

        Charset charset = Charsets.forMobiEncoding(header.textEncoding);

        if (headerEnd >= 92) {
            long nameOffset = Bytes.u32(record0, 84);
            int nameLength = Bytes.i32Or(record0, 88, 0);
            if (nameOffset > 0 && nameLength > 0 && nameOffset + nameLength <= record0.length) {
                header.fullName = Bytes.decodeString(record0, (int) nameOffset, nameLength, charset);
            }
        }

        if ((header.exthFlags & EXTH_FLAG) != 0 && headerEnd + 12 <= record0.length) {
            header.exth = parseExth(record0, headerEnd);
        }
        for (ExthEntry entry : header.exth) {
            if (entry.getType() == ExthTags.KF8_BOUNDARY) {
                header.kf8Boundary = entry.asInt();
                break;
            }
        }

        if (header.firstContentRecord <= 0 || header.firstContentRecord >= totalRecords) {
            header.firstContentRecord = 1;
        }
        if (header.firstImageIndex < 0 || header.firstImageIndex >= totalRecords) {
            header.firstImageIndex = -1;
        }
        return header;
    }

    private static List<ExthEntry> parseExth(byte[] record0, int offset) {
        if (!Bytes.startsWith(record0, offset, "EXTH")) {
            return Collections.emptyList();
        }
        long declaredLength = Bytes.u32(record0, offset + 4);
        int count = (int) Bytes.u32(record0, offset + 8);
        if (count < 0 || count > 100_000) {
            return Collections.emptyList();
        }

        long end = offset + declaredLength;
        if (declaredLength < 12 || end > record0.length) {
            end = record0.length;
        }

        List<ExthEntry> entries = new ArrayList<>(Math.min(count, 256));
        int position = offset + 12;
        for (int i = 0; i < count && position + 8 <= end; i++) {
            int type = Bytes.i32(record0, position);
            int recordLength = Bytes.i32(record0, position + 4);
            if (recordLength < 8 || position + recordLength > end) {
                break;
            }
            entries.add(new ExthEntry(type,
                    java.util.Arrays.copyOfRange(record0, position + 8, position + recordLength)));
            position += recordLength;
        }
        return Collections.unmodifiableList(entries);
    }
}
