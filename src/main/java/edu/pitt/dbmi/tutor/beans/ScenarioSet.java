/**
 * This class represents a set of all hints and error messages
 * Author: Eugene Tseytlin (University of Pittsburgh)
 */

package edu.pitt.dbmi.tutor.beans;


import java.util.*;
import java.io.*;


import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import edu.pitt.dbmi.tutor.util.OrderedMap;
import edu.pitt.dbmi.tutor.util.UIHelper;

/**
 * This class represents a set of scenario objects that encompasses
 * the entire help system
 * @author Administrator
 *
 */
public class ScenarioSet  implements Serializable {
	// list of all messages
	private OrderedMap<String,ScenarioEntry> scenarios  = new OrderedMap<String, ScenarioEntry>(); 
	// list of all tags
	private List<String> tags	= new ArrayList<String>();
	

	/**
	 * add new scenario entry
	 * @param e
	 */
	public void addScenarioEntry(ScenarioEntry e){
		scenarios.put(e.getName(),e);
	}
	
	/**
	 * add new scenario entry
	 * @param e
	 */
	public void removeScenarioEntry(ScenarioEntry e){
		scenarios.remove(e.getName());
	}
	
	/**
	 * get scenario entry for given id
	 * @param id
	 * @return
	 */
	public ScenarioEntry getScenarioEntry(String id){
		if(id == null)
			return null;
		return scenarios.get(id);	
	}
	
	/**
	 * Returns the value of tags.
	 */
	public List<String> getTags(){
		return tags;
	}

	/**
	 * Sets the value of tags.
	 * @param tags The value to assign tags.
	 */
	public void addTag(String tag){
		this.tags.add(tag);
	}

	/**
	 * get access to node map
	 */
	public OrderedMap<String,ScenarioEntry> getScenarioMap(){
		return scenarios;	 
	}
	
	/**
	 * get values
	 * @return
	 */
	public List<ScenarioEntry> getScenarioEntries(){
		return scenarios.getValues();
	}
	
	
	/**
	 * get DOM element that represents this object
	 * @return
	 */
	public Element createElement(Document doc){
		Element root = doc.createElement("ScenarioSet");
		
		// write out each scenario
		for(ScenarioEntry e: getScenarioEntries()){
			root.appendChild(e.createElement(doc));
		}
		
		return root;
	}
	
	
	/**
	 * load scenario set from an input stream
	 * @param is
	 * @throws IOException
	 */
	public void load(InputStream in) throws IOException {
		try {
			Document document = UIHelper.parseXML(in);
			
			//print out some useful info
			Element element = document.getDocumentElement();
			
			// iterate through cases
			NodeList list = element.getElementsByTagName("ScenarioEntry");
			for(int i=0;i<list.getLength();i++){
				Node node = list.item(i);
				if(node instanceof Element){
					ScenarioEntry entry = new ScenarioEntry();
					entry.parseElement((Element)node);
					addScenarioEntry(entry);
				}
			}
			
		} catch (Exception ex) {
			throw new IOException("Error: problem parsing scenario set XML",ex);
		}
	}
	
	/**
	 * load scenario set from an input stream
	 * @param is
	 * @throws IOException
	 */
	public void save(OutputStream os) throws IOException {
		try{
			// initialize document and root
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document doc = factory.newDocumentBuilder().newDocument();
			
			// create DOM object
			doc.appendChild(createElement(doc));
			
			// write out XML
			UIHelper.writeXML(doc, os);
		}catch(Exception ex){
			ex.printStackTrace();
			throw new IOException(ex.getMessage());
		}
	}
}
