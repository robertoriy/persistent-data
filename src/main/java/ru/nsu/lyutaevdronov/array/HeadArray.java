package ru.nsu.lyutaevdronov.array;


import lombok.Getter;
import lombok.Setter;
import ru.nsu.lyutaevdronov.common.BTreeNode;

@Getter
public class HeadArray<E> {
    private final BTreeNode<E> root;
    @Setter
    private int size = 0;

    public HeadArray() {
        this.root = BTreeNode.emptyNode();
    }

    public HeadArray(HeadArray<E> other) {
        this.root = BTreeNode.of(other.root);
        this.size = other.size;
    }

    public HeadArray(HeadArray<E> other, Integer sizeDelta) {
        this.root = BTreeNode.of(other.root);
        this.size = other.size + sizeDelta;
    }

    public HeadArray(HeadArray<E> other, Integer newSize, Integer maxIndex) {
        this.root = BTreeNode.of(other.root, maxIndex);
        this.size = newSize;
    }
}