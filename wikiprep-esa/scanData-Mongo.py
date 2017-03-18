#!/usr/bin/python
# -*- coding: utf-8 -*-

'''
Copyright (C) 2010  Cagatay Calli <ccalli@gmail.com>

Scans XML output (gum.xml) from Wikiprep, inserts the fields:
- title
- text

in the articles collection.

USAGE: scanData.py <hgw.xml/gum.xml file from Wikiprep> --format=<Wikiprep dump format> [--stopcats=<stop category file>]

IMPORTANT: If you use XML output from a recent version of Wikiprep
(e.g. Zemanta fork), then set FORMAT to 'Zemanta-legacy' or 'Zemanta-modern'.


ABOUT STOP CATEGORY FILTERING:
Stop category filtering is not active in default configuration. You can change
 provide an updated list of stop categories, derived from your Wikipedia dump 
with --stopcats option.
e.g. scanData.py sample.gum.xml --stopcats=sampleCategoryList.txt 

Cleaning up irrelevant articles is important in ESA so providing such a file 
is recommended.

'''

import sys
import re
import pymongo
from pymongo import MongoClient
from pymongo.errors import ConnectionFailure
import signal
from optparse import OptionParser

import lxml.html as html
import Stemmer

import xmlwikiprep

# Wikiprep dump format enum
# formats: 1) Gabrilovich 2) Zemanta-legacy 3) Zemanta-modern
F_GABRI = 0 # gabrilovich
F_ZLEGACY = 1	# zemanta legacy
F_ZMODERN = 2	# zemanta modern

usage = """
USAGE: scanData.py <hgw.xml/gum.xml file from Wikiprep> --format=<Wikiprep dump format> [--stopcats=<stop category file>]

Wikiprep dump formats:
1. Gabrilovich [gl, gabrilovich]
2. Zemanta legacy [zl, legacy, zemanta-legacy]
3. Zemanta modern [zm, modern, zemanta-modern]

'2005_wiki_stop_categories.txt' can be used for 2005 dump of Gabrilovich et al.
"""
parser = OptionParser(usage=usage)
parser.add_option("-s", "--stopcats", dest="stopcats", help="Path to stop categories file", metavar="STOPCATS")
parser.add_option("-f", "--format", dest="_format", help="Wikiprep dump format (g for Gabrilovich, zl for Zemanta-legacy,zm for Zemanta-modern)", metavar="FORMAT")


(options, args) = parser.parse_args()
if not args:
	print usage
	sys.exit()
if not options.stopcats:
        print 'Stop category list is not provided. (You can provide this with --stopcats argument.)'
	print 'Continuing without stop category filter...'

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


# scanData.py <hgw_file> [--stopcats=<stop category file>]

hgwpath = args[0] # hgw/gum.xml

TITLE_WEIGHT = 4
STOP_CATEGORY_FILTER = bool(options.stopcats)

# reToken = re.compile('[a-zA-Z\-]+')
reToken = re.compile("[^ \t\n\r`~!@#$%^&*()_=+|\[;\]\{\},./?<>:’'\\\\\"]+")
reAlpha = re.compile("^[a-zA-Z\-_]+$")
NONSTOP_THRES = 100

STEMMER = Stemmer.Stemmer('porter')

# read stop word list from 'lewis_smart_sorted_uniq.txt'
wordList = []
try:
	f = open('lewis_smart_sorted_uniq.txt','r')
	for word in f.readlines():
		wordList.append(word.strip())
	f.close()
except:
	print 'Stop words cannot be read! Please put "lewis_smart_sorted_uniq.txt" file containing stop words in this folder.'
	sys.exit(1)

STOP_WORDS = frozenset(wordList)


if STOP_CATEGORY_FILTER:
	# read list of stop categories from 'extended_stop_categories.txt'
	catList = []
	try:
		f = open(options.stopcats,'r')
		for line in f.readlines():
			strId = line.split('\t')[0]
			if strId:
				catList.append(int(strId))
		f.close()
	except:
		print 'Stop categories cannot be read!'
		sys.exit(1)

	STOP_CATS = frozenset(catList)


