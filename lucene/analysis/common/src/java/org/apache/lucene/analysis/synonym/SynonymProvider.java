package org.apache.lucene.analysis.synonym;

import java.util.List;

public interface SynonymProvider {

    List<WeightedSynonym> getSynonyms(String term);

    class WeightedSynonym {
        private String term;
        private float weight;

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
    }
}
