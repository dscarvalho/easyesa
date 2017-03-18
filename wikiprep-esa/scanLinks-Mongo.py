#!/usr/bin/python

'''
Copyright (C) 2010  Cagatay Calli <ccalli@gmail.com>

Scans XML output (gum.xml) from Wikiprep, creates 3 tables:

TABLE: pagelinks	COLUMNS: source_id INT, target_id INT
TABLE: inlinks		COLUMNS: target_id INT, inlink INT
TABLE: outlinks		COLUMNS: source_id INT, outlink INT
TABLE: namespace	COLUMNS: id INT

USAGE: scanData.py <hgw.xml file from Wikiprep>

IMPORTANT: If you use XML output from a recent version of Wikiprep
(e.g. Zemanta fork), then set FORMAT to 'Zemanta-legacy' or 'Zemanta-modern'.

'''

import sys
import re
import pymongo
from pymongo import MongoClient
from pymongo.errors import ConnectionFailure
import signal
import xmlwikiprep

LINK_LOAD_THRES = 10000

try:
	conn = MongoClient(host = "localhost")
	db = conn["wiki"]

except ConnectionFailure, e:
	print "Error %d: %s" % (e.args[0], e.args[1])
	sys.exit(1)



db.articles.drop()
db.articles.ensure_index([("id", pymongo.ASCENDING)])

## handler for SIGTERM ###
def signalHandler(signum, frame):
    global conn
    conn.close()
    sys.exit(1)

signal.signal(signal.SIGTERM, signalHandler)
#####

reOtherNamespace = re.compile("^(User|Wikipedia|File|MediaWiki|Template|Help|Category|Portal|Book|Talk|Special|Media|WP|User talk|Wikipedia talk|File talk|MediaWiki talk|Template talk|Help talk|Category talk|Portal talk):.+",re.DOTALL)

linkBuffer = []
linkBuflen = 0

nsBuffer = []
nsBuflen = 0

mainNS = []

# pageContent - <page>..content..</page>
# pageDict - stores page attribute dict
def recordArticle(pageDoc):
   global linkBuffer, linkBuflen, nsBuffer, nsBuflen

   # a simple check for content
   if pageDoc['length'] < 10:
	return

   _id = pageDoc['_id']

   # only keep articles of Main namespace
   if reOtherNamespace.match(pageDoc['title']):
        return

   nsBuffer.append((_id))
   nsBuflen += 1

   if linkBuflen >= LINK_LOAD_THRES:
	for _id in nsBuffer:
		db.articles.update({"id": _id}, {"$set": {"namespace": True}}, upsert = True)
   	
	nsBuffer = []
        nsBuflen = 0


   # write links
   for l in pageDoc['links']:
        linkBuffer.append((_id,l)) # source, target
        linkBuflen += 1

        if linkBuflen >= LINK_LOAD_THRES:
		for lnk in linkBuffer:
			db.articles.update({"id": lnk[0]}, {"$push": {"pagelinks": lnk[1]}, "$inc": {"outlinks": 1}}, upsert = True)
			db.articles.update({"id": lnk[1]}, {"$inc": {"inlinks": 1}}, upsert = True)
   	
                linkBuffer = []
                linkBuflen = 0

   return


args = sys.argv[1:]
# scanData.py <hgw_file>

if len(args) < 1:
    sys.exit()

f = open(args[0],'r')
for doc in xmlwikiprep.read(f):
	recordArticle(doc)
f.close()

if nsBuflen > 0:
    for _id in nsBuffer:
        db.articles.update({"id": _id}, {"$set": {"namespace": True}}, upsert = True)
   	
    nsBuffer = []
    nsBuflen = 0

if linkBuflen > 0:
    for lnk in linkBuffer:
        db.articles.update({"id": lnk[0]}, {"$push": {"pagelinks": lnk[1]}, "$inc": {"outlinks": 1}}, upsert = True)
        db.articles.update({"id": lnk[1]}, {"$inc": {"inlinks": 1}}, upsert = True)
   	
    linkBuffer = []
    linkBuflen = 0



conn.close()

