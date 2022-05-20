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

package org.apache.lucene.analysis.synonym;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.apache.lucene.tests.util.LuceneTestCase;
import org.junit.Test;

public class TestDl4jModelReader extends LuceneTestCase {

  private static final String WORD2VEC_MODEL_FILE = "word2vec-model.zip";
  private static final String WORD2VEC_MODEL_FILE_EMPTY = "word2vec-model-empty.zip";

  InputStream stream = TestDl4jModelReader.class.getResourceAsStream(WORD2VEC_MODEL_FILE);
  Dl4jModelReader unit = new Dl4jModelReader(WORD2VEC_MODEL_FILE, stream);

  @Test
  public void testReadCorrectSize() throws Exception {
    Word2VecModelStream modelStream = unit.read();
    long modelStreamSize = modelStream.getModelStream().count();
    assertEquals(modelStream.getSize(), modelStreamSize);
    assertEquals(235, modelStream.getSize());
  }

  @Test
  public void testReadCorrectVectorLength() throws Exception {
    Word2VecModelStream modelStream = unit.read();
    Word2VecSynonymTerm firstTerm = modelStream.getModelStream().findFirst().get();
    assertEquals(modelStream.getVectorDimension(), firstTerm.getVector().length);
    assertEquals(100, modelStream.getVectorDimension());
  }

  @Test
  public void testReadCorrectTerm() throws Exception {
    Word2VecModelStream modelStream = unit.read();
    Word2VecSynonymTerm firstTerm = modelStream.getModelStream().findFirst().get();
    assertNotEquals("B64:aXQ=", firstTerm.getWord());
    assertEquals("it", firstTerm.getWord());
  }

  @Test
  public void testDecodeTerm() throws Exception {
    assertEquals("lucene", Dl4jModelReader.decodeTerm("b64:bHVjZW5l"));
    assertEquals("lucene", Dl4jModelReader.decodeTerm("B64:bHVjZW5l"));
    assertEquals("lucene", Dl4jModelReader.decodeTerm("lucene"));
  }

  @Test
  public void testEmptyZipFile() throws Exception {
    try (InputStream stream =
        TestDl4jModelReader.class.getResourceAsStream(WORD2VEC_MODEL_FILE_EMPTY)) {
      Dl4jModelReader unit = new Dl4jModelReader(WORD2VEC_MODEL_FILE_EMPTY, stream);
      expectThrows(UnsupportedEncodingException.class, unit::read);
    }
  }
}
