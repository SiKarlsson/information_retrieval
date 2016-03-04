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
import java.util.HashMap;

/**
 *   Reads the index on file.
 */
public class IndexReader {

    private  static final int linearSearchThreshold = 1024;
    
    /**
     *  Reads the postings list on disk for the term.
     */
    public PostingsList readPostingsListFromFile(String term) {
        try {
            RandomAccessFile raf = new RandomAccessFile(Constants.postingsFileName(), "r");
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

    public String readFilePath(String id) {
        try {
            RandomAccessFile raf = new RandomAccessFile(Constants.idFileName(), "r" );
            String path = searchPath(Integer.parseInt(id), -1, raf.length()-1, raf);
            raf.close();
            return path;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }

    /**
     *  Returns the offset in the inverted index for the given term.
     */
    private long postingsListOffset(String term) {
        try {
            RandomAccessFile raf = new RandomAccessFile(Constants.indexFileName(), "r" );
            long offset = search(term, -1, raf.length()-1, raf);
            raf.close();
            return offset;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return -1;
    }

    /**
     *  Performs a binary search on the index file.
     */
    private long search(String element, long low, long high, RandomAccessFile raf) throws IOException {
        if (high - low < linearSearchThreshold) {
            return linearSearch(element, low-Constants.maxLineSize, high, raf);
        } else {
            long m = low + ((high - low) / 2);
            String s = nextLine(m, raf);
            if (s == null) {
                return -1;
            }
            String[] line = s.split(" ");
            if (line[0].compareTo(element) < 0) {
                return search(element, m, high, raf);
            } else if (line[0].compareTo(element) > 0) {
                return search(element, low, m, raf);
            } else {
                return Long.parseLong(line[1].trim());
            }
        }
    }

    /**
     *  Performs a linear search on the index file between given boundaries.
     */
    private long linearSearch(String element, long low, long high, RandomAccessFile raf) throws IOException {
        long position = low;
        while (position < high) {
            String s = nextLine(position, raf);
            if (s == null) {
                return -1;
            }
            String[] line = s.split(" ");
            if (line[0].equals(element)) {
                return Long.parseLong(line[1].trim());
            } else if (line[0].compareTo(element) > 0) {
                return -1;
            } else {
                position += s.length();
            }
        }
        return -1;
    }

    /**
     *  Performs a binary search on the index file.
     */
    private String searchPath(int element, long low, long high, RandomAccessFile raf) throws IOException {
        if (high - low < linearSearchThreshold) {
            return linearSearchPath(element, low-Constants.maxLineSize, high, raf);
        } else {
            long m = low + ((high - low) / 2);
            String s = nextLine(m, raf);
            if (s == null) {
                return null;
            }
            String[] line = s.split(" ");
            if (Integer.parseInt(line[0]) < element) {
                return searchPath(element, m, high, raf);
            } else if (Integer.parseInt(line[0]) > element) {
                return searchPath(element, low, m, raf);
            } else {
                return line[1].trim();
            }
        }
    }

    /**
     *  Performs a linear search on the index file between given boundaries.
     */
    private String linearSearchPath(int element, long low, long high, RandomAccessFile raf) throws IOException {
        long position = low;
        while (position < high) {
            String s = nextLine(position, raf);
            if (s == null) {
                return null;
            }
            String[] line = s.split(" ");
            if (Integer.parseInt(line[0]) == element) {
                return line[1].trim();
            } else if (Integer.parseInt(line[0]) > element) {
                return null;
            } else {
                position += s.length();
            }
        }
        return null;
    }

    /**
     *  Returns the next line of the random access file.
     */
    private String nextLine(long low, RandomAccessFile raf) throws IOException {
        if (low < 0) {
            raf.seek(0);
        } else {
            raf.seek(low);
        }
        byte[] buffer = new byte[Constants.maxLineSize];
        int r = raf.read(buffer);
        int lineBeginIndex = -1;
        if (low < 0) {
            lineBeginIndex = 0;
        } else {
            for (int i = 0; i < 1024; i++) {
                if (buffer[i] == '\n') {
                    lineBeginIndex = i + 1;
                    break;
                }
            }
        }
        if (lineBeginIndex == -1) {
            return null;
        }
        int start = lineBeginIndex;
        for (int i = start; i < r; i++) {
            if (buffer[i] == '\n') {
                return new String(buffer, lineBeginIndex, i - lineBeginIndex + 1);
            }
        }
        return null;
    }

    /**
     *  Parses the string representing a postings list and creates a object of it.
     */
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

    public HashMap<String, String> prepareFilePaths() {
        BufferedReader br = null;
        HashMap<String, String> map = new HashMap<String, String>();
        try {
            br = new BufferedReader(new FileReader(Constants.idFileName()));
            String line;
            while ((line = br.readLine()) != null && map.size() <= Constants.pathCacheMaxSize) {
                String[] s = line.split(" ");
                map.put(s[0], s[1]);
            }
            return map;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }   
    }

    public HashMap<String, Integer> readArticleTitles() {
        BufferedReader br = null;
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        try {
            br = new BufferedReader(new FileReader(Constants.articleTitlesDir));
            String line;
            while ((line = br.readLine()) != null) {
                String[] s = line.split(";");
                String key = Constants.defaultCorpus + s[1] + Constants.standardCorpusFileExtension;
                map.put(key, Integer.parseInt(s[0]));
            }
            return map;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            } 
        }
    }

    public HashMap<Integer, Double> readPageRanks() {
        BufferedReader br = null;
        HashMap<Integer, Double> map = new HashMap<Integer, Double>();
        try {
            br = new BufferedReader(new FileReader(Constants.pageRankDir));
            String line;
            while ((line = br.readLine()) != null) {
                String[] s = line.split(" ");
                map.put(Integer.parseInt(s[0]), Double.parseDouble(s[1]));
            }
            return map;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
