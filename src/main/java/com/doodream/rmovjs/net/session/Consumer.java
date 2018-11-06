package com.doodream.rmovjs.net.session;

public interface Consumer<T> {
    void accept(T t);
}
