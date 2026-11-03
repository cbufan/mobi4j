package com.xc.mobi4j;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 在内存中构造一个最小但结构完整的 MOBI7 文件，用于测试解析器。
 */
final class TestMobiBuilder {

    String title = "测试书名";
    String author = "张三";
    String publisher = "测试出版社";
    String description = "一本用于测试的书";
    String html = "<html><head><title>会被丢弃</title><style>p{color:red}</style></head><body>"
            + "<h1>第一章</h1><p>你好，世界！</p><p>Hello&nbsp;world &amp; more.</p></body></html>";
    byte[] cover = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4};
    int compression = 2;
    int encryptionType = 0;
    boolean includeMobiHeader = true;
    boolean withTrailingData = false;
    String language = "zh";

    byte[] build() throws Exception {
        byte[] textBytes = html.getBytes(StandardCharsets.UTF_8);
        int split = textBytes.length / 2;
        byte[] textRecord1 = compress(Arrays.copyOfRange(textBytes, 0, split));
        byte[] textRecord2 = compress(Arrays.copyOfRange(textBytes, split, textBytes.length));
        if (withTrailingData) {
            textRecord1 = appendTrailingData(textRecord1);
            textRecord2 = appendTrailingData(textRecord2);
        }
        int textRecordCount = 2;
        int firstImageIndex = 3;

        if (!includeMobiHeader) {
            ByteArrayOutputStream record0 = new ByteArrayOutputStream();
            writeShort(record0, compression);
            writeShort(record0, 0);
            writeInt(record0, textBytes.length);
            writeShort(record0, textRecordCount);
            writeShort(record0, 4096);
            writeShort(record0, encryptionType);
            writeShort(record0, 0);
            return writePdb(title, List.of(record0.toByteArray(), textRecord1, textRecord2));
        }

        byte[] titleBytes = title.getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream exthBody = new ByteArrayOutputStream();
        int exthCount = 0;
        exthCount += writeExthString(exthBody, ExthTags.AUTHOR, author);
        exthCount += writeExthString(exthBody, ExthTags.PUBLISHER, publisher);
        exthCount += writeExthString(exthBody, ExthTags.DESCRIPTION, description);
        exthCount += writeExthString(exthBody, ExthTags.SUBJECT, "小说");
        exthCount += writeExthString(exthBody, ExthTags.ISBN, "9780000000001");
        exthCount += writeExthInt(exthBody, ExthTags.COVER_OFFSET, 0);
        exthCount += writeExthInt(exthBody, ExthTags.THUMBNAIL_OFFSET, 0);
        exthCount += writeExthInt(exthBody, ExthTags.CREATOR_SOFTWARE, 201);
        exthCount += writeExthInt(exthBody, ExthTags.CREATOR_MAJOR_VERSION, 2);
        exthCount += writeExthInt(exthBody, ExthTags.CREATOR_MINOR_VERSION, 9);
        exthCount += writeExthString(exthBody, ExthTags.LANGUAGE, language);
        byte[] exthRecords = exthBody.toByteArray();
        int exthLength = 12 + exthRecords.length;
        int exthPaddedLength = exthLength + ((4 - exthLength % 4) % 4);

        byte[] mobi = new byte[232];
        putAscii(mobi, 0, "MOBI");
        putInt(mobi, 4, mobi.length);
        putInt(mobi, 8, 2);
        putInt(mobi, 12, 65001);
        putInt(mobi, 16, 12345);
        putInt(mobi, 20, 6);
        for (int offset = 24; offset <= 60; offset += 4) {
            putInt(mobi, offset, -1);
        }
        putInt(mobi, 64, firstImageIndex);
        putInt(mobi, 68, 16 + mobi.length + exthPaddedLength);
        putInt(mobi, 72, titleBytes.length);
        putInt(mobi, 76, 0x0804);
        putInt(mobi, 88, 6);
        putInt(mobi, 92, firstImageIndex);
        putInt(mobi, 112, 0x40);
        putInt(mobi, 148, -1);
        putInt(mobi, 152, -1);
        putInt(mobi, 156, -1);
        putInt(mobi, 160, 0);
        putInt(mobi, 164, 0);
        putShort(mobi, 176, 1);
        putShort(mobi, 178, textRecordCount);
        putInt(mobi, 180, 1);
        putInt(mobi, 184, -1);
        putInt(mobi, 188, -1);
        putInt(mobi, 192, -1);
        putInt(mobi, 196, -1);
        putInt(mobi, 208, -1);
        putInt(mobi, 212, 0);
        putInt(mobi, 216, -1);
        putInt(mobi, 220, -1);
        putInt(mobi, 224, withTrailingData ? 0x02 : 0);
        putInt(mobi, 228, -1);

        ByteArrayOutputStream record0 = new ByteArrayOutputStream();
        writeShort(record0, compression);
        writeShort(record0, 0);
        writeInt(record0, textBytes.length);
        writeShort(record0, textRecordCount);
        writeShort(record0, 4096);
        writeShort(record0, encryptionType);
        writeShort(record0, 0);
        record0.write(mobi);

        byte[] exth = new byte[exthLength];
        putAscii(exth, 0, "EXTH");
        putInt(exth, 4, exthLength);
        putInt(exth, 8, exthCount);
        System.arraycopy(exthRecords, 0, exth, 12, exthRecords.length);
        record0.write(exth);
        while (record0.size() % 4 != 0) {
            record0.write(0);
        }

        record0.write(titleBytes);
        record0.write(0);
        record0.write(0);
        while (record0.size() % 4 != 0) {
            record0.write(0);
        }

        List<byte[]> records = new ArrayList<>();
        records.add(record0.toByteArray());
        records.add(textRecord1);
        records.add(textRecord2);
        records.add(cover);

        return writePdb(title, records);
    }

    /** 生成一个样本文件，便于手动测试命令行：java ... com.xc.mobi4j.TestMobiBuilder sample.mobi */
    public static void main(String[] args) throws Exception {
        Path target = Path.of(args.length > 0 ? args[0] : "sample.mobi");
        Files.write(target, new TestMobiBuilder().build());
        System.out.println("written " + target.toAbsolutePath());
    }

    private byte[] appendTrailingData(byte[] record) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(record.length + 3);
        out.write(record, 0, record.length);
        out.write(0x11);
        out.write(0x22);
        out.write(0x83); // 反向编码的长度 3：2 字节数据 + 1 字节长度字段
        return out.toByteArray();
    }

    private byte[] compress(byte[] data) {
        if (compression == 1) {
            return data;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length + 8);
        for (byte b : data) {
            int value = b & 0xFF;
            if (value >= 0x09 && value <= 0x7F) {
                out.write(value);
            } else if (value == 0x00) {
                out.write(0x00);
            } else {
                out.write(0x01);
                out.write(value);
            }
        }
        return out.toByteArray();
    }

    private static byte[] writePdb(String name, List<byte[]> records) {
        int recordCount = records.size();
        int tableEnd = 78 + recordCount * 8 + 2;
        int[] offsets = new int[recordCount];
        int position = tableEnd;
        for (int i = 0; i < recordCount; i++) {
            offsets[i] = position;
            position += records.get(i).length;
        }

        byte[] file = new byte[position];
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(nameBytes, 0, file, 0, Math.min(31, nameBytes.length));
        putAscii(file, 60, "BOOK");
        putAscii(file, 64, "MOBI");
        putShort(file, 76, recordCount);
        for (int i = 0; i < recordCount; i++) {
            int entry = 78 + i * 8;
            putInt(file, entry, offsets[i]);
            file[entry + 4] = 0;
            file[entry + 5] = 0;
            file[entry + 6] = 0;
            file[entry + 7] = (byte) (i + 1);
        }
        int position2 = tableEnd;
        for (byte[] record : records) {
            System.arraycopy(record, 0, file, position2, record.length);
            position2 += record.length;
        }
        return file;
    }

    private static int writeExthString(ByteArrayOutputStream out, int type, String value) {
        return writeExth(out, type, value.getBytes(StandardCharsets.UTF_8));
    }

    private static int writeExthInt(ByteArrayOutputStream out, int type, int value) {
        return writeExth(out, type, new byte[]{
                (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value});
    }

    private static int writeExth(ByteArrayOutputStream out, int type, byte[] data) {
        writeInt(out, type);
        writeInt(out, 8 + data.length);
        out.write(data, 0, data.length);
        return 1;
    }

    private static void writeShort(ByteArrayOutputStream out, int value) {
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    private static void writeInt(ByteArrayOutputStream out, int value) {
        out.write((value >>> 24) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    private static void putShort(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >>> 8);
        data[offset + 1] = (byte) value;
    }

    private static void putInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >>> 24);
        data[offset + 1] = (byte) (value >>> 16);
        data[offset + 2] = (byte) (value >>> 8);
        data[offset + 3] = (byte) value;
    }

    private static void putAscii(byte[] data, int offset, String value) {
        for (int i = 0; i < value.length(); i++) {
            data[offset + i] = (byte) value.charAt(i);
        }
    }
}
