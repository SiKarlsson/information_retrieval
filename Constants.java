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
    /** The name of the temporary files used when indexing. */
    private  static final String tempFileName = "t";
    /** The name of the merge files used when merging the temporary files. */
    private static final String mergeFileName = "m";
    /** The name of the main inverted index on file. */
    private static final String postingsFileName = "p";
    /** The name of the index indexing the inverted index. */
    private static final String indexFileName = "i";
    /** Standard extension of text files **/
    private static final String textFileExtension = "txt";

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
}