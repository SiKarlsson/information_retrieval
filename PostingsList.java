/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */  

package ir;

import java.util.LinkedList;
import java.io.Serializable;

/**
 *   A list of postings for a given word.
 */
public class PostingsList implements Serializable {
    
    /** The postings list as a linked list. */
    private LinkedList<PostingsEntry> list = new LinkedList<PostingsEntry>();


    /**  Number of postings in this list  */
    public int size() {
	return list.size();
    }

    /**  Returns the ith posting */
    public PostingsEntry get( int i ) {
	return list.get( i );
    }

    public void insert(PostingsEntry pe) {
        if (!duplicate(pe)) {
            list.addLast(pe);
        }
    }

    public void insert(PostingsEntry pe, int offset) {
        insert(pe);
        list.get(list.size() - 1).addOffset(offset);
    }

    private Boolean duplicate(PostingsEntry pe) {
        if (list.size() > 0) {
            PostingsEntry last = list.getLast();
            if (last.docID == pe.docID) {
                return true;
            }
        }
        return false;
    }

    //
    //  YOUR CODE HERE
    //
}
	

			   
