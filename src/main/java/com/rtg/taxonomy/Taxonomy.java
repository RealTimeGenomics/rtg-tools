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

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import com.rtg.util.TsvParser;
import com.rtg.util.io.LineWriter;

/**
 * RTG's version of a taxonomy tree, loosely based on the NCBI taxonomy tree.
 * Root node is expected to have an ID of 1.
 */
public class Taxonomy {

  /** The taxonomy id of the root node */
  public static final int ROOT_ID = 1;

  private static final String TAB = "\t";
  private static final String VERSION = "1.0";
  private static final String VERSION_HEADER = "#RTG taxonomy version " + VERSION;

  /** Rank for merge nodes */
  private static final String MERGE_RANK = "same";
  /** Name for merge nodes */
  private static final String MERGE_NAME = "same";

  private final HashMap<Integer, TaxonNode> mNodes = new HashMap<>();

  private String mReason = null;

  /**
   * Creates an empty Taxonomy.
   */
  public Taxonomy() {
  }

  /**
   * Creates a taxonomy populating it from the given input stream.
   * @param is InputStream containing taxonomy file.
   * @throws IOException if an error occurs with reading.
   */
  public Taxonomy(InputStream is) throws IOException {
    read(is);
  }

  /**
   * Clears all nodes from Taxonomy.
   */
  public void clear() {
    mNodes.clear();
    mReason = null;
  }

  /**
   * Adds a node to the taxonomy.
   * @param id taxon id of node
   * @param parentId id of parent node.  If parent id is -1 or same as taxon id no parent is linked.
   * @param name name of taxon node.
   * @param rank rank of taxon node.
   */
  public void addNode(int id, int parentId, String name, String rank) {
    TaxonNode node = mNodes.get(id);
    if (node != null) {
      if (node.isLeaf()) {
        throw new IllegalArgumentException("Duplicate taxon id: " + id);
      } else if (node.getName() == null) {
        node.setName(name);
        node.setRank(rank);
      }
    } else {
      node = new TaxonNode(id, name, rank);
      mNodes.put(id, node);
    }
    if (parentId > -1 && parentId != id) {
      final TaxonNode parent = mNodes.computeIfAbsent(parentId, TaxonNode::new);
      parent.addChild(node);
    }
    mReason = null;
  }

  /**
   * Adds a special node to the taxonomy that represents merged taxon IDs.
   * @param taxonId true taxon id
   * @param mergeId an old id
   */
  public void addMergeNode(int taxonId, int mergeId) {
    addNode(mergeId, taxonId, MERGE_NAME, MERGE_RANK);
  }

  /**
   * Returns number of nodes in the taxonomy.
   * @return count of nodes.
   */
  public int size() {
    return mNodes.size();
  }

  /**
   * Returns the root node of the taxonomy tree.
   * @return node with id of 1, or null if that does not exist.
   */
  public TaxonNode getRoot() {
    return mNodes.get(ROOT_ID);
  }

  /**
   * Returns the node for the given taxonomy id.
   * @param taxonId the taxonomy id
   * @return node with the given id, or null if that does not exist.
   */
  public TaxonNode get(int taxonId) {
    return mNodes.get(taxonId);
  }

  /**
   * Returns whether the taxonomy contains the given <code>taxId</code>.
   * @param taxId id to check for.
   * @return true if in taxonomy.
   */
  public boolean contains(int taxId) {
    return mNodes.containsKey(taxId);
  }

  /**
   * Adds the specified node from the source taxonomy, including the path to the root
   * @param source the source taxonomy
   * @param taxId the taxon id to copy
   */
  public void addPath(Taxonomy source, Integer taxId) {
    TaxonNode node = source.mNodes.get(taxId);
    if (node == null) {
      throw new IllegalArgumentException("Taxonomy does not contain node with id " + taxId);
    }
    TaxonNode child = null;
    while (node != null) { // Walk up to root, adding each
      TaxonNode current = mNodes.get(node.getId());
      if (current == null) {
        current = new TaxonNode(node.getId(), node.getName(), node.getRank());
        mNodes.put(current.getId(), current);
        node = node.getParent();
      } else {
        node = null; // node already exists so parent must already be entered
      }
      current.addChild(child);
      child = current;
    }
  }

  /**
   * Adds the specified subtree from a source taxonomy to this taxonomy (including the path to the root)
   * @param source the source taxonomy
   * @param taxId the taxon id to select subtrees of
   */
  public void addSubTree(Taxonomy source, int taxId) {
    final TaxonNode node = source.mNodes.get(taxId);
    if (node == null) {
      throw new IllegalArgumentException("Taxonomy does not contain node with id " + taxId);
    }

    addPath(source, taxId);

    for (final TaxonNode tn : node.depthFirstTraversal()) {
      if (!mNodes.containsKey(tn.getId())) {
        addNode(tn.getId(), tn.getParentId(), tn.getName(), tn.getRank());
      }
    }
  }

