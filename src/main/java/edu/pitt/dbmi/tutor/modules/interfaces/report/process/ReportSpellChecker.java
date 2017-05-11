/**
 * This process scans each sentence using SentenceScanner.
 * Author: Eugene Tseytlin (University of Pittsburgh)
 */

package edu.pitt.dbmi.tutor.modules.interfaces.report.process;

import javax.swing.text.Position;
import java.awt.Container;
import javax.swing.event.*;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.pitt.dbmi.tutor.modules.interfaces.report.ReportDocument;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.text.tools.TextTools;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.Color;
import java.awt.event.*;

/**
 * This processor chunks input text into sentences (depending on section)
 * It then parses each sentence with NounPhrase parser and looks up the meaning of each noun 
 * phrase using EVS or UMLS Metathesourus and matches it to Domain Ontology 
 */
public class ReportSpellChecker implements ReportProcessor, ActionListener {
	private int lastOffset = 0;	
	private ReportDocument doc;
	private SimpleAttributeSet attr;
	private HashMap misspelled;
	private List ignored;
	private boolean typing;     // is typing/deleteing in progress 
	private final String NO_SUGGESTIONS = "no suggestions";
	
	
	//GUI components of SpellChecking dialog
	private JDialog dialog = null;
	private JButton change,ignore,cancel;
	private JTextField changeTo,badword;
	private JList suggestions;
	private Frame parent; 
	private int badwordOffset; // keep offset of misspelled word during checking
	private Object wordCorrected = new Boolean(true); //this is an object used for locking
	
	// spell checking is done in 2 modes: selection and document
	private int spellMode = 0; 
	public static final int SELECTION_MODE = 0;
	public static final int DOCUMENT_MODE = 1;
	private TextTools textTools;
	
	
	// constructor
	public ReportSpellChecker(ReportDocument doc){
		this.doc = doc;
		parent = doc.getFrame();
		
		// create attribute to mark misspelled words
		attr = new SimpleAttributeSet(ReportDocument.getNormalTextStyle());
		attr.addAttribute("misspelled",Boolean.TRUE);
		attr.addAttribute("wavyUnderline",Boolean.TRUE);
		attr.addAttribute("wavyColor",Color.red);
		//StyleConstants.setUnderline(attr,true);
		//StyleConstants.setForeground(attr,Color.orange.darker());
		
		// create a place where missplled words are stored
		misspelled = new HashMap();
		ignored = new ArrayList();
		
		textTools = doc.getTextTools();
	}
	
		
	// insert string
	public void insertString(int offs, String str, String section){
		// flag that typing is not in progress for caret
		//typing = true;
		
		// handle copy/paste of several sentences
		// pick a regular expression to detect multi-sentence input
		String regex = "(\\w*\\b)+";
				
		
		if(str.length() > 5 && str.matches(regex)){
			// calculate sentence offset
			final int offset = calculateStartOffset(offs); // lastOffset
			final int length = calculateEndOffset(offs+str.length()) - offset; 
			
			// remember last offset && section
			lastOffset = offs + str.length(); // 1??
			(new Thread(new Runnable(){
				public void run(){
					spellCheckSection(offset,length);
				}
			})).start();
		
		// if end of the sentence during interactive typing
		}else 
		
		// if end of the sentence
		if(isEndOfWord(str)){
			
			// calculate sentence offset
			final int offset = calculateStartOffset(offs);//lastOffset;
			lastOffset = offs + 1; //1str.length();
			final int length = offs - offset; // lastOffset
			
			(new Thread(new Runnable(){
				public void run(){
					if(spellCheck(offset,length) && isMisspelled(offset) && !isParsedConcept(offset)){
						doc.setCharacterAttributes(offset,length,ReportDocument.getNormalTextStyle(),true);
					}
				}
			})).start();
					
		}else
		
		// if inserting in the middle of misspelled word
		// if misspelled word is being deleted, then we need to remove formatting
		if(isMisspelled(offs-1) || isMisspelled(offs+str.length()+1)){
						
			// calculate sentence offset
			final int offset = calculateStartOffset(offs); 
			final int length = calculateEndOffset(offs) - offset;
			
			// if after removal of character word is splled correctly, then clear its attributes
			(new Thread(new Runnable(){
				public void run(){
					if(spellCheck(offset,length) && !isParsedConcept(offset)){
						doc.setCharacterAttributes(offset,length,ReportDocument.getNormalTextStyle(),true);
					}
				}
			})).start();
		}
		
		
	}
	
