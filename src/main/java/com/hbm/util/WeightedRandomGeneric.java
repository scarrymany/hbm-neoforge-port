package com.hbm.util;

public class WeightedRandomGeneric<T> extends WeightedRandom.Item {

    T item;

    public WeightedRandomGeneric(T o, int weight) {
        super(weight);
        item = o;
    }

    public T get() {
        return item;
    }
}
