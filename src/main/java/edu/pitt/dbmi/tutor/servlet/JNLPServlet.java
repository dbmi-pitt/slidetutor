package edu.pitt.dbmi.tutor.servlet;

import java.io.IOException;
import java.util.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.pitt.dbmi.tutor.util.TextHelper;

public class JNLPServlet extends HttpServlet {
	public void init( ServletConfig config ) throws ServletException {
		super.init( config );
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }
    
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }
	
	 private void processRequest(HttpServletRequest req,HttpServletResponse res) throws ServletException, IOException {
		 String jnlp = req.getParameter("jnlp");
		 if(jnlp != null){
			 // get parameters into map
			 Map<String,String> map = new HashMap<String,String>();
			 for(Enumeration<String> e = req.getParameterNames();e.hasMoreElements();){
				 String key = e.nextElement();
				 if(!"jnlp".equals(key))
					 map.put(key,filter(req.getParameter(key)));
			 }
			 // get text of web launcher
			 try{
				 String text = TextHelper.getText(getClass().getResourceAsStream("/resources/jnlp/"+jnlp),map);
				 res.setContentType("application/x-java-jnlp-file;");
		         res.getWriter().println(text);
		     }catch(Exception ex){
		    	 res.setContentType("text/plain;");
		    	 res.getWriter().println("error executing jnlp with: "+map);
		    	 ex.printStackTrace(res.getWriter());
		     }
		 }else{
			 res.setContentType("text/plain;");
	         res.getWriter().println("error");
	     }
		 res.getWriter().close(); 
	 }
	 

	/**
	  * return filtered string
	  * @param s
	  * @return
	  */
	 private String filter(String str){
		 if(str == null)
			 return null;
		 return str.replaceAll("[^\\w\\s/\\-\\.&\\?:=]","");
	 }
}
