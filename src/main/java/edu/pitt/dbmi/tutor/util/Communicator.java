package edu.pitt.dbmi.tutor.util;

import java.io.*;
import java.net.*;
import java.sql.DriverManager;
import java.util.*;


/**
 * usefull methods for talking to servlets
 * @author tseytlin
 */
public class Communicator {
	private static URL servlet;
	private static boolean connected, authenticated;
	
	/**
	 * get servlet URL
	 * @return
	 */
	public static URL getServletURL(){
		if(servlet == null){
			try{
				String url = Config.getProperty("file.manager.server.url");
				if(url != null){
					servlet = new URL(url);
					connected = true;
				}
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
		return servlet;
	}
	
	/**
	 * authenticate user/password
	 * @param user
	 * @param pass
	 * @return
	 */
	public static boolean authenticateWebsite(Properties props){
		String user   = props.getProperty("repository.username");
		String pass   = props.getProperty("repository.password");
		String inst   = props.getProperty("repository.institution");
		
		//encode password
		String epass = UnixCrypt.crypt("PW",pass);
		
		// build map
    	Map map = new HashMap(5);
		map.put("action","authenticate");
		map.put("user",user);
		map.put("pass",epass);
		map.put("place",inst);
		if(getServletURL() != null){
			try{
				String status = doPost(getServletURL(),map);
				authenticated =  "ok".equalsIgnoreCase(status.trim());
			}catch(IOException ex){
				//ex.printStackTrace();
				authenticated = false;
			}
		}
		return authenticated;
	}
	
	/**
	 * authenticate with database
	 * @param props
	 * @return
	 */
	public static boolean authenticateDatabase(Properties props){
		try{
			String driver = props.getProperty("repository.driver");
			String url    = props.getProperty("repository.url");
			String user   = props.getProperty("repository.username");
			String pass   = props.getProperty("repository.password");
			
			// if driver is missing, we are not in DB mode anyhow
			// so do authenticate it
			if(driver == null)
				return true;
			
			Class.forName(driver).newInstance();
		    DriverManager.getConnection(url,user,pass);
		    return true;
		    
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return false;
	}
	
	
	/**
	 * is connected to file manager servlet
	 * @return
	 */
	public static boolean isConnected(){
		getServletURL();
		return connected && authenticated;
	}
	
	
	/**
	 * get string response from the input (should be text/plain)
	 * @param url
	 * @return
	 */
	public static String doGet(URL url) throws IOException{
		URLConnection conn = url.openConnection();
		// Turn off caching
		conn.setUseCaches(false);
		conn.setConnectTimeout(5000);
		conn.setReadTimeout(5000);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		
		return processTextStream(conn.getInputStream());
	}
	
	
	/**
	 * get string response from the input (should be text/plain)
	 * @param url
	 * @return
	 */
	public static String doGet(URL url, Map map) throws IOException{
		StringBuffer u = new StringBuffer(""+url);
		String s = "?";
		for(Object k : map.keySet()){
			String key = ""+k;
			String val = ""+map.get(k);
			key = key.replaceAll(" ","%20");
			val = val.replaceAll(" ","%20");
			u.append(s+key+"="+val);
			s = "&";
		}
		return doGet(new URL(u.toString()));
	}
	
	
	/**
	 * get string response from the input (should be text/plain)
	 * @param url
	 * @return
	 */
	public static String doPost(URL url, Object obj) throws IOException{
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		
		// Turn off caching
		conn.setUseCaches(false);
		conn.setConnectTimeout(0);
		conn.setReadTimeout(0);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
	    
        // pure serialization
	    ObjectOutputStream objOut = new ObjectOutputStream(conn.getOutputStream());
	    objOut.writeObject(obj);
	    objOut.flush();
	    objOut.close();
       
		// read response
		return processTextStream(conn.getInputStream());
	}
	
	
	/**
	 * process text sream
	 * @param in
	 * @return
	 */
	private static String processTextStream(InputStream in) throws IOException{
		// get input
		StringBuffer buf = new StringBuffer();
	        
        //recieve object
        BufferedReader stream = null;
        try{
            stream = new BufferedReader(new InputStreamReader(in));
            for(String line=stream.readLine(); line != null; line=stream.readLine()){
                buf.append(line+"\n");
            }
        }catch(IOException ex){
            throw ex;
        }finally{
            if(stream != null){
                stream.close();
            }
            if(in != null){
            	in.close();
            }
            	
        }
        return buf.toString();
	}
	
	/**
	 * copy file from one location to another
	 * @param inputFile
	 * @param outputFile
	 * @throws IOException
	 */
	public static void copy(InputStream in,OutputStream out) throws IOException {
		try{
		    byte[] buf = new byte[1024];
		    int len;
		    while ((len = in.read(buf)) > 0){
		    	out.write(buf,0,len);
		    }
		}catch(IOException ex){
			throw ex;
		}finally{
			if(in != null)
				in.close();
			if(out != null){
				out.flush();
				out.close();
			}
		}
	}
	
	
    /**
     * Use Toolkit to process simple image
     */
    public static byte [] getData(InputStream in) throws Exception {
    	byte [] data;        //result image
    	byte [] buffer = new byte [4096]; // temp buffer	
		// temp 4k buffer
		ArrayList list = new ArrayList();
		
		// create buffered stream
		BufferedInputStream stream = new BufferedInputStream(in);
		
		// read in all data into buffer
		int size = 0;
		for(size = stream.read(buffer);size > -1;size=stream.read(buffer)){
			byte [] temp = new byte [size];
		    System.arraycopy(buffer,0,temp,0,size);
		    list.add(temp);
		}
		// close stream
		stream.close();
		//determine future data size
		size = 0;
		for(int i=0;i<list.size();i++){
		    size += ((byte []) list.get(i)).length;
		}
		
		//THE data buffer
		data = new byte[size];
		
		// fill data buffer
		for(int offs=0, i=0;i<list.size();i++){
			buffer = (byte []) list.get(i);
		    System.arraycopy(buffer,0,data,offs,buffer.length);
		    offs = offs+buffer.length;
		}
	    return data;
    }
	
    /**
     * upload file to the server
     * @return
     */
    public static boolean upload(File f, String root, String path) throws IOException{
    	if(!isConnected())
    		return false;
    	
    	// build map
    	String user   = Config.getProperty("repository.username");
		String pass   = Config.getProperty("repository.password");
		
		//encode password
		String epass = UnixCrypt.crypt("PW",pass);
    	
    	Map map = new HashMap(5);
		map.put("action","upload");
		map.put("root",root);
		map.put("path",path+"/"+f.getName());
		map.put("user",user);
		map.put("pass",epass);
		
		try{
			map.put("data",getData(new FileInputStream(f)));
		}catch(Exception ex){
			ex.printStackTrace();
		}
		String status = doPost(getServletURL(),map);
		System.out.println(status);
		if(status.startsWith("ok"))
			return true;
		System.err.println("Error: problem uploading file "+f+" cause: "+status);
		return false;
    }
    
    /**
     * upload file to the server
     * @return
     */
    public static boolean delete(String root, String path) throws IOException{
    	if(!isConnected())
    		return false;
    	
    	// build map
    	String user   = Config.getProperty("repository.username");
		String pass   = Config.getProperty("repository.password");
		
		//encode password
		String epass = UnixCrypt.crypt("PW",pass);
    	
    	Map map = new HashMap(5);
		map.put("action","delete");
		map.put("root",root);
		map.put("path",path);
		map.put("user",user);
		map.put("pass",epass);
		
		String status = doPost(getServletURL(),map);
		if(status.startsWith("ok"))
			return true;
		return false;
    }
    
	/**
	 * URL exists
	 * http://www.rgagnon.com/javadetails/java-0059.html
	 * @param URLName
	 * @return
	 */
	public static boolean exists(URL URLName) {
		try {
			HttpURLConnection.setFollowRedirects(false);
			// note : you may also need
			// HttpURLConnection.setInstanceFollowRedirects(false)
			HttpURLConnection con = (HttpURLConnection) URLName.openConnection();
			con.setRequestMethod("HEAD");
			return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
		} catch (Exception e) {
			// e.printStackTrace();
			return false;
		}
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		URL servlet = new URL("http://1upmc-opi-xip02.upmc.edu:92/domainbuilder/servlet/FileManagerServlet");
		System.out.println(doGet(new URL(servlet+"?action=list&root=image")));
		File file = new File("/home/tseytlin/Pictures/Scenery/92060.jpg");
		System.out.println(file.exists());
		Map map = new HashMap(5);
		map.put("action","upload");
		map.put("root","owl");
		map.put("path",file.getName());
		try{
			map.put("data",getData(new FileInputStream(file)));
		}catch(Exception ex){
			ex.printStackTrace();
		}
		System.out.println(doPost(servlet,map));
		
	}

}
