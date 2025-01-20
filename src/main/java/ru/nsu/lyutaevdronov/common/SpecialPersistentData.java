package ru.nsu.lyutaevdronov.common;

/**
 * Волшебный интерфейс для того, чтобы все работало!
 */
public interface SpecialPersistentData extends PersistentData {
    boolean isEmpty();

    int size();

    /**
     * Сохраняет состояние вложенной персистентной структуры для отката
     * @param obj персистентная структура, поддерживающая undo/redo
     */
    void addChildModification(SpecialPersistentData obj);

    /**
     * Добавляет ссылку на внешнюю персистентную структура, в которой лежит текущая
     * @param obj - верхнеуровневая структура
     */
    void addParent(SpecialPersistentData obj);

    /**
     * Возвращает верхнеуровневую структуру, если есть
     * @return верхнеуровневая персистентная структура
     */
    SpecialPersistentData getParent();
}