	// determine if word is misspelled at this location
	private boolean isMisspelled(int offs){
		AttributeSet attr = doc.getCharacterElement(offs).getAttributes();
		Object obj =  attr.getAttribute("misspelled");
		return obj != null  && ((Boolean)obj).booleanValue();	
	}
	
	
	//	 do the actual spell checkings
	private void spellCheckSection(int offset,int length){
		if(offset >= 0 && length > 1){
			try{
				/*
				String [] text = doc.getText(offset,length).trim().split("\\s+");
				for(int i=0,offs=offset;i<text.length;offs+=text[i++].length()+1){
					if(text[i].length()>0){
						//System.out.println(text[i]);
						spellCheck(text[i],offs);
					}
				}*/
				String text = doc.getText(offset,length);
				Matcher mt = Pattern.compile("\\w+").matcher(text);
				while(mt.find()){
					spellCheck(mt.group(),mt.start());
				}
				
			}catch(BadLocationException ex){
				ex.printStackTrace();	
			}
		}
	}
	
	/**
	 * do spell check of the document
	 */
	public void spellDocument(){
		//TODO: doesn't work 
		spellCheckSection(0,doc.getLength());
	}
	
	public void clear(){
		misspelled.clear();
		for(Element e: doc.getRootElements()){
			clear(e);
		}
	}
	
	private void clear(Element e){
		AttributeSet a = e.getAttributes();
		if(a.containsAttributes(attr)){
			SimpleAttributeSet s = new SimpleAttributeSet();
			s.addAttributes(a);
			s.removeAttributes(attr);
			doc.setCharacterAttributes(e.getStartOffset(),e.getEndOffset()-e.getStartOffset(),s,true);
		}
		for(int i=0;i<e.getElementCount();i++){
			clear(e.getElement(i));
		}
	}
	
