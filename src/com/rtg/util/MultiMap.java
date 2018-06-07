/*
 * Copyright (c) 2014. Real Time Genomics Limited.
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
package com.rtg.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 * MultiMap works a lot like a Map except put will add the value to
 * a collection of those held by the key. There are issues with
 * making use of generics for this type of class, someone with
 * more understanding of them may be able to clean this up.
 *
 * @param <K> Type for key of map
 * @param <V> Type for value of map
 */
public class MultiMap<K, V> {

  /**
   * Handy class when you want to specify the factory.
   */
  public static final class HashFactory<W> implements MultiMapFactory<W> {
    @Override
    public Collection<W> createCollection() {
      return new HashSet<>();
    }
  }

  private final Map<K, Collection<V>> mMap;
  private final MultiMapFactory<V> mFactory;

  /**
   * Creates a MultiMap backed by a HashMap. Using <code>ArrayList</code> for collections
   */
  public MultiMap() {
    this(false);
  }

  /**
   * Creates a MultiMap backed by a HashMap. Using <code>ArrayList</code> for collections
   * @param insertionOrdered true if entries should be insertion ordered
   */
  public MultiMap(boolean insertionOrdered) {
    if (insertionOrdered) {
      mMap = new LinkedHashMap<>();
    } else {
      mMap = new HashMap<>();
    }
    mFactory = new MultiMapFactory<V>() {
      @Override
      public Collection<V> createCollection() {
        return new ArrayList<>();
      }
    };
  }

  /**
   * Creates a MultiMap backed by provided Map.
   * @param map Map to back this MultiMap with.
   * @param type Class for Collection type to use for values.
   */
  public MultiMap(final Map<K, Collection<V>> map, final MultiMapFactory<V> type) {
    mMap = map;
    mFactory = type;
  }

  private Collection<V> putBase(final K key) {
    return mMap.computeIfAbsent(key, k -> mFactory.createCollection());
  }

  /**
   * Adds an empty collection if it does not already exist for the given key.
   * @param key the key to ensure an entry for.
   */
  public void put(final K key) {
    putBase(key);
  }

  /**
   * Adds value to collection held by key
   * @param key The key
   * @param value the value to add.
   */
  public void put(final K key, final V value) {
    final Collection<V> c = putBase(key);
    try {
      c.add(value);
    } catch (final UnsupportedOperationException e) {
      final Collection<V> temp = mFactory.createCollection();
      temp.addAll(c);
      temp.add(value);
      mMap.put(key, temp);
    }
  }

  /**
   * Invokes <code>put</code> on backing Map.
   * @param key see definition in <code>Map</code>.
   * @param value see definition in <code>Map.put</code>.
   * @return the underlying collection.
   */
  public Collection<V> set(final K key, final Collection<V> value) {
    return mMap.put(key, value);
  }

  /**
   * Invokes <code>get</code> on backing Map.
   * @param key see definition in <code>Map</code>.
   * @return the underlying collection.
   */
  public Collection<V> get(final K key) {
    return mMap.get(key);
  }

  /**
   * Invokes <code>remove</code> on backing Map.
   * @param key see definition in <code>Map</code>.
   * @return the underlying collection.
   */
  public Collection<V> remove(final K key) {
    return mMap.remove(key);
  }

  /**
   * Invokes <code>values</code> on backing Map.
   * @return the underlying collection.
   */
  public Collection<? extends Collection<V>> values() {
    return mMap.values();
  }


  /**
   * Invokes <code>keySet</code> on backing Map.
   * @return the underlying key set.
   */
  public Set<K> keySet() {
    return mMap.keySet();
  }

  /**
   * Invokes <code>entrySet</code> on backing Map.
   * @return the underlying entry set.
   */
  public Set<Entry<K, Collection<V>>> entrySet() {
    return mMap.entrySet();
  }

  /**
   * @return all the values in the multi-map.
   */
  public Collection<V> allValues() {
    final Collection<V> all = new LinkedList<>();
    for (final Collection<V> coll : values()) {
      all.addAll(coll);
    }
    return all;
  }

  /**
   * Invokes <code>clear</code> on backing Map.
   */
  public void clear() {
    mMap.clear();
  }

  /**
   * Invokes <code>size</code> on backing Map.
   * @return the number of keys.
   */
  public int size() {
    return mMap.size();
  }

  @Override
  public int hashCode() {
    return 31 + ((mMap == null) ? 0 : mMap.hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof MultiMap)) {
      return false;
    }
    final MultiMap<?, ?> that = (MultiMap<?, ?>) obj;
    return mMap.equals(that.mMap);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append('{').append(StringUtils.LS);
    for (final Map.Entry<K, Collection<V>> entry : mMap.entrySet()) {
      final K key = entry.getKey();
      sb.append(' ').append(key).append(" -> ");
      final Collection<V> coll = entry.getValue();
      final String collString = Arrays.toString(coll.toArray());
      sb.append(collString).append(StringUtils.LS);
    }
    sb.append('}').append(StringUtils.LS);
    return sb.toString();
  }
}

