package com.xc.mobi4j;

import java.io.IOException;

/**
 * MOBI 文件格式错误（文件损坏、不是 MOBI/PDB 文件等）。
 */
public class MobiException extends IOException {

    public MobiException(String message) {
        super(message);
    }

    public MobiException(String message, Throwable cause) {
        super(message, cause);
    }
}
