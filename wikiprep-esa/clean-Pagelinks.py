#!/usr/bin/python
# -*- coding: utf-8 -*-

import sys
import re
import pymongo
from pymongo import MongoClient
from pymongo.errors import ConnectionFailure

db = None
try:
        conn = MongoClient(host = "localhost")
        db = conn["wiki"]

except ConnectionFailure, e:
        print "Error %d: %s" % (e.args[0], e.args[1])
        sys.exit(1)


print "Cleaning pagelinks..."
art_ids = set([art["id"] for art in list(db.articles.find({}, {"id": 1}))])
nlinks = []

total_arts = db.articles.count()
print "Articles: ", total_arts
count = 0
cur = db.articles.find({}, {"id": 1, "pagelinks": 1})
for art in cur:
	if ("pagelinks" in art):
		db.articles.update({"id": int(art["id"])}, {"$set": {"pagelinks": list(set(art["pagelinks"]).intersection(art_ids))}})
	count += 1
	perc = (float(count) / total_arts) * 100
	sys.stdout.write("\r" + str(perc))


# release DB resources
conn.close()


