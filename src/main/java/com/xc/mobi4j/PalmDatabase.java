package com.xc.mobi4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Palm Database (PDB) 容器。MOBI、AZW、PRC 都是 PDB 文件。
 *
 * <pre>
 * 0x00  32 字节 数据库名
 * 0x3C   4 字节 type（"BOOK"）
 * 0x40   4 字节 creator（"MOBI"）
 * 0x4C   2 字节 记录数 n
 * 0x4E   n × 8 字节 记录信息（4 字节偏移 + 1 字节属性 + 3 字节 ID）
 * ...    2 字节 填充
 * ...    记录数据
 * </pre>
 */
final class PalmDatabase {

    private final String name;
    private final String type;
    private final String creator;
    private final List<byte[]> records;

    private PalmDatabase(String name, String type, String creator, List<byte[]> records) {
        this.name = name;
        this.type = type;
        this.creator = creator;
        this.records = records;
    }

    static PalmDatabase parse(byte[] data) throws MobiException {
        if (data.length < 78) {
            throw new MobiException("文件太小，不是有效的 PDB/MOBI 文件");
        }
        int recordCount = Bytes.u16(data, 76);
        if (recordCount <= 0) {
            throw new MobiException("PDB 记录数为 0，文件可能已损坏");
        }
        long tableEnd = 78L + (long) recordCount * 8L;
        if (tableEnd > data.length) {
            throw new MobiException("PDB 记录表超出文件末尾（记录数 " + recordCount + "）");
        }

        long[] offsets = new long[recordCount];
        for (int i = 0; i < recordCount; i++) {
            offsets[i] = Bytes.u32(data, 78 + i * 8);
            if (offsets[i] > data.length) {
                throw new MobiException("第 " + i + " 条记录的偏移超出文件末尾");
            }
        }

        List<byte[]> records = new ArrayList<>(recordCount);
        for (int i = 0; i < recordCount; i++) {
            long start = offsets[i];
            long end = i + 1 < recordCount ? offsets[i + 1] : data.length;
            if (end < start) {
                end = start;
            }
            if (end > data.length) {
                end = data.length;
            }
            records.add(Arrays.copyOfRange(data, (int) start, (int) end));
        }

        String name = Bytes.decodeString(data, 0, 32, StandardCharsets.ISO_8859_1);
        String type = Bytes.ascii(data, 60, 4);
        String creator = Bytes.ascii(data, 64, 4);
        return new PalmDatabase(name, type, creator, records);
    }

    String name() {
        return name;
    }

    String type() {
        return type;
    }

    String creator() {
        return creator;
    }

    int recordCount() {
        return records.size();
    }

    byte[] record(int index) {
        return records.get(index);
    }
}
