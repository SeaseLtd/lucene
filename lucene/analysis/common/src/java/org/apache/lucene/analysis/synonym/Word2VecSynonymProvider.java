package org.apache.lucene.analysis.synonym;


import org.apache.lucene.index.RandomAccessVectorValues;
import org.apache.lucene.index.RandomAccessVectorValuesProducer;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.index.VectorValues;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.hnsw.HnswGraph;
import org.apache.lucene.util.hnsw.HnswGraphBuilder;

import java.io.IOException;
import java.util.List;

public class Word2VecSynonymProvider implements SynonymProvider {

    public static final double DEFAULT_ACCURACY = 0.7;
    public static final VectorSimilarityFunction similarityFunction = VectorSimilarityFunction.DOT_PRODUCT;
    public static final int maxConn = 16;
    public static final int beamWidth = 10;
    public static final long seed = 42;

    private final int dimension;
    private final double accuracy;

    private final HnswGraph hnswGraph;


    public Word2VecSynonymProvider(List<SynonymTerm> vectorData) throws IOException {
        this(vectorData, DEFAULT_ACCURACY);
    }

    public Word2VecSynonymProvider(List<SynonymTerm> vectorData, double accuracy) throws IOException {
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
        VectorProvider vectors = new VectorProvider(vectorData);
        HnswGraphBuilder builder = new HnswGraphBuilder(vectors, similarityFunction, maxConn, beamWidth, seed);
        this.hnswGraph = builder.build(vectors);
    }

    @Override
    public List<WeightedSynonym> getSynonyms(String term) {
        if (term == null) {
            throw new IllegalArgumentException("Term must not be null");
        }
        return List.of();
    }


    class VectorProvider extends VectorValues implements RandomAccessVectorValues, RandomAccessVectorValuesProducer {

        int doc = -1;
        private final List<SynonymTerm> data;

        public VectorProvider(List<SynonymTerm> data) {
            this.data = data;
        }

        @Override
        public RandomAccessVectorValues randomAccess() {
            return new VectorProvider(data);
        }

        @Override
        public float[] vectorValue(int ord) throws IOException {
            return data.get(ord).getVector();
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
