package com.pippsford.common;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Implementation of a checked list. The implementation is drawn from the java.util.Collections class. Creating our own version allows us to check if a
 * collection is type safe, and what the member type is.
 *
 * @author Simon Greatrix on 01/11/2017.
 */
public class TypeSafeList<E> implements List<E> {

  /** Function for creating lists of BigDecimals. */
  public static final Function<Object, BigDecimal> F_BIG_DECIMAL = new ListType<>(TypeSafeMap::asBigDecimal);

  /** Function for creating lists of BigIntegers. */
  public static final Function<Object, BigInteger> F_BIG_INTEGER = new ListType<>(TypeSafeMap::asBigInteger);

  /** Function for creating lists of Doubles. */
  public static final Function<Object, Double> F_DOUBLE = new ListType<>(TypeSafeMap::asDouble);

  /** Function for creating lists of Integers. */
  public static final Function<Object, Integer> F_INT = new ListType<>(TypeSafeMap::asInt);

  /** Function for creating lists of Longs. */
  public static final Function<Object, Long> F_LONG = new ListType<>(TypeSafeMap::asLong);

  /** Function for creating lists of Maps. */
  public static final Function<Object, TypeSafeMap> F_MAP = new ListType<>(TypeSafeMap::asMap);

  /** Function for creating lists of Objects. */
  public static final Function<Object, Object> F_OBJECT = v -> v;

  /** Function for creating lists of Strings. */
  public static final Function<Object, String> F_STRING = v -> (v != null) ? v.toString() : null;



  /**
   * A function wrapper to convert between the asXXX static methods here, which return a null if no conversion is possible, and the asList contract which needs
   * a ClassCastException.
   */
  public static class ListType<T> implements Function<Object, T> {

    private final Function<Object, T> function;


    public ListType(Function<Object, T> function) {
      this.function = function;
    }


    @Override
    public T apply(Object o) {
      if (o == null) {
        return null;
      }
      T t = function.apply(o);
      if (t == null) {
        throw new ClassCastException(o.getClass() + " is not convertible");
      }
      return t;
    }

  }



  private final Function<Object, E> convertor;

  private final List<E> list;


  public TypeSafeList(Function<Object, E> convertor) {
    list = new ArrayList<>();
    this.convertor = Objects.requireNonNull(convertor, "type");
  }


  public TypeSafeList(Class<E> cls) {
    list = new ArrayList<>();
    convertor = cls::cast;
  }


  public TypeSafeList(List<E> c, Function<Object, E> convertor) {
    list = Objects.requireNonNull(c, "list");
    this.convertor = Objects.requireNonNull(convertor, "type");
  }


  public boolean add(E e) {
    return list.add(typeCheck(e));
  }


  public void add(int index, E element) {
    list.add(index, typeCheck(element));
  }


  /**
   * {@inheritDoc}
   */
  public boolean addAll(Collection<? extends E> coll) {
    // Doing things this way insulates us from concurrent changes
    // in the contents of coll and provides all-or-nothing
    // semantics (which we wouldn't get if we type-checked each
    // element as we added it)
    return list.addAll(checkedCopyOf(coll));
  }


  public boolean addAll(int index, Collection<? extends E> c) {
    return list.addAll(index, checkedCopyOf(c));
  }


  public void addObject(int index, Object element) {
    list.add(index, typeCheck(element));
  }


  @SuppressWarnings("unchecked")
  Collection<E> checkedCopyOf(Collection<? extends E> coll) {
    if (coll instanceof TypeSafeList) {
      TypeSafeList<?> other = (TypeSafeList<?>) coll;
      if (other.getConvertor().equals(getConvertor())) {
        return (TypeSafeList<E>) other;
      }
    }

    ArrayList<E> arrayList = new ArrayList<>();
    for (Object o : coll) {
      arrayList.add(convertor.apply(o));
    }
    return arrayList;
  }


