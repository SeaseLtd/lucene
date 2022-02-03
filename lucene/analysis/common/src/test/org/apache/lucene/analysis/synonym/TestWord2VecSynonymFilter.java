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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.tests.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.tests.analysis.MockTokenizer;

import java.io.IOException;
import java.util.List;

public class TestWord2VecSynonymFilter extends BaseTokenStreamTestCase {

  /** Set as a side effect by {@link #getAnalyzer}. */
  private Word2VecSynonymFilter synFilter;
  private SynonymProvider synonymProvider;

  public TestWord2VecSynonymFilter() throws IOException {
    synonymProvider = createSynonymProvider();
  }

  public void testBasicKeepOrigOneOutput() throws Exception {

    Analyzer a = getAnalyzer(synonymProvider, 0.8f);
    assertAnalyzesTo(
        a,
        "pre a post",
        new String[] {"pre", "a", "b", "c", "d", "e", "post"},
        new int[] {0, 4, 4, 4, 4, 4, 6},
        new int[] {3, 5, 5, 5, 5, 5, 10},
        new String[] {"word", "word", "SYNONYM", "SYNONYM", "SYNONYM", "SYNONYM", "word"},
        new int[] {1, 0, 0, 0, 0, 1, 1},
        new int[] {1, 1, 1, 1, 1, 1, 1});
    a.close();
  }

  private Analyzer getAnalyzer(SynonymProvider synonymProvider, final float accuracy) throws IOException {
    return new Analyzer() {
      @Override
      protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
        // Make a local variable so testRandomHuge doesn't share it across threads!
        Word2VecSynonymFilter synFilter = new Word2VecSynonymFilter(tokenizer, synonymProvider, accuracy);
        TestWord2VecSynonymFilter.this.synFilter = synFilter;
        return new TokenStreamComponents(tokenizer, synFilter);
      }
    };
  }

  private SynonymProvider createSynonymProvider() throws IOException {
    List<Word2VecSynonymTerm> terms = List.of(
            new Word2VecSynonymTerm("a", new float[]{10, 10}),
            new Word2VecSynonymTerm("b", new float[]{10, 9}),
            new Word2VecSynonymTerm("c", new float[]{9, 10}),
            new Word2VecSynonymTerm("d", new float[]{1, 1}),
            new Word2VecSynonymTerm("e", new float[]{99, 101}),
            new Word2VecSynonymTerm("f", new float[]{1, 10}));

    return new Word2VecSynonymProvider(terms);
  }

}
