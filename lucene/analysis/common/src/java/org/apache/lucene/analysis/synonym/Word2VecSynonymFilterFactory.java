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

import org.apache.lucene.analysis.TokenFilterFactory;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.util.ResourceLoader;
import org.apache.lucene.util.ResourceLoaderAware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Factory for {@link Word2VecSynonymFilter}.
 *
 * @lucene.experimental
 */
public class Word2VecSynonymFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {

  /** SPI name */
  public static final String NAME = "Word2VecSynonym";

  private final float accuracy;
  private final String word2vecModel;

  private SynonymProvider synonymProvider = null;

  public Word2VecSynonymFilterFactory(Map<String, String> args) {
    super(args);
    this.accuracy = getFloat(args, "accuracy", 0.7f);
    this.word2vecModel = require(args, "model");

    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }

  /** Default ctor for compatibility with SPI */
  public Word2VecSynonymFilterFactory() {
    throw defaultCtorException();
  }

  @Override
  public TokenStream create(TokenStream input) {
    // if the synonymProvider is null, it means there's actually no synonyms... just return the original stream
    return synonymProvider == null ? input : new Word2VecSynonymFilter(input, synonymProvider);
  }

  @Override
  public void inform(ResourceLoader loader) throws IOException {

    try(InputStream stream = loader.openResource(word2vecModel)) {
      List<Word2VecSynonymTerm> terms = loadWordVectors(stream);
      synonymProvider = new Word2VecSynonymProvider(terms, accuracy);
    }
  }

  private static List<Word2VecSynonymTerm> loadWordVectors(InputStream stream) throws IOException {

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
      return reader.lines()
              .skip(1)
              .map(line -> {
                String[] tokens = line.split(" ");
                String word = tokens[0];

                float[] vector = new float[tokens.length - 1];
                for (int i = 1; i < tokens.length - 1; i++) {
                  vector[i] = Float.parseFloat(tokens[i]);
                }

                return new Word2VecSynonymTerm(word, vector);
              })
              .collect(Collectors.toList());
    }
  }

}
