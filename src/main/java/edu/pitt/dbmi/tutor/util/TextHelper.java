package edu.pitt.dbmi.tutor.util;

import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.ontology.IClass;
import edu.pitt.ontology.ILogicExpression;
import edu.pitt.ontology.IProperty;
import edu.pitt.ontology.IRestriction;
import edu.pitt.slideviewer.ViewPosition;
import edu.pitt.text.tools.TextTools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.geom.Line2D;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.tree.TreePath;

public class TextHelper {
	private static Map<Character,String> escapeCodeMap;
	
	/**
	 * format date object
	 * 
	 * @param date
	 * @return
	 */
	public static String formatDate(Date d) {
		return formatDate(d,"M/d/yyyy HH:mm:ss");
	}

	/**
	 * format date object
	 * 
	 * @param date
	 * @return
	 */
	public static String formatDate(Date d, String format) {
		if (d != null) {
			SimpleDateFormat fm = new SimpleDateFormat(format);
			return fm.format(d);
		}
		return "";
	}
	
	
	/**
	 * parse date from string
	 * @param input
	 * @param format
	 * @return
	 */
	
	public static Date parseDate(String input){
		if(input == null)
			return null;
		// if input is a number, then parse as millis
		if(input.matches("\\d+"))
			return new Date(Long.parseLong(input));
		// else parse as string
		return parseDate(input,"M/d/yyyy HH:mm:ss");
	}
	
	/**
	 * parse date from string
	 * @param input
	 * @param format
	 * @return
	 */
	
	public static Date parseDate(String input,String format){
		if(!TextHelper.isEmpty(input) && !TextHelper.isEmpty(format)){
			try{
				SimpleDateFormat fm = new SimpleDateFormat(format);
				return fm.parse(input);
			}catch(ParseException ex){}
		}
		return null;
	}
	
	/**
	 * format duration of time in millis
	 * 
	 * @param time
	 * @return
	 */
	public static String formatDuration(long duration) {
		// default condition
		if (duration <= 0)
			return "";

		// convert ms to minutes and seconds
		int sec = (int) duration / 1000;
		int min = (sec > 0) ? sec / 60 : 0;
		sec = sec - min * 60;
		return (min > 0) ? min + "m " + sec + "s" : sec + "s";
	}

	/**
	 * get pretty printed HTML string for tooltips and stuff
	 * 
	 * @param string
	 *            to be formated
	 */
	public static String formatString(String text) {
		return formatString(text, 40);
	}

	/**
	 * get pretty printed HTML string for tooltips and stuff
	 * 
	 * @param string
	 *            to be formated
	 * @param length
	 *            of single line
	 */
	public static String formatString(String text, int limit) {
		int charLimit = limit;
		StringBuffer def = new StringBuffer("<html>");
		// format definition itself
		char[] str = text.toCharArray();
		boolean insideTag = false;
		String tag = "";
		for (int i = 0, k = 0; i < str.length; i++) {
			def.append(str[i]);
			if (str[i] == '\n' || (k > charLimit && str[i] == ' ')) {
				def.append("<br>");
				k = 0;
			}
			// increment char limit counter
			// skip HTML tags (assume that < is always closed by >
			if (str[i] == '<') {
				insideTag = true;
			} else if (str[i] == '>') {
				insideTag = false;
				k--;
				// check tag, if it is a newline tag
				// then reset the counter
				if (tag.equalsIgnoreCase("<hr") || tag.equalsIgnoreCase("<br") || tag.equalsIgnoreCase("<p"))
					k = 0;
				tag = "";
			}

			if (!insideTag)
				k++;
			else
				tag = tag + str[i];
		}
		return def.toString();
	}

	/**
	 * This method gets a text file (HTML too) from input stream reads it, puts
	 * it into string and substitutes keys for values in from given map
	 * 
	 * @param InputStream
	 *            text input
	 * @return String that was produced
	 * @throws IOException
	 *             if something is wrong WARNING!!! if you use this to read HTML
	 *             text and want to put it somewhere you should delete newlines
	 */
	public static String getText(InputStream in) throws IOException {
		return getText(in, null);
	}

