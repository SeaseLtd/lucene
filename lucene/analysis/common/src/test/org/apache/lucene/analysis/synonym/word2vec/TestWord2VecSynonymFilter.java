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

import java.util.List;
import java.util.stream.Stream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.tests.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.tests.analysis.MockTokenizer;
import org.junit.Test;

public class TestWord2VecSynonymFilter extends BaseTokenStreamTestCase {

  @Test
  public void synonymExpansion_oneCandidate_shouldBeExpanded() throws Exception {
    List<Word2VecSynonymTerm> word2VecModel =
        List.of(
            new Word2VecSynonymTerm("a", new float[] {10, 10}),
            new Word2VecSynonymTerm("b", new float[] {10, 8}),
            new Word2VecSynonymTerm("c", new float[] {9, 10}),
            new Word2VecSynonymTerm("d", new float[] {1, 1}),
            new Word2VecSynonymTerm("e", new float[] {99, 101}),
            new Word2VecSynonymTerm("f", new float[] {1, 10}));

    Word2VecSynonymProvider SynonymProvider = new Word2VecSynonymProvider(toStream(word2VecModel));

    Word2VecSynonymTerm inputTerm = word2VecModel.get(0);

    float similarityWithB =
        VectorSimilarityFunction.COSINE.compare(
            inputTerm.getVector(), word2VecModel.get(1).getVector());
    float similarityWithC =
        VectorSimilarityFunction.COSINE.compare(
            inputTerm.getVector(), word2VecModel.get(2).getVector());
    float similarityWithD =
        VectorSimilarityFunction.COSINE.compare(
            inputTerm.getVector(), word2VecModel.get(3).getVector());
    float similarityWithE =
        VectorSimilarityFunction.COSINE.compare(
            inputTerm.getVector(), word2VecModel.get(4).getVector());

    Analyzer a = getAnalyzer(SynonymProvider, 10, 0.8f);
    assertAnalyzesTo(
        a,
        "pre a post", // input
        new String[] {"pre", "a", "d", "e", "c", "b", "post"}, // output
        new int[] {0, 4, 4, 4, 4, 4, 6}, // start offset
        new int[] {3, 5, 5, 5, 5, 5, 10}, // end offset
        new String[] {"word", "word", "SYNONYM", "SYNONYM", "SYNONYM", "SYNONYM", "word"}, // types
        new int[] {1, 1, 0, 0, 0, 0, 1}, // posIncrements
        new int[] {1, 1, 1, 1, 1, 1, 1}, // posLenghts
        new float[] {
          1, 1, similarityWithD, similarityWithE, similarityWithC, similarityWithB, 1
        }); // boost
    a.close();
  }

  @Test
  public void synonymExpansion_twoCandidates_shouldBothBeExpanded() throws Exception {
    List<Word2VecSynonymTerm> word2VecModel =
        List.of(
            new Word2VecSynonymTerm("a", new float[] {10, 10}),
            new Word2VecSynonymTerm("b", new float[] {10, 8}),
            new Word2VecSynonymTerm("c", new float[] {9, 10}),
            new Word2VecSynonymTerm("d", new float[] {1, 1}),
            new Word2VecSynonymTerm("e", new float[] {99, 101}),
            new Word2VecSynonymTerm("f", new float[] {1, 10}),
            new Word2VecSynonymTerm("post", new float[] {-10, -11}),
            new Word2VecSynonymTerm("after", new float[] {-8, -10}));

    Word2VecSynonymProvider SynonymProvider = new Word2VecSynonymProvider(toStream(word2VecModel));

    Word2VecSynonymTerm firstInputTerm = word2VecModel.get(0);
    Word2VecSynonymTerm secondInputTerm = word2VecModel.get(6);

    float similarityWithB =
        VectorSimilarityFunction.COSINE.compare(
            firstInputTerm.getVector(), word2VecModel.get(1).getVector());
    float similarityWithC =
        VectorSimilarityFunction.COSINE.compare(
            firstInputTerm.getVector(), word2VecModel.get(2).getVector());
    float similarityWithD =
        VectorSimilarityFunction.COSINE.compare(
            firstInputTerm.getVector(), word2VecModel.get(3).getVector());
    float similarityWithE =
        VectorSimilarityFunction.COSINE.compare(
            firstInputTerm.getVector(), word2VecModel.get(4).getVector());
    float similarityWithAfter =
        VectorSimilarityFunction.COSINE.compare(
            secondInputTerm.getVector(), word2VecModel.get(7).getVector());

    Analyzer a = getAnalyzer(SynonymProvider, 10, 0.8f);
    assertAnalyzesTo(
        a,
        "pre a post", // input
        new String[] {"pre", "a", "d", "e", "c", "b", "post", "after"}, // output
        new int[] {0, 4, 4, 4, 4, 4, 6, 6}, // start offset
        new int[] {3, 5, 5, 5, 5, 5, 10, 10}, // end offset
        new String[] { // types
          "word", "word", "SYNONYM", "SYNONYM", "SYNONYM", "SYNONYM", "word", "SYNONYM"
        },
        new int[] {1, 1, 0, 0, 0, 0, 1, 0}, // posIncrements
        new int[] {1, 1, 1, 1, 1, 1, 1, 1}, // posLengths
        new float[] {
          1,
          1,
          similarityWithD,
          similarityWithE,
          similarityWithC,
          similarityWithB,
          1,
          similarityWithAfter
        }); // boost
    a.close();
  }

  @Test
  public void synonymExpansion_forMinAcceptedSimilarity_shouldExpandToNoneSynonyms()
      throws Exception {
    List<Word2VecSynonymTerm> word2VecModel =
        List.of(
            new Word2VecSynonymTerm("a", new float[] {10, 10}),
            new Word2VecSynonymTerm("b", new float[] {-10, -8}),
            new Word2VecSynonymTerm("c", new float[] {-9, -10}),
            new Word2VecSynonymTerm("f", new float[] {-1, -10}));

    Word2VecSynonymProvider SynonymProvider = new Word2VecSynonymProvider(toStream(word2VecModel));

    Analyzer a = getAnalyzer(SynonymProvider, 10, 0.8f);
    assertAnalyzesTo(
        a,
        "pre a post", // input
        new String[] {"pre", "a", "post"}, // output
        new int[] {0, 4, 6}, // start offset
        new int[] {3, 5, 10}, // end offset
        new String[] {"word", "word", "word"}, // types
        new int[] {1, 1, 1}, // posIncrements
        new int[] {1, 1, 1}, // posLengths
        new float[] {1, 1, 1}); // boost
    a.close();
  }

  private Analyzer getAnalyzer(
      SynonymProvider synonymProvider, int maxSynonymsPerTerm, float minAcceptedSimilarity) {
    return new Analyzer() {
      @Override
      protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
        // Make a local variable so testRandomHuge doesn't share it across threads!
        Word2VecSynonymFilter synFilter =
            new Word2VecSynonymFilter(
                tokenizer, synonymProvider, maxSynonymsPerTerm, minAcceptedSimilarity);
        return new TokenStreamComponents(tokenizer, synFilter);
      }
    };
  }

  private Word2VecModelStream toStream(List<Word2VecSynonymTerm> list) {
    int dictionarySize = list.size();
    int vectorDimension = list.get(0).size();
    Stream<Word2VecSynonymTerm> modelStream = list.stream();
    return new Word2VecModelStream(dictionarySize, vectorDimension, modelStream);
  }
}
