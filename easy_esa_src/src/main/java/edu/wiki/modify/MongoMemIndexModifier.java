package edu.wiki.modify;

import gnu.trove.TIntDoubleHashMap;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.mongodb.*;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Reads TF and IDF from the index and 
 * writes cosine-normalized TF.IDF values to database.
 * 
 * Normalization is performed as in Gabrilovich et al. (2009)
 * 
 * Usage: IndexModifier <Lucene index location>
 * 
 * @author Cagatay Calli <ccalli@gmail.com>
 *
 */
public class MongoMemIndexModifier {
	static MongoClient conn;
	static DB db;
		
	private static IndexReader reader = null;
	
	static int limitID;
	
	private static TIntDoubleHashMap inlinkMap;
		
	static int WINDOW_SIZE = 100;
	static float WINDOW_THRES= 0.005f;
	
	static DecimalFormat df = new DecimalFormat("#.########");
	
	static public class DocScore implements Comparable<DocScore> {
		int doc;
		float score;
		
		public DocScore(int doc, float score) {
			this.doc = doc;
			this.score = score;
		}

		@Override
		public int compareTo(DocScore o) {
			float val = (this.score - o.score);
			if(val < 0){
				return 1;	// descending
			}
			else if(val > 0){
				return -1;
			}
			return 0;
		}

	}
	
	/**
	 * global, term-doc matrix
	 */
	static HashMap<String, ArrayList<DocScore>> matrix;
		
