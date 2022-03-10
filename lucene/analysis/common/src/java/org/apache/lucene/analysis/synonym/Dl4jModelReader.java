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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Word2VecModelReader that wraps the Deeplearning4j library
 *
 * @lucene.experimental
 */
public class Dl4jModelReader implements Word2VecModelReader {

  private static final String MODEL_FILE_NAME_PREFIX = "syn0";

  private final String word2vecModelFile;
  private final ZipInputStream zipfile;

  public Dl4jModelReader(String word2vecModelFile, InputStream stream) {
    this.word2vecModelFile = word2vecModelFile;
    this.zipfile = new ZipInputStream(new BufferedInputStream(stream));
  }

  @Override
  public Word2VecModelStream parse() throws IOException {

    ZipEntry entry;
    while ((entry = zipfile.getNextEntry()) != null) {
      String name = entry.getName();
      if (name.startsWith(MODEL_FILE_NAME_PREFIX)) {
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(zipfile, StandardCharsets.UTF_8));

        String header = reader.readLine();
        String[] headerValues = header.split(" ");
        int size = Integer.parseInt(headerValues[0]);
        int dimension = Integer.parseInt(headerValues[1]);

        Stream<Word2VecSynonymTerm> modelStream =
            reader
                .lines()
                .map(
                    line -> {
                      String[] tokens = line.split(" ");
                      String term = decodeTerm(tokens[0]);

                      float[] vector = new float[tokens.length - 1];
                      for (int i = 0; i < tokens.length - 1; i++) {
                        vector[i] = Float.parseFloat(tokens[i + 1]);
                      }
                      return new Word2VecSynonymTerm(term, vector);
                    });

        return new Word2VecModelStream(size, dimension, modelStream);
      }
    }
    throw new UnsupportedEncodingException(
        "The ZIP file '"
            + word2vecModelFile
            + "' does not contain any "
            + MODEL_FILE_NAME_PREFIX
            + " file");
  }

  private String decodeTerm(String term) {
    if (term.startsWith("B64:")) {
      return new String(
          Base64.getDecoder().decode(term.substring(4).trim()), StandardCharsets.UTF_8);
    }
    return term;
  }

  @Override
  public void close() throws IOException {
    zipfile.close();
  }
}
