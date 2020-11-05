package edu.wiki.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import com.mongodb.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xerces.parsers.DOMParser;

import edu.wiki.api.concept.IConceptIterator;
import edu.wiki.api.concept.IConceptVector;
import edu.wiki.search.MongoESASearcher;
import edu.wiki.search.NormalizedWikipediaDistance;

import com.google.gson.Gson;

public class MongoESAServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	protected ServletContext context;
	
	protected NormalizedWikipediaDistance nwd;
	protected MongoESASearcher esa;
	
	DOMParser parser = new DOMParser();
	protected Document doc = new DocumentImpl();
	// protected DecimalFormat df = (DecimalFormat) NumberFormat.getInstance(Locale.US);
	protected DecimalFormat df = new DecimalFormat("#.##########");
	
	
	
	static MongoClient conn;
	static DB db;
	
	static String strTitles = "SELECT id,title FROM article WHERE id IN ";
	
	public static void initDB() throws ClassNotFoundException, IOException {		
		// read DB config
		InputStream is = MongoESASearcher.class.getResourceAsStream("/config/db.conf");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String serverName = br.readLine();
		String databaseName = br.readLine();
		br.close();

		// Create a connection to the database 
		conn = new MongoClient(serverName);
	  	db = conn.getDB(databaseName);
  }
	
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		context = config.getServletContext() ;
		
		nwd = new NormalizedWikipediaDistance(context.getInitParameter("new_index_path"));
		try {
			esa = new MongoESASearcher();
			initDB();
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServletException();
		}
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		doGet(request, response) ;

	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		//long sTime, eTime;
	    //sTime = System.currentTimeMillis();
	    
		try {

			response.setHeader("Cache-Control", "no-cache"); 
			response.setCharacterEncoding("UTF-8") ;

			String task = request.getParameter("task") ;
			
			//redirect to home page if there is no task
			if (task==null) {
				response.setContentType("text/html");
				response.getWriter().append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\"><html><head><meta http-equiv=\"REFRESH\" content=\"0;url=" + context.getInitParameter("server_path") + "></head><body></body></html>") ;
				return ;
			}
			
			//process compare request
			if (task.equals("nwd")) {
				String term1 = request.getParameter("term1");
				String term2 = request.getParameter("term2") ;
								
				if (term1 == null || term2 == null) {
					response.setContentType("application/json");
					response.getWriter().append("-1") ;
					return ;
				}
				else {
					final double distance = nwd.getDistance(term1, term2);
					response.setContentType("text/html");
					
					if(distance == 10000.0){
						response.getWriter().append(String.valueOf(distance));
					}
					else if(distance == -1){
						response.getWriter().append(String.valueOf(distance));
					}
					else {
						response.getWriter().append(df.format(distance));
					}
					
					//eTime = System.currentTimeMillis();
					//System.err.println("Total TIME (sec): "+ (eTime-sTime)/1000.0);
					
					return ;
				}
				
			}
			
			//process compare request
			if (task.equals("esa")) {
				String term1 = request.getParameter("term1");
				String term2 = request.getParameter("term2") ;
				
				if (term1 == null || term2 == null) {
					response.setContentType("application/json");
					response.getWriter().append("-1") ;
					return ;
				}
				else {
					final double sim = esa.getRelatedness(term1, term2);
					response.setContentType("application/json");
					if(sim == -1){
						response.getWriter().append(String.valueOf(sim)) ;
					}
					else {
						response.getWriter().append(df.format(sim)) ;
					}
					esa.clean();
					
					//eTime = System.currentTimeMillis();
					//System.err.println("Total TIME (sec): "+ (eTime-sTime)/1000.0);
					
					return ;
				}

			}
			
			//process compare request
			if (task.equals("vector")) {
				final String source = request.getParameter("source");				
				final String strLimit = request.getParameter("limit");
				
				response.setContentType("application/json");
				
				int limit;
				
				if(strLimit == null){
					limit = 10;
				}
				else {
					limit = Integer.valueOf(strLimit);
				}
				
				if (source == null) {
					response.getWriter().append("null") ;
					return ;
				}
				else {
					final IConceptVector cv = esa.getConceptVector(source);
					
					if(cv == null){
						response.getWriter().append("null") ;
					}
					
					else {
						final IConceptVector ncv = esa.getNormalVector(cv, limit);
						final IConceptIterator it = ncv.orderedIterator();
						
						HashMap<Integer, Double> vals = new HashMap<Integer, Double>(10);
						HashMap<Integer, String> titles = new HashMap<Integer, String>(10);
						
						int count = 0;
						while(it.next() && count < limit){
							vals.put(it.getId(),it.getValue());
							count++;
						}
						
						BasicDBObject query = new BasicDBObject("id", new BasicDBObject("$in", new ArrayList<Integer>(vals.keySet())));
						BasicDBObject fields = new BasicDBObject("id", 1).append("title", 1);
						DBCursor cur = db.getCollection("articles").find(query, fields);
						
						while(cur.hasNext()){
							DBObject art = cur.next();
							titles.put((Integer)art.get("id"), (String)art.get("title")); 
						}
						
						//ArrayList<String> resp = new ArrayList<String>();
						ArrayList<HashMap<String, Object>> resp = new ArrayList<HashMap<String, Object>>();
						it.reset();
						count = 0;
						while(it.next() && count < limit){
							HashMap<String, Object> dimObj = new HashMap<String, Object>();
							int id = it.getId();
							dimObj.put("id", id);
							dimObj.put("title", titles.get(id));
							dimObj.put("value", vals.get(id));
									
							//resp.add(id + "," + titles.get(id) + "," + df.format(vals.get(id)));
							resp.add(dimObj);
							count++;
						}
						
						response.getWriter().write(new Gson().toJson(resp));
					}
					
					//eTime = System.currentTimeMillis();
					//System.err.println("Total TIME (sec): "+ (eTime-sTime)/1000.0);
					
					return ;
				}

			}
			
			if (task.equals("explain")) {
				String term1 = request.getParameter("term1");
				String term2 = request.getParameter("term2") ;
				final String strLimit = request.getParameter("limit");
				int limit;
				
				if(strLimit == null){
					limit = 10;
				}
				else {
					limit = Integer.valueOf(strLimit);
				}
				
				final IConceptVector cv1 = esa.getConceptVector(term1);
				final IConceptVector cv2 = esa.getConceptVector(term2);
				
				if(cv1 == null || cv2 == null){
					response.getWriter().append("null") ;
				}
				else {
					final IConceptVector ncv1 = esa.getNormalVector(cv1, limit);
					final IConceptVector ncv2 = esa.getNormalVector(cv2, limit);
					final IConceptIterator it1 = ncv1.orderedIterator();
					final IConceptIterator it2 = ncv2.orderedIterator();
					
					IConceptVector intersec = esa.getVectorIntersection(ncv1, ncv2);
					HashMap<String, String[]> term1CtxWindows = esa.getConceptContextWindows(intersec, term1, 200, context.getInitParameter("new_index_path"));
					HashMap<String, String[]> term2CtxWindows = esa.getConceptContextWindows(intersec, term2, 200, context.getInitParameter("new_index_path"));
					
					HashMap<String, Double> overlapCv = new HashMap<String, Double>();
					IConceptIterator itIntersec = intersec.orderedIterator();
					while (itIntersec.next()) {
						String title = (String)db.getCollection("articles").findOne(new BasicDBObject("id", itIntersec.getId())).get("title");
						overlapCv.put(title , itIntersec.getValue());
					}
					
					HashMap<String, Object> resp = new HashMap<String, Object>();
					resp.put("overlapCV", overlapCv);
					resp.put("contextWindow1", term1CtxWindows);
					resp.put("contextWindow2", term2CtxWindows);
					
					response.setContentType("application/json");
					
					response.getWriter().write(new Gson().toJson(resp));
				}
				
				return;
			}
			
			

		} catch (Exception error) {
			response.reset() ;
			response.setContentType("application/xml");
			response.setHeader("Cache-Control", "no-cache"); 
			response.setCharacterEncoding("UTF8") ;

			response.getWriter().append("error");
		}
	}

}
