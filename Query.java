/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   First version:  Hedvig Kjellström, 2012
 */

package ir;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

public class Query {

    public LinkedList<String> terms = new LinkedList<String>();
    public HashMap<String, Double> weights = new HashMap<String, Double>();

    /**
     *  Creates a new empty Query
     */
    public Query() {
    }

    /**
     *  Creates a new Query from a string of words
     */
    public Query( String queryString  ) {
    	StringTokenizer tok = new StringTokenizer( queryString );
    	while ( tok.hasMoreTokens() ) {
            String term = tok.nextToken();
            terms.add(term);
    	    weights.put(term, new Double(1));
    	}
    }

    /**
     *  Returns the number of terms
     */
    public int size() {
	return terms.size();
    }

    /**
     *  Returns a shallow copy of the Query
     */
    public Query copy() {
	Query queryCopy = new Query();
	queryCopy.terms = (LinkedList<String>) terms.clone();
	queryCopy.weights = (HashMap<String, Double>) weights.clone();
	return queryCopy;
    }

    /**
     *  Expands the Query using Relevance Feedback
     */
    public void relevanceFeedback( PostingsList results, boolean[] docIsRelevant, Indexer indexer ) {
    	// results contain the ranked list from the current search
    	// docIsRelevant contains the users feedback on which of the 10 first hits are relevant
        double alpha = 0.9;
        double beta = 0.1;
        /* Add all relevant documents to docR */
        ArrayList<PostingsEntry> docR = new ArrayList<PostingsEntry>();
        for (int i = 0; i < docIsRelevant.length; i++) {
            if (docIsRelevant[i]) {
                docR.add(results.get(i));
            }
        }
        /* Normalize beta with number of relevant documents */
        beta = beta / docR.size();
        /* Multiply each old query term with alpha and store in new query */
        HashMap<String, Double> newQuery = new HashMap<String, Double>();
        for (int i = 0; i < terms.size(); i++) {
            String term = terms.get(i);
            double weight = weights.get(term)*alpha/terms.size();
            if (newQuery.get(term) == null) {
                newQuery.put(term, weight);
            } else {
                newQuery.put(term, newQuery.get(term) + weight);
            }
        }
        /* Go through every relevant document */
        for (int i = 0; i < docR.size(); i++) {
            int docID = docR.get(i).docID;
            System.out.println("Reading doc " + docID + "....");
            String docName = indexer.index.getFilePath(docID + "");
            /* Get all terms from current relevant document */
            HashSet<String> terms = getTermsFromDoc(docName);
            System.out.println("Done reading doc " + docID);
            System.out.println("Going through terms for doc " + docID + "...");
            /* Iterate through every term */
            Iterator<String> it = terms.iterator();
            while (it.hasNext()) {
                double score = 0.0;
                String term = it.next();
                PostingsList pl = indexer.index.getPostings(term);
                /* Find the term score for current document */
                for (int j = 0; j < pl.size(); j++) {
                    if (pl.get(j).docID == docID) {
                        score = pl.get(j).score/indexer.index.docLengths.get(docID + "");
                        break;
                    }
                }
                /* Add term of revelant doc to new query and weigh with beta */
                if (newQuery.get(term) == null) {
                    newQuery.put(term, score*beta);
                } else {
                    newQuery.put(term, newQuery.get(term) + score*beta);
                }
            }
            System.out.println("done!");
        }
        /* Create new query from the query HashMap */
        terms = new LinkedList<String>();
        weights = new HashMap<String, Double>();
        for (String key : newQuery.keySet()) {
            terms.add(key);
            weights.put(key, newQuery.get(key));
        }
    }

    public HashSet<String> getTermsFromDoc(String docName) {
        HashSet<String> termsInFile = new HashSet<String>();
        Indexer indexer = new Indexer();
        File f = new File(docName);
        if ( f.canRead() ) {
            try {
                //  Read the first few bytes of the file to see if it is
                // likely to be a PDF
                Reader reader = new FileReader( f );
                char[] buf = new char[4];
                reader.read( buf, 0, 4 );
                if ( buf[0] == '%' && buf[1]=='P' && buf[2]=='D' && buf[3]=='F' ) {
                    // We assume this is a PDF file
                    try {
                        String contents = indexer.extractPDFContents( f );
                        reader = new StringReader( contents );
                    } catch ( IOException e ) {
                        // Perhaps it wasn't a PDF file after all
                        reader = new FileReader( f );
                    }
                } else {
                    // We hope this is ordinary text
                    reader = new FileReader( f );
                }
                SimpleTokenizer tok = new SimpleTokenizer( reader );
                while ( tok.hasMoreTokens() ) {
                    String token = tok.nextToken();
                    termsInFile.add(token);
                }
                reader.close();
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
        }
        return termsInFile;
    }
}
