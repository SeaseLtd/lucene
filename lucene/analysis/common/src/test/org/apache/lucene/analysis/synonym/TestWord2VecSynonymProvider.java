package org.apache.lucene.analysis.synonym;

import org.apache.lucene.tests.util.LuceneTestCase;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TestWord2VecSynonymProvider extends LuceneTestCase {

    private SynonymProvider unit;

    public TestWord2VecSynonymProvider() throws IOException {
        List<Word2VecSynonymTerm> terms = List.of(
                new Word2VecSynonymTerm("a", new float[]{0.24f, 0.78f, 0.28f}),
                new Word2VecSynonymTerm("b", new float[]{0.44f, 0.01f, 0.81f}));
        unit = new Word2VecSynonymProvider(terms);
    }

    @Test
    public void testConstructorNullVector(){
        expectThrows(IllegalArgumentException.class,
                () -> new Word2VecSynonymProvider(null));
    }

    @Test
    public void testConstructorEmptyVector(){
        expectThrows(IllegalArgumentException.class,
                () -> new Word2VecSynonymProvider(List.of()));
    }

    @Test
    public void testConstructorWrongAccuracy(){
        List<Word2VecSynonymTerm> terms = List.of(
                new Word2VecSynonymTerm("a", new float[]{0.24f, 0.78f, 0.28f}),
                new Word2VecSynonymTerm("b", new float[]{0.44f, 0.01f, 0.81f}));
        expectThrows(IllegalArgumentException.class,
                () -> new Word2VecSynonymProvider(terms, 0));
        expectThrows(IllegalArgumentException.class,
                () -> new Word2VecSynonymProvider(terms, -0.4));
        expectThrows(IllegalArgumentException.class,
                () -> new Word2VecSynonymProvider(terms, 1.01));
    }

    @Test
    public void testSimilarityNullTerm(){
        expectThrows(IllegalArgumentException.class,
                () -> unit.getSynonyms(null));
    }

    @Test
    public void testSimilaritySearch() throws Exception {

        List<Word2VecSynonymTerm> terms = List.of(
                new Word2VecSynonymTerm("a", new float[]{10, 10}),
                new Word2VecSynonymTerm("b", new float[]{10, 9}),
                new Word2VecSynonymTerm("c", new float[]{9, 10}),
                new Word2VecSynonymTerm("d", new float[]{1, 1}),
                new Word2VecSynonymTerm("e", new float[]{99, 101}),
                new Word2VecSynonymTerm("f", new float[]{-1, 10}));

        SynonymProvider unit = new Word2VecSynonymProvider(terms);

        Set<String> synonyms = unit.getSynonyms("a").stream()
                        .map(s -> s.getTerm())
                        .collect(Collectors.toSet());

        assertEquals(5, synonyms.size());
        assertTrue(synonyms.contains("a"));
        assertTrue(synonyms.contains("b"));
        assertTrue(synonyms.contains("c"));
        assertTrue(synonyms.contains("d"));
        assertTrue(synonyms.contains("e"));
    }

}
