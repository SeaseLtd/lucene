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
import java.util.List;
import java.util.stream.Stream;
import org.apache.lucene.tests.util.LuceneTestCase;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.TermAndBoost;
import org.junit.Test;

public class TestWord2VecSynonymProvider extends LuceneTestCase {

  private static final int MAX_SYNONYMS_PER_TERM = 10;
  private static final float MIN_ACCEPTED_SIMILARITY = 0.85f;

  private final Word2VecSynonymProvider unit;

  public TestWord2VecSynonymProvider() throws IOException {
    List<Word2VecSynonymTerm> word2VecModel =
        List.of(
            new Word2VecSynonymTerm(new BytesRef("a"), new float[] {0.24f, 0.78f, 0.28f}),
            new Word2VecSynonymTerm(new BytesRef("b"), new float[] {0.44f, 0.01f, 0.81f}));
    unit = new Word2VecSynonymProvider(toStream(word2VecModel));
  }

  @Test
  public void constructor_nullVector_shouldThrowException() {
    expectThrows(IllegalArgumentException.class, () -> new Word2VecSynonymProvider(null));
  }

  @Test
  public void constructor_emptyVector_shouldThrowException() {
    expectThrows(
        IllegalArgumentException.class,
        () -> new Word2VecSynonymProvider(new Word2VecModelStream(10, 10, Stream.empty())));
  }

  @Test
  public void getSynonyms_nullToken_shouldThrowException() {
    expectThrows(
        IllegalArgumentException.class,
        () -> unit.getSynonyms(null, MAX_SYNONYMS_PER_TERM, MIN_ACCEPTED_SIMILARITY));
  }

  @Test
  public void getSynonyms_shouldReturnSynonymsBasedOnMinAcceptedSimilarity() throws Exception {
    List<Word2VecSynonymTerm> word2VecModel =
        List.of(
            new Word2VecSynonymTerm(new BytesRef("a"), new float[] {10, 10}),
            new Word2VecSynonymTerm(new BytesRef("b"), new float[] {10, 8}),
            new Word2VecSynonymTerm(new BytesRef("c"), new float[] {9, 10}),
            new Word2VecSynonymTerm(new BytesRef("d"), new float[] {1, 1}),
            new Word2VecSynonymTerm(new BytesRef("e"), new float[] {99, 101}),
            new Word2VecSynonymTerm(new BytesRef("f"), new float[] {-1, 10}));

    SynonymProvider unit = new Word2VecSynonymProvider(toStream(word2VecModel));

    BytesRef inputTerm = new BytesRef("a");
    String[] expectedSynonyms = {"d", "e", "c", "b"};
    List<TermAndBoost> actualSynonymsResults =
        unit.getSynonyms(inputTerm, MAX_SYNONYMS_PER_TERM, MIN_ACCEPTED_SIMILARITY);

    assertEquals(4, actualSynonymsResults.size());
    for (int i = 0; i < expectedSynonyms.length; i++) {
      assertEquals(new BytesRef(expectedSynonyms[i]), actualSynonymsResults.get(i).term);
    }
  }

  @Test
  public void getSynonyms_shouldCheckCorrectSynonymsWeight() throws Exception {
    List<Word2VecSynonymTerm> word2VecModel =
        List.of(
            new Word2VecSynonymTerm(new BytesRef("a"), new float[] {10, 10}),
            new Word2VecSynonymTerm(new BytesRef("b"), new float[] {1, 1}),
            new Word2VecSynonymTerm(new BytesRef("c"), new float[] {99, 101}));

    SynonymProvider unit = new Word2VecSynonymProvider(toStream(word2VecModel));

    BytesRef inputTerm = new BytesRef("a");
    List<TermAndBoost> actualSynonymsResults =
        unit.getSynonyms(inputTerm, MAX_SYNONYMS_PER_TERM, MIN_ACCEPTED_SIMILARITY);

    BytesRef expectedFirstSynonymTerm = new BytesRef("b");
    double expectedFirstSynonymWeight = 1.0;
    assertEquals(expectedFirstSynonymTerm, actualSynonymsResults.get(0).term);
    assertEquals(expectedFirstSynonymWeight, actualSynonymsResults.get(0).boost, 0.001f);
  }

