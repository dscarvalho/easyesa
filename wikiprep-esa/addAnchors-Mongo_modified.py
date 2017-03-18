#!/usr/bin/python

'''
Copyright (C) 2010  Cagatay Calli <ccalli@gmail.com>

Adds anchors from Wikiprep output to target Wikipedia articles.

Legacy input format: <Target page id>  <Source page id>  <Anchor text (up to the end of the line)>
Input format: <Target page id>  <Source page id>  <Anchor location within text>  <Anchor text (up to the end of the line)>
Output format: <Target page id>	<Anchor text>

USAGE: addAnchors.py <anchor file from Wikiprep> <any writeable folder>
The folder is used by the script to create data files that are loaded into database.

IMPORTANT: If you use XML output from a recent version of Wikiprep
(e.g. Zemanta fork), then set FORMAT to 'Zemanta-legacy' or 'Zemanta-modern'.
'''

import sys
import pymongo
from pymongo import MongoClient
from pymongo.errors import ConnectionFailure
from optparse import OptionParser

# Wikiprep dump format enum
# formats: 1) Gabrilovich 2) Zemanta-legacy 3) Zemanta-modern
F_GABRI = 0 # gabrilovich
F_ZLEGACY = 1   # zemanta legacy
F_ZMODERN = 2   # zemanta modern

usage = """
USAGE: addAnchors.py <anchor file from Wikiprep> <any writeable folder>' --format=<Wikiprep dump format>
Wikiprep dump formats:
1. Gabrilovich [gl, gabrilovich]
2. Zemanta legacy [zl, legacy, zemanta-legacy]
3. Zemanta modern [zm, modern, zemanta-modern]
"""

parser = OptionParser(usage=usage)
parser.add_option("-f", "--format", dest="_format", help="Wikiprep dump format (g for Gabrilovich, zl for Zemanta-legacy,zm for Zemanta-modern)", metavar="FORMAT")
(options, args) = parser.parse_args()
if len(args) != 1:
	print usage
	sys.exit()
if not options._format:
        print """
Wikiprep dump format not specified! Please select one from below with --format option:

Wikiprep dump formats:
1. Gabrilovich [gl, gabrilovich]
2. Zemanta legacy [zl, legacy, zemanta-legacy]
3. Zemanta modern [zm, modern, zemanta-modern]
"""
        sys.exit()

if options._format in ['zm','zemanta-modern','Zemanta-modern','Zemanta-Modern','modern']:
        FORMAT = F_ZMODERN
elif options._format in ['gl','gabrilovich','Gabrilovich']:
        FORMAT = F_GABRI
elif options._format in ['zl','zemanta-legacy','Zemanta-legacy','Zemanta-Legacy','legacy']:
        FORMAT = F_ZLEGACY

PARTITION_SIZE = 100000

if FORMAT == F_GABRI:
	FIELD_POS = 2
else:
	FIELD_POS = 3

args = sys.argv[1:]

if len(args) < 1:
        sys.exit(1)

try:
        conn = MongoClient(host = "localhost")
        db = conn["wiki"]

except ConnectionFailure, e:
        print "Error %d: %s" % (e.args[0], e.args[1])
        sys.exit(1)


f = open(args[0],'r')

cur_art = ""
cur_art_anchors = []

for i in range(3):
	temp =f.readline()
	print temp
	
print "start"

art_ids = set([int(art["id"]) for art in list(db.articles.find({}, {"id": 1}))])
cont =4
#for line in f.xreadlines():
'''
line = f.readline()
while line:
	#print line
	fields = line.split('\t')
        if (fields[0][0] =="#"):
			print "--- ",cont,"  ",line
			#print f.xreadlines()
			#print f.xreadlines()
			#print f.xreadlines()
			print f.readline()
			print f.readline()
			#print f.readline()
			print "----"
			cont +=1
			break
	cont +=1		
	line = f.readline()
	
print "cont: ", cont	
'''


#for line in f.xreadlines():
line = f.readline()
while line:
	#cont +=1
	#print cont, " -- ",line

	fields = line.split('\t')
	if (fields[0][0] =="#"):
		#print "--- ",cont,"  ",line
		print f.readline()
		print f.readline()
	else:
		try:
		 anc = fields[FIELD_POS].rstrip('\n')
		except:
			 print fields

		if (int(fields[0]) in art_ids):
			try:
				if (fields[0] != cur_art):
					if (cur_art):
						db.articles.update({"id": int(cur_art)}, {"$pushAll": {"anchors": cur_art_anchors}})
					cur_art = fields[0]
					cur_art_anchors = []
		
				cur_art_anchors.append(anc)
	
			except pymongo.errors.OperationFailure:
				print "Operation Failure: Cannot update id ", fields[0]
				print "cur_anchors: ", cur_art_anchors
				cur_art_anchors = []

	line = f.readline()

f.close()
conn.close()
