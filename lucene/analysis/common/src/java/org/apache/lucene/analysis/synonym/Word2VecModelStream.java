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

import java.util.stream.Stream;

/**
 * Word2VecModelStream is a class that embeds the Word2VecSynonymTerm stream and some properties
 * like size and vector dimensions
 *
 * @lucene.experimental
 */
public class Word2VecModelStream {

  private final int size;
  private final int vectorDimension;
  private final Stream<Word2VecSynonymTerm> modelStream;

  public Word2VecModelStream(int size, int dimension, Stream<Word2VecSynonymTerm> modelStream) {
    this.size = size;
    this.vectorDimension = dimension;
    this.modelStream = modelStream;
  }

  public int getSize() {
    return size;
  }

  public int getVectorDimension() {
    return vectorDimension;
  }

  public Stream<Word2VecSynonymTerm> getModelStream() {
    return modelStream;
  }
}
