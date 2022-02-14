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

import java.io.IOException;
import java.util.List;
import org.apache.lucene.analysis.synonym.SynonymProvider.WeightedSynonym;
import org.apache.lucene.tests.util.LuceneTestCase;
import org.junit.Test;

public class TestWord2VecSynonymProvider extends LuceneTestCase {

  private static final int MAX_RESULT = 10;
  private static final float ACCURACY = 0.7f;

  private final Word2VecSynonymProvider unit;

  public TestWord2VecSynonymProvider() throws IOException {
    List<Word2VecSynonymTerm> terms =
        List.of(
            new Word2VecSynonymTerm("a", new float[] {0.24f, 0.78f, 0.28f}),
            new Word2VecSynonymTerm("b", new float[] {0.44f, 0.01f, 0.81f}));
    unit = new Word2VecSynonymProvider(terms, MAX_RESULT, ACCURACY);
  }

  @Test
  public void testConstructorNullVector() {
    expectThrows(
        IllegalArgumentException.class,
        () -> new Word2VecSynonymProvider(null, MAX_RESULT, ACCURACY));
  }

  @Test
  public void testConstructorEmptyVector() {
    expectThrows(
        IllegalArgumentException.class,
        () -> new Word2VecSynonymProvider(List.of(), MAX_RESULT, ACCURACY));
  }

  @Test
  public void testConstructorWrongAccuracy() {
    List<Word2VecSynonymTerm> terms =
        List.of(
            new Word2VecSynonymTerm("a", new float[] {0.24f, 0.78f, 0.28f}),
            new Word2VecSynonymTerm("b", new float[] {0.44f, 0.01f, 0.81f}));
    expectThrows(
        IllegalArgumentException.class, () -> new Word2VecSynonymProvider(terms, MAX_RESULT, 0));
    expectThrows(
        IllegalArgumentException.class,
        () -> new Word2VecSynonymProvider(terms, MAX_RESULT, -0.4f));
    expectThrows(
        IllegalArgumentException.class,
        () -> new Word2VecSynonymProvider(terms, MAX_RESULT, 1.01f));
  }

  @Test
  public void testSimilarityNullTerm() {
    expectThrows(IllegalArgumentException.class, () -> unit.getSynonyms(null));
  }

  @Test
  public void testSimilaritySearch() throws Exception {

    List<Word2VecSynonymTerm> terms =
        List.of(
            new Word2VecSynonymTerm("a", new float[] {10, 10}),
            new Word2VecSynonymTerm("b", new float[] {10, 8}),
            new Word2VecSynonymTerm("c", new float[] {9, 10}),
            new Word2VecSynonymTerm("d", new float[] {1, 1}),
            new Word2VecSynonymTerm("e", new float[] {99, 101}),
            new Word2VecSynonymTerm("f", new float[] {-1, 10}));

    SynonymProvider unit = new Word2VecSynonymProvider(terms, MAX_RESULT, ACCURACY);

    String[] expected = {"d", "e", "c", "b"};
    List<WeightedSynonym> actual = unit.getSynonyms("a");

    assertEquals(4, actual.size());
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], actual.get(i).getTerm());
    }
  }

  @Test
  public void testSimilarityNoSynonymResults() throws Exception {
    List<Word2VecSynonymTerm> terms =
        List.of(
            new Word2VecSynonymTerm("a", new float[] {10, 10}),
            new Word2VecSynonymTerm("b", new float[] {-10, -8}),
            new Word2VecSynonymTerm("c", new float[] {-9, -10}),
            new Word2VecSynonymTerm("d", new float[] {6, -6}));

    SynonymProvider unit = new Word2VecSynonymProvider(terms, MAX_RESULT, ACCURACY);

    List<WeightedSynonym> actual = unit.getSynonyms("a");
    assertEquals(0, actual.size());
  }
}
