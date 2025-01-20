package ru.nsu.lyutaevdronov;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.nsu.lyutaevdronov.array.PersistentArray;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

final class PersistentArrayTest {
    @Test
    @DisplayName("Тест на добавление и удаление элементов")
    void testAddAndGet() {
        PersistentArray<String> persistentArray = new PersistentArray<>(32);

        persistentArray.add("Cat");
        persistentArray.add("Dog");
        persistentArray.add("Apple");
        persistentArray.add(1, "Cat2");

        assertThat(persistentArray.get(0)).isEqualTo("Cat");
        assertThat(persistentArray.get(1)).isEqualTo("Cat2");
        assertThat(persistentArray.get(2)).isEqualTo("Dog");
        assertThat(persistentArray.get(3)).isEqualTo("Apple");

        assertThatExceptionOfType(IndexOutOfBoundsException.class)
                .isThrownBy(() -> persistentArray.add(4, "Apple2"));
    }

    @Test
    @DisplayName("Тест на вывода данных")
    void testPrint() {
        PersistentArray<String> persistentArray = new PersistentArray<>(32);

        persistentArray.add("Cat");
        persistentArray.add("Dog");
        persistentArray.add("Apple");
        assertThat(persistentArray).hasToString("[Cat, Dog, Apple]");

        persistentArray.add(1, "Cat2");
        assertThat(persistentArray).hasToString("[Cat, Cat2, Dog, Apple]");
    }

    @Test
    @DisplayName("Проверка размера массива")
    void testPersistentArraySize() {
        PersistentArray<String> persistentArray = new PersistentArray<>(32);

        persistentArray.add("1");
        persistentArray.add("2");
        persistentArray.add("3");
        persistentArray.remove(2);

        assertThat(persistentArray).hasSize(2);
    }

    @Test
    @DisplayName("Проверка на пустоту")
    void testPersistentArrayIsEmpty() {
        PersistentArray<String> persistentArray = new PersistentArray<>(32);

        assertThat(persistentArray).isEmpty();
        persistentArray.add("A");
        assertThat(persistentArray).isNotEmpty();
    }

    @Test
    @DisplayName("Проверка undo redo операций")
    void testUndoRedo() {
        PersistentArray<String> persistentArray = new PersistentArray<>(32);

        persistentArray.add("1");
        persistentArray.add("2");
        persistentArray.add("3");
        persistentArray.add("4");

        assertThat(persistentArray.getVersionCount()).isEqualTo(5);

        persistentArray.undo();
        assertThat(persistentArray.getVersionCount()).isEqualTo(5);

        assertThat(persistentArray).hasToString("[1, 2, 3]");

        persistentArray.redo();
        assertThat(persistentArray.getVersionCount()).isEqualTo(5);
        assertThat(persistentArray).hasToString("[1, 2, 3, 4]");

        persistentArray.undo();
        persistentArray.undo();
        persistentArray.undo();
        persistentArray.undo();
        assertThat(persistentArray).hasToString("[]");

        persistentArray.redo();
        persistentArray.redo();
        assertThat(persistentArray).hasToString("[1, 2]");

        persistentArray.redo();
        persistentArray.redo();
        assertThat(persistentArray).hasToString("[1, 2, 3, 4]");

        persistentArray.add("6");
        assertThat(persistentArray.getVersionCount()).isEqualTo(6);
        assertThat(persistentArray).hasToString("[1, 2, 3, 4, 6]");

        persistentArray.undo();
        assertThat(persistentArray.getVersionCount()).isEqualTo(6);
        assertThat(persistentArray).hasToString("[1, 2, 3, 4]");
    }

    @Test
    @DisplayName("Проверка вложенности данных")
    void testInsertedData() {
        PersistentArray<PersistentArray<String>> persistentArray = new PersistentArray<>();
        PersistentArray<String> child1 = new PersistentArray<>();
        PersistentArray<String> child2 = new PersistentArray<>();
        PersistentArray<String> child3 = new PersistentArray<>();

        persistentArray.add(child1);
        persistentArray.add(child2);
        persistentArray.add(child3);

        persistentArray.get(0).add("1");
        persistentArray.get(0).add("2");
        persistentArray.get(0).add("3");

        persistentArray.get(1).add("11");
        persistentArray.get(1).add("22");
        persistentArray.get(1).add("33");

        persistentArray.get(2).add("111");
        persistentArray.get(2).add("222");
        persistentArray.get(2).add("333");

        assertThat(persistentArray).hasToString("[[1, 2, 3], [11, 22, 33], [111, 222, 333]]");
        persistentArray.undo();
        assertThat(persistentArray).hasToString("[[1, 2, 3], [11, 22, 33], [111, 222]]");

        PersistentArray<String> child4 = new PersistentArray<>();
        persistentArray.add(1, child4);
        child4.add("Cat");
        assertThat(persistentArray).hasToString("[[1, 2, 3], [Cat], [11, 22, 33], [111, 222]]");

        persistentArray.undo();
        assertThat(persistentArray).hasToString("[[1, 2, 3], [], [11, 22, 33], [111, 222]]");

        persistentArray.get(0).set(0, "Apple");
        persistentArray.get(0).set(1, "Banana");
        assertThat(persistentArray).hasToString("[[Apple, Banana, 3], [], [11, 22, 33], [111, 222]]");

        persistentArray.undo();
        assertThat(persistentArray).hasToString("[[Apple, 2, 3], [], [11, 22, 33], [111, 222]]");
    }

