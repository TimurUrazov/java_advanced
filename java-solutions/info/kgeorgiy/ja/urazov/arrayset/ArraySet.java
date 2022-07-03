package info.kgeorgiy.ja.urazov.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final Comparator<? super T> comparator;
    private final InvertibleArray<T> array;

    public ArraySet() {
        comparator = null;
        array = getEmptyArray();
    }

    public ArraySet(final Collection<? extends T> collection) {
        this(collection, null);
    }

    public ArraySet(final SortedSet<T> sortedSet) {
        comparator = sortedSet.comparator();
        array = wrapArray(sortedSet.toArray());
    }

    @SuppressWarnings("unchecked")
    public ArraySet(final Collection<? extends T> collection, final Comparator<? super T> comparator) {
        this.comparator = comparator;
        final T[] array = (T[]) collection.toArray();
        Arrays.sort(array, comparator);
        this.array = new InvertibleArray<>(removeDuplicates(array));
    }

    private ArraySet(final InvertibleArray<T> array, final Comparator<? super T> comparator) {
        this.array = array;
        this.comparator = comparator;
    }

    @SuppressWarnings("unchecked")
    private InvertibleArray<T> wrapArray(final Object[] array) {
        return new InvertibleArray<>((T[]) array);
    }

    private InvertibleArray<T> getEmptyArray() {
        return wrapArray(new Object[0]);
    }

    @Override
    public Iterator<T> iterator() {
        return new UnmodifiableArrayIterator<>(array.iterator());
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new InvertibleArray<>(array), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive,
                                  T toElement, boolean toInclusive) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("fromElement > toElement");
        }
        return createSubSet(fromElement, fromInclusive, toElement, toInclusive);
    }

    private ArraySet<T> getEmptySet() {
        return new ArraySet<>(getEmptyArray(), comparator);
    }

    private int findIndex(T e, final boolean inclusive, final boolean higher) {
        int index = binarySearchByComparator(e);
        if (index >= 0) {
            if (!inclusive) {
                index += (higher ? 1 : -1);
            }
            return index;
        }
        return -1 - index - (higher ? 0 : 1);
    }

    private int higherIndex(T e, final boolean inclusive) {
        return findIndex(e, inclusive, true);
    }

    private int lowerIndex(T e, final boolean inclusive) {
        return findIndex(e, inclusive, false);
    }

    private boolean subSetIsCorrect(final int fromIndex, final int toIndex) {
        return !isEmpty() && 0 <= fromIndex && fromIndex <= toIndex && toIndex < size();
    }

    private ArraySet<T> createSubSet(final T fromElement, final boolean fromInclusive,
                                     final T toElement, final boolean toInclusive) {
        final int fromIndex = higherIndex(fromElement, fromInclusive);
        final int toIndex = lowerIndex(toElement, toInclusive);
        if (!subSetIsCorrect(fromIndex, toIndex)) {
            return getEmptySet();
        }
        return new ArraySet<>(array.subList(fromIndex, toIndex + 1), comparator);
    }

    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return binarySearchByComparator((T) o) >= 0;
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        if (isEmpty()) {
            return getEmptySet();
        }
        return createSubSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        if (isEmpty()) {
            return getEmptySet();
        }
        return createSubSet(fromElement, inclusive, last(), true);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    private void checkSetForEmptiness() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public T first() {
        checkSetForEmptiness();
        return array.get(0);
    }

    @Override
    public T last() {
        checkSetForEmptiness();
        return array.get(size() - 1);
    }

    @Override
    public int size() {
        return array.size();
    }

    @Override
    public T lower(T e) {
        return get(lowerIndex(e, false));
    }

    @Override
    public T floor(T e) {
        return get(lowerIndex(e, true));
    }

    @Override
    public T ceiling(T e) {
        return get(higherIndex(e, true));
    }

    @Override
    public T higher(T e) {
        return get(higherIndex(e, false));
    }

    private T get(int index) {
        if (0 <= index && index < size()) {
            return array.get(index);
        }
        return null;
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    private int compare(final T first, final T second) {
        if (comparator == null) {
            return ((Comparable<? super T>) first).compareTo(second);
        }
        return comparator.compare(first, second);
    }

    private int binarySearchByComparator(final T element) {
        return Collections.binarySearch(array, element, comparator);
    }

    private T[] removeDuplicates(final T[] array) {
        int j = 0;
        for (int i = 0; i < array.length; i++) {
            if (compare(array[i], array[j]) != 0) {
                array[++j] = array[i];
            }
        }
        return Arrays.copyOf(array, array.length != 0 ? j + 1 : j);
    }

    private static final class InvertibleArray<T> extends AbstractList<T> implements RandomAccess {
        private final T[] data;
        private final int offset;
        private final int size;
        private boolean descendingOrder;

        public InvertibleArray(final T[] data) {
            this(data, 0, data.length);
        }

        public InvertibleArray(final T[] data, final int offset, final int size) {
            this.data = data;
            this.offset = offset;
            this.size = size;
        }

        public InvertibleArray(final T[] data, final int offset, final int size, final boolean descendingOrder) {
            this(data, offset, size);
            this.descendingOrder = descendingOrder;
        }

        public InvertibleArray(final InvertibleArray<T> array) {
            this(array.data, array.offset, array.size);
            descendingOrder = !array.descendingOrder;
        }

        @Override
        public InvertibleArray<T> subList(int fromIndex, int toIndex) {
            final int sizeOfSubSet = toIndex - fromIndex;
            fromIndex += offset;
            if (descendingOrder) {
                fromIndex += size - toIndex;
            }
            return new InvertibleArray<>(data, fromIndex, sizeOfSubSet, descendingOrder);
        }

        @Override
        public T get(int index) {
            return data[descendingOrder ? size + offset - 1 - index : index + offset];
        }

        @Override
        public int size() {
            return size;
        }
    }

    private static class UnmodifiableArrayIterator<T> implements Iterator<T> {
        private final Iterator<T> it;

        UnmodifiableArrayIterator(final Iterator<T> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public T next() {
            return it.next();
        }
    }
}
