package org.apache.lucene.analysis.synonym;

import java.io.IOException;
import java.util.List;


/**
 * Generic synonym provider
 *
 * @lucene.experimental
 */
public interface SynonymProvider {

    List<WeightedSynonym> getSynonyms(String term) throws IOException;

    /**
     * Term with the associated weight
     *
     * @lucene.experimental
     */
    class WeightedSynonym {
        private final String term;
        private final float weight;

        public WeightedSynonym(String term, float weight) {
            this.term = term;
            this.weight = weight;
        }

        public String getTerm() {
            return term;
        }

        public float getWeight() {
            return weight;
        }

        @Override
        public String toString() {
            return "WeightedSynonym{" +
                    "term='" + term + '\'' +
                    ", weight=" + weight +
                    '}';
        }
    }
}
