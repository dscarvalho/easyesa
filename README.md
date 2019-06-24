<center>

## <big><big>EasyESA</big></big>  

</center>

* * *

# <small>Easy Semantic Approximation with Explicit Semantic Analysis</small>

* * *

<a name="toc1"></a>

## 1\. Overview

EasyESA is an implementation of Explicit Semantic Analysis (ESA) based on the Wikiprep-ESA code from Çağatay Çallı ([https://github.com/faraday/wikiprep-esa](https://github.com/faraday/wikiprep-esa)). It runs as a JSON webservice which can be queried for the semantic relatedness measure, concept vectors or the context windows.

This manual provides information on the functionality, setup and usage of EasyESA package.

<a name="toc2"></a>

## 2\. Explicit Semantic Analysis

Explicit Semantic Analysis (ESA) is a technique for text representation that uses Wikipedia commonsense knowledge base using the co-occurrence of words in the text. The articles' words are associated with its concept using TF-IDF scoring, and a word can be represented as a vector of its associations to each concept and thus "semantic relatedness" between any two words can be measured by means of cosine similarity. A document containing a string of words is represented as the centroid of the vectors representing its words.

For more information on ESA, please refer to the paper by Evgeniy Gabrilovich and Shaul Markovitch: "Wikipedia-based semantic interpretation for natural language processing" ([http://www.jair.org/media/2669/live-2669-4346-jair.pdf](http://www.jair.org/media/2669/live-2669-4346-jair.pdf)).

<a name="toc3"></a>

## 3\. EasyESA

EasyESA provides the following functionalities:

<dl>

<dt><u>Semantic relatedness measure</u></dt>

<dd>Given two terms, returns the semantic relatedness: a real number in the [0,1] interval, representing how semantically close are the terms. The more related the terms are, the higher the value returned.</dd>

<dt><u>Concept vector</u></dt>

<dd>Given a term, returns the concept vector: a list of Wikipedia article titles (concepts) with the associated score for the term.</dd>

<dt><u>Query explanation</u></dt>

<dd>Given two terms, returns the concept vector overlapping between them and the "context windows" for both terms on each overlapping concept. A context window for a given pair (term, concept) is a short segment from the Wikipedia article represented by the concept, containing the term.</dd>

</dl>

1.  EasyESA was developed as an improvement over Wikiprep-ESA. The main differences are:

*   Easy setup package for quick deployment of local ESA infrastructure.
*   Performance improvements.
*   Robust concurrent queriesThe setup package also facilitates the generation of new ESA databases from the latest wikipedia dumps.

<a name="toc4"></a>  

## 4\. Downloads

Install [MongoDB](https://www.mongodb.org/downloads)  

EasyESA distribution package: [easyEsa](http://labcores.ppgi.ufrj.br/easyesa/files/easyesa_dist.7z) (includes binaries and source)

Database and Indexes: [English Wikipedia 2013](http://labcores.ppgi.ufrj.br/easyesa/files/data_wikipedia_en_2013.tar.gz) ([Index](http://labcores.ppgi.ufrj.br/easyesa/files/index_wikipedia_en_2013.tar.gz)) or [English Wikipedia 2006](http://labcores.ppgi.ufrj.br/easyesa/files/data_wikipedia_en_2006.7z).  

## 5\. Installation

The EasyESA package includes a setup script for linux.

The setup script will perform the following steps:

1.  Download the latest Wikipedia dump.
2.  Download and install all the dependencies.
3.  Split the Wikipedia dump, using more than one thread.
4.  Preprocess the dump using Wikiprep (Zemanta's version) ([http://www.tablix.org/~avian/git/wikiprep.git](http://www.tablix.org/%7Eavian/git/wikiprep.git)).
5.  Generate the ESA terms and concept vectors from the Wikipedia preprocessed dump.
6.  Generate the database and indexes.
7.  Start the EasyESA services.

The setup can be done in three ways, depending on the user needs and memory/storage availability:

<a name="toc6"></a>

### 5.1\. Simple run (Recommended)  

You can download the EasyESA database and indexes for [English Wikipedia 2013](http://labcores.ppgi.ufrj.br/easyesa/files/data_wikipedia_en_2013.tar.gz) ([Index](http://labcores.ppgi.ufrj.br/easyesa/files/index_wikipedia_en_2013.tar.gz)) or [English Wikipedia 2006](http://labcores.ppgi.ufrj.br/easyesa/files/data_wikipedia_en_2006.7z).  

Simple procedure:  

1\. Extract easy_esa.tar.gz into INSTALL_DIR/.  
2\. Extract data*.tar.gz into INSTALL_DIR/mongodb/data.  
3\. Extract index*.tar.gz into INSTALL_DIR/index.  
4\. Start mongodb: mongod --dbpath mongodb/data/db  
5\. Start the EasyESA service: java -jar easy_esa.jar 8890 INSTALL_DIR/index &  

On Linux, you can execute the run.sh script for steps 4 and 5:

<pre>  $./run.sh <destination dir>
</pre>

<a name="toc10"></a>

### 5.2\. From setup script only (full setup)

Simply execute the setup_all.sh script:

<pre>  $./setup_all.sh <destination dir> <number of preprocessing threads>
</pre>

where

*   <destination dir> is the directory where the package will be installed.
*   <number of preprocessing threads> is the number of threads to be used in the preprocessing step. A value up to the number of processors/cores available is recommended. HyperThreading processors can use up to 1.5 times the number of cores with noticeable performance gain.

Step 4 will take about 3 days to complete on a modern computer (I7 quad core) and use about 200GB of storage space for the early 2013 Wikipedia dump. Step 5 will take about 4 days to complete and use about 30GB on the same specs and Wikipedia dump.

<a name="toc7"></a>

### 5.3\. From setup script with previously downloaded Wikipedia dump

If you already have a wikipedia dump and wish to use it, just comment line 5 of setup_all.sh and put your enwiki-???-pages-articles.xml.bz2 renamed to enwiki-latest-pages-articles.xml.bz2 in the destination directory. The setup script will skip only the download step (step 1).

<a name="toc8"></a>

<!--
### 5.4\. From setup script with preprocessed Wikipedia database

If you already have a wikipedia preprocessed dump (Zemanta format), place all the preprocessed .xml files in the destination directory and execute the setup_preprocessed.sh script:

<pre>  $./setup_preprocessed.sh <destination dir>
</pre>

The steps 1, 3 and 4 will be skipped.
-->

<a name="toc9"></a>

## 6\. Usage & Online Service  

EasyESA service can be used online from

<pre>  http://lasics.dcc.ufrj.br/esaservice 
</pre>

or locally

<pre>  http://localhost:8890/esaservice
</pre>

The service parameters are:

<dl>

<dt>**task**</dt>

<dd>The query function to be called. The choices and their parameters are:

*   **esa**: semantic relatedness measure.
    *   term1, term2 (the two words to measure)
*   **vector**: concept vector.
    *   source: the word for which the concept vector will be returned.
    *   limit: maximum size of the concept vector. The concept vector will be truncated if larger than the limit.
*   **explain**: concept vector overlapping and context windows.
    *   term1, term2 (the two words to compare)
    *   limit: maximum size of the concept vector. the overlapping is calculated after any truncation.

</dd>

</dl>

<a name="toc11"></a>

### 6.1\. Examples

<a name="toc12"></a>

#### 6.1.1\. Semantic relatedness measure query

<pre>  http://lasics.dcc.ufrj.br/esaservice?task=esa&term1=computing&term2=sensor
</pre>

Query for the semantic relatedness measure between the words _computing_ and _sensor_.

<a name="toc13"></a>

#### 6.1.2\. Concept vector query

<pre>  http://lasics.dcc.ufrj.br/esaservice?task=vector&source=coffee&limit=50
</pre>

Query for the concept vector of the word _coffee_ with maximum length of 50 concepts.

<a name="toc14"></a>

#### 6.1.3\. Explain query

<pre>  http://lasics.dcc.ufrj.br/esaservice?task=explain&term1=computing&term2=sensor&limit=10000
</pre>

Query for the concept vector overlapping between the words _computing_ and _sensor_, and the context windows of both words for each concept in the overlap.  

<!-- ## 7\. Demonstrations  -->

<!--
Simple semantic search ([link](http://labcores.ppgi.ufrj.br/esa-demo/semsearch.html)): Add data items (e.g. apple, juice, einstein, theory, speed of light, ...) and do a keyword search (e.g. relativity). Video: ([avi](http://treo.deri.ie/iswc2014demo/semantic_search.avi)) ([ogv](http://treo.deri.ie/iswc2014demo/semantic_search.ogv)).  

Simple word sense disambiguation ([link](http://labcores.ppgi.ufrj.br/esa-demo/sensedisambig.html)): Add a sentence (e.g. the power grid went down), select a word to get the ranked senses (e.g. power). Video: ([avi](http://treo.deri.ie/iswc2014demo/word_sense_disambiguation.avi)) ([ogv](http://treo.deri.ie/iswc2014demo/word_sense_disambiguation.ogv)).
-->

## 8\. License

EasyESA is distributed under [GPL](https://www.gnu.org/copyleft/gpl.html).  

## 9\. Benchmark  

An EasyESA benchmark is available [here](http://easy-esa.org/easyesa_bench_results.txt).  

## 10\. Publication  

Please refer to the publication below if you are using ESA in your experiments.  

Danilo Carvalho, Çağatay Çallı, André Freitas, Edward Curry, _EasyESA: A Low-effort Infrastructure for Explicit Semantic Analysis_, In Proceedings of the 13th International Semantic Web Conference (ISWC), Rival del Garda, 2014\. (Demonstration Paper in Proceedings) ([pdf](http://andrefreitas.org/papers/easyesa_demo_2014.pdf))

## 11\. Contact  

Danilo Carvalho, Çağatay Çallı, Andre Freitas, Edward Curry.  

Insight Centre for Data Analytics  
Digital Enterprise Research Institute (DERI)  
National University of Ireland, Galway  

contact: danilo | at | jaist [dot] ac [dot] jp, andre - dot - freitas | at | deri [dot] org
