#! /bin/python

import sys
import string

inputFileName = sys.argv[1]
outputFileName = sys.argv[2]

print inputFileName
print outputFileName

#print "ndlasdas"


sourceFile = open(inputFileName, 'r')
outputFile = open(outputFileName, 'w')

for line in sourceFile:
#	print line
	tempString = line.split(',',3)[0]+","+line.split(',',3)[1]+","+str(string.atof(line.split(',',3)[2])-45.8204)+"\n"
	
	outputFile.write(tempString)