  @Test
  public void forMinAcceptedSimilarity_shouldNotReturnSynonyms() throws Exception {
    List<Word2VecSynonymTerm> word2VecModel =
        List.of(
            new Word2VecSynonymTerm(new BytesRef("a"), new float[] {10, 10}),
            new Word2VecSynonymTerm(new BytesRef("b"), new float[] {-10, -8}),
            new Word2VecSynonymTerm(new BytesRef("c"), new float[] {-9, -10}),
            new Word2VecSynonymTerm(new BytesRef("d"), new float[] {6, -6}));

    SynonymProvider unit = new Word2VecSynonymProvider(toStream(word2VecModel));

    BytesRef inputTerm = newBytesRef("a");
    List<TermAndBoost> actualSynonymsResults =
        unit.getSynonyms(inputTerm, MAX_SYNONYMS_PER_TERM, MIN_ACCEPTED_SIMILARITY);
    assertEquals(0, actualSynonymsResults.size());
  }

  @Test
  public void testVectorProducer() throws Exception {
    List<Word2VecSynonymTerm> word2VecModel =
        List.of(
            new Word2VecSynonymTerm(new BytesRef("a"), new float[] {10, 10}),
            new Word2VecSynonymTerm(new BytesRef("b"), new float[] {10, 8}),
            new Word2VecSynonymTerm(new BytesRef("c"), new float[] {9, 10}),
            new Word2VecSynonymTerm(new BytesRef("f"), new float[] {-1, 10}));

    Word2VecSynonymProvider.SynonymVector vector =
        new Word2VecSynonymProvider.SynonymVector(toStream(word2VecModel));
    float[] vectorIdA = vector.vectorValue(new BytesRef("a"));
    float[] vectorIdF = vector.vectorValue(new BytesRef("f"));
    assertArrayEquals(new float[] {0.70710f, 0.70710f}, vectorIdA, 0.001f);
    assertArrayEquals(new float[] {-0.0995f, 0.99503f}, vectorIdF, 0.001f);
  }

  @Test
  public void oneVectorBadDimension_shouldThrowException() throws Exception {
    List<Word2VecSynonymTerm> word2VecModel =
        List.of(
            new Word2VecSynonymTerm(new BytesRef("a"), new float[] {10, 10}),
            new Word2VecSynonymTerm(new BytesRef("b"), new float[] {10, 8}),
            new Word2VecSynonymTerm(new BytesRef("c"), new float[] {9}),
            new Word2VecSynonymTerm(new BytesRef("f"), new float[] {-1, 10}));

    expectThrows(
        IllegalArgumentException.class,
        () -> new Word2VecSynonymProvider.SynonymVector(toStream(word2VecModel)));
  }

  @Test
  public void normalizedVector_shouldReturnLength1() throws Exception {
    Word2VecSynonymTerm synonymTerm =
        new Word2VecSynonymTerm(new BytesRef("a"), new float[] {10, 10});
    synonymTerm.normalizeVector();
    float[] vector = synonymTerm.getVector();
    float len = 0;
    for (int i = 0; i < vector.length; i++) {
      len += vector[i] * vector[i];
      System.out.println(vector[i]);
    }
    assertEquals(1, Math.sqrt(len), 0.0001f);
  }

  private Word2VecModelStream toStream(List<Word2VecSynonymTerm> list) {
    int dictionarySize = list.size();
    int vectorDimension = list.get(0).size();
    Stream<Word2VecSynonymTerm> modelStream = list.stream();
    return new Word2VecModelStream(dictionarySize, vectorDimension, modelStream);
  }
}
