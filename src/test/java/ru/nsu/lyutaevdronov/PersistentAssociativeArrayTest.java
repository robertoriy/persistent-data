package ru.nsu.lyutaevdronov;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.nsu.lyutaevdronov.map.PersistentAssociativeArray;

import static org.assertj.core.api.Assertions.assertThat;

final class PersistentAssociativeArrayTest {
    @Test
    @DisplayName("Тест на проверку наличия добавленных элементов")
    void testPutGet() {
        PersistentAssociativeArray<String, Integer> persistentMap = new PersistentAssociativeArray<>();
        persistentMap.put("A", 1);
        persistentMap.put("B", 2);
        persistentMap.put("C", 3);

        assertThat(persistentMap)
                .containsEntry("A", 1)
                .containsEntry("B", 2)
                .containsEntry("C", 3)
                .containsKey("A")
                .containsKey("B")
                .containsKey("C")
                .containsValue(1)
                .containsValue(2)
                .containsValue(3);
    }


    @Test
    @DisplayName("Тест на undo/redo для map")
    void testUndoRedo() {
        PersistentAssociativeArray<String, Integer> persistentMap = new PersistentAssociativeArray<>();
        persistentMap.put("A", 1);
        persistentMap.put("B", 2);
        persistentMap.put("C", 3);

        persistentMap.undo();
        assertThat(persistentMap)
                .containsEntry("A", 1)
                .containsEntry("B", 2)
                .doesNotContainEntry("C", 3);

        persistentMap.undo();
        assertThat(persistentMap)
                .containsEntry("A", 1)
                .doesNotContainEntry("B", 2)
                .doesNotContainEntry("C", 3);

        persistentMap.redo();
        assertThat(persistentMap)
                .containsEntry("A", 1)
                .containsEntry("B", 2)
                .doesNotContainEntry("C", 3);

        persistentMap.redo();
        assertThat(persistentMap)
                .containsEntry("A", 1)
                .containsEntry("B", 2)
                .containsEntry("C", 3);

        persistentMap.undo();
        persistentMap.undo();
        persistentMap.undo();
        assertThat(persistentMap).isEmpty();

        persistentMap.put("Cat", 1);
        assertThat(persistentMap)
                .containsEntry("Cat", 1);
    }

    @Test
    @DisplayName("Тест по foreach")
    void testForEach() {
        PersistentAssociativeArray<String, Integer> persistentMap = new PersistentAssociativeArray<>();
        persistentMap.put("A", 1);
        persistentMap.put("B", 2);
        persistentMap.put("C", 3);

        StringBuilder stringBuilder = new StringBuilder();
        persistentMap.forEach((k, v) -> stringBuilder.append(k).append(":").append(v).append(" "));
        String result = stringBuilder.toString();

        assertThat(result).contains("A:1").contains("B:2").contains("C:3");
    }

    @Test
    @DisplayName("Тест на clear map")
    void testClear() {
        PersistentAssociativeArray<String, Integer> persistentMap = new PersistentAssociativeArray<>();
        persistentMap.put("A", 1);
        persistentMap.put("B", 2);
        persistentMap.put("C", 3);

        assertThat(persistentMap).hasSize(3);

        persistentMap.clear();
        assertThat(persistentMap).isEmpty();
    }

    @Test
    @DisplayName("Тест на удаление")
    void testRemove() {
        PersistentAssociativeArray<String, Integer> persistentMap = new PersistentAssociativeArray<>();
        persistentMap.put("A", 1);
        persistentMap.put("B", 2);
        persistentMap.put("C", 3);

        persistentMap.remove("A");
        assertThat(persistentMap).doesNotContainEntry("A", 1);

        persistentMap.remove("C");
        assertThat(persistentMap).doesNotContainEntry("C", 3);

        persistentMap.undo();
        persistentMap.undo();

        assertThat(persistentMap)
                .containsEntry("A", 1)
                .containsEntry("B", 2)
                .containsEntry("C", 3);

        persistentMap.redo();

        assertThat(persistentMap)
                .doesNotContainEntry("A", 1)
                .containsEntry("B", 2)
                .containsEntry("C", 3);
    }

    @Test
    @DisplayName("Тест на undo/redo с модификацией элемента")
    void testModifyAndUndoRedo() {
        PersistentAssociativeArray<String, Integer> persistentMap = new PersistentAssociativeArray<>();
        persistentMap.put("A", 1);
        persistentMap.put("B", 2);
        persistentMap.put("C", 3);

        persistentMap.put("A", 100);
        assertThat(persistentMap)
                .containsEntry("A", 100)
                .doesNotContainEntry("A", 1);

        persistentMap.undo();
        assertThat(persistentMap)
                .containsEntry("A", 1)
                .doesNotContainEntry("A", 100);

        persistentMap.redo();
        assertThat(persistentMap)
                .containsEntry("A", 100)
                .doesNotContainEntry("A", 1);
    }

    @Test
    @DisplayName("Тест на вложенность")
    void testCascade() {
        var persistentMap = new PersistentAssociativeArray<String, PersistentAssociativeArray<String, Integer>>();
        var first = new PersistentAssociativeArray<String, Integer>();
        var second = new PersistentAssociativeArray<String, Integer>();
        var third = new PersistentAssociativeArray<String, Integer>();

        persistentMap.put("A", first);
        persistentMap.put("B", second);
        persistentMap.put("C", third);
        first.put("A", 1);
        second.put("AA", 11);
        third.put("AAA", 111);
        first.put("B", 2);
        second.put("BB", 22);
        third.put("BBB", 222);
        third.put("CCC", 333);

        assertThat(first)
                .hasSize(2)
                .containsEntry("A", 1)
                .containsEntry("B", 2);
        assertThat(second)
                .hasSize(2)
                .containsEntry("AA", 11)
                .containsEntry("BB", 22);
        assertThat(third)
                .hasSize(3)
                .containsEntry("AAA", 111)
                .containsEntry("BBB", 222)
                .containsEntry("CCC", 333);

        persistentMap.undo();
        persistentMap.undo();
        persistentMap.undo();
        persistentMap.undo();

        assertThat(first)
                .hasSize(1)
                .containsEntry("A", 1)
                .doesNotContainEntry("B", 2);
        assertThat(second)
                .hasSize(1)
                .containsEntry("AA", 11)
                .doesNotContainEntry("BB", 22);
        assertThat(third)
                .hasSize(1)
                .containsEntry("AAA", 111)
                .doesNotContainEntry("BBB", 222)
                .doesNotContainEntry("CCC", 333);

        persistentMap.redo();
        persistentMap.redo();

        assertThat(first)
                .hasSize(2)
                .containsEntry("A", 1)
                .containsEntry("B", 2);
        assertThat(second)
                .hasSize(2)
                .containsEntry("AA", 11)
                .containsEntry("BB", 22);
        assertThat(third)
                .hasSize(1)
                .containsEntry("AAA", 111)
                .doesNotContainEntry("BBB", 222)
                .doesNotContainEntry("CCC", 333);
    }

    @Test
    @DisplayName("Тест на conj")
    void testConj() {
        var first = new PersistentAssociativeArray<String, Integer>();
        first.put("A", 1);
        var second = first.conj("B", 2);

        assertThat(first)
                .hasSize(1)
                .containsEntry("A", 1)
                .doesNotContainEntry("B", 2);
        assertThat(second)
                .hasSize(2)
                .containsEntry("A", 1)
                .containsEntry("B", 2);
    }
}