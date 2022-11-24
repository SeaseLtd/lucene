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

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.lucene.tests.util.LuceneTestCase;
import org.apache.lucene.util.CharsRef;
import org.junit.Test;

public class TestDl4jModelReader extends LuceneTestCase {

  private static final String WORD2VEC_MODEL_FILE = "word2vec-model.zip";
  private static final String WORD2VEC_MODEL_FILE_EMPTY = "word2vec-model-empty.zip";

  InputStream stream = TestDl4jModelReader.class.getResourceAsStream(WORD2VEC_MODEL_FILE);
  Dl4jModelReader unit = new Dl4jModelReader(WORD2VEC_MODEL_FILE, stream);

  @Test
  public void read_zipFile_shouldCheckCorrectDictionarySize() throws Exception {
    Word2VecModelStream modelStream = unit.read();
    long modelStreamSize = modelStream.getModelStream().count();
    assertEquals(modelStream.getDictionarySize(), modelStreamSize);
    long expectedDictionarySize = 235;
    assertEquals(expectedDictionarySize, modelStream.getDictionarySize());
  }

  @Test
  public void read_zipFile_shouldCheckCorrectVectorLength() throws Exception {
    Word2VecModelStream modelStream = unit.read();
    Word2VecSynonymTerm firstTerm = modelStream.getModelStream().findFirst().get();
    assertEquals(modelStream.getVectorDimension(), firstTerm.getVector().length);
    int expectedVectorDimension = 100;
    assertEquals(expectedVectorDimension, modelStream.getVectorDimension());
  }

  @Test
  public void read_zipFile_shouldCheckCorrectTermDecoding() throws Exception {
    Word2VecModelStream modelStream = unit.read();
    Word2VecSynonymTerm firstTerm = modelStream.getModelStream().findFirst().get();
    String encodedFirstTerm = "B64:aXQ=";
    assertNotEquals(encodedFirstTerm, firstTerm.getWord());
    CharsRef expectedDecodedFirstTerm = new CharsRef("it");
    assertEquals(expectedDecodedFirstTerm, firstTerm.getWord());
  }

  @Test
  public void base64encodedTerm_shouldCheckCorrectTermDecoding() throws Exception {
    byte[] originalInput = "lucene".getBytes(StandardCharsets.UTF_8);
    String B64encodedLuceneTerm = Base64.getEncoder().encodeToString(originalInput);
    String word2vecEncodedLuceneTerm = "B64:" + B64encodedLuceneTerm;
    assertArrayEquals(
        "lucene".toCharArray(), Dl4jModelReader.decodeTerm(word2vecEncodedLuceneTerm));
  }

  @Test
  public void read_EmptyZipFile_shouldThrowException() throws Exception {
    try (InputStream stream =
        TestDl4jModelReader.class.getResourceAsStream(WORD2VEC_MODEL_FILE_EMPTY)) {
      Dl4jModelReader unit = new Dl4jModelReader(WORD2VEC_MODEL_FILE_EMPTY, stream);
      expectThrows(UnsupportedEncodingException.class, unit::read);
    }
  }
}
