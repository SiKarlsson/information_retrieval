### Compile with: ###

javac -Xlint:none -cp .:/Users/simon/Documents/skola/ir16/lab/pdfbox ir/*.java

### Run with: ###

java -Xmx1024m -cp .:/Users/simon/Documents/skola/ir16/lab/pdfbox ir.SearchGUI
-d /Users/simon/Documents/skola/ir16/lab/davisWiki

### Debugging: ###

## Single term querys: ##

grep –rEil ”\bTERM\b” . > textfile

## Intersection: ##

grep -Fx -f file1 file2 > file3