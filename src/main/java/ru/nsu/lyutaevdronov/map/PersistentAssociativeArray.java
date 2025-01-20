package ru.nsu.lyutaevdronov.map;

import org.jetbrains.annotations.NotNull;
import ru.nsu.lyutaevdronov.array.PersistentArray;
import ru.nsu.lyutaevdronov.common.SpecialPersistentData;

import java.util.*;

/**
 * Persistent ассоциативный массив, который поддерживает undo redo
 *
 * @param <K> тип ключей
 * @param <V> тип значений
 */
public class PersistentAssociativeArray<K, V> extends AbstractMap<K, V> implements SpecialPersistentData {
    private static final int TABLE_MAX_SIZE = 16;
    private final List<PersistentArray<Pair<K, V>>> table;
    /**
     * Стек для хранения индексов Entry, изменения к которым могут быть отменены
     */
    private final Deque<Integer> undo = new ArrayDeque<>();
    /**
     * Стек для хранения индексов Entry, изменения к которым могут быть повторно применены
     */
    private final Deque<Integer> redo = new ArrayDeque<>();
    /**
     * Стек для хранения вложенных персистентных структур, изменения к которым могут быть отменены
     */
    private final Deque<SpecialPersistentData> insertedUndoStack = new ArrayDeque<>();
    /**
     * Стек для хранения вложенных персистентных структур, изменения к которым могут быть повторно применены
     */
    private final Deque<SpecialPersistentData> insertedRedoStack = new ArrayDeque<>();

    /**
     * Ссылка на родительский ассоциативный массив, если текущий является частью его вложенности
     */
    private SpecialPersistentData parent;
    private int countInsertedMaps = 0;

    public PersistentAssociativeArray() {
        this.table = new ArrayList<>(30);
        for (int i = 0; i < TABLE_MAX_SIZE; i++) {
            table.add(new PersistentArray<>());
        }
    }

    public PersistentAssociativeArray(PersistentAssociativeArray<K, V> other) {
        this.table = new ArrayList<>(30);
        for (int i = 0; i < TABLE_MAX_SIZE; i++) {
            table.add(new PersistentArray<>(other.table.get(i)));
        }
        this.undo.addAll(other.undo);
        this.redo.addAll(other.redo);
    }

    @Override
    public void undo() {
        if (!insertedUndoStack.isEmpty()) {
            if (insertedUndoStack.peek().isEmpty()) {
                insertedRedoStack.push(insertedUndoStack.pop());
                standardUndo();
            } else {
                SpecialPersistentData persistentData = insertedUndoStack.pop();
                persistentData.undo();
                insertedRedoStack.push(persistentData);
            }
        } else {
            standardUndo();
        }
    }

    @Override
    public void redo() {
        if (!insertedRedoStack.isEmpty()) {
            if (insertedRedoStack.peek().isEmpty()) {
                if (insertedRedoStack.peek().getParent().size() == countInsertedMaps) {
                    standardInsertedRedo();
                } else {
                    insertedUndoStack.push(insertedRedoStack.pop());
                    standardRedo();
                }
            } else {
                standardInsertedRedo();
            }
        } else {
            standardRedo();
        }
    }

    private void standardUndo() {
        if (!undo.isEmpty()) {
            table.get(undo.peek()).undo();
            redo.push(undo.pop());
        }
    }

    private void standardRedo() {
        if (!redo.isEmpty()) {
            table.get(redo.peek()).redo();
            undo.push(redo.pop());
        }
    }

    private void standardInsertedRedo() {
        SpecialPersistentData persistentData = insertedRedoStack.pop();
        persistentData.redo();
        insertedUndoStack.push(persistentData);
    }

    private void tryParentUndo(V value) {
        if (value instanceof SpecialPersistentData persistentData) {
            countInsertedMaps++;
            persistentData.addParent(this);
            insertedUndoStack.push(persistentData);
            redo.clear();
            insertedRedoStack.clear();
        }

        if (parent != null) {
            parent.addChildModification(this);
        }
    }