	public static void initDB() throws ClassNotFoundException, IOException {
		// read DB config
		InputStream is = MongoMemIndexModifier.class.getResourceAsStream("/config/db.conf");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String serverName = br.readLine();
		String databaseName = br.readLine();
		br.close();

		// Create a connection to the database 
		conn = new MongoClient(serverName);
	  	db = conn.getDB(databaseName);
		
		db.getCollection("terms").drop();
		
		limitID = (int)db.getCollection("articles").count();
		
		
		// read inlink counts	
		inlinkMap = new TIntDoubleHashMap(limitID);
		
		int targetID, numInlinks; 
		DBCursor cur = db.getCollection("articles").find(new BasicDBObject(), new BasicDBObject("id", 1).append("inlinks", 1));
		while(cur.hasNext()){
			DBObject art = cur.next();
			targetID = (Integer)art.get("id");
			numInlinks = (Integer)art.get("inlinks");
			inlinkMap.put(targetID, Math.log(1+Math.log(1+numInlinks)));
		}
		
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws NoSuchAlgorithmException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
			    		
		try {
	    	Directory fsdir = FSDirectory.open(new File(args[0]));
			reader = IndexReader.open(fsdir,true);
	    } catch (Exception ex) {
	      System.out.println("Cannot create index..." + ex.getMessage());
	      System.exit(-1);
	    }
	    
	    initDB();
	    
	    long sTime, eTime;
	    
	    sTime = System.currentTimeMillis();
	    
	    int maxid = reader.maxDoc();
	    TermFreqVector tv;
	    String[] terms;
	    String term = "";
	    
	    Term t;
	    
	    int tcount;
	    int tfreq = 0;
	    float idf;
	    float tf;
	    float tfidf;
	    double inlinkBoost;
	    double sum;
	    
	    int wikiID;
	    
	    int hashInt;
	    	    
	    int numDocs = reader.numDocs();
	    
	    TermEnum tnum = reader.terms();
	    HashMap<String, Float> idfMap = new HashMap<String, Float>(500000);
	    
	    HashMap<String, Float> tfidfMap = new HashMap<String, Float>(5000);

	    HashMap<String, Integer> termHash = new HashMap<String, Integer>(500000);
	    	    	    
	    
	    tnum = reader.terms();
	    
	    hashInt = 0;
	    tcount = 0;
	    while(tnum.next()){
	    	t = tnum.term();
	    	term = t.text();
	    	
	    	tfreq = tnum.docFreq();	// get DF for the term
	    	
	    	// skip rare terms
	    	if(tfreq < 3){
	    		continue;
	    	}
	    	
	    	// idf = (float)(Math.log(numDocs/(double)(tfreq+1)) + 1.0);
	    	idf = (float)(Math.log(numDocs/(double)(tfreq))); 	
	    	// idf = (float)(Math.log(numDocs/(double)(tfreq)) / Math.log(2)); 	

	    	idfMap.put(term, idf);
	    	termHash.put(term, hashInt++);
	    	
	    	tcount++;
	    }
	    
	    matrix = new HashMap<String, ArrayList<DocScore>>(tcount);

	    
	    for(int i=0;i<maxid;i++){
	    	if(!reader.isDeleted(i)){
	    		//System.out.println(i);
	    		
	    		wikiID = Integer.valueOf(reader.document(i).getField("id").stringValue());
	    		inlinkBoost = inlinkMap.get(wikiID);
	    			    		
	    		tv = reader.getTermFreqVector(i, "contents");
	    		try {
	    			terms = tv.getTerms();
	    			
	    			int[] fq = tv.getTermFrequencies();
	    		
	    		
		    		sum = 0.0;	   
		    		tfidfMap.clear();
		    		
		    		// for all terms of a document
		    		for(int k=0;k<terms.length;k++){
		    			term = terms[k];
		    			if(!idfMap.containsKey(term))
		    				continue;
		    			
		    			tf = (float) (1.0 + Math.log(fq[k]));
		    			// tf = (float) (1.0 + Math.log(fq[k]) / Math.log(2));
	
		    			idf = idfMap.get(term);
		    			
		    			tfidf = (float) (tf * idf);
		    			tfidfMap.put(term, tfidf);
		    			
		    			sum += tfidf * tfidf;
		    				    			
		    		}
		    		
		    		
		    		sum = Math.sqrt(sum);
		    		
		    		// for all terms of a document
		    		for(int k=0;k<terms.length;k++){
		    			term = terms[k];
		    			if(!idfMap.containsKey(term))
		    				continue;
		    				    			
		    			tfidf = (float) (tfidfMap.get(term) / sum * inlinkBoost);
		    			
		    				    				    			
		    			// System.out.println(i + ": " + term + " " + fq[k] + " " + tfidf);
		    			
		    			// ++++ record to DB (term,doc,tfidf) +++++
		    			
		    			if(matrix.containsKey(term)){
		    				matrix.get(term).add(new DocScore(wikiID, tfidf));
		    			}
		    			else {
		    				ArrayList<DocScore> dsl = new ArrayList<DocScore>();
		    				dsl.add(new DocScore(wikiID, tfidf));
		    				matrix.put(term, dsl);
		    			}
						
		    		}
	    		
	    		}
	    		catch(Exception e){
	    			e.printStackTrace();
	    			System.out.println("ERR: " + wikiID + " " + tv);
	    			continue;
	    		}
	    		
	    	}
	    }
		
		int doc;
		float score;
		
		// for pruning
		int mark, windowMark;
	    float first = 0, last = 0, highest = 0;
	    float [] window = new float[WINDOW_SIZE];
	    
	    for(String k : matrix.keySet()){
	    	term = k;
	    	List<DocScore> ds = matrix.get(k);
	    	Collections.sort(ds);
	    		    	
	    	// prune and write the vector
	    	{	
		    	
		    	// prune the vector
		    	
		    	mark = 0;
				windowMark = 0;
				highest = first = last = 0;
		    	
		    	ByteArrayOutputStream baos = new ByteArrayOutputStream(50000);
		    	DataOutputStream tdos = new DataOutputStream(baos);
		    	
		    	for(DocScore d : ds){
		    		doc = d.doc;
		    		score = d.score;
		    				    		
		    		// sliding window
		    		
		    		window[windowMark] = score;
		    		
		    		if(mark == 0){
		    			highest = score;
		    			first = score;
		    		}
		    		    		
		    		if(mark < WINDOW_SIZE){
			    		tdos.writeInt(doc);
			    		tdos.writeFloat(score);
		    		}
		    		else if( highest*WINDOW_THRES < (first - last) ){
		    			tdos.writeInt(doc);
			    		tdos.writeFloat(score);

		    			if(windowMark < WINDOW_SIZE-1){
		    				first = window[windowMark+1];
		    			}
		    			else {
		    				first = window[0];
		    			}
		    		}
		    		
		    		else {
		    			// truncate
		    			break;
		    		}	
		    		
		    		last = score;

		    		mark++;
		    		windowMark++;
		    		
		    		windowMark = windowMark % WINDOW_SIZE;
		    		
		    	}
		    			    	
		    	ByteArrayOutputStream dbvector = new ByteArrayOutputStream();
		    	DataOutputStream dbdis = new DataOutputStream(dbvector);
		    	dbdis.writeInt(mark);
		    	dbdis.flush();
		    	dbvector.write(baos.toByteArray());
		    	dbvector.flush();
		    	
		    	dbdis.close();
		    			    			    		    		
	    		// write to DB
		    	BasicDBObject insTerm = new BasicDBObject("term", term);
		    	insTerm.put("idf", idfMap.get("term"));
		    	insTerm.put("vector", dbvector.toByteArray());
		    	db.getCollection("terms").insert(insTerm);
		    	
		    	tdos.close();
		    	baos.close();
				
	    	}
	    		    	
	    }
    	
		db.getCollection("terms").ensureIndex("term");
    	
	    eTime = System.currentTimeMillis();
	    
		System.out.println("Total TIME (sec): "+ (eTime-sTime)/1000.0);
	    
		
	    reader.close();
	    conn.close();
	    
	}

}