  public void clear() {
    list.clear();
  }


  public boolean contains(Object o) {
    return list.contains(o);
  }


  public boolean containsAll(Collection<?> coll) {
    return list.containsAll(coll);
  }


  public boolean equals(Object o) {
    return o == this || list.equals(o);
  }


  // Override default methods in Collection
  @Override
  public void forEach(Consumer<? super E> action) {
    list.forEach(action);
  }


  public E get(int index) {
    return list.get(index);
  }


  public Function<Object, E> getConvertor() {
    return convertor;
  }


  public int hashCode() {
    return list.hashCode();
  }


  public int indexOf(Object o) {
    return list.indexOf(o);
  }


  public boolean isEmpty() {
    return list.isEmpty();
  }


  /** {@inheritDoc} */
  public Iterator<E> iterator() {
    // JDK-6363904 - unwrapped iterator could be typecast to
    // ListIterator with unsafe set()
    final Iterator<E> it = list.iterator();
    return new Iterator<>() {
      public boolean hasNext() {
        return it.hasNext();
      }


      public E next() {
        return it.next();
      }


      @Override
      public void remove() {
        it.remove();
      }
    };
  }


  public int lastIndexOf(Object o) {
    return list.lastIndexOf(o);
  }


  public ListIterator<E> listIterator() {
    return listIterator(0);
  }


  /** {@inheritDoc} */
  public ListIterator<E> listIterator(final int index) {
    final ListIterator<E> i = list.listIterator(index);

    return new ListIterator<>() {
      public void add(E e) {
        i.add(typeCheck(e));
      }


      @Override
      public void forEachRemaining(Consumer<? super E> action) {
        i.forEachRemaining(action);
      }


      public boolean hasNext() {
        return i.hasNext();
      }


      public boolean hasPrevious() {
        return i.hasPrevious();
      }


      public E next() {
        return i.next();
      }


      public int nextIndex() {
        return i.nextIndex();
      }


      public E previous() {
        return i.previous();
      }


      public int previousIndex() {
        return i.previousIndex();
      }


      public void remove() {
        i.remove();
      }


      public void set(E e) {
        i.set(typeCheck(e));
      }
    };
  }


  @Override
  public Stream<E> parallelStream() {
    return list.parallelStream();
  }


  public boolean remove(Object o) {
    return list.remove(o);
  }


  public E remove(int index) {
    return list.remove(index);
  }


  public boolean removeAll(Collection<?> coll) {
    return list.removeAll(coll);
  }


  @Override
  public boolean removeIf(Predicate<? super E> filter) {
    return list.removeIf(filter);
  }


  /**
   * {@inheritDoc}
   *
   * @throws ClassCastException if the class of an element returned by the operator prevents it from being added to this collection. The exception may be
   *                            thrown after some elements of the list have already been replaced.
   */
  @Override
  public void replaceAll(UnaryOperator<E> operator) {
    Objects.requireNonNull(operator);
    list.replaceAll(e -> typeCheck(operator.apply(e)));
  }


  public boolean retainAll(Collection<?> coll) {
    return list.retainAll(coll);
  }


  public E set(int index, E element) {
    return list.set(index, typeCheck(element));
  }


  public int size() {
    return list.size();
  }


  @Override
  public void sort(Comparator<? super E> c) {
    list.sort(c);
  }


  @Override
  public Spliterator<E> spliterator() {
    return list.spliterator();
  }


  @Override
  public Stream<E> stream() {
    return list.stream();
  }


  public List<E> subList(int fromIndex, int toIndex) {
    return new TypeSafeList<>(list.subList(fromIndex, toIndex), convertor);
  }


  public Object[] toArray() {
    return list.toArray();
  }


  public <T> T[] toArray(T[] a) {
    return list.toArray(a);
  }


  public String toString() {
    return list.toString();
  }


  E typeCheck(Object o) {
    return convertor.apply(o);
  }

}
