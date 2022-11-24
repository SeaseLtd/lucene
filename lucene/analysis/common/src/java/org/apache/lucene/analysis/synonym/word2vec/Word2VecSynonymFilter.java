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
import java.util.LinkedList;
import java.util.List;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.word2vec.SynonymProvider.WeightedSynonym;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.search.BoostAttribute;
import org.apache.lucene.util.CharsRef;

/**
 * Applies single-token synonyms from a Word2Vec trained network to an incoming {@link TokenStream}.
 *
 * @lucene.experimental
 */
public final class Word2VecSynonymFilter extends TokenFilter {

  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final PositionIncrementAttribute posIncrementAtt =
      addAttribute(PositionIncrementAttribute.class);
  private final PositionLengthAttribute posLenAtt = addAttribute(PositionLengthAttribute.class);
  private final BoostAttribute boostAtt = addAttribute(BoostAttribute.class);
  private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

  private final SynonymProvider synonymProvider;
  private final int maxSynonymsPerTerm;
  private final float minAcceptedSimilarity;
  private final LinkedList<WeightedSynonym> synonymBuffer = new LinkedList<>();
  private State lastState = null;

  /**
   * Apply previously built synonymProvider to incoming tokens.
   *
   * @param input input tokenstream
   * @param synonymProvider synonym provider
   * @param maxSynonymsPerTerm maximum number of result returned by the synonym search
   * @param minAcceptedSimilarity minimal value of cosine similarity between the searched vector and
   *     the retrieved ones
   */
  public Word2VecSynonymFilter(
      TokenStream input,
      SynonymProvider synonymProvider,
      int maxSynonymsPerTerm,
      float minAcceptedSimilarity) {
    super(input);
    this.synonymProvider = synonymProvider;
    this.maxSynonymsPerTerm = maxSynonymsPerTerm;
    this.minAcceptedSimilarity = minAcceptedSimilarity;
  }

  @Override
  public boolean incrementToken() throws IOException {

    if (!synonymBuffer.isEmpty()) {
      // We still have pending outputs from a prior synonym match:
      releaseBufferedToken();
      return true;
    }

    if (input.incrementToken()) {
      CharsRef term = new CharsRef(termAtt.buffer(), 0, termAtt.length());
      List<WeightedSynonym> synonyms =
          this.synonymProvider.getSynonyms(term, maxSynonymsPerTerm, minAcceptedSimilarity);
      if (synonyms.size() > 0) {
        // The synonyms list does not contain the original term so, the first time it returns the
        // original term
        // and store the other synonyms in a buffer. These synonyms will be returned in a future
        // call
        this.lastState = captureState();
        this.synonymBuffer.addAll(synonyms);
      }
      return true;
    }
    return false;
  }

  private void releaseBufferedToken() {
    assert synonymBuffer.size() > 0;

    WeightedSynonym synonym = synonymBuffer.pollFirst();
    clearAttributes();
    restoreState(this.lastState);
    termAtt.setEmpty();
    termAtt.append(synonym.getTerm());
    boostAtt.setBoost(synonym.getWeight());
    typeAtt.setType(SynonymGraphFilter.TYPE_SYNONYM);
    posLenAtt.setPositionLength(1);
    posIncrementAtt.setPositionIncrement(0);
  }

  @Override
  public void reset() throws IOException {
    super.reset();
    synonymBuffer.clear();
  }
}
