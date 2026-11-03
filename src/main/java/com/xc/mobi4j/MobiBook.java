package com.xc.mobi4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 解析结果：元数据 + 正文（HTML / 纯文本）+ 内嵌图片。
 *
 * <p>正文不可用时（例如文件受 DRM 保护、使用了不支持的 HUFF/CDIC 压缩），
 * {@link #getHtml()} / {@link #getText()} 返回空字符串，具体原因见 {@link #getWarnings()}。</p>
 */
public final class MobiBook {

    private final MobiMetadata metadata;
    private final String html;
    private final String text;
    private final List<MobiImage> images;
    private final List<String> warnings;

    MobiBook(MobiMetadata metadata, String html, String text, List<MobiImage> images, List<String> warnings) {
        this.metadata = metadata;
        this.html = html;
        this.text = text;
        this.images = List.copyOf(images);
        this.warnings = List.copyOf(warnings);
    }

    public MobiMetadata getMetadata() {
        return metadata;
    }

    /** 原始正文标记（MOBI7/KF8 都是 HTML 片段）。不可用时为空字符串。 */
    public String getHtml() {
        return html;
    }

    /** 从 HTML 提取的纯文本。不可用时为空字符串。 */
    public String getText() {
        return text;
    }

    /** 内嵌图片（含封面）。 */
    public List<MobiImage> getImages() {
        return images;
    }

    /** 解析过程中的非致命问题，例如“文件受 DRM 保护”。 */
    public List<String> getWarnings() {
        return warnings;
    }

    /** 是否成功读出了正文。 */
    public boolean isContentAvailable() {
        return !html.isEmpty();
    }

    /** 把正文纯文本写入文件（UTF-8）。 */
    public void writeText(Path file) throws IOException {
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    /** 把原始 HTML 写入文件（UTF-8）。 */
    public void writeHtml(Path file) throws IOException {
        Files.writeString(file, html, StandardCharsets.UTF_8);
    }
}
