package org.apache.lucene.analysis.synonym;


import java.io.InputStream;
import java.util.List;

public class Word2VecSynonymProvider implements SynonymProvider {

    public Word2VecSynonymProvider(InputStream model) {

    }

    @Override
    public List<WeightedSynonym> getSynonyms(String term, boolean ignoreCase) {
        return null;
    }
}