    @Test
    @DisplayName("Тест на foreach")
    void testForEach() {
        PersistentArray<String> persistentArray = new PersistentArray<>(32);

        persistentArray.add("1");
        persistentArray.add("2");
        persistentArray.add("3");

        StringBuilder stringBuilder = new StringBuilder();
        for (String s : persistentArray) {
            stringBuilder.append(s);
        }
        assertThat(stringBuilder).hasToString("123");

        stringBuilder = new StringBuilder();
        persistentArray.forEach(stringBuilder::append);
        assertThat(stringBuilder).hasToString("123");
    }

    @Test
    @DisplayName("Тест на undo и pop")
    void testPop() {
        PersistentArray<String> persistentArray = new PersistentArray<>(32);

        persistentArray.add("1");
        persistentArray.add("2");
        persistentArray.add("3");

        assertThat(persistentArray.pop()).isEqualTo("3");
        assertThat(persistentArray.pop()).isEqualTo("2");

        persistentArray.undo();
        persistentArray.undo();
        assertThat(persistentArray.pop()).isEqualTo("3");
    }

    @Test
    @DisplayName("Тест на undo/redo и set")
    void testPersistentArraySet() {
        PersistentArray<String> persistentArray = new PersistentArray<>(32);

        persistentArray.add("1");
        persistentArray.add("2");
        persistentArray.add("3");

        assertThat(persistentArray).hasToString("[1, 2, 3]");

        persistentArray.set(0, "A");
        persistentArray.set(1, "B");
        persistentArray.set(2, "C");

        assertThat(persistentArray).hasToString("[A, B, C]");

        persistentArray.undo();
        persistentArray.undo();
        assertThat(persistentArray).hasToString("[A, 2, 3]");

        persistentArray.redo();
        assertThat(persistentArray).hasToString("[A, B, 3]");
    }

    @Test
    @DisplayName("Тест на conj/assoc")
    void testPersistentArrayCascade() {
        PersistentArray<String> first = new PersistentArray<>(32);
        first.add("A");
        PersistentArray<String> second = first.conj("B");
        assertThat(first).hasToString("[A]");
        assertThat(second).hasToString("[A, B]");

        PersistentArray<String> third = second.assoc(0, "C");
        assertThat(third).hasToString("[C, B]");
    }

    @Test
    @DisplayName("Тест на операции со stream")
    void testPersistentArrayStream() {
        PersistentArray<Integer> persistentArray = new PersistentArray<>();
        persistentArray.add(1);
        persistentArray.add(2);
        persistentArray.add(3);
        persistentArray.add(4);
        persistentArray.add(5);
        persistentArray.add(6);

        assertThat(persistentArray.stream()
                .map(i -> i * 3)
                .filter(i -> i > 10)
                .toArray()
        ).contains(12, 15, 18);

        persistentArray.undo();
        assertThat(persistentArray.stream()
                .map(i -> i * 3)
                .filter(i -> i > 10)
                .toArray()
        ).contains(12, 15);
    }

    @Test
    @DisplayName("Тест на удаление по индексу")
    void testRemoveByIndex() {
        PersistentArray<String> persistentArray = new PersistentArray<>(32);

        persistentArray.add("1");
        persistentArray.add("2");
        persistentArray.add("3");

        assertThatExceptionOfType(IndexOutOfBoundsException.class)
                .isThrownBy(() -> persistentArray.remove(3));
        assertThatExceptionOfType(IndexOutOfBoundsException.class)
                .isThrownBy(() -> persistentArray.remove(-1));

        assertThat(persistentArray.remove(1)).isEqualTo("2");
        assertThat(persistentArray).hasToString("[1, 3]");

        assertThat(persistentArray.remove(1)).isEqualTo("3");
        assertThat(persistentArray).hasToString("[1]");

        assertThat(persistentArray.remove(0)).isEqualTo("1");
        assertThat(persistentArray).hasToString("[]");

        persistentArray.undo();
        persistentArray.undo();
        persistentArray.undo();

        assertThat(persistentArray).hasToString("[1, 2, 3]");
    }

    @Test
    @DisplayName("Тест на clear")
    void testClear() {
        PersistentArray<String> persistentArray = new PersistentArray<>(32);

        persistentArray.add("1");
        persistentArray.add("2");
        persistentArray.add("3");

        persistentArray.clear();
        assertThat(persistentArray).hasToString("[]");

        persistentArray.undo();
        assertThat(persistentArray).hasToString("[1, 2, 3]");
    }
}