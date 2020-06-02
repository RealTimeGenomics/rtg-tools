/*
 * Copyright (c) 2017. Real Time Genomics Limited.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.rtg.util.iterators;

import java.util.Iterator;

import com.reeltwo.jumble.annotations.TestClass;


/**
 * @param <X> the class being transformed from.
 * @param <Y> the class being transformed to.
 */
@TestClass({"com.rtg.util.iterators.TransformTest", "com.rtg.util.iterators.ArrayToIteratorTest", "com.rtg.util.iterators.ComposeIteratorsTest"})
public abstract class Transform<X, Y> {

  /**
   * @param x value to be transformed.
   * @return value after transformation.
   */
  public abstract Y trans(X x);

  private static final class IterTrans<X, Y> implements Iterator<Y> {
    private final Transform<X, Y> mTrans;
    private final Iterator<X> mIt;

    /**
     * @param trans transform to be applied to the iterator.
     * @param it iterator to be transformed.
     */
    IterTrans(final Transform<X, Y> trans, final Iterator<X> it) {
      mTrans = trans;
      mIt = it;
    }

    @Override
    public boolean hasNext() {
      return mIt.hasNext();
    }

    @Override
    public Y next() {
      return mTrans.trans(mIt.next());
    }

    @Override
    public void remove() {
      mIt.remove();
    }
  }

  /**
   * Transform an iterator.
   * @param xit iterator to be transformed.
   * @return an iterator all the objects of which have been transformed.
   */
  public Iterator<Y> trans(Iterator<X> xit) {
    return new IterTrans<>(this, xit);
  }

  private static final class TransCompose<X, Y, Z> extends Transform<X, Z> {

    private final Transform<X, Y> mXY;
    private final Transform<Y, Z> mYZ;
    TransCompose(Transform<X, Y> xy, Transform<Y, Z> yz) {
      mXY = xy;
      mYZ = yz;
    }
    @Override
    public Z trans(X x) {
      return mYZ.trans(mXY.trans(x));
    }
  }

  /**
   * Compose two transforms to form a single new transform.
   * @param xy transform from x to y.
   * @param yz transform from y to z.
   * @return a transform from x to z.
   * @param <X> type of original values.
   * @param <Y> type of intermediate values.
   * @param <Z> type of final values.
   */
  public static <X, Y, Z> Transform<X, Z> compose(Transform<X, Y> xy, Transform<Y, Z> yz) {
    return new TransCompose<>(xy, yz);
  }

  /**
   * Given an iterator and a transform that takes each value in the iterator to another iterator,
   * flatten all this to give a single iterator over the final values.
   * @param x the initial iterator.
   * @param xiy the transformer that takes initial values to iterators over the final values.
   * @return a single iterator over the final values.
   * @param <X> the type of the initial values.
   * @param <Y> the type of the final values.
   */
  public static <X, Y> Iterator<Y> flatten(Iterator<X> x, Transform<X, Iterator<Y>> xiy) {
    return new ComposeIterators<>(x, xiy);
  }

  /**
   * Given an iterator over iterables over values,
   * flatten all this to give a single iterator over the values.
   * @param x the iterator over <code>Iterables</code>.
   * @return a single iterator over the final values.
   * @param <I> the iterable type.
   * @param <Y> the type of the final values.
   */
  public static <I extends Iterable<Y>, Y> Iterator<Y> flatten(Iterator<I> x) {
    final Transform<I, Iterator<Y>> trans = new Transform<I, Iterator<Y>>() {
      @Override
      public Iterator<Y> trans(I x) {
        return x.iterator();
      }
    };
    return new ComposeIterators<>(x, trans);
  }

  /**
   * Convert an array to an iterator over those values.
   * This is handy for testing.
   * @param array the array of values.
   * @return an iterator over the values.
   * @param <X> the type of the values.
   */
  public static <X> Iterator<X> array2Iterator(X[] array) {
    return new ArrayToIterator<>(array);
  }
}
