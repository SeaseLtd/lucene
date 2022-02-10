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

import org.apache.lucene.tests.analysis.BaseTokenStreamFactoryTestCase;
import org.apache.lucene.util.ClasspathResourceLoader;
import org.apache.lucene.util.ResourceLoader;


public class TestWord2VecSynonymFilterFactory extends BaseTokenStreamFactoryTestCase {

  public static final String FACTORY_NAME = "Word2VecSynonym";
  private static final String WORD2VEC_MODEL_FILE = "word2vec-model.txt";

  public void testInform() throws Exception {
    ResourceLoader loader = new ClasspathResourceLoader(getClass());
    assertTrue("loader is null and it shouldn't be", loader != null);
    Word2VecSynonymFilterFactory factory =
            (Word2VecSynonymFilterFactory) tokenFilterFactory(FACTORY_NAME,
                    "model", WORD2VEC_MODEL_FILE,
                    "accuracy", "0.7");

    SynonymProvider synonymProvider = factory.getSynonymProvider();
    assertNotEquals(null, synonymProvider);
  }
}
