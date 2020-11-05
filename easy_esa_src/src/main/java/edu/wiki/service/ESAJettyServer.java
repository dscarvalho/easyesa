package edu.wiki.service;


import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import jakarta.servlet.Servlet;


public class ESAJettyServer {
	
	public static void main(String[] args) throws Exception {
		int port;
		String index_path;
		
		try {
			port = Integer.parseInt(args[0]);
		}
		catch (Exception e) {
			port = 8800;
		}
		try {
			index_path = args[1];
		}
		catch (Exception e) {
			index_path = "./index";
		}
		
		Server server = new Server(port);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		context.setInitParameter("new_index_path", index_path);
		context.setInitParameter("index_path", index_path);
		context.setInitParameter("server_path", "esa");
		
		server.setHandler(context);
		 
		context.addServlet(new ServletHolder((Servlet) new MongoESAServlet()),"/esaservice");
		//context.addServlet(new ServletHolder(new ESAServlet()),"/service");
		
		server.start();
		server.join();
	}

}
