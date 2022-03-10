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
import java.util.Locale;
import java.util.Map;
import org.apache.lucene.analysis.TokenFilterFactory;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.synonym.Word2VecSynonymProviderFactory.Word2VecSupportedFormats;
import org.apache.lucene.util.ResourceLoader;
import org.apache.lucene.util.ResourceLoaderAware;

/**
 * Factory for {@link Word2VecSynonymFilter}.
 *
 * @lucene.experimental
 * @lucene.spi {@value #NAME}
 */
public class Word2VecSynonymFilterFactory extends TokenFilterFactory
    implements ResourceLoaderAware {

  /** SPI name */
  public static final String NAME = "Word2VecSynonym";

  public static final int DEFAULT_MAX_RESULT = 10;
  public static final float DEFAULT_ACCURACY = 0.7f;

  private final int maxResult;
  private final float accuracy;
  private final Word2VecSupportedFormats format;
  private final String word2vecModel;

  private SynonymProvider synonymProvider = null;

  public Word2VecSynonymFilterFactory(Map<String, String> args) {
    super(args);
    this.maxResult = getInt(args, "maxResult", DEFAULT_MAX_RESULT);
    this.accuracy = getFloat(args, "accuracy", DEFAULT_ACCURACY);
    this.word2vecModel = require(args, "model");

    String modelFormat = get(args, "format", "dl4j").toUpperCase(Locale.ROOT);
    this.format = Word2VecSupportedFormats.valueOf(modelFormat);

    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
    if (accuracy <= 0 || accuracy > 1) {
      throw new IllegalArgumentException(
          "Accuracy must be in the range (0, 1]. Found: " + accuracy);
    }
    if (maxResult < 0) {
      throw new IllegalArgumentException(
          "maxResult must be a positive integer. Found: " + maxResult);
    }
  }

  /** Default ctor for compatibility with SPI */
  public Word2VecSynonymFilterFactory() {
    throw defaultCtorException();
  }

  SynonymProvider getSynonymProvider() {
    return this.synonymProvider;
  }

  @Override
  public TokenStream create(TokenStream input) {
    // if the synonymProvider is null, it means there's actually no synonyms... just return the
    // original stream
    return synonymProvider == null
        ? input
        : new Word2VecSynonymFilter(input, synonymProvider, maxResult, accuracy);
  }

  @Override
  public void inform(ResourceLoader loader) throws IOException {
    this.synonymProvider =
        Word2VecSynonymProviderFactory.getSynonymProvider(loader, word2vecModel, format);
  }
}
