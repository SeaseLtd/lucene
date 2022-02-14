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

import static org.apache.lucene.codecs.lucene91.Lucene91HnswVectorsFormat.DEFAULT_BEAM_WIDTH;
import static org.apache.lucene.codecs.lucene91.Lucene91HnswVectorsFormat.DEFAULT_MAX_CONN;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.lucene.index.RandomAccessVectorValues;
import org.apache.lucene.index.RandomAccessVectorValuesProducer;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.index.VectorValues;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.hnsw.HnswGraph;
import org.apache.lucene.util.hnsw.HnswGraphBuilder;
import org.apache.lucene.util.hnsw.NeighborQueue;

/**
 * Implementation of a {@link SynonymProvider} using vector similarity technique
 *
 * @lucene.experimental
 */
public class Word2VecSynonymProvider implements SynonymProvider {

  public static final VectorSimilarityFunction similarityFunction = VectorSimilarityFunction.COSINE;
  public static final long SEED = System.currentTimeMillis();

  private final int topK;
  private final float distanceThreshold;

  private final VectorProvider vectors;
  private final HnswGraph hnswGraph;

  /**
   * SynonymProvider constructor
   *
   * @param vectorData list of SynonymTerms
   * @param maxResult maximum number of result returned by the synonym search
   * @param accuracy minimal value of cosign similarity between the searched vector and the
   *     retrieved ones
   */
  public Word2VecSynonymProvider(
      List<Word2VecSynonymTerm> vectorData, int maxResult, float accuracy) throws IOException {

    if (vectorData == null) {
      throw new IllegalArgumentException("VectorData must be set");
    }
    if (vectorData.size() <= 0) {
      throw new IllegalArgumentException("VectorData must not be empty");
    }
    if (accuracy <= 0 || accuracy > 1) {
      throw new IllegalArgumentException(
          "Accuracy must be in the range (0, 1]. Found: " + accuracy);
    }
    this.distanceThreshold = accuracy;
    this.topK = maxResult;

    //        long startTime = System.currentTimeMillis();

    // Create the provider which will feed the vectors for the graph
    vectors = new VectorProvider(vectorData);
    HnswGraphBuilder builder =
        new HnswGraphBuilder(
            vectors, similarityFunction, DEFAULT_MAX_CONN, DEFAULT_BEAM_WIDTH, SEED);
    this.hnswGraph = builder.build(vectors);

    //        long endTime = System.currentTimeMillis();
    //        System.out.println("HnswGraph Builder - Elapsed time: " + (endTime-startTime) + " ms
    // (" + ((endTime-startTime)/60000) + " min)");
  }

  @Override
  public List<WeightedSynonym> getSynonyms(String token) throws IOException {
    //        long startTime = System.currentTimeMillis();

    if (token == null) {
      throw new IllegalArgumentException("Term must not be null");
    }
    LinkedList<WeightedSynonym> result = new LinkedList<>();
    float[] query = vectors.vectorValue(token);
    if (query != null) {
      NeighborQueue synonyms =
          HnswGraph.search(query, topK, vectors, similarityFunction, hnswGraph, null);

      int size = synonyms.size();
      for (int i = 0; i < size; i++) {
        int id = synonyms.pop();
        Word2VecSynonymTerm term = vectors.getSynonymTerm(id);
        float similarity = similarityFunction.compare(term.getVector(), query);
        if (!term.getWord().equals(token) && similarity >= this.distanceThreshold) {
          result.addFirst(new WeightedSynonym(term.getWord(), similarity));
        }
      }
    }
    //        long endTime = System.currentTimeMillis();
    //        System.out.println("getSynonyms - Found " + result.size() + " terms - Elapsed time: "
    // + (endTime-startTime) + " ms (" + ((endTime-startTime)/60000) + " min)");
    return result;
  }

  class VectorProvider extends VectorValues
      implements RandomAccessVectorValues, RandomAccessVectorValuesProducer {

    int doc = -1;
    private final List<Word2VecSynonymTerm> data;
    private final Map<String, Word2VecSynonymTerm> word2VecMap = new HashMap<>();

    public VectorProvider(List<Word2VecSynonymTerm> data) {
      this.data = data;
      data.forEach(synTerm -> word2VecMap.put(synTerm.getWord(), synTerm));
    }

    @Override
    public RandomAccessVectorValues randomAccess() {
      return new VectorProvider(data);
    }

    @Override
    public float[] vectorValue(int ord) throws IOException {
      return data.get(ord).getVector();
    }

    public Word2VecSynonymTerm getSynonymTerm(int ord) {
      return data.get(ord);
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
      return data.get(0).size();
    }

    @Override
    public int size() {
      return data.size();
    }

    @Override
    public float[] vectorValue() throws IOException {
      return vectorValue(doc);
    }

    @Override
    public int docID() {
      return doc;
    }

    @Override
    public int nextDoc() throws IOException {
      return advance(doc + 1);
    }

    @Override
    public int advance(int target) throws IOException {
      if (target >= 0 && target < data.size()) {
        doc = target;
      } else {
        doc = NO_MORE_DOCS;
      }
      return doc;
    }

    @Override
    public long cost() {
      return data.size();
    }

    public VectorProvider copy() {
      return new VectorProvider(data);
    }
  }
}
