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
import java.io.InputStream;
import java.util.List;
import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.sequencevectors.SequenceVectors;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;

/**
 * Word2VecModelReader that wraps the Deeplearning4j library
 *
 * @lucene.experimental
 */
public class Dl4jModelReader implements Word2VecModelReader {

  public Dl4jModelReader(String word2vecModelFile) {}

  @Override
  public List<Word2VecSynonymTerm> parse(InputStream stream) throws IOException {

    SequenceVectors<VocabWord> vectors = WordVectorSerializer.readSequenceVectors(stream, true);

    WeightLookupTable<VocabWord> lookupTable = vectors.getLookupTable();
    VocabCache<VocabWord> vocab = vectors.getVocab();

    return List.of();
  }
}
