# mobi4j(所有内容皆由ai生成)

一个零依赖的 Java MOBI / AZW / PRC 解析器，用来读取电子书的 **元数据** 和 **正文内容**。

- 纯 Java 17，无任何运行时第三方依赖
- 解析 PDB 容器、PalmDOC 头、MOBI 头、EXTH 元数据
- 解压 PalmDOC（LZ77）正文，支持 UTF-8 / CP1252 / Shift-JIS / GBK / Big5 等编码
- 输出原始 HTML、提取后的纯文本
- 提取内嵌图片，并识别封面 / 缩略图
- 提供命令行工具和可嵌入的 Java API

## 构建与运行

需要 JDK 17+ 和 Maven。

```bash
mvn test          # 运行单元测试
mvn package       # 生成 target/mobi4j-1.0.0.jar
```

命令行用法：

```bash
# 元数据 + 正文预览（默认前 2000 字符）
java -jar target/mobi4j-1.0.0.jar book.mobi

# 只看元数据
java -jar target/mobi4j-1.0.0.jar book.mobi --metadata

# 输出全文
java -jar target/mobi4j-1.0.0.jar book.mobi --text

# 输出原始 HTML
java -jar target/mobi4j-1.0.0.jar book.mobi --html

# 导出正文、HTML 和图片到目录
java -jar target/mobi4j-1.0.0.jar book.mobi --out ./output

# 以 JSON 输出元数据和全部 EXTH 记录
java -jar target/mobi4j-1.0.0.jar book.mobi --json

# 打印全部 EXTH 记录
java -jar target/mobi4j-1.0.0.jar book.mobi --metadata --exth

# 限制输出字符数
java -jar target/mobi4j-1.0.0.jar book.mobi --text --limit 5000
```

`--out` 会生成：

```
output/
├── book.txt
├── book.html
└── images/
    ├── image-001.jpg
    └── ...
```

## 作为库使用

```java
import com.xc.mobi4j.MobiBook;
import com.xc.mobi4j.MobiImage;
import com.xc.mobi4j.MobiMetadata;
import com.xc.mobi4j.MobiParser;

import java.nio.file.Path;

MobiBook book = MobiParser.parse(Path.of("book.mobi"));

MobiMetadata meta = book.getMetadata();
System.out.println("书名: " + meta.getTitle());
System.out.println("作者: " + meta.getAuthors());
System.out.println("出版社: " + meta.getPublisher());
System.out.println("简介: " + meta.getDescription());
System.out.println("语言: " + meta.getLanguage());
System.out.println("ISBN: " + meta.getIsbn());
System.out.println("压缩: " + meta.getCompressionName());
System.out.println("编码: " + meta.getCharset());

// 纯文本正文
String text = book.getText();

// 原始 HTML
String html = book.getHtml();

// 内嵌图片（含封面）
for (MobiImage image : book.getImages()) {
    byte[] data = image.getData();
    System.out.println(image.getRecordIndex() + " " + image.getMimeType() + " " + data.length);
}

// 只读元数据，不解析正文
MobiMetadata onlyMeta = MobiParser.readMetadata(Path.of("book.mobi"));
```

正文不可用时（DRM、不支持的压缩方式），`getHtml()` / `getText()` 返回空字符串，
具体原因在 `book.getWarnings()` 中；元数据仍然可以正常读取。

## 支持的元数据

常见字段已映射为独立 getter：

| 字段 | EXTH |
| --- | --- |
| 标题 | MOBI 全名字段 / EXTH 503 |
| 作者 | 100（可多个） |
| 出版社 | 101 |
| 简介 | 103 |
| ISBN | 104 |
| 主题 | 105（可多个） |
| 出版日期 | 106 |
| 贡献者 | 108 |
| 版权 | 109 |
| 来源 | 112 |
| ASIN | 113 / 504 |
| 语言 | 524，否则由 locale 推导 |
| 书籍类型 | 123 |
| 生成工具 | 204–207 |

所有 EXTH 记录都可以通过 `metadata.getExthEntries()` 或
`metadata.getExthValue(ExthTags.XXX)` 访问，未映射的字段也不会丢失。

## 解析流程

1. 读取 PDB 头（记录表、数据库名、`BOOK`/`MOBI` 标识）
2. 解析 record 0 的 PalmDOC 头（压缩方式、正文长度、记录数、编码）
3. 解析 MOBI 头（全名、语言、首个正文记录、首个图片记录、Extra Data Flags）
4. 解析 EXTH（作者、出版社、封面偏移等）
5. 逐条读取正文记录，去掉 trailing entries，按 PalmDOC LZ77 解压
6. 按声明的字符集解码，截取 `textLength` 字节
7. 用轻量 HTML 处理器提取纯文本；按文件头魔数识别图片并提取封面

## 限制

- **不破解 DRM**。加密文件的元数据可读，但正文为空。
- 暂不支持 **HUFF/CDIC** 压缩（PalmDOC 头中的 `compression=17480`）。
  绝大多数 KindleGen / Calibre 生成的 MOBI 使用 PalmDOC（`compression=2`）。
- 双格式文件（同时含 KF7 和 KF8）按第一个 MOBI 部分解析；KF8-only 的 `.azw3` 可以正常读取。
- 只提取图片，不导出字体、CSS、NCX/目录等资源。
- HTML → 纯文本是轻量转换，不渲染排版。

## 项目结构

```
src/main/java/com/xc/mobi4j/
├── MobiParser.java          解析入口
├── MobiBook.java            解析结果（元数据 + 正文 + 图片）
├── MobiMetadata.java        元数据模型
├── MobiImage.java           图片记录
├── ExthEntry.java           单条 EXTH 记录
├── ExthTags.java            EXTH 字段常量
├── PalmDatabase.java        PDB 容器
├── MobiHeader.java          PalmDOC + MOBI 头
├── PalmDocDecompressor.java PalmDOC LZ77 解压
├── TrailingData.java        正文记录尾部附加数据
├── HtmlTextExtractor.java   HTML → 纯文本
├── Charsets.java            code page → Java Charset
├── Bytes.java               大端序读取
└── cli/Main.java            命令行入口
```

测试代码里的 `TestMobiBuilder` 会在内存中构造一个结构完整的 MOBI 文件，
覆盖元数据、两段压缩正文、封面图片等场景，因此不需要外部样本文件即可验证解析器。
