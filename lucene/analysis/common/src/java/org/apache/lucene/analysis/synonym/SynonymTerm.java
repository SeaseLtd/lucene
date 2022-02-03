package org.apache.lucene.analysis.synonym;

public class SynonymTerm {

    private final String word;
    private final float[] vector;

    public SynonymTerm(String word, float[] vector){
        this.word = word;
        this.vector = vector;
    }

    public String getWord(){
        return this.word;
    }

    public float[] getVector(){
        return this.vector;
    }

    public int size(){
        return vector.length;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");
        if( vector.length > 0) {
            for (int i = 0; i < vector.length - 1; i++) {
                builder.append(String.format("%.3f,", vector[i]));
            }
            builder.append(String.format("%.3f]", vector[vector.length - 1]));
        }
        return builder.toString();
    }
}