	private boolean isParsedConcept(int offset){
		AttributeSet e = doc.getCharacterElement(offset).getAttributes();
		return (e.getAttribute("object") != null);
	}
	
	
	/**
	 * spell check word, return true if correct, false otherwise
	 * @param text
	 * @return
	 */
	public boolean spellWord(String text){
		return null == textTools.spell(text);
	}
	
	
	/**
	 * add word to ignore list
	 * @param text
	 */
	public void ignoreWord(String text){
		ignored.add(text);
	}
	
	
	// do the actual spell checkings
	private boolean spellCheck(int offset,int length){
		if(offset >= 0 && length > 1){
			try{
				String text = doc.getText(offset,length).trim();
				return spellCheck(text,offset);
			}catch(BadLocationException ex){
				ex.printStackTrace();	
			}
		}
		return true;
	}
	
	
	// do the actual spell checkings
	private boolean spellCheck(String text,int offset){
		if(!doc.isEditableAttribute(offset))
			return false;
		
		int length = text.length();
		if(text.length() > 0){
			// check if it is one of the ignored words
			if(ignored.contains(text))
				return true;
			
			// ignore numbers or words with numbers and chars
			//if(text.matches("\\d*([\\./]\\d+)?"))
			if(text.matches("(.*[\\./\\-\\d]+.*)+"))
				return true;			
			
			// make sure that misspelled word is not parsed concept
			if(isParsedConcept(offset))
				return true;
			
			// strip some chars before lookup
			// strip posessive suffix
			if(text.endsWith("'s") || text.endsWith("'S"))
				text = text.substring(0,text.length()-2);
			
			// strip paranthesis
			if(text.startsWith("(") || text.startsWith("[") || text.startsWith("{"))
				text = text.substring(1);
			
			// strip posessive suffix
			if(text.endsWith(")")|| text.endsWith("]") || text.endsWith("}"))
				text = text.substring(0,text.length()-1);
			
						
			// which way do we want to parse
			Properties props = new Properties();
			props.setProperty("word",text);
		
			String [] sugs = textTools.spell(text);
			
			// if sugs == null then word is spelled correctly
			if(sugs != null){
				// now highlight the word
				//System.out.println("Misspelled: "+text+" "+offset+" "+length);
				//doc.setCharacterAttributes(offset,length,attr.copyAttributes(),true);
				
				// create a menu with suggestions and remember it
				Position p = null;
				try{
					p = doc.createPosition(offset);	
				}catch(BadLocationException ex){
					ex.printStackTrace();	
				}
				SpellMenu menu = new SpellMenu(text,p,sugs);

				//System.out.println("Word: "+text);
				for(int i=0;i<sugs.length;i++){
					//System.out.println("Suggestion: "+sugs[i]);
					JMenuItem item = new JMenuItem(sugs[i]);
					//item.setBackground(color);
					//item.setForeground(Color.black);
					item.addActionListener(this);
					menu.add(item);
					
					// check if it is NO_SUGGESTIONS
					if(sugs[i].equalsIgnoreCase(NO_SUGGESTIONS))
						item.setEnabled(false);
					
				}
				misspelled.put(text,menu);
				
				// now highlight the word
				//System.out.println("Misspelled: "+text+" "+offset+" "+length);
				/*
				SimpleAttributeSet a = new SimpleAttributeSet();
				a.addAttribute("suggestions",menu);
				a.addAttributes(attr);
				if(!doc.isEditableAttribute(offset+length/2)){
					//System.out.println(offset+length/2);
					a.addAttributes(ReportDocument.getDisabledTextStyle());
				}
				doc.setCharacterAttributes(offset,length,a,false);
				*/
				if(doc.isEditableAttribute(offset+length/2)){
					SimpleAttributeSet a = new SimpleAttributeSet();
					a.addAttributes(doc.getCharacterElement(offset).getAttributes());
					a.addAttribute("suggestions",menu);
					a.addAttributes(attr);
					doc.setCharacterAttributes(offset,length,a,true);
				}
				
				return false;
			}
		}	
		return true;
	}

	
	// remove string
	public void removeString(int offs, String str, String section){
		// flag that typing is not in progress for caret
		//typing = true;
		/*
		// if misspelled word is being deleted, then we need to remove formatting
		if(isMisspelled(offs)){
					
			// calculate sentence offset
			final int offset = calculateStartOffset(offs); 
			final int length = calculateEndOffset(offs) - offset;
			
			// now, since the string is actually removed we need to compensate
			try{
				StringBuffer buffer = new StringBuffer(doc.getText(offset,length));
				int strOffset = offs - offset;
				final String word = buffer.replace(strOffset,strOffset+str.length(),"").toString();
				
				// if after removal of character word is splled correctly, then clear its attributes
				//(new Thread(new Runnable(){
				//	public void run(){
				//		System.out.println("Checking spelling of "+offset+" "+word);
				if(spellCheck(word,offset)){
					doc.setCharacterAttributes(offset,length,ReportDocument.getNormalTextStyle(),true);
				//	System.out.println("Changing attrs "+offset+" "+length);	
				}
				//	}
				//})).start();
			}catch(BadLocationException ex){
				ex.printStackTrace();
			}
		}
		*/
		// else do what needs to be done		
		//if(offs < lastOffset)
		//	lastOffset = lastOffset - str.length();
		lastOffset = offs;
	}
	