    /**
     * Связывает указанное значение с указанным ключом в этом ассоциативном массиве.
     * Если ассоциативный массив ранее содержал сопоставление для ключа, старое значение заменяется указанным значением.
     *
     * @param key   ключ, с которым должно быть связано указанное значение
     * @param value значение, которое будет связано с указанным ключом
     * @return предыдущее значение, связанное с ключом, или null, если не было сопоставления для ключа
     * (возврат null также может указывать на то, что ассоциативный массив ранее связывал null с ключом)
     */
    @Override
    public V put(K key, V value) {
        V result = get(key);

        int index = calculateIndex(key.hashCode());
        for (int i = 0; i < table.get(index).size(); i++) {
            Pair<K, V> pair = table.get(index).get(i);
            if (pair.getKey().equals(key)) {
                table.get(index).set(i, new Pair<>(key, value));
                undo.push(index);
                redo.clear();
                tryParentUndo(value);

                return result;
            }
        }

        table.get(index).add(new Pair<>(key, value));
        undo.push(index);
        redo.clear();
        tryParentUndo(value);

        return result;
    }

    /**
     * Копирует все сопоставления с указанного ассоциативого массива в этот ассоциативный массив.
     * <p>
     * Эффект от этого вызова эквивалентен эффекту вызова put(k, v) на этой карте один раз
     * для каждого отображения ключа k на значение v в указанной карте.
     * Эта реализация выполняет итерацию по коллекции entrySet() указанного ассоциативного массива
     * и вызывает операцию put этого ассоциативного массива один раз для каждой записи, возвращаемой итерацией.
     * </p>
     *
     * @param m сопоставления, которые будут храниться в этом ассоциативном массиве
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Возвращает копию ассоциативного массива, в которой связывает указанное значение с указанным ключом.
     *
     * @param key   ключ, с которым должно быть связано указанное значение
     * @param value значение, которое будет связано с указанным ключом
     * @return измененная копия ассоциативного массива
     */
    public PersistentAssociativeArray<K, V> conj(K key, V value) {
        PersistentAssociativeArray<K, V> result = new PersistentAssociativeArray<>(this);
        result.put(key, value);
        return result;
    }

    /**
     * Удаляет сопоставление для ключа из этого ассоциативного массива, если оно присутствует.
     * Возвращает значение, с которым эта карта ранее связала ключ, или null, если карта не содержала сопоставления для ключа.
     * Ассоциативный массив не будет содержать сопоставления для указанного ключа после возврата вызова.
     *
     * @param key ключ, сопоставление которого должно быть удалено из ассоциативного массива
     * @return предыдущее значение, связанное с ключом, или null, если не было сопоставления для указаннного ключа
     */
    @Override
    public V remove(Object key) {
        int index = calculateIndex(key.hashCode());
        for (int i = 0; i < table.get(index).size(); i++) {
            Pair<K, V> pair = table.get(index).get(i);
            if (pair.getKey().equals(key)) {
                V value = pair.getValue();
                table.get(index).remove(i);
                undo.push(index);
                redo.clear();
                tryParentUndo((V) this);
                return value;
            }
        }
        return null;
    }

    /**
     * Удаляет все сопоставления из этого ассоциативного массива.
     * Ассоциативный массив будет пустым после возврата этого вызова.
     */
    @Override
    public void clear() {
        for (PersistentArray<Pair<K, V>> pairs : table) {
            pairs.clear();
        }
    }

    /**
     * Возвращает значение, которому сопоставлен указанный ключ, или null, если этот ассоциативный массив не содержит сопоставления для ключа.
     *
     * @param key ключ, ассоциированное значение которого должно быть возвращено
     * @return значение, которому сопоставлен указанный ключ, или null, если этот ассоциативный массив не содержит сопоставления для ключа
     */
    @Override
    public V get(Object key) {
        int index = calculateIndex(key.hashCode());
        PersistentArray<Pair<K, V>> get = table.get(index);
        for (Pair<K, V> pair : get) {
            if (pair.getKey().equals(key)) {
                return pair.getValue();
            }
        }
        return null;
    }

