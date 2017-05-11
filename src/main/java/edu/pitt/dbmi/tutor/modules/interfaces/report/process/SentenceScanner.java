/**
 * This is a sentence scanner.
 * It breaks down a sentence into noun phrases and then tries to match
 * each noun phrase with one or more concepts using either EVS from NCI
 * or MMTX mechanism
 * Author: Eugene Tseytlin (University of Pittsburgh)
 */

package edu.pitt.dbmi.tutor.modules.interfaces.report.process;

import javax.swing.text.StyleConstants;
import java.awt.Color;
import javax.swing.JLabel;
import java.util.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;

import edu.pitt.dbmi.tutor.modules.interfaces.report.ConceptLabel;
import edu.pitt.dbmi.tutor.modules.interfaces.report.ReportData;
import edu.pitt.dbmi.tutor.modules.interfaces.report.ReportDocument;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.text.tools.TextTools;
import gov.nih.nlm.nls.nlp.textfeatures.*;


/**
 * Scan a sentence of data
 */
public class SentenceScanner implements Runnable {

	private ReportDocument document;
	private Sentence [] sentences;
	//private final int wordTermLimit = 4;
	private int endCharOffset;//, startCharOffset;
	private static int lastCharOffset;
	private boolean useParser;
	//private static Map<String,List<Concept>> expressionList;
	
	// stuf 4 parsing
	private JLabel status;
	private ReportData reportData;
	//private NegEx negex;
	private TextTools textTools;
	//private Terminology terminology;
	private SimpleAttributeSet locked,unlocked, highlight, unhighlight;
	private static boolean provideFeedback = true; 
	
	/*
	static {
		expressionList = new LinkedHashMap<String,List<Concept>>();
		expressionList.put(".*\\b(\\d+\\.\\d+)\\b.*",Collections.singletonList(new Concept(NUMERIC+WORD)));
		expressionList.put(".*\\b(\\d+)\\b.*",Collections.singletonList(new Concept(NUMERIC+WORD)));
	}*/
	
	
	public SentenceScanner(ReportDocument doc,int offset, int length) throws BadLocationException {
		this(doc,offset,length,null);
	}

	/**
	 * Scan single sentence.
	 */	
	public SentenceScanner(ReportDocument doc,int offset, int length, String dsec) throws BadLocationException {
		this(doc,new int [] {offset}, new int [] {length},dsec);
	}
	
	
	/**
	 * Scan multple sentences.
	 */	
	public SentenceScanner(ReportDocument doc,int [] offset, int [] length, String dsec) throws BadLocationException {
		this.document = doc;
		textTools = doc.getTextTools();
		//terminology = doc.getTerminology();
		
		// init 
		reportData = doc.getReportData();
		

		if(reportData != null){
			useParser = Config.getBooleanProperty(reportData.getReportInterface(),"report.use.sentence.parser");
		}
		
	
		
		
		//negex = new NegEx();
		//this.diagnosisSection = (dsec != null)?dsec.equalsIgnoreCase(ReportInitializer.diagnosisHeading):false;
		sentences = new Sentence [offset.length];
		
		// iterate over sentences
		for(int i=0;i<offset.length;i++){
			String text = document.getText(offset[i],length[i]);
			//sentenceSeparator = document.getText(offset+length,1); // this should never throw an exception
			
			// create sentence
			if(text.trim().length() > 0){
				sentences[i] = new Sentence(text,offset[i],offset[i]+length[i]);
				// remember char offsets
				//startCharOffset = offset[0];
				endCharOffset = offset[i]+length[i];
			}	
		}
		
		
		// setup locked attributes
		locked = new SimpleAttributeSet();
		locked.addAttribute("locked",Boolean.TRUE);
		
		unlocked = new SimpleAttributeSet();
		unlocked.addAttribute("locked",Boolean.FALSE);
		
		highlight = new SimpleAttributeSet();
		StyleConstants.setBackground(highlight,Color.yellow);
		
		unhighlight = new SimpleAttributeSet();
		StyleConstants.setBackground(unhighlight,Color.white);
	
		status = doc.getStatusLabel();
	}
	
	
	// indicate that scan is commencing
	private void startScan()throws Exception {
		//System.out.println("locking until "+endCharOffset);
		if(provideFeedback)
			document.setCharacterAttributes(0,endCharOffset,locked,false);
		
		status.setForeground(Color.green);
		status.setText("processing input ...");
		status.setEnabled(true);
		
		//System.out.println("Starting thread "+Thread.currentThread().getName());			
	}
	
