package com.hbm.lib.maps;

@FunctionalInterface
public interface LongObjectConsumer<T> {
    void accept(long key, T value);
}
