package ru.nsu.lyutaevdronov.common;

/**
 * Описывает класс абстрактной persisntent структура, которая поддерживает undo redo
 */
public abstract class AbstractPersistentData implements PersistentData {
    /**
     * Глубина структуры данных.
     */
    public final int depth;

    /**
     * Маска, используемая для вычисления индексов в узлах.
     */
    public final int mask;

    /**
     * Максимальный размер коллекции, равен 2^(bitPerEdge*depth)
     */
    public final int maxSize;

    /**
     * Количество бит, используемых для представления каждого уровня в структуре
     * данных.
     */
    public final int bitPerEdge;

    /**
     * Ширина структуры данных, равна 2^bitPerEdge
     */
    public final int width;

    protected AbstractPersistentData(int depth, int bitPerEdge) {
        this.depth = depth;
        this.bitPerEdge = bitPerEdge;

        mask = (int) Math.pow(2, bitPerEdge) - 1;
        maxSize = (int) Math.pow(2, bitPerEdge * depth);

        width = (int) Math.pow(2, bitPerEdge);
    }

    protected static double log(int n, int newBase) {
        return (Math.log(n) / Math.log(newBase));
    }
}