	// update index
	public void updateOffset(int newoffset){
		//System.out.println(newoffset+" "+lastOffset);
		if(!typing){
			// we need to process previously typed stuff, unless there was not any or
			// all movement was on the same line
			if(lastOffset > 0 && ! inSameWord(lastOffset,newoffset)){
				// now we need to process everything that was typed eariler
				// calculate sentence offset
	
				final int offset = calculateStartOffset(lastOffset);
				final int length = calculateEndOffset(lastOffset) - offset; 
			
				// if after removal of cursor word is splled correctly, then clear its attributes
				(new Thread(new Runnable(){
					public void run(){
						if(spellCheck(offset,length)){
							// only clear this word if it was misspelled
							if( isMisspelled(offset))				
								doc.setCharacterAttributes(offset,length,ReportDocument.getNormalTextStyle(),true);
							
						}
					}
				})).start();
			}
			
			// change offset to new location, unless it is in "forbidden" zone
			if(doc.isEditable(newoffset))
				lastOffset = newoffset;
					
		}
		//typing = false;
	}
	
	public void finishReport(){	}
	
	/**
	 * Determine end of the sentence.
	 */	
	private boolean isEndOfWord(String str) {
		return str.matches(".*[\\s\\.\\?!,:;\\n\\r]$");
	}
	
	/**
	 * determine if there is a misspelled word under a cursor and 
	 * return popup list.
	 */
	public JPopupMenu getSuggestionList(int position){
		String word = getWord(position);
		//System.out.println("-"+word+"-");
		if(word != null && misspelled.containsKey(word)){
			SpellMenu menu = (SpellMenu) misspelled.get(word);
			//menu.position = word.offset;
			return menu;
		}else 
			return null;
	}
	
	
	// find word at cursor posisition
	private String getWord(int pos){
		//Text word = null;
		String word = null;
		try{
			// lets assume that word is less then 50 characters long
			// unless bound by start and end of a document
			int offset = (pos > 25)?(pos-25):0;
			int length = ((offset+50) < doc.getLength())?50:(doc.getLength()-offset);
			
			//  get text
			String text = doc.getText(offset,length);
			
			// now starting from position lets find bounds.
			int start; // = text.lastIndexOf(" ",pos);
			for(start=(pos-offset);start>=0;start--){
				char c = text.charAt(start);
				if(c==' '||c==','||c=='\n'||c=='.'||c=='!'||c=='?'||c==';'||c==':'||c=='\t'){
					start ++;
					break;
				}
			}
			int end; //   = text.indexOf(" ",pos);
			for(end=(pos-offset);end<length;end++){
				char c = text.charAt(end);
				if(c==' '||c==','||c=='\n'||c=='.'||c=='!'||c=='?'||c==';'||c==':'||c=='\t'){
					break;
				}
			}
			if(start < end){
				/*
				word = new Text();
				word.text = text.substring(start,end);
				word.offset = offset + start;
				*/
				word = text.substring(start,end);
			}
		}catch(BadLocationException ex){
			ex.printStackTrace();
		}
		return word;
	}
	

	/**
	 * see of two indicies are on the same word
	 */
	 private boolean inSameWord(int x, int y){
		int start = Math.min(x,y);
		int end = Math.max(x,y);
		// go through text and see if there are newlines
		try{
			String text = doc.getText(start,end-start);
			for(int i=0;i<text.length();i++){
				char c = text.charAt(i);
				if(c==' '||c==','||c=='\n'||c=='.'||c=='!'||c=='?'||c==';'||c==':'||c=='\t'||c=='\r')
					return false;
			}
		}catch(BadLocationException ex){
			//ex.printStackTrace();
		}
		return true;
	 }
	  
	
	
