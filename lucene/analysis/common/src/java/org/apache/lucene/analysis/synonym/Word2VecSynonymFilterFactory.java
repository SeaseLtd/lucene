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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Factory for {@link Word2VecSynonymFilter}.
 *
 * @lucene.experimental
 * @lucene.spi {@value #NAME}
 */
public class Word2VecSynonymFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {

  /** SPI name */
  public static final String NAME = "Word2VecSynonym";

  public static final int DEFAULT_MAX_RESULT = 10;
  public static final float DEFAULT_ACCURACY = 0.7f;
  private static final String MODEL_FILE_NAME_PREFIX = "syn0";

  private final int maxResult;
  private final float accuracy;
  private final String word2vecModel;

  private SynonymProvider synonymProvider = null;

  public Word2VecSynonymFilterFactory(Map<String, String> args) {
    super(args);
    this.maxResult = getInt(args, "maxResult", DEFAULT_MAX_RESULT);
    this.accuracy = getFloat(args, "accuracy", DEFAULT_ACCURACY);
    this.word2vecModel = require(args, "model");

    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }

  /** Default ctor for compatibility with SPI */
  public Word2VecSynonymFilterFactory() {
    throw defaultCtorException();
  }

  SynonymProvider getSynonymProvider(){
    return this.synonymProvider;
  }

  @Override
  public TokenStream create(TokenStream input) {
    // if the synonymProvider is null, it means there's actually no synonyms... just return the original stream
    return synonymProvider == null ? input : new Word2VecSynonymFilter(input, synonymProvider);
  }

  @Override
  public void inform(ResourceLoader loader) throws IOException {
    try(InputStream stream = loader.openResource(word2vecModel)) {
      printJVMMemory("before reading file");
      List<Word2VecSynonymTerm> terms = loadWordVectors(stream);
      printJVMMemory("before creating graph");
      synonymProvider = new Word2VecSynonymProvider(terms, maxResult, accuracy);
      printJVMMemory("everything initialized");
    }
  }

  List<Word2VecSynonymTerm> loadWordVectors(InputStream stream) throws IOException {
    long startTime = System.currentTimeMillis();
    try (ZipInputStream zipfile = new ZipInputStream(new BufferedInputStream(stream))) {

      ZipEntry entry;
      while ((entry = zipfile.getNextEntry()) != null) {
        String name = entry.getName();
        if (name.startsWith(MODEL_FILE_NAME_PREFIX)) {
          BufferedReader reader = new BufferedReader(new InputStreamReader(zipfile));
          List<Word2VecSynonymTerm> result = reader.lines()
                  .skip(1)
                  .map(line -> {
                    String[] tokens = line.split(" ");
                    String term = decode(tokens[0]);

                    float[] vector = new float[tokens.length - 1];
                    for (int i = 0; i < tokens.length - 1; i++) {
                      vector[i] = Float.parseFloat(tokens[i + 1]);
                    }
                    return new Word2VecSynonymTerm(term, vector);
                  })
                  .collect(Collectors.toList());
          long endTime = System.currentTimeMillis();
          System.out.println("loadWordVectors - Elapsed time: " + (endTime-startTime) + " ms (" + ((endTime-startTime)/60000) + " min)");
          return result;
        }
      }
      throw new UnsupportedEncodingException("The ZIP file '" + word2vecModel + "' does not contain any "
              + MODEL_FILE_NAME_PREFIX + " file");
    }
  }

  private String decode(String term) {
    if(term.startsWith("B64:")) {
      return new String(Base64.getDecoder().decode(term.substring(4).trim()));
    }
    return term;
  }

  private void printJVMMemory(String prefix){
    long total = Runtime.getRuntime().totalMemory() / 1024 / 1024;
    System.out.println(prefix + ": total memory = " + total + "Mb");
  }

}
