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

package org.apache.lucene.analysis.synonym.word2vec;

import static org.apache.lucene.util.hnsw.HnswGraphBuilder.DEFAULT_BEAM_WIDTH;
import static org.apache.lucene.util.hnsw.HnswGraphBuilder.DEFAULT_MAX_CONN;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.lucene.index.VectorEncoding;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.index.VectorValues;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.TermAndBoost;
import org.apache.lucene.util.TermAndVector;
import org.apache.lucene.util.hnsw.HnswGraph;
import org.apache.lucene.util.hnsw.HnswGraphBuilder;
import org.apache.lucene.util.hnsw.HnswGraphSearcher;
import org.apache.lucene.util.hnsw.NeighborQueue;
import org.apache.lucene.util.hnsw.RandomAccessVectorValues;

/**
 * Implementation of a {@link SynonymProvider} using vector similarity technique
 *
 * @lucene.experimental
 */
public class Word2VecSynonymProvider implements SynonymProvider {

  private static final VectorSimilarityFunction SIMILARITY_FUNCTION =
      VectorSimilarityFunction.DOT_PRODUCT;
  private static final VectorEncoding VECTOR_ENCODING = VectorEncoding.FLOAT32;
  private final Word2VecModel word2VecModel;
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
    word2VecModel = new Word2VecModel(vectorStream);
    HnswGraphBuilder<?> builder =
        HnswGraphBuilder.create(
            word2VecModel,
            VECTOR_ENCODING,
            SIMILARITY_FUNCTION,
            DEFAULT_MAX_CONN,
            DEFAULT_BEAM_WIDTH,
            HnswGraphBuilder.randSeed);
    this.hnswGraph = builder.build(word2VecModel.copy());
  }

  @Override
  public List<TermAndBoost> getSynonyms(
      BytesRef term, int maxSynonymsPerTerm, float minAcceptedSimilarity) throws IOException {

    if (term == null) {
      throw new IllegalArgumentException("Term must not be null");
    }

    LinkedList<TermAndBoost> result = new LinkedList<>();
    float[] query = word2VecModel.vectorValue(term);
    if (query != null) {
      NeighborQueue synonyms =
          HnswGraphSearcher.search(
              query,
              maxSynonymsPerTerm,
              word2VecModel,
              VECTOR_ENCODING,
              SIMILARITY_FUNCTION,
              hnswGraph,
              null,
              word2VecModel.dictionarySize);

      int size = synonyms.size();
      for (int i = 0; i < size; i++) {
        float similarity = synonyms.topScore();
        int id = synonyms.pop();

        BytesRef synonym = word2VecModel.binaryValue(id);
        if (!synonym.equals(term) && similarity >= minAcceptedSimilarity) {
          result.addFirst(new TermAndBoost(synonym, similarity));
        }
      }
    }
    return result;
  }

  static class Word2VecModel extends VectorValues implements RandomAccessVectorValues {

    private final int dictionarySize;
    private final int vectorDimension;
    private final TermAndVector[] data;
    private final Map<BytesRef, TermAndVector> word2Vec;

    private int currentIndex = -1;

    public Word2VecModel(Word2VecModelStream vectorStream) {
      this.dictionarySize = vectorStream.getDictionarySize();
      this.vectorDimension = vectorStream.getVectorDimension();
      this.data = new TermAndVector[dictionarySize];
      this.word2Vec = new HashMap<>();

      AtomicInteger loadedCount = new AtomicInteger(0);
      vectorStream
          .getModelStream()
          .forEach(
              modelEntry -> {
                float[] vector = modelEntry.getVector();
                // normalize vector so, in the future, we can use the dot_product instead of the
                // cosine similarity function
                modelEntry.normalizeVector();
                this.data[loadedCount.getAndIncrement()] = modelEntry;
                this.word2Vec.put(modelEntry.getWord(), modelEntry);
              });

      if (loadedCount.get() != dictionarySize) {
        throw new IllegalArgumentException(
            "Word2Vec model file corrupted. Declared "
                + dictionarySize
                + " records but found "
                + loadedCount.get());
      }
    }

    private Word2VecModel(
        int dictionarySize,
        int vectorDimension,
        TermAndVector[] data,
        Map<BytesRef, TermAndVector> word2Vec) {
      this.dictionarySize = dictionarySize;
      this.vectorDimension = vectorDimension;
      this.data = data;
      this.word2Vec = word2Vec;
    }

    @Override
    public float[] vectorValue(int ord) throws IOException {
      return data[ord].getVector();
    }

    public float[] vectorValue(BytesRef word) {
      TermAndVector term = word2Vec.get(word);
      return (term == null) ? null : term.getVector();
    }

    @Override
    public BytesRef binaryValue(int targetOrd) throws IOException {
      return data[targetOrd].getWord();
    }

    @Override
    public int dimension() {
      return vectorDimension;
    }

    @Override
    public int size() {
      return dictionarySize;
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
      if (target >= 0 && target < dictionarySize) {
        currentIndex = target;
      } else {
        currentIndex = NO_MORE_DOCS;
      }
      return currentIndex;
    }

    @Override
    public RandomAccessVectorValues copy() throws IOException {
      return new Word2VecModel(this.dictionarySize, this.vectorDimension, this.data, this.word2Vec);
    }
  }
}
