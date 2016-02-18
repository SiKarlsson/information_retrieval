/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 *   Additions: Hedvig Kjellström, 2012-14
 */  


package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;



/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {

    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();
    private HashMap<String, String> docIDs = new HashMap<String,String>();
    private Queue<String> cache = new LinkedList<String>();
    private Queue<String> pathCache = new LinkedList<String>();
    private int numDocs = 0;


    /**
     *  Inserts this token in the index.
     */
    public void insert( String token, int docID, int offset ) {
        PostingsList pl = index.get(token);
        if (pl == null) {
            pl = new PostingsList();
        }
        PostingsEntry pe = new PostingsEntry(docID);
        pl.insert(pe, offset);
        index.put(token, pl);
    }


    /**
     *  Returns all the words in the index.
     */
    public Iterator<String> getDictionary() {
	   return index.keySet().iterator();
    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        PostingsList pl = index.get(token);
        if (pl == null && !Constants.keepInMemory) {
            IndexReader ir = new IndexReader();
            pl = ir.readPostingsListFromFile(token);
            if (pl != null) {
                if (index.size() >= Constants.cacheMaxSize) {
                    index.remove(cache.poll());
                }
                index.put(token, pl);
                cache.add(token);
            }
        }
        return pl;
    }


    /**
     *  Searches the index for postings matching the query.
     */
    public PostingsList search( Query query, int queryType, int rankingType, int structureType ) {
        switch (queryType) {
            case INTERSECTION_QUERY:  return intersect(query);
            case PHRASE_QUERY:        return phrase(query);
            case RANKED_QUERY:        return ranked(query);
            default:                  return null;
        }
    }

    /**
     *  Intersects the terms in a query
     */
    public PostingsList intersect(Query query) {
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
     *  Performs a phrase query
     */
    public PostingsList phrase(Query query) {
        if (query.terms.size() > 0) {
            String term = query.terms.getFirst();
            PostingsList phrase = getPostings(term);

            for (int i = 1; i < query.terms.size(); i++) {
                String nextTerm = query.terms.get(i);
                phrase = phrase(phrase, getPostings(nextTerm));
                if (phrase == null) {
                    return null;
                }
            }
            return phrase;
        } else { 
            return null;
        }   
    }

    /**
     *  Performs a phrase query between two postingslist
     */
    public PostingsList phrase(PostingsList l1, PostingsList l2) {
        if (l1 == null || l2 == null) {
            return null;
        }
        PostingsList answer = new PostingsList();
        int p1 = 0;
        int p2 = 0;
        while (p1 < l1.size() && p2 < l2.size()) {
            if (l1.get(p1).docID == l2.get(p2).docID) {
                ArrayList<Integer> matchingOffsets = succeedingIndices(l1.get(p1).getOffsets(), l2.get(p2).getOffsets());
                if (matchingOffsets.size() > 0) {
                    PostingsEntry pe = new PostingsEntry(l1.get(p1).docID);
                    pe.setOffsets(matchingOffsets);
                    answer.insert(pe);
                }
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
     *  Returns true if the two lists have succeeding indices
     */
    public ArrayList<Integer> succeedingIndices(ArrayList<Integer> i1, ArrayList<Integer> i2) {
        ArrayList<Integer> offsets = new ArrayList<Integer>();
        int pp1 = 0;
        int pp2 = 0;
        while (pp1 < i1.size() && pp2 < i2.size()) {
            if (i2.get(pp2) - i1.get(pp1) == 1) {
                offsets.add(i2.get(pp2));
                pp1++; pp2++;
            } else if (i2.get(pp2) - i1.get(pp1) > 1) {
                pp1++;
            } else {
                pp2++;
            }
        }
        return offsets;
    }

    public PostingsList ranked(Query query) {
        HashMap<Integer, PostingsEntry> docs = new HashMap<Integer, PostingsEntry>();
        for (int i = 0; i < query.terms.size(); i++) {
            PostingsList pl = getPostings(query.terms.get(i));
            if (pl != null) { 
                double tfIdfQ = tfIdf(1, numDocs, pl.size());
                for (int j = 0; j < pl.size(); j++) {
                    PostingsEntry pe = pl.get(j);
                    PostingsEntry res = docs.get(pl.get(j).docID);
                    if (res == null) {
                        res = new PostingsEntry(pe.docID);
                    }
                    res.score += pe.score*tfIdfQ;
                    docs.put(res.docID, res);
                }
            }
        }

        LinkedList<PostingsEntry> postingsList = new LinkedList<PostingsEntry>(docs.values());
        PostingsList answer = new PostingsList();
        answer.setPostingsList(postingsList);

        for (int i = 0; i < answer.size(); i++) {
            PostingsEntry pe = answer.get(i);
            pe.score = pe.score/(docLengths.get("" + pe.docID) * query.terms.size());
        }

        answer.sort();

        return answer;
    }

    /**
     *  Returns the number postings list in the index
     */
    public int size() {
        return index.size();
    }

    /**
     *  Moves the entire index to disk and clears it from working memory
     */
    public void transferIndexToDisk(int blockID) {
        IndexWriter iw = new IndexWriter();
        iw.writeIndexToDisk(index, Integer.toString(blockID), docIDs);
        cleanup();
    }

    /**
     *  Clears the index
     */
    public void cleanup() {
        index.clear();
        docIDs.clear();
    }

    public String getFilePath(String id) {
        String path = docIDs.get(id);
        if (path == null) {
            IndexReader ir = new IndexReader();
            path = ir.readFilePath(id);
            if (path != null) {
                if (docIDs.size() >= Constants.pathCacheMaxSize) {
                    docIDs.remove(pathCache.poll());
                }
                docIDs.put(id, path);
                pathCache.add(id);
            }
        }
        return path;
    }

    public void addFilePath(String key, String value) {
        docIDs.put(key, value);
    }

    public void setFilePaths(HashMap<String, String> map) {
        for (String key : map.keySet()) {
            pathCache.add(key);
        }
        docIDs = map;
    }

    public void setNumDocs(int n) {
        numDocs = n;
    }

    private double tfIdf(int tf, int numDocs, int df) {
        double idf = Math.log((double)numDocs/(double)df);
        return tf*idf;
    }

    public void calculateScores() {
        Iterator it = getDictionary();
        while (it.hasNext()) {
            String term = (String)it.next();
            PostingsList pl = index.get(term);
            int documentFreq = pl.size();
            for (int i = 0; i < pl.size(); i++) {
                PostingsEntry pe = pl.get(i);
                int len = docLengths.get("" + pe.docID);
                pe.calculateScore(numDocs, documentFreq);
            }
        }
    }
}