	/**
	 * Determine "Real" offset of the typed sentence based on last offset
	 */
	private int calculateStartOffset(int offset){
		// get text from this offset till the begining and cound backwords until
		// end of sentence is reached
		// getting the entire report, might be an overkill, but I don't think it is
		// resource prohibitive, plus it simplifies calculations and might be faster
		// some arbitrary number could be used here, but why bother?
		try{
			String text = doc.getText(0,offset);
			for(int i=text.length()-1;i>=0;i--){
				char c = text.charAt(i);
				boolean b = (c =='.');
				// check if it is abriviation or a float
				if(b){
					// if next character is a digit then we have a float
					if(i<text.length()-1 && Character.isDigit(text.charAt(i+1)))
						continue;
					
					// check for abbriviations
					String str = text.substring(Math.max(0,i-5),i);
					int spaceOffset = str.lastIndexOf(" ");
					if(spaceOffset == -1)
						spaceOffset = str.lastIndexOf("\n");
					
                    // if space is there and it is not right before . then it might be abbr
					if(spaceOffset > -1 && spaceOffset < str.length()-1){
						// if char after space is Uppercase, then it is abbreviation
						if(Character.isUpperCase(str.charAt(spaceOffset+1)))
							continue;
					}							
				}
				
				b = b || (c==' '||c==','||c=='\n'||c=='!'||c=='?'||c==';'||c==':'||c=='\t'||c=='\r');
				
				if(b)
					return i+1;	
			}
		}catch(BadLocationException ex){
			ex.printStackTrace();	
		}
		return offset;	 
	}
	
	/**
	 * Determine "Real" offset of the typed sentence based on last offset
	 */
	private int calculateEndOffset(int offset){
		// get text from this offset till the begining and cound backwords until
		// end of sentence is reached
		// getting the entire report, might be an overkill, but I don't think it is
		// resource prohibitive, plus it simplifies calculations and might be faster
		// some arbitrary number could be used here, but why bother?
		try{
			String text = doc.getText(offset,doc.getLength()-offset);
			for(int i=0;i<text.length();i++){
				char c = text.charAt(i);
				boolean b = (c =='.');
				
				// check if it is abriviation or a float
				if(b){
					// if next character is a digit then we have a float
					if(i<text.length()-1 && Character.isDigit(text.charAt(i+1)))
						continue;
					
					// check for abbriviations
					String str = text.substring(Math.max(0,i-5),i);
					int spaceOffset = str.lastIndexOf(" ");
					if(spaceOffset == -1)
						spaceOffset = str.lastIndexOf("\n");
					
					// if space is there and it is not right before . then it might be abbr
					if(spaceOffset > -1 && spaceOffset < str.length()-1){
						// if char after space is Uppercase, then it is abbreviation
						if(Character.isUpperCase(str.charAt(spaceOffset+1)))
							continue;
					}							
				}
				
				b = b || (c==' '||c==','||c=='\n'||c=='!'||c=='?'||c==';'||c==':'||c=='\t'||c=='\r');
				
				if(b)
					return i+offset;	
			}
		}catch(BadLocationException ex){
			ex.printStackTrace();	
		}
		return offset;	 
	}
	
	
	// replace words
	public void actionPerformed(ActionEvent e){
		Object source = e.getSource();
				
		// Get menu item and spell menu
		if(source instanceof JMenuItem){
			JMenuItem item = (JMenuItem) source;
			SpellMenu menu = (SpellMenu) item.getParent();
			
			// replace text
			correctWord(menu.getOffset(),menu.getWord(),item.getText());
		}else if( source == cancel){
			dialog.setVisible(false);
			// unhighlight word
			unhighlightWord(badwordOffset,badword.getText().length(),false);
			if(spellMode == DOCUMENT_MODE){
				synchronized(wordCorrected){
					wordCorrected.notifyAll();
				}
			}
		}else if( source == change){
			String changeText = changeTo.getText();
			if(changeText.length() > 0 && !changeText.equals(NO_SUGGESTIONS)){
				correctWord(badwordOffset,badword.getText(),changeText);
				if(spellMode == SELECTION_MODE){
					dialog.setVisible(false);
				}else{  // if(spellMode == DOCUMENT_MODE){
					synchronized(wordCorrected){
						wordCorrected.notifyAll();
					}
				}
			}
		}else if(source == ignore){
			ignored.add(badword.getText());
			// unhighlight word
			unhighlightWord(badwordOffset,badword.getText().length(),true);
			if(spellMode == SELECTION_MODE){
				dialog.setVisible(false);
			}else{  //if(spellMode == DOCUMENT_MODE){
				synchronized(wordCorrected){
					wordCorrected.notifyAll();		
				}	
			}
		}
	}
	
