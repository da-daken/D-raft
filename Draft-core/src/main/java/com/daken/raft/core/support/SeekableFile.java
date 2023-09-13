package com.daken.raft.core.support;

import java.io.IOException;
import java.io.InputStream;

/**
 * SeekableFile 把要用到的 RandomAccessFile 方法抽象出来
 */
public interface SeekableFile {

    // 获取当前位置
    long position() throws IOException;

    // 移动到指定位置
    void seek(long position) throws IOException;

    // 写入整数
    void writeInt(int i) throws IOException;

    // 写入长整数
    void writeLong(long l) throws IOException;

    // 写入字节数组
    void write(byte[] b) throws IOException;

    // 读取整数
    int readInt() throws IOException;

    // 读取长整数
    long readLong() throws IOException;

    // 读取字节数组，返回读取的长度
    int read(byte[] b) throws IOException;

    // 获取文件的大小
    long size() throws IOException;

    // 裁剪到指定大小 (增加的， 用于日志冲突时删除索引后的文件)
    void truncate(long size) throws IOException;

    // 获取从指定位置开始的输入流（增加的， 用于之后的日志快照）
    InputStream inputStream(long start) throws IOException;

    // 强制输出到磁盘
    void flush() throws IOException;

    // 关闭文件
    void close() throws IOException;
}
