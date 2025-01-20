package ru.nsu.lyutaevdronov.common;

/**
 * Структура данных поддерживающая операции возврата к предыдущему состоянию.
 */
public interface PersistentData {
    /**
     * Выполняет возврат к предыдущей версии.
     */
    void undo();

    /**
     * Отменяет возврат к предыдущей версии.
     */
    void redo();
}
