/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Simon Karlsson, 2016
 */  


package ir;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 *   Writes an inverted index to file
 */
public class IndexWriter {

    /** The directory to write the index to. */
    private static final String writeDir = "/Users/simon/Documents/skola/ir16/lab/index";

    /**
     * Writes the index to given file
     */
    public void writeIndexToDisk(HashMap<String,PostingsList> index, String fileName) {
        writePostingsList(sortIndex(index), index, fileName);
    }

    /**
     * Returns a sorted ArrayList of the terms in the index
     */
    private ArrayList<String> sortIndex(HashMap<String,PostingsList> index) {
        ArrayList terms = new ArrayList(index.keySet());
        Collections.sort(terms);
        return terms;
    }

    /**
     * Write a postings list to file
     */
    private void writePostingsList(ArrayList<String> terms, HashMap<String,PostingsList> index, String fileName) {
        try {
            String filename = writeDir + "/postingslist_" +  fileName +".txt";
            FileWriter fw = new FileWriter(filename, true);
            //for (String term : terms) {
            for (int t = 0; t < terms.size(); t++) {
                String term = terms.get(t);
                fw.write(term);
                PostingsList pl = index.get(term);
                for (int i = 0; i < pl.size(); i++) {
                    PostingsEntry pe = pl.get(i);
                    fw.write(" " + pe.docID + ":");
                    ArrayList offsets = pe.getOffsets();
                    for (int j = 0; j < offsets.size(); j++) {
                        fw.write(offsets.get(j).toString());
                        if (j != offsets.size() - 1) {
                            fw.write(',');
                        }
                    }
                }
                if (t != terms.size() - 1) {
                    fw.write('\n');
                }
            }
            fw.close();
        } catch(IOException ioe) {
            System.err.println("IOException: " + ioe.getMessage());
        }
    }
}
