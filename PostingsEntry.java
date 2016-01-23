/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */  

package ir;

import java.io.Serializable;
import java.util.ArrayList;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {
    
    public int docID;
    public double score;

    private ArrayList<Integer> offsets;

    public PostingsEntry(int docID) {
        this.docID = docID;
        offsets = new ArrayList<Integer>();
    }

    /**
     *  PostingsEntries are compared by their score (only relevant 
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in 
     *  descending order.
     */
    public int compareTo( PostingsEntry other ) {
	return Double.compare( other.score, score );
    }

    /**
     * Add an offset to the entry.
     */
    public void addOffset(int offset) {
        offsets.add(offset);
    }

    //
    //  YOUR CODE HERE
    //

}

    