	// replace misspelled word with correct one
	private void correctWord(int offset,String origWord, String correctWord){
		//typing = false;
		
		// correct correctWord if orig is uppercase
		if(origWord.matches("[A-Z \\-\\']+"))
			correctWord = correctWord.toUpperCase();
		
		// replace text
		try{
			typing = true;
			doc.replaceText(offset,origWord.length(),correctWord,null); //ReportDocument.getNormalTextStyle()
			doc.getTextEditor().setCaretPosition(offset+correctWord.length());
			typing = false;
		}catch(BadLocationException ex){
			ex.printStackTrace();	
		}
		
		// remove from misspelled list
		misspelled.remove(origWord);
	}
	
	
	// this class extends JPopupMenu to have some meta data
	static class SpellMenu extends JPopupMenu {
		private String word;
		private Position position;
		private String [] list;
		
		// constructor
		public SpellMenu(String word,Position offset, String [] sugs){
			this.word = word;
			this.list = sugs;
			this.position = offset;
		}
		// get misspelled word
		public String getWord(){
			return word;
		}
		// get misspelled word offset
		public int getOffset(){
			return 	position.getOffset();
		}
		// set misspelled word offset
		public void setOffset(Position offset){
			this.position = offset;
		}
		
		// get list of suggestions
		public String [] getSuggestions(){
			return list;	
		}
	}
	
	/**
	 * This sets the spell mode. If it is selection only or document replace
	 *
	public void setSpellMode(int mode){
		spellMode = mode;	 
	}
	*/
	/**
	 * Open spell dialog box for text at offset
	 */
	public void doSpellCheck(String text, int offset){
		spellMode = SELECTION_MODE;
		if(!spellCheck(text,offset)){
			// get suggestions
			SpellMenu menu = (SpellMenu) misspelled.get(text);
			// this should always be true
			if(menu != null)
				showSpellDialog(menu);
		}else{
			JOptionPane.showMessageDialog(parent,"Spell check is done!");
		}
	}
	
	/**
	 * Open spell dialog box for text at offset
	 */
	public void doSpellCheck(){
		spellMode = DOCUMENT_MODE;
		(new Thread(new Runnable(){
			public void run(){
				// loop over misspelled words
				ArrayList list = new ArrayList(misspelled.keySet());
				for(Iterator i=list.iterator();i.hasNext();){
					String word = (String) i.next();
					SpellMenu menu = (SpellMenu) misspelled.get(word);
					//System.out.println(menu.getWord()+" "+menu.getOffset());
					// check if this word is still there (was not deleted)
					if(!checkWord(menu.getWord(),menu.getOffset())){
						misspelled.remove(word);
						continue;
					}
					
					// highlight misspelled word
					highlightWord(menu.getOffset(),menu.getWord().length());
					// show spell dialog
					showSpellDialog(menu);
					synchronized(wordCorrected){
						try{
							wordCorrected.wait();
						}catch(InterruptedException ex){
							ex.printStackTrace();	
						}
					}
					// if dialog was canceled
					if(!dialog.isVisible())
						break;
				}
				if(dialog != null)
					dialog.setVisible(false);
				JOptionPane.showMessageDialog(parent,"Spell check is done!");
			}
		})).start();
	}
	
	/**
	 * checks that word is still in same location as it was
	 */
	private boolean checkWord(String word,int offset){
		// check offset 
		if(offset + word.length() > doc.getLength())
			return false;
		String text="";
		try{
			text = doc.getText(offset,word.length());
			//System.out.println(text+" vs "+word);
		}catch(BadLocationException ex){
			ex.printStackTrace();	
		}
		return text.equals(word);
	}
	
	/**
	 * highlight word
	 */
	private void highlightWord(int offset,int length){
		SimpleAttributeSet a = new SimpleAttributeSet();
		StyleConstants.setForeground(a,Color.orange);
		doc.setCharacterAttributes(offset,length,a,false);	
	}
	
	/**
	 * unhighlight word
	 */
	private void unhighlightWord(int offset,int length,boolean replace){
		SimpleAttributeSet a = new SimpleAttributeSet();
		StyleConstants.setForeground(a,Color.black);
		doc.setCharacterAttributes(offset,length,a,replace);	
	}
	
