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
import java.util.List;
import org.apache.lucene.tests.util.LuceneTestCase;

public class TestDl4jModelReader extends LuceneTestCase {

  private static final String WORD2VEC_MODEL_FILE = "word2vec-model.txt";

  Dl4jModelReader unit = new Dl4jModelReader(WORD2VEC_MODEL_FILE);

  public void testDl4jModelParser() throws Exception {
    try (InputStream stream = TestDl4jModelReader.class.getResourceAsStream(WORD2VEC_MODEL_FILE)) {
      List<Word2VecSynonymTerm> terms = unit.parse(stream);
      assertEquals(235, terms.size());
      assertEquals(100, terms.get(0).getVector().length);
      assertNotEquals("aXQ=", terms.get(0).getWord());
    }
  }
}