	/**
	 * This method gets a text file (HTML too) from input stream reads it, puts
	 * it into string and substitutes keys for values in from given map
	 * 
	 * @param InputStream
	 *            text input
	 * @param Map
	 *            key/value substitution (used to substitute paths of images for
	 *            example)
	 * @return String that was produced
	 * @throws IOException
	 *             if something is wrong WARNING!!! if you use this to read HTML
	 *             text and want to put it somewhere you should delete newlines
	 */
	public static String getText(InputStream in, Map<String, String> sub) throws IOException {
		if(in == null)
			return "";
		
		StringBuffer strBuf = new StringBuffer();
		BufferedReader buf = new BufferedReader(new InputStreamReader(in));
		try {
			for (String line = buf.readLine(); line != null; line = buf.readLine()) {
				strBuf.append(line.trim() + "\n");
			}
		} catch (IOException ex) {
			throw ex;
		} finally {
			buf.close();
		}
		// we have our text
		String text = strBuf.toString();
		// do substitution
		if (sub != null) {
			for (String key : sub.keySet()) {
				text = text.replaceAll(key, sub.get(key));
			}
		}
		return text;
	}

	/**
	 * extract section text from full report
	 * 
	 * @param section
	 * @param entire
	 *            report text
	 * @return
	 */
	public static String getSectionText(String section, String text) {
		String str = "";
		Pattern pt = Pattern.compile("^" + section + ":$(.*?)^[A-Z ]+:$", Pattern.MULTILINE | Pattern.DOTALL);
		Matcher mt = pt.matcher(text);
		if (mt.find()) {
			str = mt.group(1);
		} else {
			pt = Pattern.compile("^" + section + ":$(.*)", Pattern.MULTILINE | Pattern.DOTALL);
			mt = pt.matcher(text);
			if (mt.find())
				str = mt.group(1);
		}

		// if we fail here maybe sections are marked differently in this text
		if(str.length() == 0){
			pt = Pattern.compile("^\\[" + section + "\\]$(.*?)^(\\[[A-Z ]+\\]|[A-Z]+:)$", Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
			mt = pt.matcher(text);
			if (mt.find()) {
				str = mt.group(1);
			} else {
				pt = Pattern.compile("^\\["+ section + "\\]$(.*)", Pattern.MULTILINE | Pattern.DOTALL  | Pattern.CASE_INSENSITIVE);
				mt = pt.matcher(text);
				if (mt.find())
					str = mt.group(1);
			}
		}
		
		
		
		// strip last end-of-line
		if (str.endsWith("\n"))
			str = str.substring(0, str.length() - 1);

		return str;
	}

	/**
	 * get character count from string
	 * 
	 * @param str
	 * @return
	 */
	public static int getSequenceCount(String text, String str) {
		int count = 0;
		for (int i = text.indexOf(str); i > -1; i = text.indexOf(str, i + 1)) {
			count++;
		}
		return count;
	}

	/**
	 * get properties from string
	 * 
	 * @param text
	 * @return
	 */
	public static Properties getProperties(String text) {
		Properties p = new Properties();
		if (text != null) {
			try {
				p.load(new ByteArrayInputStream(text.getBytes()));
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return p;
	}

	/**
	 * parse list that is represented, by java list dump
	 * 
	 * @param str
	 * @return
	 */
	public static String[] parseList(String str) {
		if (str == null)
			return new String[0];
		str = str.trim();
		if (str.startsWith("["))
			str = str.substring(1);
		if (str.endsWith("]"))
			str = str.substring(0, str.length() - 1);
		if (str.length() == 0 || str.equals("null"))
			return new String[0];
		String[] slist = str.split(",");
		for (int i = 0; i < slist.length; i++)
			slist[i] = slist[i].trim();
		return slist;
	}

	/**
	 * parse list that is represented, by java list dump
	 * 
	 * @param str
	 * @return
	 */
	public static Map<String, String> parseMap(String str) {
		Map<String, String> map = new HashMap<String, String>();

		if (str == null)
			return map;
		str = str.trim();
		if (str.startsWith("{"))
			str = str.substring(1);
		if (str.endsWith("}"))
			str = str.substring(0, str.length() - 1);
		if (str.length() == 0 || str.equals("null"))
			return map;
		for (String s : str.split(",")) {
			String[] p = s.split("=");
			if (p.length == 2) {
				String key = p[0].trim();
				String val = p[1].trim();
				if (!key.equals("null") && !val.equals("null"))
					map.put(key, val);
			}
		}
		return map;
	}

	/**
	 * parse input
	 * 
	 * @param str
	 * @return
	 */
	public static Object parseMessageInput(String str) {
		if(str == null)
			return null;
		Pattern pt = Pattern.compile("\\(([\\w\\-\\s\\.]+)\\s+\"(.*?)\"\\)");
		Matcher mt = pt.matcher(str);
		Map<String, String> map = new LinkedHashMap<String, String>();
		while (mt.find()) {
			map.put(mt.group(1).trim(), mt.group(2).trim());
		}
		// if input is only one entry and key is input, return it
		if (map.size() == 1 && map.containsKey("input"))
			return map.get("input");
		return map;
	}

	/**
	 * get input stream for object
	 * 
	 * @param text
	 * @return
	 */
	public static InputStream toInputStream(Object text) {
		return new ByteArrayInputStream(text.toString().getBytes());
	}

	/**
	 * convert object to string
	 * 
	 * @param obj
	 * @return
	 */
	public static String toString(Object obj) {
		if (obj == null || isEmpty(""+obj))
			return "";

		if (obj instanceof ConceptEntry)
			return ((ConceptEntry) obj).getText();

		// display map or collection
		if (obj instanceof Collection || obj instanceof Map) {
			String s = "" + obj;
			s = s.substring(1, s.length() - 1);

			// remove nulls from string
			s = s.replaceAll("\\bnull\\b", "");

			return s;
		}
		
		// display arrays
		if (obj instanceof Object []) {
			String s = Arrays.toString((Object [])obj);
			s = s.substring(1, s.length() - 1);
			
			// remove nulls from string
			s = s.replaceAll("\\bnull\\b", "");
			
			return s;
		}

		// display TreePath
		if(obj instanceof TreePath){
			String s = ""+obj;
			return s.substring(1, s.length() - 1);
		}
		
		
		return obj.toString();
	}

	/**
	 * convert a list of concept entries to text
	 * 
	 * @param e
	 * @return
	 */
	public static String toText(List<ConceptEntry> e) {
		List<String> s = new ArrayList<String>();
		for (ConceptEntry c : e)
			s.add(toString(c));
		return toString(s);
	}

	/**
	 * create URI from string
	 * 
	 * @param s
	 * @return
	 */

	public static URI toURI(String s) {
		try {
			return new URI(s.replaceAll("\\s", "%20"));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Map<Character,String> getURLEscapeCode(){
		if(escapeCodeMap == null){
			escapeCodeMap = new HashMap<Character, String>();
			escapeCodeMap.put(' ',"%20");
			escapeCodeMap.put('<',"%3C");
			escapeCodeMap.put('>',"%3E");
			escapeCodeMap.put('#',"%23");
			escapeCodeMap.put('%',"%25");
			escapeCodeMap.put('{',"%7B");
			escapeCodeMap.put('}',"%7D");
			escapeCodeMap.put('|',"%7C");
			escapeCodeMap.put('\\',"%5C");
			escapeCodeMap.put('^',"%5E");
			escapeCodeMap.put('~',"%7E");
			escapeCodeMap.put('[',"%5B");
			escapeCodeMap.put(']',"%5D");
			escapeCodeMap.put('`',"%60");
			escapeCodeMap.put(';',"%3B");
			escapeCodeMap.put('/',"%2F");
			escapeCodeMap.put('?',"%3F");
			escapeCodeMap.put(':',"%3A");
			escapeCodeMap.put('@',"%40");
			escapeCodeMap.put('=',"%3D");
			escapeCodeMap.put('&',"%26");
			escapeCodeMap.put('$',"%24");
		}
		return escapeCodeMap;
	}
	
	
	/**
	 * URL escape filter
	 * @param s
	 * @return
	 */
	public static String escape(String s){
		StringBuffer str = new StringBuffer();
		Map<Character,String> m = getURLEscapeCode();
		for(char x: s.toCharArray()){
			if(m.containsKey(x))
				str.append(m.get(x));
			else
				str.append(x);
		}
		return str.toString();
	}
	
	
	/**
	 * return a unique set of string values from given list
	 * 
	 * @param list
	 * @return
	 */
	public static Set<String> getValues(List list) {
		Set<String> values = new LinkedHashSet<String>();
		for (Object obj : list) {
			String s = (String) obj;
			if (s != null && s.length() > 0)
				values.add(s);
		}
		return values;
	}

	/**
	 * extract tags that are defined in text. a tug looks like: <tag> or [tag]
	 * 
	 * @param text
	 * @return
	 */
	public static List<String> getTextTags(String text) {
		List<String> tags = new ArrayList<String>();
		Pattern pt = Pattern.compile("(<\\w+>|\\[\\w+\\])");
		Matcher mt = pt.matcher(text);
		while (mt.find()) {
			if(mt.group().length() > 3)
				tags.add(mt.group());
		}
		return tags;
	}

	/**
	 * parse rectangle string in whatever format
	 * 
	 * @param str
	 * @return
	 */
	public static Rectangle parseRectangle(String str) {
		try {
			int i = 0;
			int[] r = new int[4];
			for (String s : str.split("[^\\d]+")) {
				if (s.length() > 0 && i < r.length) {
					r[i++] = Integer.parseInt(s);
				}
			}
			return new Rectangle(r[0], r[1], r[2], r[3]);
		} catch (Exception ex) {
			// log.severe("can't parse value "+value+" of property "+k+" Cause: "+exe.getMessage());
		}
		return new Rectangle(0, 0, 0, 0);
	}

	/**
	 * parse line
	 * @param str
	 * @return
	 */
	public static Line2D parseLine(String str){
		Rectangle r = parseRectangle(str);
		Point e     = new Point(r.width,r.height);
		return new Line2D.Double(r.getLocation(),e);
	}
	
	/**
	 * parse view position string in whatever format
	 * 
	 * @param str
	 * @return
	 */
	public static ViewPosition parseViewPosition(Map<String,String> map) {
		try{
			int x = Integer.parseInt(map.get("x"));
			int y = Integer.parseInt(map.get("y"));
			int w = Integer.parseInt(map.get("width"));
			int h = Integer.parseInt(map.get("height"));
			double s = Double.parseDouble(map.get("scale"));
			return new ViewPosition(x,y,w,h,s);
		}catch(Exception ex){}
		return new ViewPosition(0,0,0);
	}
	
	/**
	 * parse view position string in whatever format
	 * 
	 * @param str
	 * @return
	 */
	public static ViewPosition parseViewPosition(String str) {
		try {
			int i = 0;
			int x = 0;
			int y = 0;
			float z = 0;
			for (String s : str.split("[^\\d\\.]+")) {
				if (s.length() > 0) {
					if(0 == i)
						x = Integer.parseInt(s);
					else if(1 == i)
						y = Integer.parseInt(s);
					else if(2 == i)
						z = Float.parseFloat(s);
					i++;
				}
			}
			return new ViewPosition(x,y, z);
		} catch (Exception ex) {
			// log.severe("can't parse value "+value+" of property "+k+" Cause: "+exe.getMessage());
		}
		return new ViewPosition(0, 0, 0.0);
	}
	
	
	/**
	 * parse integer
	 * @param value
	 * @return
	 */
	public static Dimension parseDimension(String value){
		// check for max value for size
		if(value.toLowerCase().startsWith("max")){
			return Toolkit.getDefaultToolkit().getScreenSize();
		}
		try{
        	int i = 0;
    		int [] wh = new int [2];
    	
    		// split any string into potential rgb
    		for(String n : value.split("[^\\d]")){
    			if(n.length() > 0 && i < wh.length){
    				 wh[i++] = Integer.parseInt(n);
    			}
    		}
    		
    		return new Dimension(wh[0],wh[1]);
    	}catch(Exception exe){
    		//log.severe("can't parse value "+value+" of property "+k+" Cause: "+exe.getMessage());
    	}
		return new Dimension(0,0);
	}
	
	/**
	 * create a pretty expression for diagnostic rules
	 * 
	 * @param exp
	 * @return
	 */
	public static String formatExpression(ILogicExpression exp) {
		StringBuffer buffer = new StringBuffer();
		int count = 1;
		if (exp.getExpressionType() == ILogicExpression.OR)
			count = exp.size();
		buffer.append("<table width=500 border=0><tr>");
		for (int i = 0; i < count; i++) {
			ILogicExpression e = (count > 1) ? (ILogicExpression) exp.get(i) : exp;
			buffer.append("<td valign=top>");
			if (!e.isEmpty()) {
				for (Object o : e) {
					buffer.append(formatParameter(o) + "<br>");
				}
				// strip last newline
				buffer.replace(buffer.length() - 8, buffer.length(), "");
			}
			buffer.append("</td>");
		}
		buffer.append("</tr></table>");
		return buffer.toString();
	}

	/**
	 * create a pretty expression for diagnostic rules
	 * 
	 * @param exp
	 * @return
	 */
	private static String formatParameter(Object obj) {
		if (obj instanceof IRestriction) {
			IRestriction r = (IRestriction) obj;
			IProperty p = r.getOntology().getProperty(OntologyHelper.HAS_NO_FINDING);
			String s = formatParameter(r.getParameter());
			if (r.getProperty().equals(p))
				return "NO " + s;
			return s;
		} else if (obj instanceof IClass) {
			return UIHelper.getTextFromName(((IClass) obj).getName());
		} else if (obj instanceof ILogicExpression) {
			StringBuffer b = new StringBuffer();
			ILogicExpression exp = (ILogicExpression) obj;
			String type = getExpressionTypAsString(exp);
			for (Object p : exp) {
				b.append(formatParameter(p) + " " + type + " ");
			}
			String text = b.toString();
			return (text.endsWith(type + " ")) ? text.substring(0, text.length() - type.length() - 2) : text;
		}
		return "" + obj;
	}

	private static String getExpressionTypAsString(ILogicExpression exp) {
		if (exp.getExpressionType() == ILogicExpression.AND)
			return "AND";
		else if (exp.getExpressionType() == ILogicExpression.OR)
			return "OR";
		else if (exp.getExpressionType() == ILogicExpression.NOT)
			return "NOT";
		return "";
	}

	/**
	 * is power 1 smaller then power 2
	 * 
	 * @param p1
	 * @param p2
	 * @return
	 */
	public static boolean isSmallerPower(String p1, String p2) {
		// take care of defaults
		if (p1 == null || p2 == null) {
			return false;
		}

		// check power
		if ("low".equalsIgnoreCase(p1)) {
			return "medium".equalsIgnoreCase(p2) || "high".equalsIgnoreCase(p2);
		} else if ("medium".equalsIgnoreCase(p1)) {
			return "high".equalsIgnoreCase(p2);
		}
		return false;
	}

	/**
	 * check if string is empty (take care of null)
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isEmpty(String str) {
		return str == null || str.length() == 0 || "null".equals(str);
	}

	/**
	 * get the name part of a URL
	 * 
	 * @param url
	 * @return
	 */
	public static String getName(String url) {
		String name = url;

		if(name == null)
			return null;
		
		// parse name
		int i = name.lastIndexOf("/");
		if (i > -1) {
			name = name.substring(i + 1);
		}

		// strip the query part of the name
		i = name.lastIndexOf("?");
		if (i > -1)
			name = name.substring(0, i);

		// strip the suffix
		i = name.lastIndexOf(".");
		if (i > -1) {
			name = name.substring(0, i);
		}

		return name;
	}

	/**
	 * convert regular text report to HTML
	 * 
	 * @param txt
	 * @return
	 */
	public static String convertToHTML(String txt) {
		return (txt + "\n").replaceAll("\n", "<br>").replaceAll("(^|<br>)([A-Z ]+:)<br>", "$1<b>$2</b><br>").
							replaceAll("(^|<br>)(\\[[A-Za-z ]+\\])<br>", "$1<b>$2</b><br>");
	}

	/**
	 * This function attempts to convert vaires types of input into numerical
	 * equivalent
	 */
	public static double parseDecimalValue(String text) {
		double value = 0;
		if(text == null)
			return value;
		
		// check if this is a float
		if (text.matches("\\d+\\.\\d+")) {
			// try to parse regular number
			try {
				value = Double.parseDouble(text);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {
			value = parseIntegerValue(text);
		}
		return value;
	}

	/**
	 * This function attempts to convert vaires types of input into numerical
	 * equivalent
	 */
	public static int parseIntegerValue(String text) {
		int value = 0;

		// try to parse roman numerals
		if (text.matches("[IiVvXx]+")) {
			boolean oneLess = false;
			for (int i = 0; i < text.length(); i++) {
				switch (text.charAt(i)) {
				case 'i':
				case 'I':
					value++;
					oneLess = true;
					break;
				case 'v':
				case 'V':
					value += ((oneLess) ? 3 : 5);
					oneLess = false;
					break;
				case 'x':
				case 'X':
					value += ((oneLess) ? 8 : 10);
					oneLess = false;
					break;
				}
			}

			return value;
		}
		// try to parse words
		if (text.matches("[a-zA-Z]+")) {
			if (text.equalsIgnoreCase("zero"))
				value = 0;
			else if (text.equalsIgnoreCase("one"))
				value = 1;
			else if (text.equalsIgnoreCase("two"))
				value = 2;
			else if (text.equalsIgnoreCase("three"))
				value = 3;
			else if (text.equalsIgnoreCase("four"))
				value = 4;
			else if (text.equalsIgnoreCase("five"))
				value = 5;
			else if (text.equalsIgnoreCase("six"))
				value = 6;
			else if (text.equalsIgnoreCase("seven"))
				value = 7;
			else if (text.equalsIgnoreCase("eight"))
				value = 8;
			else if (text.equalsIgnoreCase("nine"))
				value = 9;
			else if (text.equalsIgnoreCase("ten"))
				value = 10;
			else if (text.equalsIgnoreCase("eleven"))
				value = 11;
			else if (text.equalsIgnoreCase("twelve"))
				value = 12;
			else
				value = (int) OntologyHelper.NO_VALUE;

			return value;
		}

		// try to parse regular number
		try {
			value = Integer.parseInt(text);
		} catch (NumberFormatException ex) {
			// ex.printStackTrace();
			return (int) OntologyHelper.NO_VALUE;
		}
		return value;
	}

	/**
	 * is string a number
	 * 
	 * @param text
	 * @return
	 */
	public static boolean isNumber(String text) {
		return text.matches("\\d+(\\.\\d+)?");
	}

	/**
	 * pretty print number as integer or 2 precision float format numeric value
	 * as string
	 * 
	 * @return
	 */
	public static String toString(double numericValue) {
		Formatter f = new Formatter();
		if ((numericValue * 10) % 10 == 0)
			f.format("%d", (int) numericValue);
		else
			f.format("%.2f", numericValue);
		return "" + f.out();
	}
	
	/**
	 * get a useful errror message
	 * @param ex
	 * @return
	 */
	public static String getErrorMessage(Throwable ex){
		if(ex == null)
			return "Null Exception";
		String msg = ex.getClass().getSimpleName()+" : "+ex.getMessage();
		
		//get stack trace element that links it to the cause
		for(StackTraceElement st: ex.getStackTrace()){
			// if we get our thing
			if(st.getClassName().contains("edu.pitt")){
				msg += " : "+st.getFileName()+" ("+st.getLineNumber()+" )";
				break;
			}
		}
		return msg;
	}
	
	/**
     * filter input string
     * @param field
     * @param limit
     * @return
     */
    public static String filter(String field, int limit){
    	if(field == null || limit < 0)
    		return field;
    	int l = field.length();
    	if(l > limit)
    		return field.substring(0,limit)+" ...";
    	return field;
    }
    
    
    /**
     * compare two strings that might have numbers at the end
     * @param a
     * @param b
     * @return
     */
    public static int compare(String a, String b){
    	// take care of null
    	if(a == null)
    		return 1;
    	if(b == null)
    		return -1;
    	
    	Pattern pt = Pattern.compile("(\\w+[A-Za-z_])(\\d+)");
    	Matcher m1 = pt.matcher(a);
    	Matcher m2 = pt.matcher(b);
    	if(m1.matches() && m2.matches()){
    		String t1 = m1.group(1);
    		String t2 = m2.group(1);
    		int x = t1.compareToIgnoreCase(t2);
    		if(x == 0){
    			String n1 = m1.group(2);
    			String n2 = m2.group(2);
    			return Integer.parseInt(n1) - Integer.parseInt(n2);
    		}else
    			return x;
    	}
    	
    	return a.compareToIgnoreCase(b);
    	
    }
    
    /**
	 * create ontology friendly class name
	 * @param name
	 * @return
	 */
	public static String getClassName(String name){
		return name.trim().replaceAll("\\s*\\(.+\\)\\s*","").replaceAll("[^\\w\\-]","_").replaceAll("_+","_");
	}
	
	/**
	 * Derive prettier version of a class name
	 * @param name
	 * @return
	 */
	public static String getPrettyClassName(String name){
		// if name is in fact URI, just get a thing after hash
		int i = name.lastIndexOf("#");
		if(i > -1){
			name = name.substring(i+1);
		}
				
		// strip prefix (if available)
		i = name.indexOf(":");
		if(i > -1){
			name = name.substring(i+1);
		}
		
		
		// strip suffix
		//if(name.endsWith(OntologyHelper.WORD))
		//	name  = name.substring(0,name.length()-OntologyHelper.WORD.length());
		
		// possible lowercase values to make things look prettier
		if(!name.matches("[A-Z_\\-\\'0-9 ]+") &&
		   !name.matches("[a-z][A-Z_\\-\\'0-9]+[\\w\\-]*")	)
			name = name.toLowerCase();
			
		// now replace all underscores with spaces
		return name.replaceAll("_"," ");
	}
	
	
	/**
	 * create appropriate concept name
	 * @param parent
	 * @return
	 */
	public static String createConceptName(boolean diagnosis,String name){
		// create appropriate class name
		name = getClassName(name);
		if(name.length() == 0)
			return null;
		
		
		if(diagnosis){
			name = name.toUpperCase();
		}else{
			// if single-word and mixed case, then leave it be
			// this excludes weird names like pT2a or pMX
			if(!name.matches("[a-z]+[A-Z]+[a-z0-9]*")){
				// do camelBack notation
				StringBuffer nm = new StringBuffer();
				for(String n: name.toLowerCase().split("_")){
					String w = (TextTools.isStopWord(n))?n:(""+n.charAt(0)).toUpperCase()+n.substring(1);
					nm.append(w+"_");
				}
				name = nm.toString().substring(0,nm.length()-1);
			}
		}
		return name;
	}
	
	
	/**
	 * get list of some type
	 * @param list
	 * @param obj
	 * @return
	 */
	public static <T> T get(Collection<T> list, T obj){
		for(T x: list){
			if(x.equals(obj))
				return x;
		}
		return null;
	}
	
	/**
	 * compare too objects (factor in null)
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean equals(Object a, Object b){
		return (a == null && b == null) || (a != null && b != null && a.equals(b));
	}
	
	
	
	/**
	 * given an entire list, create a filtered list that takes into account include and exclude filters
	 * @param list
	 * @param include
	 * @param exclude
	 * @return
	 */
	public List getFilteredList(List list, List include, List exclude){
		boolean hasInclude = include != null && !include.isEmpty();
		boolean hasExclude = exclude != null && !exclude.isEmpty();
		
		
		// shortcut if no filters are there
		if(!hasInclude && !hasExclude)
			return list;
	
		// copy into new list
		List flist = new ArrayList();
		
		// check include filters
		for(Object o : list){
			if(!hasInclude || include.contains(o))
				flist.add(o);
		}
		
		// check exclude filters
		if(hasExclude){
			for(ListIterator i=flist.listIterator();i.hasNext();){
				if(exclude.contains(i.next()))
					i.remove();
			}
		}
		
		return flist;
	}
	
}