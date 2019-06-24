package edu.wiki.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import com.mongodb.*;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Performs indexing with Lucene.
 * Keeps term frequency vectors for further use.
 * 
 * Usage: ESAWikipediaIndexer <Lucene index location>
 * 
 * @author Cagatay Calli <ccalli@gmail.com>
 *
 */
public class MongoESAWikipediaIndexer {
	
	  private IndexWriter writer;
	  
	  static MongoClient conn;
	  static DB db;
	  int addCount = 0;
	  
	  public static void initDB() throws ClassNotFoundException, IOException {
			// read DB config
			InputStream is = MongoESAWikipediaIndexer.class.getResourceAsStream("/config/db.conf");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String serverName = br.readLine();
			String databaseName = br.readLine();
			br.close();

			// Create a connection to the database 
			conn = new MongoClient(serverName);
		  	db = conn.getDB(databaseName);
		}
	  
	  public static void main(String[] args) throws IOException, ClassNotFoundException {
		
	    if(args.length < 1){
	    	System.out.println("Usage: ESAWikipediaIndexer <index path>");
	    	System.exit(-1);
	    }

	    String s = args[0];
	    
	    initDB();

	    MongoESAWikipediaIndexer indexer = null;
	    try {
	    	Directory fsdir = FSDirectory.open(new File(s));
	      indexer = new MongoESAWikipediaIndexer(fsdir);
	    } catch (Exception ex) {
	      System.out.println("Cannot create index..." + ex.getMessage());
	      System.exit(-1);
	    }

	    indexer.indexDB();
	    

	    //===================================================
	    //after adding, we always have to call the
	    //closeIndex, otherwise the index is not created    
	    //===================================================
	    indexer.closeIndex();
	    conn.close();
	  }

	  /**
	   * Constructor
	   * @param indexDir the name of the folder in which the index should be created
	   * @throws java.io.IOException
	   */
	  MongoESAWikipediaIndexer(Directory indexDir) throws IOException {
	    // the boolean true parameter means to create a new index everytime, 
	    // potentially overwriting any existing files there.
	    writer = new IndexWriter(indexDir, new WikipediaAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED); 
	  }

	  /**
	   * Indexes a file or directory
	   * @param fileName the name of a text file or a folder we wish to add to the index
	   * @throws java.io.IOException
	 * @throws SQLException 
	   */
	  public void indexDB() throws IOException {
	    
	    int originalNumDocs = writer.numDocs();
	    int id = 0;
	    String title;
	    String text;
	    // float prScore;
	    	    
	    writer.setSimilarity(new ESASimilarity());
	    	
	    DBCursor cur = db.getCollection("articles").find(new BasicDBObject(), new BasicDBObject("id", 1).append("title", 1).append("text", 1));
	    	
    	while(cur.hasNext()){	// there are articles to process 
    		DBObject art = cur.next();
			id = (Integer)art.get("id");
			title = (String)art.get("title");
			text = (String)art.get("text");
			
			try {
		        Document doc = new Document();

		        //===================================================
		        // add contents of file
		        //===================================================
		        
		        // doc.add(new Field("contents", new InputStreamReader(text_blob.getBinaryStream())));

		        doc.add(new Field("contents", new StringReader(text),Field.TermVector.WITH_OFFSETS));
		        
		        // ===
		        // second field - id
		        // ===  
		        doc.add(new Field("id", String.valueOf(id),
		        		Field.Store.YES,
		        		Field.Index.NOT_ANALYZED));
		        
		        // ====
		        // third field - title
		        // ====
		        doc.add(new Field("title", title,
		                Field.Store.YES,
		                Field.Index.NOT_ANALYZED));

		        writer.addDocument(doc);
		        //System.out.println("Added: " + id);
		        addCount++;
		        
		      } catch (Exception e) {
		    	e.printStackTrace();
		        System.out.println("Could not add: " + id);
		      }
			
		}
    	
    	System.out.println("Added: " + addCount);
	    
	    int newNumDocs = writer.numDocs();
	    System.out.println("");
	    System.out.println("************************");
	    System.out.println((newNumDocs - originalNumDocs) + " documents added.");
	    System.out.println("************************");

	  }

	  /**
	   * Close the index.
	   * @throws java.io.IOException
	   */
	  public void closeIndex() throws IOException {
	    writer.optimize();
	    writer.close();
	  }
}
