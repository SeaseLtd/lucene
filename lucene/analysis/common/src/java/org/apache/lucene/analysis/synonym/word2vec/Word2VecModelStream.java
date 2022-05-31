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

import java.util.stream.Stream;

/**
 * Word2VecModelStream is a class that embeds the Word2VecSynonymTerm stream and some properties
 * like dictionary size and vector dimension
 *
 * @lucene.experimental
 */
public class Word2VecModelStream {

  private final int dictionarySize;
  private final int vectorDimension;
  private final Stream<Word2VecSynonymTerm> modelStream;

  public Word2VecModelStream(
      int dictionarySize, int vectorDimension, Stream<Word2VecSynonymTerm> modelStream) {
    this.dictionarySize = dictionarySize;
    this.vectorDimension = vectorDimension;
    this.modelStream = modelStream;
  }

  public int getDictionarySize() {
    return dictionarySize;
  }

  public int getVectorDimension() {
    return vectorDimension;
  }

  public Stream<Word2VecSynonymTerm> getModelStream() {
    return modelStream;
  }
}
