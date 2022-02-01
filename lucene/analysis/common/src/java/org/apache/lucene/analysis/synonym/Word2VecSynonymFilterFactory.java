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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for {@link Word2VecSynonymFilter}.
 *
 * @lucene.experimental
 * @since 6.4.0
 * @lucene.spi {@value #NAME}
 */
public class Word2VecSynonymFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {

  /** SPI name */
  public static final String NAME = "Word2VecSynonym";

  private final boolean ignoreCase;
  private final String word2vecModel;
  private final Map<String, String> tokArgs = new HashMap<>();

  private SynonymProvider synonymProvider = null;

  public Word2VecSynonymFilterFactory(Map<String, String> args) {
    super(args);
    ignoreCase = getBoolean(args, "ignoreCase", false);
    word2vecModel = require(args, "model");

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
    return synonymProvider == null ? input : new Word2VecSynonymFilter(input, synonymProvider, ignoreCase);
  }

  @Override
  public void inform(ResourceLoader loader) throws IOException {
    try(InputStream modelInputStream = loader.openResource(word2vecModel)){
      synonymProvider = new Word2VecSynonymProvider(modelInputStream);
    }
  }

}