    /**
     * Возвращает множество набора ключей, содержащихся в этом ассоциативном массиве.
     *
     * @return множество ключей, содержащихся в этом ассоциативном массиве
     */
    @Override
    public @NotNull Set<K> keySet() {
        Set<K> keySet = new HashSet<>();
        for (PersistentArray<Pair<K, V>> pairs : table) {
            for (Pair<K, V> pair : pairs) {
                keySet.add(pair.getKey());
            }
        }
        return keySet;
    }

    /**
     * Возвращает множество сопоставлений, содержащихся в этом ассоциативном массиве.
     *
     * @return набор сопоставлений, содержащихся в этом ассоциативном массиве
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> entrySet = new HashSet<>();
        for (PersistentArray<Pair<K, V>> pairs : table) {
            entrySet.addAll(pairs);
        }
        return entrySet;
    }

    /**
     * Возвращает список значений, содержащихся в этом ассоциативном массиве.
     *
     * @return список значений, содержащихся в этом ассоциативном массиве
     */
    @Override
    public @NotNull List<V> values() {
        List<V> values = new ArrayList<>();
        for (PersistentArray<Pair<K, V>> pairs : table) {
            for (Pair<K, V> pair : pairs) {
                values.add(pair.getValue());
            }
        }
        return values;
    }

    /**
     * Возвращает строковое представление этого ассоциативного массива.
     *
     * @return строковое представление этого ассоциативного массива
     */
    @Override
    public String toString() {
        if (isEmpty()) {
            return "{}";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        for (Entry<K, V> entry : this.entrySet()) {
            stringBuilder.append(entry).append(", ");
        }
        stringBuilder.delete(stringBuilder.lastIndexOf(", "), stringBuilder.lastIndexOf(", ") + 2);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    private int calculateIndex(int hashcode) {
        return hashcode & (TABLE_MAX_SIZE - 1);
    }

    @Override
    public void addChildModification(SpecialPersistentData obj) {
        insertedUndoStack.push(obj);
    }

    @Override
    public void addParent(SpecialPersistentData obj) {
        this.parent = obj;
    }

    @Override
    public SpecialPersistentData getParent() {
        return parent;
    }

    /**
     * Запись ассоциативного массива (пара ключ-значение).
     */
    static class Pair<K, V> implements Entry<K, V> {
        private final K key;
        private V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Возвращает ключ, соответствующий этой записи.
         *
         * @return ключ, соответствующий этой записи
         */
        @Override
        public K getKey() {
            return key;
        }

        /**
         * Возвращает значение, соответствующее этой записи.
         *
         * @return значение, соответствующее этой записи
         */
        @Override
        public V getValue() {
            return value;
        }

        /**
         * Заменяет значение, соответствующее этой записи, на указанное значение.
         *
         * @param value новое значение, которое будет сохранено в этой записи
         * @return старое значение, соответствующее записи
         */
        @Override
        public V setValue(V value) {
            return this.value = value;
        }

        /**
         * Возвращает строковое представление этой записи ассоциативного массива.
         *
         * @return строковое представление этой записи ассоциативного массива
         */
        @Override
        public String toString() {
            return key + "=" + value;
        }

        /**
         * Возвращает значение хэш-кода для этой записи карты.
         *
         * @return значение хэш-кода для этой записи карты
         */
        @Override
        public int hashCode() {
            return key.hashCode() * 13 + (value == null ? 0 : value.hashCode());
        }

        /**
         * Сравнивает указанный объект с этой записью на равенство.
         * Возвращает true, если данный объект также является записью ассоциативного массива и две записи представляют одно и то же отображение.
         *
         * @param o объект для сравнения на равенство с этой записью ассоциативного массива
         * @return true, если указанный объект равен этой записи ассоциативного массива
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof Pair<?, ?> pair) {
                if (!Objects.equals(key, pair.key)) {
                    return false;
                }
                return Objects.equals(value, pair.value);
            }
            return false;
        }
    }
}