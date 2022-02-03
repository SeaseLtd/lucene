package org.apache.lucene.analysis.synonym;


import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.RandomAccessVectorValues;
import org.apache.lucene.index.RandomAccessVectorValuesProducer;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.index.VectorValues;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.hnsw.HnswGraph;
import org.apache.lucene.util.hnsw.HnswGraphBuilder;
import org.apache.lucene.util.hnsw.NeighborQueue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SplittableRandom;
import java.util.TreeMap;

/**
 * Implementation of a {@link SynonymProvider} using vector similarity technique
 *
 * @lucene.experimental
 */
public class Word2VecSynonymProvider implements SynonymProvider {

    public static final double DEFAULT_ACCURACY = 0.7;
    public static final VectorSimilarityFunction similarityFunction = VectorSimilarityFunction.COSINE;
    public static final int maxConn = 16;
    public static final int beamWidth = 10;
    public static final long seed = 42;

    private final int dimension;
    private final double accuracy;

    private final VectorProvider vectors;
    private final HnswGraph hnswGraph;


    public Word2VecSynonymProvider(List<Word2VecSynonymTerm> vectorData) throws IOException {
        this(vectorData, DEFAULT_ACCURACY);
    }

    public Word2VecSynonymProvider(List<Word2VecSynonymTerm> vectorData, double accuracy) throws IOException {
        if (vectorData == null){
            throw new IllegalArgumentException("VectorData must be set");
        }
        if (vectorData.size() <= 0){
            throw new IllegalArgumentException("VectorData must not be empty");
        }
        if (accuracy <= 0 || accuracy > 1) {
            throw new IllegalArgumentException("Accuracy must be in the range (0, 1]. Found: " + accuracy);
        }
        this.accuracy = accuracy;
        this.dimension = vectorData.get(0).size();

        // Create the provider which will feed the vectors for the graph
        vectors = new VectorProvider(vectorData);
        HnswGraphBuilder builder = new HnswGraphBuilder(vectors, similarityFunction, maxConn, beamWidth, seed);
        this.hnswGraph = builder.build(vectors);
    }

    @Override
    public List<WeightedSynonym> getSynonyms(String token) throws IOException {
        if (token == null) {
            throw new IllegalArgumentException("Term must not be null");
        }
        ArrayList<WeightedSynonym> result = new ArrayList<>();
        float[] query = vectors.vectorValue(token);
        if (query != null) {
            NeighborQueue neighbor = HnswGraph.search(
                    query,
                    10,
                    vectors,
                    similarityFunction,
                    hnswGraph,
                    null);

            System.out.println("neighbor size: " + neighbor.size() + " Top:" + neighbor.topNode());
            int size = neighbor.size();
            for(int i = 0; i < size; i++){
                int id = neighbor.pop();
                Word2VecSynonymTerm term = vectors.getSynonymTerm(id);
                float similarity = similarityFunction.compare(term.getVector(), query);
                System.out.println("id: " + id + " word: " + term.getWord() + " similarity:" + similarity);
                if (similarity >= this.accuracy) {
                    result.add(new WeightedSynonym(term.getWord(), similarity));
                }
            }
        }
        return result;
    }


    class VectorProvider extends VectorValues implements RandomAccessVectorValues, RandomAccessVectorValuesProducer {

        int doc = -1;
        private final List<Word2VecSynonymTerm> data;
        private final Map<String, Word2VecSynonymTerm> word2VecMap = new HashMap<>();

        public VectorProvider(List<Word2VecSynonymTerm> data) {
            this.data = data;
            data.forEach(synTerm -> word2VecMap.put(synTerm.getWord(), synTerm));
        }

        @Override
        public RandomAccessVectorValues randomAccess() {
            return new VectorProvider(data);
        }

        @Override
        public float[] vectorValue(int ord) throws IOException {
            return data.get(ord).getVector();
        }

        public Word2VecSynonymTerm getSynonymTerm(int ord) {
            return data.get(ord);
        }

        public float[] vectorValue(String word) {
            Word2VecSynonymTerm term = word2VecMap.get(word);
            return (term == null)? null : term.getVector();
        }

        @Override
        public BytesRef binaryValue(int targetOrd) throws IOException {
            return null;
        }

        @Override
        public int dimension() {
            return data.get(0).size();
        }

        @Override
        public int size() {
            return data.size();
        }

        @Override
        public float[] vectorValue() throws IOException {
            return vectorValue(doc);
        }

        @Override
        public int docID() {
            return doc;
        }

        @Override
        public int nextDoc() throws IOException {
            return advance(doc + 1);
        }

        @Override
        public int advance(int target) throws IOException {
            if (target >= 0 && target < data.size()) {
                doc = target;
            } else {
                doc = NO_MORE_DOCS;
            }
            return doc;
        }

        @Override
        public long cost() {
            return data.size();
        }

        public VectorProvider copy() {
            return new VectorProvider(data);
        }
    }
}