	// indicate that scan has finished
	private void stopScan() throws Exception{
		ThreadGroup threads = ReportScanner.getSentenceGroup();
		int numThreads = threads.activeCount();
		//System.out.println("Stopping thread "+Thread.currentThread().getName()+" "+numThreads);
	
		
		// correct (output) current thread count (exclude generic threads)
		// all generic threads start w/ Thread
		if(numThreads > 1){
			Thread t[] = new Thread[numThreads];
			threads.enumerate(t);
			int count = 0;
			for (int i = 0; i < t.length; i++) {
				//if(t[i].getName().startsWith("Thread"))
				//	numThreads --;
				if(t[i].getName().startsWith(ReportScanner.SECTION_THREAD_NAME) ||
				   t[i].getName().startsWith(ReportScanner.SENTENCE_THREAD_NAME))
					count ++;
				//System.out.println("\t"+t[i].getName());
			}
			numThreads = count;
		}
	
		// make sure that the highest last char is unlocked
		synchronized (SentenceScanner.class){
			if(endCharOffset > lastCharOffset)
				lastCharOffset = endCharOffset;
		}
		
		
		// only unlock document when this is the last thread
		if(numThreads <=1){		
			//System.out.println("unlocking until "+lastCharOffset+" vs "+endCharOffset);
			if(provideFeedback)
				document.setCharacterAttributes(0,lastCharOffset,unlocked,false);
			else
				document.setCharacterAttributes(0,lastCharOffset,unhighlight,false);
			
			status.setForeground(Color.black);
			status.setText("not processing");
			status.setEnabled(false);
			synchronized (SentenceScanner.class){
				lastCharOffset = 0;
			}
		}
	}
	
	
	/**
	 * If set to TRUE (default) each scanned section is locked and
	 * labels are attached to each concept.
	 * If set to FALSE, no labels are attached, document is not locked 
	 * (should be locked globaly by calling function) and progress bar
	 * is displayed
	 */
	public static void setProvideFeedback(boolean b){
		provideFeedback = b;	 
	}
	public static boolean getProvideFeedback(){
		return provideFeedback;	 
	}
	/**
	 * This is where everything is done.
	 */
	public void run(){
		try{
			// lock text up to the end of sentence
			startScan();
			for(int i=0;i<sentences.length;i++)
				processSentence(parseSentence(sentences[i]));
		}catch(Exception ex){
			ex.printStackTrace();	
		}finally {
			try{
				stopScan();
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}
		
	/**
	 * Parse a given sentence
	 */		
	private Sentence parseSentence(Sentence s) throws Exception {
		if(s == null)
			return null;
		return (useParser)?textTools.parseSentence(s):s;
	}
	
	/**
	 * extract number of words
	 *
	 private int getWordCount(String str){
		return str.split(" ").length;
	 }
	*/
	
	
	/**
	 * Process parsed document
	 */
	private void processSentence(Sentence sentence) throws Exception{
		if(sentence == null)
			return;
				
		// clear text of any privious markup "stale labels"
		//removeConceptLabels(sentence);
		document.setCharacterAttributes(sentence.getCharOffset(),sentence.getOriginalString().length(),
				ConceptLabel.getRemoveConceptAttribute(),false);
		
		Config.getLogger().fine("Setence: \""+sentence.getTrimmedString()+"\""); //+sentence.getSpan()
		
		// add to protocol
		// break sentence into noun-phrases and try to match each noun-phrase
		//Vector phrases = sentence.getPhrases();
		List messages = new ArrayList();
		/*
		List concepts = new ArrayList();
		for(int i=0;i<phrases.size();i++){
			Phrase p = ((Phrase) phrases.elementAt(i)); //.getNounPhrase();
			if(p != null && p.getTrimmedString() != null){
				concepts.addAll(processPhrase(p,messages));
			}
		}
	
		// now let's run this sentence through NegEx to detect negaion or lack there of
		negex.process(sentence,concepts);
		
		// highlight labels for negation
		//if(provideFeedback)
		for(ConceptLabel lbl: reportData.processNegation(negex,messages)){
			attachConceptLabel(lbl);	
		}
		*/
		
		// highlight phase if i non-interactive mode
		if(!provideFeedback){
			int offset = sentence.getSpan().getBeginCharacter();
			int length = sentence.getSpan().getEndCharacter() - offset; //+1
			document.setCharacterAttributes(offset,length,highlight,false);
		}
		
		// attach all generated labels
		for(ConceptLabel lbl: reportData.processSentence(sentence,messages)){
			attachConceptLabel(lbl);	
		}
		
		
		
		// report any error messages
		//if(messages.length() > 0)
		if(messages != null && messages.size() > 0)
			document.firePropertyChange("BUG",null,messages);
		else
			document.firePropertyChange("CLEAR",null,null);
	}
	
	/**
	 * Does processed negation redefine already parsed and placed concept
	 * @return
	 *
	private boolean doesNegationRedefineConcepts(List concepts, List labels){
		boolean redefine = false;
		//	make sure that new labels don't encrouch on something that was already parsed
		for(int i=0;i<concepts.size();i++){
			KeyEntry key = (KeyEntry) concepts.get(i);
			int start = key.getOffset();
			int end   = start+key.getLength();
			for(int j=0;j<labels.size();j++){
				ConceptLabel lbl = (ConceptLabel) labels.get(j);
				int offs = lbl.getOffset();
				// check if label lies within already parsed key
				if(offs >= start && offs < end)
					return true;
			}
		}
		return redefine;
	}
	*/
	
	
	/**
	 * Process individual term
	 *
	private Collection<Concept> processPhrase(Phrase phrase, List errors) throws Exception {
		String term = phrase.getTrimmedString();
		UIHelper.debug("Phrase: \""+term+"\"");

		// highlight phase if i non-interactive mode
		if(!provideFeedback){
			int offset = phrase.getSpan().getBeginCharacter();
			int length = phrase.getSpan().getEndCharacter() - offset+1;
			document.setCharacterAttributes(offset,length,highlight,false);
		}
			
		// search in lexicon
		List<Concept> keys = lookupConcepts(phrase);
	
		//System.out.println(keys);
		// process phrase
		Collection<ConceptLabel> labels = reportData.processPhrase(keys,errors);
		
		// display labels
		//if(provideFeedback)
		for(ConceptLabel lbl: labels)
			attachConceptLabel(lbl);	
		return keys;
	}
	*/
	
	/**
	 * Remove all attributes from string.
	 *
	private void removeConceptLabels(Sentence s) throws BadLocationException{
		//document.attachInProgress = true;
		int offset = s.getSpan().getBeginCharacter();		
		int length = s.getOriginalString().length();
		List elements = new ArrayList();
		
		// remember any "misspelled" elements
		Element p = document.getParagraphElement(offset);
		for(int i=0;i<p.getElementCount();i++){
			Element e = p.getElement(i);
			int st = e.getStartOffset();
			int en = e.getEndOffset();
			Object  o = e.getAttributes().getAttribute("misspelled"); //"concept" , "misspelled"
			boolean inside  = (offset <= st && en <= s.getSpan().getEndCharacter());
			boolean concept = (o != null && o instanceof Boolean)?((Boolean)o).booleanValue():false;
			if(inside && concept){	
				elements.add(new AttributeContainer(st,en-st,e.getAttributes().copyAttributes()));
			}
		}
		
		// clear everything
		SimpleAttributeSet a = new SimpleAttributeSet(ReportDocument.getNormalTextStyle());
		if(provideFeedback)
			a.addAttributes(locked);
		
		//document.replaceText(offset,length,s.getOriginalString(),a);
		document.setCharacterAttributes(offset,length,a,true);
		
		// now restore attributes
		if(!elements.isEmpty()){
			for(int i=0;i<elements.size();i++){
				AttributeContainer ac = (AttributeContainer) elements.get(i);
				document.setCharacterAttributes(ac.offset,ac.length,ac.attributes,false);
				
				// restore offset pointer
				Object obj = ac.attributes.getAttribute("suggestions");
				if(obj != null && obj instanceof ReportSpellChecker.SpellMenu){
					ReportSpellChecker.SpellMenu menu = (ReportSpellChecker.SpellMenu) obj;
					menu.setOffset(document.createPosition(ac.offset));
				}
			}
		 }
		//document.attachInProgress = false;
	}
	*/
	
	/**
	 * Attach ConceptLabel to text in the document
	 */
	private void attachConceptLabel(ConceptLabel lbl) throws BadLocationException {
		//System.out.println("New Label: "+lbl.getText()+" "+lbl.getOffset()+" "+lbl.getLength());
		//document.attachInProgress = true;
		// it is better to replace whole text, to solve some ComponentView issues
		//document.replaceText(lbl.getOffset(),lbl.getLength(),lbl.getText(),lbl.getAttributeSet());
		document.setCharacterAttributes(lbl.getOffset(),lbl.getLength(),lbl.getAttributeSet(),true);
		lbl.setPosition(document.createPosition(lbl.getOffset()));
		//document.attachInProgress = false;
	}
	
	/**
	 * Helper method to attach a ConceptLabel outside of this class
	 */
	public static void attachConceptLabel(ConceptLabel lbl,ReportDocument document) {
		if(provideFeedback){
			try{
				//document.attachInProgress = true;
				//document.replaceText(lbl.getOffset(),lbl.getLength(),lbl.getText(),lbl.getAttributeSet());
				document.setCharacterAttributes(lbl.getOffset(),lbl.getLength(),lbl.getAttributeSet(),true);
				lbl.setPosition(document.createPosition(lbl.getOffset()));
				//document.attachInProgress = false;
			}catch(BadLocationException ex){
				ex.printStackTrace();	
			}
		}
	}
	/**
 	 * This is a container to store misc attributes and offset
	 */
	private class AttributeContainer{
		public AttributeSet attributes;
		public int offset,length;
		public String text;
		public AttributeContainer(int offs,int len,AttributeSet attr){
			this(offs,len,"",attr);	
		}
		public AttributeContainer(int offs,int len, String txt, AttributeSet attr){
			offset = offs;
			length = len;
			text = txt;
			attributes = attr;
		}
	}
	
	/**
	 * lookup concepts
	 * @param text
	 * @return
	 *
	private List<Concept> lookupConcepts(Phrase text){
		// search in lexicon
		List<Concept> keys = new ArrayList<Concept>();
		try{
			// check out results
			for(Concept c: terminology.search(text.getTrimmedString())){
				c.getText(); // trigger offset calculation
				c.setOffset(c.getOffset()+text.getCharOffset());
				keys.add(c);
				//keys.add(new KeyEntry(c.getText(),c.getCode(),c.getOffset()+text.getCharOffset(),c));
			}
			
			// add expression lookup
			keys.addAll(lookupExpressions(text));
			
		}catch(TerminologyException ex){
			ex.printStackTrace();
		}
		return keys;
	}
	*/
	
	// search blacklist that has REs and find all matches
	/*
	private List<Concept> lookupExpressions(Phrase phrase ) {
		int offset = phrase.getSpan().getBeginCharacter();
		String term = phrase.getOriginalString();
		// iterate over expression
		for(String re: expressionList.keySet()){
			// match regexp from file to
			Pattern p = Pattern.compile(re,Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher( term );
			if ( m.matches() ){
				List<Concept> concepts = expressionList.get(re);
				for(int i=0;i<concepts.size();i++){
					String txt = m.group(i+1);    // THIS BETTER BE THERE,
					Concept c = concepts.get(i);
					c.setText(txt);
					c.setOffset(offset+term.indexOf(txt));
				}
				return concepts;
			}
		}
		return Collections.EMPTY_LIST;
	}
	*/
}