  /**
   * Adds the specified subtrees from a source taxonomy to this taxonomy (including each path to the root)
   * @param source the source taxonomy
   * @param ids the taxon ids to select subtrees of
   */
  public void addSubTrees(Taxonomy source, Collection<Integer> ids) {
    for (final Integer taxId : ids) {
      addSubTree(source, taxId);
    }
  }

  /**
   * Adds the specified nodes from a source taxonomy to this taxonomy (including each path to the root)
   * @param source the source taxonomy
   * @param ids the taxon ids to add
   */
  public void addPaths(Taxonomy source, Collection<Integer> ids) {
    for (final Integer taxId : ids) {
      addPath(source, taxId);
    }
  }

  /**
   * Returns a subset of the taxonomy that contains nodes with the given <code>ids</code> and their ancestors back to the root.
   * @param ids taxon ids to include in the subset.
   * @return a new taxonomy subset.
   */
  public Taxonomy subset(Collection<Integer> ids) {
    final Taxonomy subset = new Taxonomy();
    subset.addPaths(this, ids);
    return subset;
  }

  /**
   * Remove the sub-tree from the given node from the taxonomy.
   * @param taxId id of root node of sub-tree to remove
   * @return true if node(s) were removed.
   */
  public boolean removeSubTree(int taxId) {
    final TaxonNode node = mNodes.get(taxId);
    if (node == null) {
      return false;
    }
    for (final TaxonNode tn : node.depthFirstTraversal()) {
      mNodes.remove(tn.getId());
    }
    node.detach();
    return true;
  }

  /**
   * Writes the current taxonomy to the given <code>writer</code> as a tab separated value file.
   * @param writer where to write to
   * @throws IOException if an error occurs while writing.
   */
  public void write(Writer writer) throws IOException {
    final LineWriter lw = new LineWriter(writer);

    lw.writeln(VERSION_HEADER);
    lw.writeln("#taxID" + TAB + "parentID" + TAB + "rank" + TAB + "name");

    final TaxonNode root = getRoot();
    if (root != null) {
      for (final TaxonNode node : root.depthFirstTraversal()) {
        lw.writeln(node.getId() + TAB + node.getParentId() + TAB + node.getRank() + TAB + node.getName());
      }
    }
  }

  /**
   * Reads a tab separated value file containing a taxonomy structure.
   * @param in InputStream to read from
   * @throws IOException if an error occurs while reading.
   */
  public final void read(InputStream in) throws IOException {
    mReason = null;
    final TaxonomyParser r = new TaxonomyParser();
    r.parse(in);
  }

  private class TaxonomyParser extends TsvParser<Void> {
    @Override
    protected void parseHeader(String line) throws IOException {
      if (lineNumber() == 1) {
        // expect first line to have RTG taxomony version
        if (!line.contains("RTG taxonomy")) {
          throw new IOException("No version information on first line of file.");
        }
        final String[] parts = line.split("\\s");
        if (!VERSION.equals(parts[parts.length - 1])) {
          throw new IOException("Expecting version " + VERSION + " but saw " + parts[parts.length - 1]);
        }
      }
    }

    @Override
    protected void parseLine(String... columns) throws IOException {
      if (columns.length != 4) {
        throw new IOException("Malformed taxonomy file line " + lineNumber() + ": " + line());
      }
      try {
        final int taxId = Integer.parseInt(columns[0]);
        final int parentId = Integer.parseInt(columns[1]);
        final String rank = new String(columns[2].toCharArray());
        final String name = new String(columns[3].toCharArray());
        try {
          addNode(taxId, parentId, name, rank);
        } catch (final IllegalArgumentException e) {
          throw new IOException(e.getMessage(), e);
        }
      } catch (final NumberFormatException e) {
        throw new IOException("Malformed taxonomy file line " + lineNumber() + ": " + line(), e);
      }
    }
  }

  /**
   * Returns whether the taxonomy is currently consistent or not.
   * @return is taxonomy complete.
   */
  public boolean isConsistent() {
    // check taxonomy is consistent - all nodes filled out, only a single root
    final TaxonNode root = getRoot();
    if (root == null) {
      mReason = "No root node.";
      return false;
    }
    final HashSet<TaxonNode> seen = new HashSet<>(); // to detect loops
    for (final TaxonNode node : mNodes.values()) {
      if (node.getName() == null) {
        mReason = "Node " + node.getId() + " does not have a name.";
        return false;
      }
      if (node.getRank() == null) {
        mReason = "Node " + node.getId() + " does not have a rank.";
        return false;
      }
      // search to root of taxonomy
      TaxonNode x = node;
      seen.clear();
      while (x != null && x != root && !seen.contains(x)) {
        seen.add(x);
        x = x.getParent();
      }
      if (x != root) {
        mReason = "Node " + node.getId() + " does not link to root.";
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the reason the taxonomy is not consistent.  This is produced as a result of calling <code>isConsistent</code>.
   * @return inconsistency reason string, null it taxonomy is consistent.
   */
  public String getInconsistencyReason() {
    return mReason;
  }
}