# read disambig IDs for legacy format
disambigList = []
# No disambigs from Gabrilovich's 20051105 preprocessed dump.
#if FORMAT != F_ZMODERN:
#	disambigPath = hgwpath.replace('hgw.xml','disambig')
#	print disambigPath
#	try:
#		f = open(disambigPath, 'r')
#
#		for i in range(3):
#        		f.readline()
#
#		prevId = ''
#		for line in f.readlines():
#        		if prevId and line.startswith(prevId):
#	                	continue
#	        	id = line.split('\t',1)[0].strip()
#		        disambigList.append(int(id))
#        		prevId = id
#
#		f.close()
#	except:
#		print 'Disambig file cannot be read! Please check if a file with .disambig suffix exists in Wikiprep dump location'
#		sys.exit(1)

DISAMBIG_IDS = frozenset(disambigList)

try:
        conn = MongoClient(host = "localhost")
        db = conn["wiki"]

except ConnectionFailure, e:
        print "Error %d: %s" % (e.args[0], e.args[1])
        sys.exit(1)


## handler for SIGTERM ###
def signalHandler(signum, frame):
    global conn 
    conn.close()
    sys.exit(1)

signal.signal(signal.SIGTERM, signalHandler)
#####

reOtherNamespace = re.compile("^(User|Wikipedia|File|MediaWiki|Template|Help|Category|Portal|Book|Talk|Special|Media|WP|User talk|Wikipedia talk|File talk|MediaWiki talk|Template talk|Help talk|Category talk|Portal talk):.+",re.DOTALL)

# category, disambig, stub pages are removed by flags

# regex as article filter (dates, lists, etc.)
'''
# slightly improved title filter, filtering all dates,lists etc.

re_strings = ['^(January|February|March|April|May|June|July|August|September|October|November|December) \d+$',
	      '^\d+((s|(st|nd|th) (century|millenium)))?( (AD|BC|AH|BH|AP|BP))?( in [^$]+)?$',
	      '.+\(disambiguation\)']
'''

# title filter of Gabrilovich et al. contains: * year_in ... * month_year * digit formats
re_strings = ['^(January|February|March|April|May|June|July|August|September|October|November|December) \d{4}$',
	      '^\d{4} in [^$]+?$',
	      '^\d+$']
piped_re = re.compile( "|".join( re_strings ) , re.DOTALL|re.IGNORECASE)

# list filter
reList = re.compile('^List of .+',re.DOTALL|re.IGNORECASE)


###
articleBuffer = []	# len: 100  / now: 200
aBuflen = 0

textBuffer = []		# same as articleBuffer, stores text

###

inlinkDict = {}
outlinkDict = {}

cur = db.articles.find({}, {"id": 1, "inlinks": 1, "outlinks": 1})
for art in cur:
	if ("inlinks" in art):
		inlinkDict[art["id"]] = art["inlinks"]
	if ("outlinks" in art):
		outlinkDict[art["id"]] = art["outlinks"]

# for logging
# Filtered concept id=12 (hede hodo) [minIncomingLinks]
log = open('log.txt','w')

