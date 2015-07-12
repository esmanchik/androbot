package com.httpuart;

public interface Uart {
    void open();
    void write(byte[] bytes);
    void close();
}

