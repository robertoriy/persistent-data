package ru.nsu.lyutaevdronov.array;


import org.jetbrains.annotations.NotNull;
import ru.nsu.lyutaevdronov.common.AbstractPersistentData;
import ru.nsu.lyutaevdronov.common.BTreeNode;
import ru.nsu.lyutaevdronov.common.Pair;
import ru.nsu.lyutaevdronov.common.SpecialPersistentData;

import java.util.*;

/**
 * Persistent массив, который поддерживает undo redo
 */
public class PersistentArray<E> extends AbstractPersistentData implements List<E> {
    /**
     * Стек для хранения состояний, изменения к которым могут быть отменены
     */
    private final Deque<HeadArray<E>> undoStack = new ArrayDeque<>();
    /**
     * Стек для хранения состояний, изменения к которым могут быть повторно применены
     */
    private final Deque<HeadArray<E>> redoStack = new ArrayDeque<>();
    /**
     * Стек для хранения вложенных состояний, которые были добавлены в текущий массив. Этот
     * стек используется для реализации операции undo, чтобы можно было отменить вложенные операции
     */
    private final Deque<SpecialPersistentData> insertedUndoStack = new ArrayDeque<>();
    /**
     * Стек для хранения вложенных состояний, которые были удалены из стека insertedUndo.
     * Этот стек используется для реализации операции redo, чтобы можно было
     * повторно применить операции для вложенных состояний
     */
    private final Deque<SpecialPersistentData> insertedRedoStack = new ArrayDeque<>();

    /**
     * Ссылка на родительский массив, если текущий массив является часть его вложенности
     */
    private SpecialPersistentData parent;

    public PersistentArray() {
        this(6, 5);
    }

    public PersistentArray(int maxSize) {
        this((int) Math.ceil(log(maxSize, (int) Math.pow(2, 5))), 5);
    }

    public PersistentArray(int depth, int bitPerEdge) {
        super(depth, bitPerEdge);
        HeadArray<E> head = new HeadArray<>();
        undoStack.push(head);
        redoStack.clear();
    }

    public PersistentArray(PersistentArray<E> other) {
        super(other.depth, other.bitPerEdge);
        this.undoStack.addAll(other.undoStack);
        this.redoStack.addAll(other.redoStack);
    }

    @Override
    public void undo() {
        if (!insertedUndoStack.isEmpty()) {
            insertedUndoStack.peek().undo();
            insertedRedoStack.push(insertedUndoStack.pop());
        } else {
            if (!undoStack.isEmpty()) {
                redoStack.push(undoStack.pop());
            }
        }
    }

    @Override
    public void redo() {
        if (!insertedRedoStack.isEmpty()) {
            insertedRedoStack.peek().redo();
            insertedUndoStack.push(insertedRedoStack.pop());
        } else {
            if (!redoStack.isEmpty()) {
                undoStack.push(redoStack.pop());
            }
        }
    }

    private void tryParentUndo(E value) {
        if (value instanceof SpecialPersistentData persistentData) {
            persistentData.addParent(this);
        }
        if (parent != null) {
            parent.addChildModification(this);
        }
    }

