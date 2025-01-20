package ru.nsu.lyutaevdronov.common;

public record Pair<K, V>(K key, V value) {
    @Override
    public String toString() {
        return "(" + key + ", " + value + ")";
    }
}
