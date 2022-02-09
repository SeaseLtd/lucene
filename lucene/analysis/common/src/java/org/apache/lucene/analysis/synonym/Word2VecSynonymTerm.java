package org.apache.lucene.analysis.synonym;

/**
 * Word2Vec unit composed by a term with the associated vector
 *
 * @lucene.experimental
 */
public class Word2VecSynonymTerm {

    private final String word;
    private final float[] vector;

    public Word2VecSynonymTerm(String word, float[] vector){
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
        StringBuilder builder = new StringBuilder(this.word);
        builder.append(" [");
        if( vector.length > 0) {
            for (int i = 0; i < vector.length - 1; i++) {
                builder.append(String.format("%.3f,", vector[i]));
            }
            builder.append(String.format("%.3f]", vector[vector.length - 1]));
        }
        return builder.toString();
    }
}
