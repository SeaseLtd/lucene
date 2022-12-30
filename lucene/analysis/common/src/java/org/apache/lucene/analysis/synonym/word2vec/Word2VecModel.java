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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.index.VectorValues;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.TermAndVector;
import org.apache.lucene.util.hnsw.RandomAccessVectorValues;

/**
 * Word2VecModel is a class representing the parsed Word2Vec model containing the vectors for each
 * word in dictionary
 *
 * @lucene.experimental
 */
public class Word2VecModel extends VectorValues implements RandomAccessVectorValues {

  private final int dictionarySize;
  private final int vectorDimension;
  private final TermAndVector[] data;
  private final Map<BytesRef, TermAndVector> word2Vec;

  private int loadedCount = 0;

  private int currentIndex = -1;

  public Word2VecModel(int dictionarySize, int vectorDimension) {
    this.dictionarySize = dictionarySize;
    this.vectorDimension = vectorDimension;
    this.data = new TermAndVector[dictionarySize];
    this.word2Vec = new HashMap<>();
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

  public void addTermAndVector(TermAndVector modelEntry) {
    modelEntry.normalizeVector();
    this.data[loadedCount++] = modelEntry;
    this.word2Vec.put(modelEntry.getWord(), modelEntry);
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
