/*  
 *   Evaluate different Monte-Carlo methods of computing PageRank
 * 
 *   First version:  Simon Karlsson, 2016   
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

public class MonteCarloEvaluation {

    PageRank pr;
    
    ArrayList<Document> pageRank;

    int numDataPoints = 15;
    int avg = 10;

    double[] mc1Score = new double[numDataPoints];
    double[] mc2Score = new double[numDataPoints];
    double[] mc3Score = new double[numDataPoints];
    double[] mc4Score = new double[numDataPoints];
    double[] mc5Score = new double[numDataPoints];

    int[] N = new int[numDataPoints];

    public MonteCarloEvaluation(String filename) {
        pr = new PageRank(filename);
        pageRank = pr.docsToList(pr.computePagerank());
        Collections.sort(pageRank);
        evaluate();
        printResultsToFile();
    }

    public void evaluate() {
        for (int i = 1; i <= numDataPoints; i++) {
            System.out.println("EVALUATING WITH ITER: " + i);
            N[i-1] = i*pr.numberOfDocs;
            for (int j = 0; j < avg; j++) {
                double[] mc1 = pr.mc1(i*pr.numberOfDocs);
                double[] mc2 = pr.mc2(i);
                double[] mc3 = pr.mc3(i);
                double[] mc4 = pr.mc4(i);
                double[] mc5 = pr.mc5(i*pr.numberOfDocs);
                mc1Score[i-1] += squaredDiffTop(mc1);
                mc2Score[i-1] += squaredDiffTop(mc2);
                mc3Score[i-1] += squaredDiffTop(mc3);
                mc4Score[i-1] += squaredDiffTop(mc4);
                mc5Score[i-1] += squaredDiffTop(mc5);
            }
            mc1Score[i-1] = mc1Score[i-1] / avg;
            mc2Score[i-1] = mc2Score[i-1] / avg;
            mc3Score[i-1] = mc3Score[i-1] / avg;
            mc4Score[i-1] = mc4Score[i-1] / avg;
            mc5Score[i-1] = mc5Score[i-1] / avg;
        }
    }

    public double squaredDiffTop(double[] x) {
        double diff = 0;
        boolean top = false;
        if (top) {
            for (int i = 0; i < 50; i++) {
                double realVal = pageRank.get(i).getRankScore();
                int realIndex = pr.docNumber.get(pageRank.get(i).getDocNumber());    
                diff += Math.pow((realVal-x[realIndex]), 2);
            }
        } else {
            for (int i = (pageRank.size() - 1); i >= pageRank.size() - 50; i--) {
                double realVal = pageRank.get(i).getRankScore();
                int realIndex = pr.docNumber.get(pageRank.get(i).getDocNumber());    
                diff += Math.pow((realVal-x[realIndex]), 2);   
            }
        }
        return diff;
    }

    public void printResultsToFile() {
        try {
            PrintWriter writer = new PrintWriter("results.txt", "UTF-8");
            writer.println("mc1 = [");
            for (double score : mc1Score) {
                writer.println(score);
            }
            writer.println("];");
            writer.println("mc2 = [");
            for (double score : mc2Score) {
                writer.println(score);
            }
            writer.println("];");
            writer.println("mc3 = [");
            for (double score : mc3Score) {
                writer.println(score);
            }
            writer.println("];");
            writer.println("mc4 = [");
            for (double score : mc4Score) {
                writer.println(score);
            }
            writer.println("];");
            writer.println("mc5 = [");
            for (double score : mc5Score) {
                writer.println(score);
            }
            writer.println("];");
            writer.println("N = [");
            for (int n : N) {
                writer.println(n);
            }
            writer.println("];");
            writer.close();
        } catch (IOException ioe) {
            ioe.getMessage();
        }
    }

    public static void main( String[] args ) {
        if ( args.length != 1 ) {
            System.err.println( "Please give the name of the link file" );
        } else {
            new MonteCarloEvaluation(args[0]);
        }
    }
}
