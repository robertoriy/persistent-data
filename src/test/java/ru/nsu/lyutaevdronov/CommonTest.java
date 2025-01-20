package ru.nsu.lyutaevdronov;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.nsu.lyutaevdronov.array.PersistentArray;
import ru.nsu.lyutaevdronov.common.PersistentData;
import ru.nsu.lyutaevdronov.map.PersistentAssociativeArray;

import static org.assertj.core.api.Assertions.assertThat;

final class CommonTest {
    @Test
    @DisplayName("Тест на добавление и удаление элементов")
    void testAddAndGet() {
        PersistentArray<PersistentData> persistentArray = new PersistentArray<>(32);

        PersistentArray<String> array = new PersistentArray<>(32);
        PersistentAssociativeArray<String, Integer> map = new PersistentAssociativeArray<>();

        persistentArray.add(array);
        persistentArray.add(map);

        array.add("Cat");
        array.add("Dog");
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);

        assertThat(array.get(0)).isEqualTo("Cat");
        assertThat(array.get(1)).isEqualTo("Dog");

        assertThat(map)
                .containsEntry("A", 1)
                .containsEntry("B", 2)
                .containsEntry("C", 3);
    }
}