	/**
	 * open/show spell dialog for new word
	 */
	private void showSpellDialog(SpellMenu menu){
		JDialog dialog = getSpellDialog();
		//change.setEnabled(true);
		
		// setup misspelled word box
		badword.setText(menu.getWord());
		badwordOffset = menu.getOffset();
		
		// get suggestions
		suggestions.setListData(menu.getSuggestions());
		if(menu.getSuggestions().length > 0){
			suggestions.setSelectedIndex(0);
			//if(NO_SUGGESTIONS.equals(suggestions.getSelectedValue().toString()))
			//	change.setEnabled(false);
		}
		
		// show dialog
		dialog.setVisible(true);
	}
	
	/**
	 * create spell checking dialog
	 */	
	private JDialog createSpellDialog(){
		JDialog dialog = new JDialog(parent);
		dialog.setTitle("Spell Checker");
		dialog.setModal(false);
		UIHelper.centerWindow(parent,dialog);
		
		
		// Labels
		JLabel l1 = new JLabel("Original Text  ");
		JLabel l2 = new JLabel("Change To  ");
		JLabel l3 = new JLabel("Suggestions  ");
		
		// boxes
		badword = new JTextField(15);
		badword.setEditable(false);
		badword.setForeground(Color.red);
		
		changeTo = new JTextField(15);
		changeTo.setPreferredSize(badword.getPreferredSize());
		
		suggestions = new JList();
		suggestions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		suggestions.setVisibleRowCount(5);
		JScrollPane scroll = new JScrollPane(suggestions);
		int w = badword.getPreferredSize().width;
		int h = (badword.getPreferredSize().height+4)*5;
		scroll.setPreferredSize(new Dimension(w,h));
		suggestions.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e){
				Object sel = suggestions.getSelectedValue();
				if(sel != null)
					changeTo.setText(sel.toString());
			}
		});
		
		// buttons
		change = new JButton("Change");
		change.addActionListener(this);
		ignore = new JButton("Ignore");
		ignore.addActionListener(this);
		cancel = new JButton("Cancel");
		cancel.addActionListener(this);
		
		// reset dimensions for buttons
		ignore.setPreferredSize(change.getPreferredSize());
		ignore.setMinimumSize(change.getPreferredSize());
		ignore.setMaximumSize(change.getPreferredSize());
		cancel.setPreferredSize(change.getPreferredSize());
		cancel.setMinimumSize(change.getPreferredSize());
		cancel.setMaximumSize(change.getPreferredSize());
		
		// layout components
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth  = 1;
		c.gridheight = 1;
		
		Container panel = dialog.getContentPane();
		panel.setLayout(gridbag);
		
		gridbag.setConstraints(l1,c);
		panel.add(l1);
		c.gridx++;
		gridbag.setConstraints(badword,c);
		panel.add(badword);
		c.gridx++;
		gridbag.setConstraints(ignore,c);
		panel.add(ignore);
		c.gridx=0;
		c.gridy++;
		gridbag.setConstraints(l2,c);
		panel.add(l2);
		c.gridx++;
		gridbag.setConstraints(changeTo,c);
		panel.add(changeTo);
		c.gridx++;
		gridbag.setConstraints(change,c);
		panel.add(change);
		c.gridx=0;
		c.gridy++;
		gridbag.setConstraints(l3,c);
		panel.add(l3);
		c.gridx++;
		c.gridheight = 5;
		gridbag.setConstraints(scroll,c);
		panel.add(scroll);
		c.gridx++;
		c.gridy+=3;
		c.gridheight = 1;
		c.gridwidth = 1;
		gridbag.setConstraints(cancel,c);
		panel.add(cancel);
		
		//dialog.getContentPane().add(panel);
		dialog.pack();
		return dialog;
	}
	
	/**
	 * Get instance of spell checking dialog
	 */
	 public JDialog getSpellDialog(){
		if(dialog == null)
			dialog = createSpellDialog();
		return dialog;
	 }
}
