package com.hbm.lib.maps;

@FunctionalInterface
public interface LongObjectBiFunction<V, R> {
    R apply(long key, V value);
}