    /**
     * Возвращает количество элементов в массиве.
     *
     * @return количество элементов в массиве
     */
    @Override
    public int size() {
        return size(getCurrentHead());
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

    private int size(HeadArray<E> head) {
        return head.getSize();
    }

    protected HeadArray<E> getCurrentHead() {
        return this.undoStack.peek();
    }

    private void checkIndex(int index) {
        checkIndex(getCurrentHead(), index);
    }

    private void checkIndex(HeadArray<E> head, int index) {
        if ((index < 0) || (index >= head.getSize())) {
            throw new IndexOutOfBoundsException();
        }
    }

    private boolean isFull(HeadArray<E> head) {
        return head.getSize() >= maxSize;
    }

    /**
     * Возвращает true, если массив не содержит элементов.
     *
     * @return true, если массив не содержит элементов
     */
    @Override
    public boolean isEmpty() {
        return getCurrentHead().getSize() <= 0;
    }

    /**
     * Возвращает количество версий массива.
     *
     * @return количество версий массива
     */
    public int getVersionCount() {
        return undoStack.size() + redoStack.size();
    }

    /**
     * Заменяет элемент в указанной позиции этого массива указанным элементом.
     *
     * @param index   индекс заменяемого элемента
     * @param element элемент, который будет сохранен в указанной позиции
     * @return заменяемый элемент
     */
    @Override
    public E set(int index, E element) {
        checkIndex(index);

        E oldElem = get(index);

        // Копируем путь + получаем лист
        Pair<BTreeNode<E>, Integer> copedNodePath = copyLeafToChange(getCurrentHead(), index);
        BTreeNode<E> copedNode = copedNodePath.key();
        int leafIndex = copedNodePath.value();
        copedNode.getValues().set(leafIndex, element);

        tryParentUndo(element);

        return oldElem;
    }

    /**
     * Возвращает копию массива, в которой заменяет элемент в указанной позиции указанным элементом.
     *
     * @param index   индекс заменяемого элемента
     * @param element элемент, который будет сохранен в указанной позиции
     * @return измененная копия массива
     */
    public PersistentArray<E> assoc(int index, E element) {
        PersistentArray<E> result = new PersistentArray<>(this);
        result.set(index, element);
        return result;
    }

    /**
     * Добавление нового элемента в конец массива.
     *
     * @param element добавляемый элемент
     * @return true если массив изменился в результате вызова
     */
    @Override
    public boolean add(E element) {
        if (isFull(getCurrentHead())) {
            throw new IllegalStateException();
        }

        HeadArray<E> newHead = new HeadArray<>(getCurrentHead());
        undoStack.push(newHead);
        redoStack.clear();
        tryParentUndo(element);

        return add(newHead, element);
    }

    /**
     * Возвращает копию массива, в конец которой добавлен указанный элемент.
     *
     * @param element добавляемый элемент
     * @return измененная копия массива
     */
    public PersistentArray<E> conj(E element) {
        PersistentArray<E> result = new PersistentArray<>(this);
        result.add(element);
        return result;
    }

    /**
     * Добавление нового элемента по индексу.
     * <p>
     * Вставляет указанный элемент в указанную позицию в этом массиве (дополнительная операция).
     * Сдвигает элемент, находящийся в данный момент в этой позиции (если есть),
     * и любые последующие элементы вправо (добавляет единицу к их индексам).
     * </p>
     *
     * @param index   индекс, по которому указанный элемент должен быть вставлен
     * @param element элемент, который нужно вставить
     */
    @Override
    public void add(int index, E element) {
        checkIndex(index);
        if (isFull(getCurrentHead())) {
            throw new IllegalStateException();
        }

        HeadArray<E> oldHead = getCurrentHead();

        Pair<BTreeNode<E>, Integer> copedNodeP = copyLeafToMove(oldHead, index);
        int leafIndex = copedNodeP.value();
        BTreeNode<E> copedNode = copedNodeP.key();
        copedNode.getValues().set(leafIndex, element);

        HeadArray<E> newHead = getCurrentHead();
        for (int i = index; i < oldHead.getSize(); i++) {
            add(newHead, get(oldHead, i));
        }
        tryParentUndo(element);
    }

    private boolean add(HeadArray<E> head, E newElement) {
        add(head).getValues().add(newElement);

        return true;
    }

    private BTreeNode<E> add(HeadArray<E> head) {
        if (isFull(head)) {
            throw new IllegalStateException();
        }

        head.setSize(head.getSize() + 1);
        BTreeNode<E> currentNode = head.getRoot();
        for (int level = bitPerEdge * (depth - 1); level > 0; level -= bitPerEdge) {
            int widthIndex = ((head.getSize() - 1) >> level) & mask;
            BTreeNode<E> tmp;
            BTreeNode<E> newNode;

            if (currentNode.getChilds() == null) {
                currentNode.setChilds(new ArrayList<>());
                newNode = BTreeNode.emptyNode();
                currentNode.getChilds().add(newNode);
            } else {
                if (widthIndex == currentNode.getChilds().size()) {
                    newNode = BTreeNode.emptyNode();
                    currentNode.getChilds().add(newNode);
                } else {
                    tmp = currentNode.getChilds().get(widthIndex);
                    newNode = BTreeNode.of(tmp);
                    currentNode.getChilds().set(widthIndex, newNode);
                }
            }
            currentNode = newNode;
        }

        if (currentNode.getValues() == null) {
            currentNode.setValues(new ArrayList<>());
        }
        return currentNode;
    }

    /**
     * Удаляет последний элемент массива.
     *
     * @return последний элемент массива
     */
    public E pop() {
        if (isEmpty()) {
            throw new NoSuchElementException("Array is empty");
        }

        HeadArray<E> newHead = new HeadArray<>(getCurrentHead(), -1);
        undoStack.push(newHead);
        redoStack.clear();
        LinkedList<Pair<BTreeNode<E>, Integer>> path = new LinkedList<>();
        path.add(new Pair<>(newHead.getRoot(), 0));
        for (int level = bitPerEdge * (depth - 1); level > 0; level -= bitPerEdge) {
            int index = (newHead.getSize() >> level) & mask;
            BTreeNode<E> tmp;
            BTreeNode<E> newNode;
            tmp = path.getLast().key().getChilds().get(index);
            newNode = BTreeNode.of(tmp);
            path.getLast().key().getChilds().set(index, newNode);
            path.add(new Pair<>(newNode, index));
        }

        int index = newHead.getSize() & mask;
        E result = path.getLast().key().getValues().remove(index);

        // удаляем с конца ноды на пути к последнему элементу, если в них нет ни значений ни дочерних узлов
        for (int i = path.size() - 1; i >= 1; i--) {
            Pair<BTreeNode<E>, Integer> elem = path.get(i);
            if (elem.key().isEmpty()) {
                path.get(i - 1).key().getChilds().remove((int) elem.value());
            } else {
                break;
            }
        }

        return result;
    }

    /**
     * Удаляет элемент по указанному индексу.
     * <p>
     * Удаляет элемент в указанной позиции в этом массиве.
     * Сдвигает любые последующие элементы влево (вычитает единицу из их индексов).
     * Возвращает элемент, который был удален из массива.
     * </p>
     *
     * @param index индекс удаляемого элемента
     * @return удаленный элемент
     */
    @Override
    public E remove(int index) {
        checkIndex(index);

        E result = get(index);

        HeadArray<E> oldHead = getCurrentHead();
        HeadArray<E> newHead;

        if (index == 0) {
            newHead = new HeadArray<>();
            undoStack.push(newHead);
            redoStack.clear();
        } else {
            Pair<BTreeNode<E>, Integer> copedNodeP = copyLeafToMove(oldHead, index);
            int leafIndex = copedNodeP.value();
            BTreeNode<E> copedNode = copedNodeP.key();
            copedNode.getValues().remove(leafIndex);

            newHead = getCurrentHead();
            newHead.setSize(newHead.getSize() - 1);
        }

        for (int i = index + 1; i < oldHead.getSize(); i++) {
            add(newHead, get(oldHead, i));
        }

        return result;
    }

    /**
     * Удаляет все элементы из этого массива.
     * Массив будет пуст после возврата этого вызова.
     */
    @Override
    public void clear() {
        HeadArray<E> head = new HeadArray<>();
        undoStack.push(head);
        redoStack.clear();
    }

    /**
     * Копируем все ноды на пути до нужного индекса, остальные переиспользуются
     * Для операций, изменения элемента по индексу
     */
    private Pair<BTreeNode<E>, Integer> copyLeafToChange(HeadArray<E> head, int index) {
        HeadArray<E> newHead = new HeadArray<>(head);
        undoStack.push(newHead);
        redoStack.clear();

        BTreeNode<E> currentNode = newHead.getRoot();
        for (int level = bitPerEdge * (depth - 1); level > 0; level -= bitPerEdge) {
            int widthIndex = (index >> level) & mask;
            BTreeNode<E> tmp;
            BTreeNode<E> newNode;
            tmp = currentNode.getChilds().get(widthIndex);
            newNode = BTreeNode.of(tmp);
            currentNode.getChilds().set(widthIndex, newNode);
            currentNode = newNode;
        }

        return new Pair<>(currentNode, index & mask);
    }

    /**
     * Копирует ноды следующим образом:
     * элементы "до нужного индекса" переиспользуются,
     * "после нужного индекса" пропускаются
     * Для операций, требующих сдвига всех элементов (вставка/удаление в середину)
     */
    private Pair<BTreeNode<E>, Integer> copyLeafToMove(HeadArray<E> oldHead, int index) {
        int level = bitPerEdge * (depth - 1);
        HeadArray<E> newHead = new HeadArray<>(oldHead, index + 1, (index >> level) & mask);
        undoStack.push(newHead);
        redoStack.clear();
        BTreeNode<E> currentNode = newHead.getRoot();
        for (; level > 0; level -= bitPerEdge) {
            int widthIndex = (index >> level) & mask;
            int widthIndexNext = (index >> (level - bitPerEdge)) & mask;
            BTreeNode<E> tmp;
            BTreeNode<E> newNode;
            tmp = currentNode.getChilds().get(widthIndex);
            newNode = BTreeNode.of(tmp, widthIndexNext);
            currentNode.getChilds().set(widthIndex, newNode);
            currentNode = newNode;
        }
        return new Pair<>(currentNode, index & mask);
    }

    /**
     * Возвращает элемент в указанной позиции в массиве.
     *
     * @param index индекс возвращаемого элемента
     * @return элемент в указанной позиции в массиве
     */
    @Override
    public E get(int index) {
        return get(getCurrentHead(), index);
    }

    private E get(HeadArray<E> head, int index) {
        checkIndex(head, index);
        return getLeaf(head, index).getValues().get(index & mask);
    }

    private BTreeNode<E> getLeaf(HeadArray<E> head, int index) {
        checkIndex(head, index);
        // 0101 0111 0100 1010 1010 1001
        BTreeNode<E> node = head.getRoot();
        for (int level = bitPerEdge * (depth - 1); level > 0; level -= bitPerEdge) {
            int widthIndex = (index >> level) & mask;
            node = node.getChilds().get(widthIndex);
        }

        return node;
    }

    /**
     * Возвращает строковое представление содержимого массива.
     * <p>
     * Строковое представление состоит из списка элементов этого персистентного массива, заключенного в квадратные скобки («[]»).
     * Смежные элементы разделяются символами «, » (запятая с последующим пробелом).
     *
     * @return строковое представление массива
     */
    @Override
    public String toString() {
        return toString(getCurrentHead());
    }

    private String toString(HeadArray<E> head) {
        return Arrays.toString(toArray(head));
    }

    /**
     * Возвращает массив, содержащий все элементы этого персистентного массива в правильной последовательности (от первого до последнего элемента).
     *
     * @return массив, содержащий все элементы этого персистентного массива в правильной последовательности
     */
    @Override
    public Object @NotNull [] toArray() {
        return toArray(getCurrentHead());
    }

    private Object[] toArray(HeadArray<E> head) {
        Object[] objects = new Object[head.getSize()];
        for (int i = 0; i < objects.length; i++) {
            objects[i] = this.get(head, i);
        }
        return objects;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return false;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public int indexOf(Object o) {
        return 0;
    }

    @Override
    public int lastIndexOf(Object o) {
        return 0;
    }

    @Override
    public ListIterator<E> listIterator() {
        return null;
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return null;
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return null;
    }

    @Override
    public Iterator<E> iterator() {
        return new PersistentArrayIterator<>();
    }

    /**
     * Итератор над персистентным массивом.
     */
    public class PersistentArrayIterator<T> implements Iterator<T> {
        int index = 0;

        /**
         * Возвращает true, если итерация содержит больше элементов.
         *
         * @return true, если итерация имеет больше элементов
         */
        @Override
        public boolean hasNext() {
            return index < size();
        }

        /**
         * Возвращает следующий элемент в итерации.
         *
         * @return следующий элемент в итерации
         */
        @Override
        public T next() {
            return (T) get(index++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}