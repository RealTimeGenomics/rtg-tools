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

package com.rtg.taxonomy;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * RTG taxonomy tree node.
 */
public final class TaxonNode implements Comparable<TaxonNode> {

  private final int mId;
  private String mName = null;
  private String mRank = null;

  private TaxonNode mParent = null;

  final TreeSet<TaxonNode> mChildren = new TreeSet<>();

  TaxonNode(int id, String name, String rank) {
    mId = id;
    setName(name);
    setRank(rank);
  }

  TaxonNode(int id) {
    this(id, null, null);
  }

  public int getId() {
    return mId;
  }

  void addChild(TaxonNode node) {
    if (node != null) {
      mChildren.add(node);
      node.mParent = this;
    }
  }

  void detach() {
    if (mParent != null) {
      mParent.mChildren.remove(this);
      mParent = null;
    }
  }

  void setRank(String rank) {
    mRank = rank == null ? null : rank.replaceAll("\\s", " ");
  }

  public String getRank() {
    return mRank;
  }

  void setName(String name) {
    mName = name;
  }

  public String getName() {
    return mName;
  }

  public int getParentId() {
    return mParent == null ? -1 : mParent.mId;
  }

  public TaxonNode getParent() {
    return mParent;
  }

  /**
   * Get a list of the taxon nodes, ordered depth first
   * @return the list of nodes
   */
  public List<TaxonNode> depthFirstTraversal() {
    final ArrayList<TaxonNode> nodes = new ArrayList<>();
    depthFirstTraversal(nodes);
    return nodes;
  }


  /**
   * @return true if this node has no children
   */
  public boolean isLeaf() {
    return mChildren.isEmpty();
  }

  /**
   * @return the number of children
   */
  public int numChildren() {
    return mChildren.size();
  }

  /**
   * @return list of immediate child nodes
   */
  public List<TaxonNode> getChildren() {
    return new ArrayList<>(mChildren);
  }

  private void depthFirstTraversal(ArrayList<TaxonNode> nodes) {
    nodes.add(this);
    for (final TaxonNode child : mChildren) {
      child.depthFirstTraversal(nodes);
    }
  }

  @Override
  public String toString() {
    return getId() + "\t" + getParentId() + "\t" + getRank() + "\t" + getName();
  }

  @Override
  public boolean equals(Object other) {
    return other != null && mId == ((TaxonNode) other).mId;
  }

  @Override
  public int hashCode() {
    return mId;
  }

  @Override
  public int compareTo(TaxonNode o) {
    return mId - o.mId;
  }

}
