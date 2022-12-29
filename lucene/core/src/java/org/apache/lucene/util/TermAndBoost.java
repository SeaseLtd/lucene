package org.apache.lucene.util;

/**
 * Wraps a term and boost
 */
public class TermAndBoost {
    /**
     * the term
     */
    public final CharsRef term;
    /**
     * the boost
     */
    public final float boost;

    /**
     * Creates a new TermAndBoost
     */
    public TermAndBoost(CharsRef term, float boost) {
        this.term = CharsRef.deepCopyOf(term);
        this.boost = boost;
    }

    @Override
    public String toString() {
        return "TermAndBoost{term='" + term + '\'' + ", boost=" + boost + '}';
    }
}
