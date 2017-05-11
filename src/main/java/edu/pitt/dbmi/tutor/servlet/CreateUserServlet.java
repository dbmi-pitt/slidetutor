package edu.pitt.dbmi.tutor.servlet;

import java.io.IOException;
import java.util.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.tanesha.recaptcha.ReCaptchaImpl;
import net.tanesha.recaptcha.ReCaptchaResponse;

import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;

public class CreateUserServlet extends HttpServlet {
	private final String CAPTCHA_PUBLIC_KEY = "6Lef5skSAAAAAIPeT-OsTZiQ-Cao-MbBAycWaBP2";
	private final String CAPTCHA_PRIVATE_KEY = "6Lef5skSAAAAACJVoRCiVfCX8s3b0XMSpmVNIqym";
	
	private ProtocolModule protocol;
	private String experiment = "Deployment";
	
	public void init(ServletConfig config) throws ServletException {
		super.init( config );
		protocol = new DatabaseProtocolModule();
	
		// init parameters
		String dr =  config.getInitParameter("protocol.driver");
		String url = config.getInitParameter("protocol.url");
		String us =  config.getInitParameter("protocol.username");
		String pw =  config.getInitParameter("protocol.password");
		String ex =  config.getInitParameter("protocol.study");
		
		if(dr != null)
			protocol.getDefaultConfiguration().setProperty("protocol.driver",dr);
		if(url != null)
			protocol.getDefaultConfiguration().setProperty("protocol.url",url);
		if(us != null)
			protocol.getDefaultConfiguration().setProperty("protocol.username",us);
		if(pw != null)
			protocol.getDefaultConfiguration().setProperty("protocol.password",pw);
		if(ex != null)
			experiment = ex;
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }
    
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }
	
	private void processRequest(HttpServletRequest req,HttpServletResponse res) throws ServletException, IOException {
		 // get parameters into map
		 Map<String,String> map = new HashMap<String,String>();
		 for(Enumeration<String> e = req.getParameterNames();e.hasMoreElements();){
			 String key = e.nextElement();
			 map.put(key,filter(req.getParameter(key)));
		 }
		 res.setContentType("text/plain;");
		
		 // make sure that right parameters are there
		 if(!isHuman(req))
			 res.getWriter().println("Error: failed humanity test");
		 else if(!map.containsKey("username"))
			 res.getWriter().println("Error: no username");
		 else if(!map.get("username").matches("\\w+"))
			 res.getWriter().println("Error: invalid username");
		 else if(!map.containsKey("password"))
			 res.getWriter().println("Error: no password"); 
		 else if(!map.get("password").equals(map.get("rpassword")))
			 res.getWriter().println("Error: passwords do not match"); 
		 else if(protocol.getUsers(null).contains(map.get("username")))
			 res.getWriter().println("Error: username already exists"); 
		 else{
			 List<String> excludes = Arrays.asList("username","password","rpassword","recaptcha_response_field","recaptcha_challenge_field");
			 Properties p = new Properties();
			 for(String key: map.keySet()){
				 if(!excludes.contains(key))
					 p.put(key,map.get(key));
			 }
			 if(!protocol.getExperiments().contains(experiment))
				 protocol.addExperiment(experiment);
			 protocol.addUser(map.get("username"),map.get("password"),experiment, p);
			 res.getWriter().println("success!"); 
		 }
		 res.getWriter().close(); 
	 }
	
	
	/**
	 * check if input is human
	 * @param req
	 * @return
	 */
	private boolean isHuman(HttpServletRequest req){
		 String remoteAddr = req.getRemoteAddr();
         ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
         reCaptcha.setPrivateKey(CAPTCHA_PRIVATE_KEY);
	     String challenge = req.getParameter("recaptcha_challenge_field");
	     String uresponse = req.getParameter("recaptcha_response_field");
	     ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer(remoteAddr, challenge, uresponse);
	     return reCaptchaResponse.isValid();
	}
	

	/**
	  * return filtered string
	  * @param s
	  * @return
	  */
	 private String filter(String str){
		 if(str == null)
			 return null;
		 return str.replaceAll("[^\\w\\s/\\-\\.&\\?]","");
	 }
}

