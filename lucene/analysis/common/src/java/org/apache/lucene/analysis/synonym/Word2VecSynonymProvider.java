/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.lucene.analysis.synonym;

import static org.apache.lucene.codecs.lucene92.Lucene92HnswVectorsFormat.DEFAULT_BEAM_WIDTH;
import static org.apache.lucene.codecs.lucene92.Lucene92HnswVectorsFormat.DEFAULT_MAX_CONN;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.lucene.index.RandomAccessVectorValues;
import org.apache.lucene.index.RandomAccessVectorValuesProducer;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.index.VectorValues;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.hnsw.HnswGraph;
import org.apache.lucene.util.hnsw.HnswGraphBuilder;
import org.apache.lucene.util.hnsw.HnswGraphSearcher;
import org.apache.lucene.util.hnsw.NeighborQueue;

/**
 * Implementation of a {@link SynonymProvider} using vector similarity technique
 *
 * @lucene.experimental
 */
public class Word2VecSynonymProvider implements SynonymProvider {

  public static final VectorSimilarityFunction similarityFunction = VectorSimilarityFunction.COSINE;
  public static final long SEED = System.currentTimeMillis();

  private final VectorProducer vectorProducer;
  private final HnswGraph hnswGraph;

  /**
   * SynonymProvider constructor
   *
   * @param vectorStream stream of SynonymTerms
   */
  public Word2VecSynonymProvider(Word2VecModelStream vectorStream) throws IOException {

    if (vectorStream == null) {
      throw new IllegalArgumentException("VectorData must be set");
    }

    // Create the provider which will feed the vectors for the graph
    vectorProducer = new VectorProducer(vectorStream);
    HnswGraphBuilder builder =
        new HnswGraphBuilder(
            vectorProducer, similarityFunction, DEFAULT_MAX_CONN, DEFAULT_BEAM_WIDTH, SEED);
    this.hnswGraph = builder.build(vectorProducer.randomAccess());
  }

  @Override
  public List<WeightedSynonym> getSynonyms(String token, int maxResult, float accuracy)
      throws IOException {

    if (token == null) {
      throw new IllegalArgumentException("Term must not be null");
    }

    SynonymVector synonymVector = (SynonymVector) vectorProducer.randomAccess();

    LinkedList<WeightedSynonym> result = new LinkedList<>();
    float[] query = synonymVector.vectorValue(token);
    if (query != null) {
      NeighborQueue synonyms =
          HnswGraphSearcher.search(
              query,
              maxResult,
              synonymVector,
              similarityFunction,
              hnswGraph,
              null,
              Integer.MAX_VALUE);

      int size = synonyms.size();
      for (int i = 0; i < size; i++) {
        float similarity = synonyms.topScore();
        int id = synonyms.pop();
        Word2VecSynonymTerm term = synonymVector.getSynonymTerm(id);
        if (!term.getWord().equals(token) && similarity >= accuracy) {
          result.addFirst(new WeightedSynonym(term.getWord(), similarity));
        }
      }
    }
    return result;
  }

  static class VectorProducer implements RandomAccessVectorValuesProducer {

    private final int size;
    private final int dimension;
    private final Word2VecSynonymTerm[] data;
    private final Map<String, Word2VecSynonymTerm> word2Vec;

    public VectorProducer(Word2VecModelStream vectorStream) {
      this.size = vectorStream.getSize();
      this.dimension = vectorStream.getVectorDimension();
      this.data = new Word2VecSynonymTerm[size];
      this.word2Vec = new HashMap<>();

      AtomicInteger loaded = new AtomicInteger(0);
      vectorStream
          .getModelStream()
          .forEach(
              synTerm -> {
                if (this.dimension != synTerm.getVector().length) {
                  throw new IllegalArgumentException(
                      "Word2Vec model file corrupted. Declared vectors of size "
                          + this.dimension
                          + " but found vector of size "
                          + synTerm.getVector().length
                          + " at line "
                          + loaded.get()
                          + 2); // +2 because the first line of the model file is the header
                }
                this.data[loaded.getAndIncrement()] = synTerm;
                this.word2Vec.put(synTerm.getWord(), synTerm);
              });

      if (loaded.get() != size) {
        throw new IllegalArgumentException(
            "Word2Vec model file corrupted. Declared "
                + size
                + " records but found "
                + loaded.get());
      }
    }

    @Override
    public RandomAccessVectorValues randomAccess() {
      return new SynonymVector(data, word2Vec, size, dimension);
    }
  }

  static class SynonymVector extends VectorValues implements RandomAccessVectorValues {

    private final int size;
    private final int dimension;
    private final Word2VecSynonymTerm[] data;
    private final Map<String, Word2VecSynonymTerm> word2VecMap;

    private int currentIndex = -1;

    SynonymVector(
        Word2VecSynonymTerm[] data,
        Map<String, Word2VecSynonymTerm> word2VecMap,
        int size,
        int dimension) {
      this.size = size;
      this.dimension = dimension;
      this.data = data;
      this.word2VecMap = word2VecMap;
    }

    @Override
    public float[] vectorValue(int ord) throws IOException {
      return data[ord].getVector();
    }

    public Word2VecSynonymTerm getSynonymTerm(int ord) {
      return data[ord];
    }

    public float[] vectorValue(String word) {
      Word2VecSynonymTerm term = word2VecMap.get(word);
      return (term == null) ? null : term.getVector();
    }

    @Override
    public BytesRef binaryValue(int targetOrd) throws IOException {
      return null;
    }

    @Override
    public int dimension() {
      return dimension;
    }

    @Override
    public int size() {
      return size;
    }

    @Override
    public float[] vectorValue() throws IOException {
      return vectorValue(currentIndex);
    }

    @Override
    public int docID() {
      return currentIndex;
    }

    @Override
    public int nextDoc() throws IOException {
      return advance(currentIndex + 1);
    }

    @Override
    public int advance(int target) throws IOException {
      if (target >= 0 && target < size) {
        currentIndex = target;
      } else {
        currentIndex = NO_MORE_DOCS;
      }
      return currentIndex;
    }

    @Override
    public long cost() {
      return size;
    }
  }
}
