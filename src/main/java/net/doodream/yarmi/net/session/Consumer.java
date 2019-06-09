package net.doodream.yarmi.net.session;

public interface Consumer<T> {
    void accept(T t);
}
