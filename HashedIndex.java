/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 *   Additions: Hedvig Kjellström, 2012-14
 */  


package ir;

import java.util.HashMap;
import java.util.Iterator;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {

    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();

    /**
     *  Inserts this token in the index.
     */
    public void insert( String token, int docID, int offset ) {
        PostingsList pl = getPostings(token);
        if (pl == null) {
            pl = new PostingsList();
        }
        PostingsEntry pe = new PostingsEntry(docID);
        pl.insert(pe);
        index.put(token, pl);
    }


    /**
     *  Returns all the words in the index.
     */
    public Iterator<String> getDictionary() {
	// 
	//  REPLACE THE STATEMENT BELOW WITH YOUR CODE
	//
	return null;
    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
       return index.get(token);
    }


    /**
     *  Searches the index for postings matching the query.
     */
    public PostingsList search( Query query, int queryType, int rankingType, int structureType ) {
        if (query.terms.size() > 0) {
            String term = query.terms.getFirst();
            PostingsList intersection = getPostings(term);
            
            for (int i = 1; i < query.terms.size(); i++) {
                String nextTerm = query.terms.get(i);
                intersection = intersect(intersection, getPostings(nextTerm));
                if (intersection == null) {
                    return null;
                }
            }
            return intersection;    
        } else { 
            return null;
        }
    }

    /**
     *  Intersects two postings lists.
     */
    public PostingsList intersect(PostingsList l1, PostingsList l2) {
        if (l1 == null || l2 == null) {
            return null;
        }
        PostingsList answer = new PostingsList();
        int p1 = 0;
        int p2 = 0;
        while (p1 < l1.size() && p2 < l2.size()) {
            if (l1.get(p1).docID == l2.get(p2).docID) {
                PostingsEntry pe = new PostingsEntry(l1.get(p1).docID);
                answer.insert(pe);
                p1++; p2++;
            } else if (l1.get(p1).docID < l2.get(p2).docID) {
                p1++;
            } else {
                p2++;
            }
        }
        return answer;
    }


    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }
}
