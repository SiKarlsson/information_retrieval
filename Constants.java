/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version: Simon Karlsson, 2016
 */  

package ir;

public class Constants {
    /** The directory to write the index to. */
    private  static final String writeDir = "/Users/simon/Documents/skola/ir16/lab/index/";
    /** The directory to read the page rank from. */
    public  static final String pageRankDir = "/Users/simon/Documents/skola/ir16/lab/ir/pagerank/page_rank.txt";
    /** The with translations from pageRand document id to file name. */
    public  static final String articleTitlesDir = "/Users/simon/Documents/skola/ir16/lab/ir/pagerank/articleTitles.txt";
    /** Default corpus. */
    public  static final String defaultCorpus = "/Users/simon/Documents/skola/ir16/lab/davisWiki/";
    /** The name of the temporary files used when indexing. */
    private  static final String tempFileName = "t";
    /** The name of the merge files used when merging the temporary files. */
    private static final String mergeFileName = "m";
    /** The name of the temporary files used for the docIDs. */
    private  static final String idFileName = "id";
    /** The name of the main inverted index on file. */
    private static final String postingsFileName = "p";
    /** The name of the index indexing the inverted index. */
    private static final String indexFileName = "i";
    /** Standard extension of text files **/
    private static final String textFileExtension = "txt";
    /** Standard extension of document in the corpus **/
    public static final String standardCorpusFileExtension = ".f";
    /** The maximum line size in the small index (Kb) **/
    public static final int maxLineSize = 65 * 1024;
    /** The lates docID-path pair written to file **/
    public static int nextID = 0;
    /** The cache size of the postings list cache **/
    public static final int cacheMaxSize = 10000000;
    /** The max size of the path cache **/
    public static final int pathCacheMaxSize = 100000;
    /** Keep in memory **/
    public static boolean keepInMemory = true;

    public static final String postingsFileName() {
        return writeDir + postingsFileName + "." + textFileExtension;
    }

    public static final String indexFileName() {
        return writeDir + indexFileName + "." + textFileExtension;
    }

    public static final String tempFileName(String fileID) {
        return writeDir + tempFileName + "_" + fileID + "." + textFileExtension;
    }

    public static final String mergeFileName(String fileID) {
        return writeDir + mergeFileName + "_" + fileID + "." + textFileExtension;
    }

    public static final String idFileName() {
        return writeDir + idFileName + "." + textFileExtension;
    }
}