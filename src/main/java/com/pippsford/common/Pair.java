package com.pippsford.common;

import java.util.Objects;

/**
 * A holder for a pair of objects.
 *
 * @author Simon Greatrix on 2019-03-22.
 */
public class Pair<L, R> {

  private L left;

  private R right;


  public Pair() {
    left = null;
    right = null;
  }


  public Pair(L left, R right) {
    this.left = left;
    this.right = right;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Pair)) {
      return false;
    }
    Pair<?, ?> pair = (Pair<?, ?>) o;
    return Objects.equals(left, pair.left) && Objects.equals(right, pair.right);
  }


  @Override
  public int hashCode() {
    return Objects.hash(left, right);
  }


  public L left() {
    return left;
  }


  public R right() {
    return right;
  }


  public void set(L left, R right) {
    this.left = left;
    this.right = right;
  }


  public void setLeft(L left) {
    this.left = left;
  }


  public void setRight(R right) {
    this.right = right;
  }


  @Override
  public String toString() {
    return "Pair{" + "left=" + left
        + ", right=" + right
        + '}';
  }

}
