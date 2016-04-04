/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */


package ir;

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.*;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.pdmodel.PDDocument;


/**
 *   Processes a directory structure and indexes all PDF and text files.
 */
public class Indexer {

    /** The index to be built up by this indexer. */
    public Index index;

    /** The next docID to be generated. */
    private int lastDocID = 0;

    /** The current block identifier. */
    private int lastBlockID = 0;

    /** The limit to store in the index before writing to file. */
    private int memoryLimit = 70371;

    /** The maximum number of bigrams to store in memory. */
    private int numBigrams = 1700;
    private int numDocsApprox = 17000;
    private int bigramCount = 0;


    /* ----------------------------------------------- */


    /** Generates a new document identifier as an integer. */
    private int generateDocID() {
	return lastDocID++;
    }

    /** Generates a new document identifier based on the file name. */
    private int generateDocID( String s ) {
	return s.hashCode();
    }


    /* ----------------------------------------------- */


    /**
     *  Initializes the index as a HashedIndex.
     */
    public Indexer() {
	index = new HashedIndex();
    }


    /* ----------------------------------------------- */


    /**
     *  Tokenizes and indexes the file @code{f}. If @code{f} is a directory,
     *  all its files and subdirectories are recursively processed.
     */
    public void processFiles( File f ) {
    System.out.println(f.getAbsolutePath());
    // do not try to index fs that cannot be read
    if ( f.canRead() ) {
	    if ( f.isDirectory() ) {
		String[] fs = f.list();
		// an IO error could occur
		if ( fs != null ) {
		    for ( int i=0; i<fs.length; i++ ) {
			processFiles( new File( f, fs[i] ));
		    }
		}
	    } else {
		//System.err.println( "Indexing " + f.getPath() );
		// First register the document and get a docID
		int docID = generateDocID();
		index.addFilePath( "" + docID, f.getPath() );
		try {
		    //  Read the first few bytes of the file to see if it is
		    // likely to be a PDF
		    Reader reader = new FileReader( f );
		    char[] buf = new char[4];
		    reader.read( buf, 0, 4 );
		    if ( buf[0] == '%' && buf[1]=='P' && buf[2]=='D' && buf[3]=='F' ) {
			// We assume this is a PDF file
			try {
			    String contents = extractPDFContents( f );
			    reader = new StringReader( contents );
			}
			catch ( IOException e ) {
			    // Perhaps it wasn't a PDF file after all
			    reader = new FileReader( f );
			}
		    }
		    else {
			// We hope this is ordinary text
			reader = new FileReader( f );
		    }
		    SimpleTokenizer tok = new SimpleTokenizer( reader );
		    int offset = 0;
            String prevToken = "";
            boolean indexDoc = false;
            Random r = new Random();
            if (r.nextDouble() <= (double)numBigrams/(double)numDocsApprox) {
                indexDoc = true;
                bigramCount++;
            }
		    while ( tok.hasMoreTokens() ) {
			String token = tok.nextToken();
			insertIntoIndex( docID, token, offset );
            if (indexDoc) {
                insertIntoBigramIndex(docID, prevToken + "," + token, offset);
                prevToken = token;
            }
            offset++;
		    }
		    index.docLengths.put( "" + docID, offset );
		    reader.close();
		}
		catch ( IOException e ) {
		    e.printStackTrace();
		}
	    }
	}
    }


    /* ----------------------------------------------- */


    /**
     *  Extracts the textual contents from a PDF file as one long string.
     */
    public String extractPDFContents( File f ) throws IOException {
	FileInputStream fi = new FileInputStream( f );
	PDFParser parser = new PDFParser( fi );
	parser.parse();
	fi.close();
	COSDocument cd = parser.getDocument();
	PDFTextStripper stripper = new PDFTextStripper();
	String result = stripper.getText( new PDDocument( cd ));
	cd.close();
	return result;
    }


    /* ----------------------------------------------- */


    /**
     *  Indexes one token.
     */
    public void insertIntoIndex( int docID, String token, int offset ) {
		if (index.size() > memoryLimit && !Constants.keepInMemory) {
			transferIndexToDisk();
		}
		index.insert( token, docID, offset );
    }

    /**
     *  Indexes one bigram.
     */
    public void insertIntoBigramIndex( int docID, String token, int offset) {
		index.insertBigram(token, docID, offset);
    }

    /**
     *  Transfers the current index in working memory to disk.
     */
    public void transferIndexToDisk() {
    	index.transferIndexToDisk(lastBlockID++);
    }

    /**
     *  Merges all separate index files built up in the indexing into one.
     */
    public void mergeIndexFiles() {
    	IndexWriter iw = new IndexWriter();
    	iw.mergeIndexFiles(lastBlockID);
    }

    public boolean needIndexing() {
    	File index = new File(Constants.indexFileName());
    	File invertedIndex = new File(Constants.postingsFileName());
		if (index.exists() && !index.isDirectory() && invertedIndex.exists() && !invertedIndex.isDirectory()) {
    		return false;
		} else {
			return true;
		}
    }

    public void finalPreparations() {
    	IndexReader ir = new IndexReader();
        if (!Constants.keepInMemory) {
    		HashMap<String, String> map = ir.prepareFilePaths();
    		if (map != null) {
    			index.setFilePaths(map);
    			index.setNumDocs(map.size());
    		}
    	} else {
    		index.setNumDocs(lastDocID);
    	}
        index.setArticleTitles(ir.readArticleTitles());
        index.setPageRanks(ir.readPageRanks());
        index.setNumBigramDocs(bigramCount);
    }

    public void calculateScores() {
    	index.calculateScores();
        index.calculateBigramScores();
    }
}
