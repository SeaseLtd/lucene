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

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.synonym.SynonymProvider.WeightedSynonym;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.search.BoostAttribute;

import java.io.IOException;
import java.util.LinkedList;


/**
 * Applies single-token synonyms from a Word2Vec trained network to an incoming {@link TokenStream}.
 *
 * @lucene.experimental
 */
public final class Word2VecSynonymFilter extends TokenFilter {

  public static final String TYPE_SYNONYM = "SYNONYM";

  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final BoostAttribute boostAtt = addAttribute(BoostAttribute.class);
  private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

  private final SynonymProvider synonymProvider;
  private final boolean ignoreCase;

  private final LinkedList<WeightedSynonym> outputBuffer = new LinkedList<>();



  /**
   * Apply previously built synonymProvider to incoming tokens.
   *
   * @param input input tokenstream
   * @param synonymProvider synonym provider
   * @param ignoreCase case-folds input for matching with {@link Character#toLowerCase(int)}. Note,
   *     if you set this to true, it's your responsibility to lowercase the input entries when you
   *     create the {@link SynonymMap}
   */
  public Word2VecSynonymFilter(TokenStream input, SynonymProvider synonymProvider, boolean ignoreCase) {
    super(input);
    this.synonymProvider = synonymProvider;
    this.ignoreCase = ignoreCase;
  }

  @Override
  public boolean incrementToken() throws IOException {

    if (!outputBuffer.isEmpty()) {
      // We still have pending outputs from a prior synonym match:
      releaseBufferedToken();
      return true;
    }

    if(input.incrementToken()) {
      String term = new String(termAtt.buffer(), 0, termAtt.length());
      outputBuffer.addAll(this.synonymProvider.getSynonyms(term, ignoreCase));
      return true;
    }
    return false;
  }


  private void releaseBufferedToken() throws IOException {

    WeightedSynonym synonym = outputBuffer.pollFirst();

    termAtt.setEmpty();
    termAtt.append(synonym.getTerm());
    boostAtt.setBoost(synonym.getWeight());
    typeAtt.setType(TYPE_SYNONYM);
  }


  @Override
  public void reset() throws IOException {
    super.reset();
    outputBuffer.clear();
  }

}
