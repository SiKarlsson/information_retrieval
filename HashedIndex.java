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
    private HashMap<String,PostingsList> bigramIndex = new HashMap<String,PostingsList>();
    private HashMap<String, String> docIDs = new HashMap<String,String>();
    private HashMap<String, Integer> articleTitles = new HashMap<String, Integer>();
    private HashMap<Integer, Double> pageRanks = new HashMap<Integer, Double>();
    private Queue<String> cache = new LinkedList<String>();
    private Queue<String> pathCache = new LinkedList<String>();
    private int numDocs = 0;
    private final static int IDF_THRESHOLD = 1;
    private final static boolean ELIMINATE_INDEX = false;


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

    public void insertBigram(String token, int docID, int offset) {
        PostingsList pl = bigramIndex.get(token);
        if (pl == null) {
            pl = new PostingsList();
        }
        PostingsEntry pe = new PostingsEntry(docID);
        pl.insert(pe, offset);
        bigramIndex.put(token, pl);
    }

    /**
     *  Returns all the words in the index.
     */
    public Iterator<String> getDictionary() {
	   return index.keySet().iterator();
    }

    /**
     *  Returns all the words in the bigram index.
     */
    public Iterator<String> getBigramDictionary() {
	   return bigramIndex.keySet().iterator();
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
            case RANKED_QUERY:
                if (structureType == UNIGRAM) {
                    return ranked(query, rankingType, true);
                } else if (structureType == BIGRAM) {
                    return ranked(createBigramQuery(query), rankingType, false);
                } else {
                    PostingsList answer;
                    if (query.terms.size() < 2) {
                        answer = ranked(query, rankingType, true);
                    } else {
                        answer = ranked(createBigramQuery(query), rankingType, false);
                    }
                    if (query.terms.size() >= 2 && answer.size() < 10) {
                        PostingsList answer2 = ranked(query, rankingType, true);
                        if (answer.size() > answer2.size()) {
                            answer = mergeAnswers(answer, answer2);
                        } else {
                            answer = mergeAnswers(answer2, answer);
                        }
                    }
                    return answer;
                }
            default:
                return null;
        }
    }

    /**
     *  Intersects the terms in a query
     */
    public PostingsList intersect(Query query) {
        if (query.terms.size() > 0) {
            System.out.println("NOW DOING SEARCH...");
            long startTime = System.nanoTime();
            String term = query.terms.getFirst();
            PostingsList intersection = getPostings(term);

            for (int i = 1; i < query.terms.size(); i++) {
                String nextTerm = query.terms.get(i);
                PostingsList pl = getPostings(nextTerm);
                if (eliminateIndex(pl)) {
                    continue;
                }
                intersection = intersect(intersection, pl);
                if (intersection == null) {
                    return null;
                }
            }
            long estimatedTime = System.nanoTime() - startTime;
            System.out.println("DONE WITH SEARCH after: " + estimatedTime/(double)1000000);
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

    public PostingsList ranked(Query query, int rankingType, boolean unigram) {
        System.out.println("NOW DOING SEARCH...");
        long startTime = System.nanoTime();
        HashMap<Integer, PostingsEntry> docs = new HashMap<Integer, PostingsEntry>();
        for (int i = 0; i < query.terms.size(); i++) {
            PostingsList pl;
            if (unigram) {
                pl = getPostings(query.terms.get(i));
            } else {
                pl = bigramIndex.get(query.terms.get(i));
            }
            if (ELIMINATE_INDEX && eliminateIndex(pl)) {
                continue;
            }
            if (pl != null) {
                for (int j = 0; j < pl.size(); j++) {
                    PostingsEntry pe = pl.get(j);
                    PostingsEntry res = docs.get(pl.get(j).docID);
                    if (res == null) {
                        res = new PostingsEntry(pe.docID);
                    }
                    res.score += pe.score*query.weights.get(query.terms.get(i));
                    docs.put(res.docID, res);
                }
            }
        }

        LinkedList<PostingsEntry> postingsList = new LinkedList<PostingsEntry>(docs.values());
        PostingsList answer = new PostingsList();
        answer.setPostingsList(postingsList);

        answer = lengthNormalize(answer, query.terms.size());

        if (rankingType == Index.PAGERANK) {
            for (int i = 0; i < answer.size(); i++) {
                PostingsEntry pe = answer.get(i);
                pe.score = pageRank(pe.docID);
            }
        }

        if (rankingType == Index.COMBINATION) {
            for (int i = 0; i < answer.size(); i++) {
                PostingsEntry pe = answer.get(i);
                pe.score = pageRankFunction(pe.score, pe.docID);
            }
        }

        answer.sort();

        long estimatedTime = System.nanoTime() - startTime;
        System.out.println("DONE WITH SEARCH after: " + estimatedTime/(double)1000000);
        return answer;
    }

    private PostingsList lengthNormalize(PostingsList answer, int queryLength) {
        for (int i = 0; i < answer.size(); i++) {
            PostingsEntry pe = answer.get(i);
            pe.score = pe.score/((Math.log(docLengths.get("" + pe.docID)) + 1) * queryLength);
            //pe.score = pe.score/(docLengths.get("" + pe.docID) * queryLength);
        }
        return answer;
    }

    public double pageRank(int docID) {
        String filePath;
        int docNumber;
        double pageRank;
        if (getFilePath("" + docID) == null) {
            return 0.0;
        } else {
            filePath = getFilePath("" + docID);
        }
        if (articleTitles.get(filePath) == null) {
            return 0.0;
        } else {
            docNumber = articleTitles.get(filePath);
        }
        if (pageRanks.get(docNumber) == null) {
            return 0.0;
        } else {
            pageRank = pageRanks.get(docNumber);
        }
        return pageRank;
    }

    public double pageRankFunction(double tfIdfScore, int docID) {
        double a = 1;
        double b = 0.75;
        return a*tfIdfScore + b*pageRank(docID);
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

    public void setArticleTitles(HashMap<String, Integer> map) {
        articleTitles = map;
    }

    public void setPageRanks(HashMap<Integer, Double> map) {
        pageRanks = map;
    }

    public void setNumDocs(int n) {
        numDocs = n;
    }

    private double tfIdf(int tf, int numDocs, int df) {
        double idf = Math.log((double)numDocs/(double)df);
        return tf*idf;
    }

    private boolean eliminateIndex(PostingsList pl) {
        return Math.log(numDocs/pl.size()) < IDF_THRESHOLD;
    }

    public void calculateScores() {
        Iterator it = getDictionary();
        while (it.hasNext()) {
            String term = (String)it.next();
            PostingsList pl = index.get(term);
            int documentFreq = pl.size();
            for (int i = 0; i < pl.size(); i++) {
                PostingsEntry pe = pl.get(i);
                pe.calculateScore(numDocs, documentFreq);
            }
        }
    }

    public void calculateBigramScores() {
        Iterator it = getBigramDictionary();
        while (it.hasNext()) {
            String term = (String)it.next();
            PostingsList pl = bigramIndex.get(term);
            int documentFreq = pl.size();
            for (int i = 0; i < pl.size(); i++) {
                PostingsEntry pe = pl.get(i);
                pe.calculateScore(1000, documentFreq);
            }
        }
    }

    private Query createBigramQuery(Query query) {
        Query q = new Query();
        String prevTerm = "";
        for (int i = 0; i < query.terms.size(); i++) {
            String term = query.terms.get(i);
            String newTerm = prevTerm + "," + term;
            q.terms.add(newTerm);
            q.weights.put(newTerm, new Double(1));
            prevTerm = term;
        }
        return q;
    }

    private PostingsList mergeAnswers(PostingsList answer1, PostingsList answer2) {
        HashMap<Integer, Double> answerMap1 = new HashMap<Integer, Double>();
        HashMap<Integer, Double> answerMap2 = new HashMap<Integer, Double>();
        for (int i = 0; i < answer1.size(); i++) {
            PostingsEntry pe = answer1.get(i);
            answerMap1.put(pe.docID, pe.score);
        }
        for (int i = 0; i < answer2.size(); i++) {
            PostingsEntry pe = answer2.get(i);
            answerMap2.put(pe.docID, pe.score);
        }
        HashMap<Integer, Double> answer = new HashMap<Integer, Double>();
        for (Integer key : answerMap1.keySet()) {
            Double score = answerMap2.get(key);
            if (score == null) {
                answer.put(key, answerMap1.get(key));
            } else {
                answer.put(key, answerMap1.get(key) + score);
                answerMap2.remove(key);
            }
        }
        for (Integer key : answerMap2.keySet()) {
            answer.put(key, answerMap2.get(key));
        }
        PostingsList pl = new PostingsList();
        for (Integer key : answer.keySet()) {
            PostingsEntry pe = new PostingsEntry(key);
            pe.score = answer.get(key);
            pl.insert(pe);
        }
        pl.sort();

        System.out.println(pl.size());

        return pl;
    }
}
