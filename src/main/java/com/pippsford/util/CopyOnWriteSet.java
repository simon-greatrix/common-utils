package com.pippsford.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * A thread safe set which is copied on every change. Ideally this set is used only when changes to the set are rare.
 *
 * @author Simon Greatrix
 */
public class CopyOnWriteSet<E> implements Set<E> {

  /** The write lock. */
  private final Object writeLock = new Object();

  /** The backing set. */
  protected Set<E> set;


  /** Create a concurrent set. */
  public CopyOnWriteSet() {
    set = createNewSet(-1);
  }


  @Override
  public boolean add(E e) {
    synchronized (getLock()) {
      if (set.contains(e)) {
        return false;
      }
      Set<E> copy = copySet(set.size() + 1);
      boolean r = copy.add(e);
      set = copy;
      return r;
    }
  }


  @Override
  public boolean addAll(@Nonnull Collection<? extends E> c) {
    synchronized (getLock()) {
      Set<E> copy = copySet(set.size() + 1);
      boolean r = copy.addAll(c);
      set = copy;
      return r;
    }
  }


  @Override
  public void clear() {
    synchronized (getLock()) {
      set = createNewSet(-1);
    }
  }


  @Override
  public boolean contains(Object o) {
    return set.contains(o);
  }


  @Override
  public boolean containsAll(@Nonnull Collection<?> c) {
    return set.containsAll(c);
  }


  /**
   * Create a copy of the backing set for update.
   *
   * @param size target capacity of new set
   *
   * @return copy of this set
   */
  protected Set<E> copySet(int size) {
    Set<E> copy = createNewSet(size);
    copy.addAll(set);
    return copy;
  }


  /**
   * Create a new set. Override this method if you want to use a different backing set from a java.util.HashSet
   *
   * @param suggestedCapacity the expected capacity required. Use {@literal <=0} to suggest a default.
   *
   * @return a new map of the correct type
   */
  protected Set<E> createNewSet(int suggestedCapacity) {
    if (suggestedCapacity <= 0) {
      return new HashSet<>();
    }

    // The JavaDoc for HashSet specifies a default load factor of 0.75f.
    // The map is rehashed when size > (capacity * load-factor) so to
    // contain our specified number of entries we must have:
    // capacity > ( size / load-factor )
    return new HashSet<>((int) (1 + (suggestedCapacity / 0.75)), 0.75f);
  }


  /**
   * Get the object which protects the map during writes. When synchronized on this object, no other thread will modify this map.
   *
   * @return the lock object
   */
  public Object getLock() {
    return writeLock;
  }


  @Override
  public boolean isEmpty() {
    return set.isEmpty();
  }


  @Override
  @Nonnull
  public Iterator<E> iterator() {
    final Iterator<E> itr = set.iterator();
    return new Iterator<>() {
      private E current = null;

      private boolean hasCurrent = false;


      @Override
      public boolean hasNext() {
        return itr.hasNext();
      }


      @Override
      public E next() {
        E e = itr.next();
        hasCurrent = true;
        current = e;
        return e;
      }


      @Override
      public void remove() {
        if (!hasCurrent) {
          throw new IllegalStateException("No current value");
        }
        CopyOnWriteSet.this.remove(current);
        hasCurrent = false;
      }

    };
  }


  @Override
  public boolean remove(Object o) {
    synchronized (getLock()) {
      if (!set.contains(o)) {
        return false;
      }
      Set<E> copy = copySet(set.size());
      boolean r = copy.remove(o);
      set = copy;
      return r;
    }
  }


  @Override
  public boolean removeAll(@Nonnull Collection<?> c) {
    synchronized (getLock()) {
      Set<E> copy = copySet(set.size());
      boolean r = copy.removeAll(c);
      set = copy;
      return r;
    }
  }


  /**
   * Replace the contents of this set with the contents of the supplied set.
   *
   * @param s the new contents
   */
  public void replace(Set<E> s) {
    Set<E> ns = createNewSet(s.size());
    ns.addAll(s);
    set = ns;
  }


  @Override
  public boolean retainAll(@Nonnull Collection<?> c) {
    synchronized (getLock()) {
      Set<E> copy = copySet(set.size());
      boolean r = copy.retainAll(c);
      set = copy;
      return r;
    }
  }


  @Override
  public int size() {
    return set.size();
  }


  @Override
  @Nonnull
  public Object[] toArray() {
    return set.toArray();
  }


  @Override
  @Nonnull
  public <T> T[] toArray(@Nonnull T[] a) {
    return set.toArray(a);
  }


  @Override
  public String toString() {
    return set.toString();
  }

}
