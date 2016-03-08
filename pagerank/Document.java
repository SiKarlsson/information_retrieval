/*  
 *   Represent a document with a score
 * 
 *   First version:  Simon Karlsson, 2016   
 */  

public class Document implements Comparable<Document> {

    private String docNumber;
    private double rankScore;

    public Document(String docNumber, double rankScore) {
        this.docNumber = docNumber;
        this.rankScore = rankScore;
    }

    public double getRankScore() {
        return rankScore;
    }

    public String getDocNumber() {
        return docNumber;
    }

    public int compareTo(Document other) {
        return Double.compare(other.rankScore, rankScore);
    }
}
