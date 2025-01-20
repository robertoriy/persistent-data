package ru.nsu.lyutaevdronov.common;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Узел B-дерева для персистентной коллекции
 */
@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BTreeNode<E> {
    private List<BTreeNode<E>> childs;

    private List<E> values;

    public static <E> BTreeNode<E> emptyNode() {
        return new BTreeNode<>();
    }

    public static <E> BTreeNode<E> of(BTreeNode<E> other) {
        return new BTreeNode<>(other);
    }

    public static <E> BTreeNode<E> of(BTreeNode<E> other, int maxIndex) {
        return new BTreeNode<>(other, maxIndex);
    }

    private BTreeNode(BTreeNode<E> other) {
        if (other != null) {
            if (other.childs != null) {
                childs = new ArrayList<>(other.childs);
            }

            if (other.values != null) {
                values = new ArrayList<>(other.values);
            }
        }
    }

    private BTreeNode(BTreeNode<E> other, int maxIndex) {
        if (other.childs != null) {
            childs = new ArrayList<>();
            for (int i = 0; i <= maxIndex; i++) {
                childs.add(other.childs.get(i));
            }
        }

        if (other.values != null) {
            values = new ArrayList<>();
            for (int i = 0; i <= maxIndex; i++) {
                values.add(other.values.get(i));
            }
        }
    }

    /**
     * Возвращает true, если узел не имеет потомков и не содержит значений.
     *
     * @return true, если узел не имеет потомков и не содержит значений
     */
    public boolean isEmpty() {
        if ((childs == null) && (values == null)) {
            return true;
        }

        if ((values != null) && (!values.isEmpty())) {
            return false;
        }

        return (childs == null) || (childs.isEmpty());
    }

    /**
     * Возвращает строковое представление содержимого узла.
     *
     * @return строковое представление содержимого узла
     */
    @Override
    public String toString() {
        String childNodes = childs == null ? "[child null]" : Arrays.toString(childs.toArray());
        String values = this.values == null ? "[values null]" : Arrays.toString(this.values.toArray());
        return String.format("%09x %s %s", hashCode(), childNodes, values);
    }

    private String drawTab(int count) {
        return "  ".repeat(Math.max(0, count));
    }

    private String drawGraph(BTreeNode<E> node, int level) {
        String hash = String.format("%09x", node.hashCode()) + " ";
        StringBuilder result = new StringBuilder();
        if (node.childs == null) {
            if (node.values == null) {
                return drawTab(level) + hash + "\n";
            } else {
                return drawTab(level) + hash + node.values.toString() + "\n";
            }
        } else {
            result.append(drawTab(level)).append(hash).append("\n");

            for (BTreeNode<E> n : node.childs) {
                if (n != null) {
                    result.append(drawGraph(n, level + 1));
                }
            }
        }
        return result.toString();
    }

    public String drawGraph() {
        return drawGraph(this, 0);
    }
}