/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2012
 *   Completed:      Simon Karlsson, 2016      
 */  

import java.util.*;
import java.io.*;

public class PageRank{

    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     *   Mapping from document names to document numbers.
     */
    Hashtable<String,Integer> docNumber = new Hashtable<String,Integer>();

    /**
     *   Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**  
     *   A memory-efficient representation of the transition matrix.
     *   The outlinks are represented as a Hashtable, whose keys are 
     *   the numbers of the documents linked from.<p>
     *
     *   The value corresponding to key i is a Hashtable whose keys are 
     *   all the numbers of documents j that i links to.<p>
     *
     *   If there are no outlinks from i, then the value corresponding 
     *   key i is null.
     */
    Hashtable<Integer,Hashtable<Integer,Boolean>> link = new Hashtable<Integer,Hashtable<Integer,Boolean>>();

    /**
     *   The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     *   The number of documents with no outlinks.
     */
    int numberOfSinks = 0;

    /**
     *   The probability that the surfer will be bored, stop
     *   following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     *   The probability that the surfer will NOT be bored.
     */
    final static double NOT_BORED = 1 - BORED;

    /**
     *   Convergence criterion: Transition probabilities do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;

    /**
     *   Never do more than this number of iterations regardless
     *   of whether the transistion probabilities converge or not.
     */
    final static int MAX_NUMBER_OF_ITERATIONS = 1000;

    final static int N = MAX_NUMBER_OF_DOCS;

    final static int M = 100;

    int numberOfDocs;

    /**
     * 's' for standar
     * 'mc1' for monte carlo 1
     * 'mc2' for monte carlo 2
     */
    static String computingMethod;

    public PageRank(String filename, String computingMethod) {
        this.computingMethod = computingMethod;
        numberOfDocs = readDocs(filename);
        double[] x;
        switch (computingMethod) {
            case "s":   x = computePagerank();
                        break;
            case "mc1": x = mc1(N);
                        break;
            case "mc2": x = mc2(M);
                        break;
            case "mc3": x = mc3(M);
                        break;
            case "mc4": x = mc4(M);
                        break;
            case "mc5": x = mc5(N);
                        break;
            default:    x = computePagerank();
                        break;
        }
        printScoreToFile(x);
    }

    public PageRank(String filename) {
        numberOfDocs = readDocs(filename);
    }

