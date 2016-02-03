/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Simon Karlsson, 2016
 */  


package ir;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 *   Writes an inverted index to file
 */
public class IndexWriter {

    /** The directory to write the index to. */
    private static final String writeDir = "/Users/simon/Documents/skola/ir16/lab/index/";
    /** The name of the temporary files used when indexing. */
    private static final String tempFileName = "tmp_";
    /** The name of the merge files used when merging the temporary files. */
    private static final String mergeFileName = "mrg_";
    /** The name of the main inverted index on file. */
    private static final String postingsFileName = "pl";
    /** The name of the index indexing the inverted index. */
    private static final String indexFileName = "pl_index";

    /**
     *  Writes the index to given file
     */
    public void writeIndexToDisk(HashMap<String,PostingsList> index, String fileName) {
        writePostingsList(sortIndex(index), index, fileName);
    }

    /**
     *  Merges all the temporary index files into one.
     */
    public void mergeIndexFiles(int n) {
        for (int i = 1; i < n; i++) {
            mergeFile(i);
        }
        cleanupFiles(n);
        createIndex();
    }

    /**
     *  Returns a sorted ArrayList of the terms in the index
     */
    private ArrayList<String> sortIndex(HashMap<String,PostingsList> index) {
        ArrayList terms = new ArrayList(index.keySet());
        Collections.sort(terms);
        return terms;
    }

    /**
     *  Write a postings list to file
     */
    private void writePostingsList(ArrayList<String> terms, HashMap<String,PostingsList> index, String fileName) {
        try {
            String filename = writeDir + tempFileName + fileName +".txt";
            FileWriter fw = new FileWriter(filename, true);
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

    /**
     *  Merge the given file to the already merged files.
     */
    private void mergeFile(int file) {
        String baseFilename;
        if (file == 1) {
            baseFilename = writeDir + tempFileName + "0.txt";
        } else {
            baseFilename = writeDir + mergeFileName + (file - 1) + ".txt";
        }
        String filename = writeDir + tempFileName + file + ".txt";
        String targetFilename = writeDir + mergeFileName + (file) + ".txt";
        try {
            FileWriter fw = new FileWriter(targetFilename);
            BufferedReader br1 = new BufferedReader(new FileReader(baseFilename));
            BufferedReader br2 = new BufferedReader(new FileReader(filename));
            String[] line1 = readTerm(br1.readLine());
            String[] line2 = readTerm(br2.readLine());
            while (line1 != null || line2 != null) {
                if (line1 == null) {
                    writePostingsList(fw, line2);
                    line2 = readTerm(br2.readLine());
                } else if (line2 == null) {
                    writePostingsList(fw, line1);
                    line1 = readTerm(br1.readLine());
                } else if (line1[0].equals(line2[0])) {
                    writePostingsList(fw, concatPostingsLists(line1, line2));
                    line1 = readTerm(br1.readLine());
                    line2 = readTerm(br2.readLine());
                } else if (line1[0].compareTo(line2[0]) < 0) {
                    writePostingsList(fw, line1);
                    line1 = readTerm(br1.readLine());
                } else {
                    writePostingsList(fw, line2);
                    line2 = readTerm(br2.readLine());
                }
            }
            fw.close();
            br1.close();
            br2.close();
        } catch (IOException ioe) {
            System.out.println("Exception thrown  :" + ioe);
        }
    }

    /**
     *  Extracts the term from the postings list line.
     */
    private String[] readTerm(String line) {
        if (line == null) {
            return null;
        } else {
            return line.split(" ");
        }
    }

    /**
     *  Writes a postings list to given file writer.
     */
    private void writePostingsList(FileWriter fw, String[] postingsList) throws IOException {
        fw.write(postingsList[0]);
        for (int i = 1; i < postingsList.length; i++) {
            fw.write(" " + postingsList[i]);
        }
        fw.write('\n');
    }

    /**
     * Concatenates two postings list with the same terms.
     */
    private String[] concatPostingsLists(String[] a, String[] b) {
        int aLen = a.length;
        int bLen = b.length;
        // Do not include the term for second array
        String[] c = new String[aLen+bLen-1];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 1, c, aLen, bLen - 1);
        return c;
    }

    /**
     * Deletes all temporary files and merge files used when indexing.
     */
    private void cleanupFiles(int numFiles) {
        try {
            for (int i = 0; i < numFiles; i++) {
                File file1 = new File(writeDir + tempFileName + i + ".txt");
                file1.delete();
                if (i != numFiles - 1) {
                    File file2 = new File(writeDir + mergeFileName + (i) + ".txt");
                    file2.delete();
                } else {
                    File oldfile = new File(writeDir + mergeFileName + (i) + ".txt");
                    File newfile = new File(writeDir + postingsFileName + ".txt");
                    oldfile.renameTo(newfile);
                }
            }
        } catch(Exception e){  
            e.printStackTrace();
        }
    }

    /**
     * Indexing the big index based on offset.
     */
    private void createIndex() {
        try {
            String line;
            RandomAccessFile raf = new RandomAccessFile(writeDir + postingsFileName + ".txt", "r" );
            FileWriter fw = new FileWriter(writeDir + indexFileName + ".txt");
            long offset = raf.getFilePointer();
            line = raf.readLine();
            line = new String(line.getBytes("ISO-8859-1"), "UTF-8");
            while (line != null) {
                fw.write(line.split(" ")[0] + " " + offset + "\n");
                offset = raf.getFilePointer();
                line = raf.readLine();
                if (line != null) {
                    line = new String(line.getBytes("ISO-8859-1"), "UTF-8");
                }
            }
            raf.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