# pageContent - <page>..content..</page>
# pageDict - stores page attribute dict
def recordArticle(pageDoc):
   global articleBuffer, textBuffer, aBuflen, STEMMER

   if FORMAT == F_ZMODERN and (pageDoc['disambig'] or pageDoc['category'] or pageDoc['image']):
	return

   # a simple check for content
   if pageDoc['length'] < 10:
	return

   title = pageDoc['title']
   _id = pageDoc['_id']

   # only keep articles of Main namespace
   if reOtherNamespace.match(title):
        return

   # skip disambig   
   if FORMAT != F_ZMODERN and _id in DISAMBIG_IDS:
	return

   # ** stop category filter **
   if STOP_CATEGORY_FILTER:
   	cats = frozenset(pageDoc['categories'])

   	# filter article with no category or belonging to stop categories
   	if not cats or STOP_CATS.intersection(cats):
	        try:
        	    log.write('Filtered concept id='+str(_id)+' ('+ title +') [stop category]\n')
        	except:
		    print "Encode error!!!!!"
		return
   # ******

   # ** title filter **
   if piped_re.match(title):
       try:
           log.write('Filtered concept id='+str(_id)+' ('+ title +') [regex]\n')
       except:
	   print "Encode error!!!!"
       return

   '''if reList.match(title):
       log.write('Filtered concept id='+str(id)+' ('+ title +') [list]\n')
       return'''
   # ******

   # ** inlink-outlink filter **
   try:
       if not inlinkDict.has_key(_id) or inlinkDict[_id] < 5:
            log.write('Filtered concept id='+str(_id)+' ('+ title.encode('utf8') +') [minIncomingLinks]\n')
	    return

       if not outlinkDict.has_key(_id) or outlinkDict[_id] < 5:
            log.write('Filtered concept id='+str(_id)+' ('+ title.encode('utf8') +') [minOutgoingLinks]\n')
	    return
   except:
       print "Encode error!"
   # ******

   text = pageDoc['text']

   # convert HTML to plain text
   t = html.fromstring(title)
   ctitle = t.text_content()

   ctext = ''
   try: 
        t = html.fromstring(text)
        ctext = t.text_content()
   except Exception:
        print "Empty document: ", ctitle

   # filter articles with fewer than 100 -UNIQUE- non-stop words
   cmerged = ctitle + ' \n ' + ctext

   tokens = set()
   wordCount = 0
   for m in reToken.finditer(cmerged):
	w = m.group()
	if not w or len(w) <= 2 or not reAlpha.match(w):
		continue
	lword = w.lower()
	if not lword in STOP_WORDS:
		sword = STEMMER.stemWord(STEMMER.stemWord(STEMMER.stemWord(lword)))	# 3xPorter
		if not sword in tokens:
			wordCount += 1
			tokens.add(sword)
			if wordCount == NONSTOP_THRES:
				break

   if wordCount < NONSTOP_THRES:
        try:
            log.write('Filtered concept id='+str(_id)+' ('+ title.encode('utf8') +') [minNumFeaturesPerArticle]\n')
        except:
	    print "Encode error!!"
	return


   cadd = ''
   for i in range(TITLE_WEIGHT):
	cadd += ctitle + ' \n '
   cadd += ctext

   # write article info (id,title,text)
   try:
       ctitle.encode('utf8')
       cadd.encode('utf8')
       articleBuffer.append((_id,ctitle.encode('utf8')))
       textBuffer.append((_id,cadd.encode('utf8')))
       aBuflen += 1
   except:
       print "Encode error!!!"

   if aBuflen >= 200:
	for i in xrange(0, len(articleBuffer)):
		db.articles.update({"id": articleBuffer[i][0]}, {"$set": {"title": articleBuffer[i][1], "text": textBuffer[i][1]}}, upsert = True)

	articleBuffer = []
	textBuffer = []
	aBuflen = 0

   return


f = open(hgwpath,'r')
for doc in xmlwikiprep.read(f):
	recordArticle(doc)
f.close()

if aBuflen > 0:
	for i in xrange(0, len(articleBuffer)):
		db.articles.update({"id": articleBuffer[i][0]}, {"$set": {"title": articleBuffer[i][1], "text": textBuffer[i][1]}}, upsert = True)

	articleBuffer = []
	textBuffer = []


# remove links to articles that are filtered out
print "Cleaning empty docs..."
db.articles.remove({"title": {"$exists": False}})

print "Cleaning pagelinks..."
art_ids = set([art["id"] for art in list(db.articles.find({}, {"id": 1}))])
nlinks = []

cur = db.articles.find({}, {"pagelinks": 1})
for art in cur:
	if ("pagelinks" in art):
		nlinks.extend(art["pagelinks"])

nlinks = list(set(nlinks) - art_ids)
db.articles.update({}, {"$pullAll": {"pagelinks": nlinks}}, multi = True)

print "Articles: ", db.articles.count()

# release DB resources
conn.close()

log.close()