    /**
     *   Reads the documents and creates the docs table. When this method 
     *   finishes executing then the @code{out} vector of outlinks is 
     *   initialised for each doc, and the @code{p} matrix is filled with
     *   zeroes (that indicate direct links) and NO_LINK (if there is no
     *   direct link. <p>
     *
     *   @return the number of documents read.
     */
    int readDocs( String filename ) {
    int fileIndex = 0;
    try {
        System.err.print( "Reading file... " );
        BufferedReader in = new BufferedReader( new FileReader( filename ));
        String line;
        while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
        int index = line.indexOf( ";" );
        String title = line.substring( 0, index );
        Integer fromdoc = docNumber.get( title );
        //  Have we seen this document before?
        if ( fromdoc == null ) {    
            // This is a previously unseen doc, so add it to the table.
            fromdoc = fileIndex++;
            docNumber.put( title, fromdoc );
            docName[fromdoc] = title;
        }
        // Check all outlinks.
        StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
        while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
            String otherTitle = tok.nextToken();
            Integer otherDoc = docNumber.get( otherTitle );
            if ( otherDoc == null ) {
            // This is a previousy unseen doc, so add it to the table.
            otherDoc = fileIndex++;
            docNumber.put( otherTitle, otherDoc );
            docName[otherDoc] = otherTitle;
            }
            // Set the probability to 0 for now, to indicate that there is
            // a link from fromdoc to otherDoc.
            if ( link.get(fromdoc) == null ) {
            link.put(fromdoc, new Hashtable<Integer,Boolean>());
            }
            if ( link.get(fromdoc).get(otherDoc) == null ) {
            link.get(fromdoc).put( otherDoc, true );
            out[fromdoc]++;
            }
        }
        }
        if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
        System.err.print( "stopped reading since documents table is full. " );
        }
        else {
        System.err.print( "done. " );
        }
        // Compute the number of sinks.
        for ( int i=0; i<fileIndex; i++ ) {
        if ( out[i] == 0 )
            numberOfSinks++;
        }
    }
    catch ( FileNotFoundException e ) {
        System.err.println( "File " + filename + " not found!" );
    }
    catch ( IOException e ) {
        System.err.println( "Error reading file " + filename );
    }
    System.err.println( "Read " + fileIndex + " number of documents" );
    return fileIndex;
    }

    /*
    *   Computes the pagerank of each document.
    */
    double[] computePagerank() {
        System.err.print("Computing PageRank... Running first iteration...");
        double[] x_prime = initProbDist();
        double[] x = new double[numberOfDocs];
        int iter = 0;
        while (diff(x, x_prime) > EPSILON && iter < MAX_NUMBER_OF_ITERATIONS) {
            x = x_prime.clone();
            for (int i = 0; i < x_prime.length; i++) {
                x_prime[i] = 0;
                for (int j = 0; j < x.length; j++) {
                    x_prime[i] += x[j] * prob(i, j);
                }
            }
            iter++;
            System.err.print(" Done with iteration " + iter + "...");
        }
        System.err.print( "done. " );
        return x_prime;
    }

    private double prob(int i, int j) {
        if (out[j] == 0) {
            return 1.0/numberOfDocs;
        } else if (link.get(j).get(i) == null) {
            return BORED/numberOfDocs;
        } else {
            return NOT_BORED/out[j] + BORED/numberOfDocs; 
        }
    }

    private double diff(double x1[], double x2[]) {
        double diff = 0;
        for (int i = 0; i < x1.length; i++) {
            diff += Math.abs(x1[i] - x2[i]);
        }
        return diff;
    }

    private double[] initProbDist() {
        double[] x = new double[numberOfDocs];
        for (int i = 0; i < x.length; i++) {
            x[i] = 1.0/numberOfDocs;
        }
        return x;
    }

    public double[] mc1(int N) {
        Random r = new Random();
        double[] x = new double[numberOfDocs];
        for (int i = 0; i < N; i++) {
            x[simulatedRandomWalk(r.nextInt(numberOfDocs))]++;
        }
        for (int i = 0; i < numberOfDocs; i++) {
            x[i] = x[i]/N;
        }
        return x;
    }

    public double[] mc2(int M) {
        double[] x = new double[numberOfDocs];
        for (int i = 0; i < numberOfDocs; i++) {
            for (int j = 0; j < M; j++) {
                x[simulatedRandomWalk(i)]++;
            }
        }
        for (int i = 0; i < numberOfDocs; i++) {
            x[i] = x[i]/(numberOfDocs*M);
        }
        return x;
    }

    public double[] mc3(int M) {
        Random r = new Random();
        double[] x = new double[numberOfDocs];
        int visited = 0;
        for (int i = 0; i < numberOfDocs; i++) {
            for (int j = 0; j < M; j++) {
                int currPage = i;
                Boolean walking = true;
                while (walking) {
                    visited++;
                    x[currPage]++;
                    if (r.nextDouble() <= BORED) {
                        walking = false;
                    } else if (out[currPage] > 0) {
                        currPage = randomOutlink(currPage);
                    } else {
                        currPage = r.nextInt(numberOfDocs);
                    }
                }
            }
        }
        for (int i = 0; i < numberOfDocs; i++) {
            x[i] = (x[i]*BORED)/(numberOfDocs*M);
        }
        return x;
    }

    public double[] mc4(int M) {
        Random r = new Random();
        double[] x = new double[numberOfDocs];
        int visited = 0;
        for (int i = 0; i < numberOfDocs; i++) {
            for (int j = 0; j < M; j++) {
                int currPage = i;
                Boolean walking = true;
                while (walking) {
                    visited++;
                    x[currPage]++;
                    if (r.nextDouble() <= BORED) {
                        walking = false;
                    } else if (out[currPage] > 0) {
                        currPage = randomOutlink(currPage);
                    } else {
                        walking = false;
                    }
                }
            }
        }
        for (int i = 0; i < numberOfDocs; i++) {
            x[i] = x[i]/visited;
        }
        return x;
    }

    public double[] mc5(int N) {
        Random r = new Random();
        double[] x = new double[numberOfDocs];
        int visited = 0;
        for (int i = 0; i < N; i++) {
            int currPage = r.nextInt(numberOfDocs);
            Boolean walking = true;
            while (walking) {
                visited++;
                x[currPage]++;
                if (r.nextDouble() <= BORED) {
                    walking = false;
                } else if (out[currPage] > 0) {
                    currPage = randomOutlink(currPage);
                } else {
                    walking = false;
                }
            }
        }
        for (int i = 0; i < numberOfDocs; i++) {
            x[i] = x[i]/visited;
        }
        return x;
    }

    private int simulatedRandomWalk(int startPage) {
        Random r = new Random();
        int currPage = startPage;
        while (r.nextDouble() > BORED) {
            if (out[currPage] > 0) {
                currPage = randomOutlink(currPage);
            } else {
                currPage = r.nextInt(numberOfDocs);
            }
        }
        return currPage;
    }

    private int randomOutlink(int page) {
        Hashtable outlinks = link.get(page);
        Object[] keys = outlinks.keySet().toArray();
        Object key = keys[new Random().nextInt(keys.length)];    
        return (Integer)key;
    }

    public void printScoreToFile(double[] x) {
        ArrayList<Document> docs = docsToList(x);
        Collections.sort(docs);
        try {
            PrintWriter writer = new PrintWriter("page_rank_" + computingMethod + ".txt", "UTF-8");
            for (int i = 0; i < 60; i++) {
                writer.println("" + (i+1) + ": " + docs.get(i).getDocNumber() + " " + docs.get(i).getRankScore());
            }
            writer.close();
        } catch (IOException ioe) {
            ioe.getMessage();
        }
    }

    public ArrayList<Document> docsToList(double[] x) {
        ArrayList<Document> docs = new ArrayList<Document>();
        for (int i = 0; i < x.length; i++) {
            docs.add(new Document(docName[i], x[i]));
        }
        return docs;
    }

    public static void main( String[] args ) {
        if ( args.length != 2 ) {
            System.err.println( "Please give the name of the link file and method of computing pagerank" );
        } else {
            new PageRank(args[0], args[1]);
        }
    }
}
