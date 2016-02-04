/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Simon Karlsson, 2016
 */


package ir;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 *   Reads the index on file
 */
public class IndexReader {
    
    public PostingsList readPostingsListFromFile(String term) {
        try {
            RandomAccessFile raf = new RandomAccessFile(Constants.postingsFileName(), "r" );
            String postingsListString = null;
            long offset = postingsListOffset(term);
            if (offset != -1) {
                raf.seek(offset);
                postingsListString = raf.readLine();
            }
            raf.close();
            return createPostingsList(postingsListString);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private long postingsListOffset(String term) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(Constants.indexFileName()));
            String line;
            while ((line = br.readLine()) != null) {
                String[] s = line.split(" ");
                if (term.equals(s[0])) {
                    return Long.parseLong(s[1]);
                }
            }
            br.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return -1;
    }

    private PostingsList createPostingsList(String postingsListString) {
        if (postingsListString == null) {
            return null;
        }
        PostingsList pl = new PostingsList();
        String[] s = postingsListString.split(" ");
        for (int i = 1; i < s.length; i++) {
            String[] e = s[i].split(",");
            PostingsEntry pe = new PostingsEntry(Integer.parseInt(e[0]));
            for (int j = 1; j < e.length; j++) {
                pe.addOffset(Integer.parseInt(e[j]));
            }
            pl.insert(pe);
        }
        return pl;
    }

}
