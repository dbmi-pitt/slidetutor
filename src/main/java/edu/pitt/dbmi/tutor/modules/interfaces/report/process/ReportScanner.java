/**
 * This process scans each sentence using SentenceScanner.
 * Author: Eugene Tseytlin (University of Pittsburgh)
 */

package edu.pitt.dbmi.tutor.modules.interfaces.report.process;

import javax.swing.text.*;

import edu.pitt.dbmi.tutor.modules.interfaces.report.ConceptLabel;
import edu.pitt.dbmi.tutor.modules.interfaces.report.ReportDocument;
import edu.pitt.dbmi.tutor.util.Config;

import java.util.*;

/**
 * This processor chunks input text into sentences (depending on section)
 * It then parses each sentence with NounPhrase parser and looks up the meaning of each noun 
 * phrase using EVS or UMLS Metathesourus and matches it to Domain Ontology 
 */
public class ReportScanner implements ReportProcessor {
	public static final String SENTENCE_THREAD_NAME = "Sentence-";
	public static final String SECTION_THREAD_NAME = "Section-";
	public static final String COMMENT = "COMMENT";
	
	private final int AUTO_COMMIT = 1000; // timeout before automatic parse
	private int lastOffset = 0;
	private String lastSection = "";
	private ReportDocument doc;

	
	// thread groups for sentence scanners and labels
	private static ThreadGroup sentenceGroup = new ThreadGroup("sentenceGroup");
	private static ThreadGroup labelGroup = new ThreadGroup("labelGroup");
	private AutoCommit timer;

	
	// constructor
	public ReportScanner(ReportDocument doc) {
		this.doc = doc;
		timer = new AutoCommit();
		timer.start();
	}

	// stop auto commit
	public void stopAutoCommit() {
		timer.stopThread();
	}

	
	// return thread group
	public static ThreadGroup getSentenceGroup() {
		return sentenceGroup;
	}

	// return thread group
	public static ThreadGroup getLabelGroup() {
		return labelGroup;
	}

	// insert string
	public void insertString(int offs, String str, String section) {
		// handle copy/paste of several sentences
		// pick a regular expression to detect multi-sentence input
		String regex = "(.*[,:;\\n\\r\\.?!].*)+";
		//if(section != null && section.equalsIgnoreCase(commentHeading))
		//	regex = "(.*[.?!].*)+";
		// if text was pasted and contains end-of-fragment chars
		if (str.length() > 5 && str.matches(regex)) {
			
			// calculate sentence offset
			int offset = calculateStartOffset(offs, section); // lastOffset
			int length = calculateEndOffset(offs + str.length(), section) - offset;
			//System.out.println("pasted text: "+str+" "+offset+" "+length);
			Thread t = scanSection(offset, length, section);
			if (t != null)
				t.start();

			// if end of the sentence during interactive typing
		} else if (isEndOfSentence(offs, str, section)) {
			// calculate sentence offset
			int offset = calculateStartOffset(offs, section); // lastOffset
			int length = offs - offset; // lastOffset

			// scan sentence
			Thread t = scanSentence(offset, length, section);
			if (t != null)
				t.start();
		} else {
			timer.init();
		}

		// remember last offset && section
		lastOffset = offs + str.length(); // 1??
		lastSection = section;

	}

	// check if text is locked on those bounds
	private boolean isLocked(int offset, int length) {
		return SentenceScanner.getProvideFeedback() && !(doc.isEditable(offset) && doc.isEditable(offset + length - 1));
	}

	// call sentence scanner to scan a sentence
	private Thread scanSentence(int offset, int length, String section) {
		//System.out.println("scan: "+offset+" length: "+length);
		Thread thread = null;
		if (!isLocked(offset, length)) {
			try {
				// parse sentence/
				SentenceScanner scan = new SentenceScanner(doc, offset, length, section);
				// start scan
				thread = new Thread(sentenceGroup, scan, SENTENCE_THREAD_NAME + offset);
			} catch (Exception ex) { //BadLocation
				ex.printStackTrace();
			}
		}
		timer.reset();
		return thread;
	}

	// call sentence scanner to scan a sentence
	private Thread scanSection(int offs, int len, String section) {
		Thread thread = null;
		//System.out.println("is locked? "+isLocked(offs,len));
		if (!isLocked(offs, len)) {
			try {
				List offs_len = new ArrayList();

				// check for empty inserts
				String str = doc.getText(offs, len);
				int loffs = getLetterOffset(str);
				//int llen  = str.trim().length();
				// everything is whitespace
				if (loffs == -1)
					return null;

				// offset is letter offset + real offset
				int offset = loffs + offs;
				do {
					int s_offs = offset;
					int e_offs = calculateEndOffset(offset, section);
					offs_len.add(new int[] { s_offs, e_offs - s_offs });
					offset = e_offs + 1;
				} while (offset < (offs + len));

			
				// now convert to two arrays
				int[] offsets = new int[offs_len.size()];
				int[] lengths = new int[offsets.length];

				for (int i = 0; i < offsets.length; i++) {
					int[] ol = (int[]) offs_len.get(i);
					offsets[i] = ol[0];
					lengths[i] = ol[1];
				}
				// now lunch the scanner
				SentenceScanner scan = new SentenceScanner(doc, offsets, lengths, section);
				thread = new Thread(sentenceGroup, scan, SECTION_THREAD_NAME + offs);
				//thread.start();
			} catch (Exception ex) { //BadLocation
				ex.printStackTrace();
			}
		}
		timer.reset();
		return thread;
	}

	// remove string
	public void removeString(int offs, String str, String section) {
		timer.reset();
		
		// if label is being deleted, then we need to remove all ConceptLabels
		int st = offs, en;
		do{	
			Element e = doc.getCharacterElement(st);
			st = e.getStartOffset();
			en = e.getEndOffset();
			boolean concept = Boolean.parseBoolean(""+e.getAttributes().getAttribute("concept")); 
			if(concept){	
				doc.getReportData().removeConceptLabel((ConceptLabel)e.getAttributes().getAttribute("object"));
			}
			st += en;
		}while(en < offs+str.length());
		
		// change last offset to new position		
		lastOffset = offs;
	}

	// update index
	public void updateOffset(int newoffset) {
		// we want to reparse only if there was something inserted or removed
		// and current text is not locked
		//if (typing) {
			// we need to process previously typed stuff, unless there was not any or
			// all movement was on the same line
			if (lastOffset > 0 && !areInSameSentence(lastOffset, newoffset)) {
				// now we need to process everything that was typed eariler
				// calculate sentence offset

				int offset = calculateStartOffset(lastOffset, lastSection);
				int length = calculateEndOffset(lastOffset, lastSection) - offset;

				Thread t = scanSentence(offset, length, lastSection);
				if (t != null)
					t.start();
			}

			// change offset to new location, unless it is in "forbidden" zone
			if (doc.isEditable(newoffset))
				lastOffset = newoffset;
		//}
		//typing = false;
		timer.reset();
	}

	/**
	 * This method should be called, to scan any text that has
	 * not been yet scan. Ex. before submit.
	 * This method returns ONLY after text is parsed
	 * it doesn't lunch a separate thread
	 */
	public void flushText() {
		if (lastOffset > 0) {
			int offset = calculateStartOffset(lastOffset, lastSection);
			int length = calculateEndOffset(lastOffset, lastSection) - offset;
			// WHY IS IT NOT OK to have concepts?
			//if (!regionHasConcepts(offset, length)) {
			lastOffset = offset + length;
			Thread t = scanSentence(offset, length, lastSection);
			if (t != null) {
				t.start();
				try {
					//Thread.sleep(500);
					t.join();
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
			}
			//}
		}
	}

	/**
	 * Does this region has any concepts?
	 */
	public boolean regionHasConcepts(int offset, int length) {
		// remember any "misspelled" elements
		Element p = doc.getParagraphElement(offset);
		for (int i = 0; i < p.getElementCount(); i++) {
			Element e = p.getElement(i);
			int st = e.getStartOffset();
			int en = e.getEndOffset();
			Object o = e.getAttributes().getAttribute("concept");
			boolean inside = (offset <= st && en <= (offset + length));
			boolean concept = (o != null && o instanceof Boolean) ? ((Boolean) o).booleanValue() : false;
			if (inside && concept) {
				return true;
			}
		}
		return false;
	}

	public void finishReport() {
	}

	/**
	 * see of two indicies are in different phrses
	 */
	private boolean areInSameSentence(int x, int y) {
		int start = Math.min(x, y);
		int end = Math.max(x, y);

		// check bounds
		if (start < 0)
			start = 0;
		if (end >= doc.getLength())
			end = doc.getLength() - 1;

		// go through text and see if there are newlines
		if((end - start) > 0){
			try {
				String text = doc.getText(start, end - start);
				for (int i = 0; i < text.length(); i++) {
					char c = text.charAt(i);
					if (c == '.') {
						return false;
					} else if (c == ',' || c == '\n' || c == '!' || c == '?' || c == ';' || c == ':' || c == '\t'
							|| c == '\r')
						return false;
				}
			} catch (BadLocationException ex) {
				ex.printStackTrace();
			}
		}
		return true;
	}


	/**
	 * Determine "Real" offset of the typed sentence based on last offset
	 */
	private int calculateStartOffset(int offset, String section) {
		return calculateStartOffset(offset, doc, !section.equalsIgnoreCase(COMMENT));
	}

	/**
	 * Determine "Real" offset of the typed sentence based on last offset
	 */
	public static int calculateStartOffset(int offset, StyledDocument doc, boolean terms) {
		// check extremes
		if (offset >= doc.getLength())
			return doc.getLength();

		// see if we are parsing terms, or sentences
		//boolean terms = (section == null  || !section.equalsIgnoreCase(commentHeading));
		//boolean terms = true;
		// get text from this offset till the begining and cound backwords until
		// end of sentence is reached
		// getting the entire report, might be an overkill, but I don't think it is
		// resource prohibitive, plus it simplifies calculations and might be faster
		// some arbitrary number could be used here, but why bother?
		try {
			String text = doc.getText(0, offset);
			char prev = ' ';
			for (int i = text.length() - 1; i >= 0; i--) {
				char c = text.charAt(i);
				boolean b = (c == '.');

				// check if it is abriviation or a float
				if (b) {
					// if previous character is a digit then we have a float
					if (i > 0 && Character.isDigit(text.charAt(i - 1)))
						continue;

					// check for abbriviations
					String str = text.substring(Math.max(0, i - 5), i);
					int spaceOffset = str.lastIndexOf(" ");
					if (spaceOffset == -1)
						spaceOffset = str.lastIndexOf("\n");

					// if space is there and it is not right before . then it might be abbr
					if (spaceOffset > -1 && spaceOffset < str.length() - 1) {
						// if char after space is Uppercase, then it is abbreviation
						if (Character.isUpperCase(str.charAt(spaceOffset + 1)))
							continue;
					}
				}
				//|| c == ':'
				b = b || (c == '!' || c == '?' );
				// if using term processing,then check other chars as well
				if (terms) {
					b = b || (c == ',' || c == '\n' || c == ';' || c == '\t' || c == '\r');
					// if not using term processing, we want to make sure, that we don't run into section
					// heading or stop when there is an empty line
				} else if (prev == '\n') {
					if (c == '\n' || isHeader(i + 1, doc))
						return i + 1;
				}
				// if it is a end-sentence character, then return
				if (b)
					return i + 1;
				prev = c;
			}
		} catch (BadLocationException ex) {
			ex.printStackTrace();
		}
		return offset;
	}

	/**
	 * Determine "Real" offset of the typed sentence based on last offset
	 */
	private int calculateEndOffset(int offset, String section) {
		return calculateEndOffset(offset, doc, !section.equalsIgnoreCase(COMMENT));
	}

	/**
	 * Determine "Real" offset of the typed sentence based on last offset
	 */
	private static int calculateEndOffset(int offset, StyledDocument doc, boolean terms) {
		// check extremes
		if (offset >= doc.getLength())
			return doc.getLength();

		// see if we are parsing terms, or sentences
		//boolean terms = (section == null || !section.equalsIgnoreCase(commentHeading));
		//boolean terms = true;
		// get text from this offset till the begining and cound backwords until
		// end of sentence is reached
		// getting the entire report, might be an overkill, but I don't think it is
		// resource prohibitive, plus it simplifies calculations and might be faster
		// some arbitrary number could be used here, but why bother?
		try {
			String text = doc.getText(offset, doc.getLength() - offset);
			char prev = ' ';
			for (int i = 0; i < text.length(); i++) {
				char c = text.charAt(i);
				boolean b = (c == '.');

				// check if it is abriviation or a float
				if (b) {
					// if next character is a digit then we have a float
					if (i < text.length() - 1 && Character.isDigit(text.charAt(i + 1)))
						continue;

					// check for abbriviations
					String str = text.substring(Math.max(0, i - 5), i);
					int spaceOffset = str.lastIndexOf(" ");
					if (spaceOffset == -1)
						spaceOffset = str.lastIndexOf("\n");

					// if space is there and it is not right before . then it might be abbr
					if (spaceOffset > -1 && spaceOffset < str.length() - 1) {
						// if char after space is Uppercase, then it is abbreviation
						if (Character.isUpperCase(str.charAt(spaceOffset + 1)))
							continue;
					}
				}

				b = b || (c == '!' || c == '?');
				// if using term processing,then check other chars as well
				if (terms) {
					// took out commas, cause it causes trouble c == ',' ||  c == ':' ||
					b = b || (c == '\n' || c == ';' || c == '\t' || c == '\r');
					// if not using term processing, we want to make sure, that we don't run into section
					// heading
				} else if (prev == '\n') {
					if (c == '\n' || isHeader(i + offset, doc))
						return i + offset - 1;
				}

				// if it is a end-sentence character, then return
				if (b)
					return i + offset;

				prev = c;
			}
		} catch (BadLocationException ex) {
			ex.printStackTrace();
		}
		return offset;
	}

	// checks that character at this offset is a header
	private static boolean isHeader(int offs, StyledDocument doc) throws BadLocationException {
		Object obj = doc.getCharacterElement(offs).getAttributes().getAttribute("isheader");
		return (obj != null && ((Boolean) obj).booleanValue());
	}

	/**
	 * get index of the first non-whitespace character
	 */
	private int getLetterOffset(String str) {
		for (int i = 0; i < str.length(); i++) {
			if (!Character.isWhitespace(str.charAt(i)))
				return i;
		}
		return -1;
	}

	/**
	 * Determine end of the sentence.
	 */
	private boolean isEndOfSentence(int offs, String str, String section) {
		String text = "";
		
		//if (section == null || section.equalsIgnoreCase("COMMENT"))
		//	return false;

		// check for numbers and abbriviations
		if (str.endsWith(".")) {
			try {
				int offset = calculateStartOffset(offs, section);
				text = doc.getText(offset, offs - offset);
				//text = doc.getText(lastOffset,offs - lastOffset);
				//System.out.println("End of sentence? "+text);
			} catch (BadLocationException ex) {
				ex.printStackTrace();
				return false;
			}
			// if sentence size is < then 4 characters it is probably abriviation
			if (text.length() < 3)
				return false;

			// we have a float number if we got a "sentence" ending with a number
			if (text.matches(".*\\s[0-9]+$"))
				return false;

			// otherwise it is an end of the sentence 
			// (since both sections use . as sentence delimeter)
			return true;
		}

		// if diagnosis section
		//if(section.equalsIgnoreCase(diagnosisHeading))
		//	return str.matches(".*[\\.!\\?,\\n;]$");

		// if comment section
		//if(section.equalsIgnoreCase(commentHeading))
		//	return str.matches(".*[\\.!\\?]$");
		//else
		
		//return str.matches(".*[\\.!\\?\\n,;:\\r]$");
		return str.matches(".*[\\.!\\?\\n,;\\r]$");
		//return false;		
	}

	////////////////////////////////// STAND ALONE SCANNING ///////////////////////////////////////////

	/**
	 * scan all editable sections of the report.
	 * This method joins all threads, and takes a long time
	 * it is recommended to use this method inside of the new thread
	 */
	public void scanDocument() {
		SentenceScanner.setProvideFeedback(false);

		// setup locked attributes
		SimpleAttributeSet locked = new SimpleAttributeSet();
		locked.addAttribute("locked", Boolean.TRUE);

		SimpleAttributeSet unlocked = new SimpleAttributeSet();
		unlocked.addAttribute("locked", Boolean.FALSE);

		//doc.lock();
		// lock document
		doc.setCharacterAttributes(0, doc.getLength(), locked, false);
		
		// clear all existing  markings
		doc.getReportData().clear();
		
		String[] sections = doc.getEditableSections();
		for (int i = 0; i < sections.length; i++) {
			//System.out.println(sections[i]);
			Thread t = scanSection(sections[i]);
			if (t != null) {
				t.start();
				try {
					t.join();
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
			}
		}
		// unlock document
		doc.setCharacterAttributes(0, doc.getLength(), unlocked, false);
		//doc.unlock();
		SentenceScanner.setProvideFeedback(true);
	}

	/**
	 * scan section
	 */
	public Thread scanSection(String sectionName) {
		String text = "";
		String section = sectionName + ":\n";
		// get all text
		try {
			text = doc.getText(0, doc.getLength());
		} catch (BadLocationException ex) {
			ex.printStackTrace();
		}
		// it is better for us to have a newline at the end of last section.
		// i don't even remember why it is important, but it works only if it is there
		if (!text.endsWith("\n"))
			doc.appendText("\n");

		// now do everything
		int offset = text.indexOf(section);
		int length = 0;
		if (offset > -1) {
			offset = offset + section.length();
			// break rest of text into multiple lines and find next section header
			String[] lines = text.substring(offset).split("\n");
			for (int i = 0; i < lines.length; i++) {
				if (lines[i].matches("^[A-Z ]+:$"))
					break;
				else
					length = length + lines[i].length() + 1; // add 1 to compensate for newline
			}
		}
		//System.out.println("Scanning text "+offset+" "+length+" "+doc.getLength());

		if (offset < 0 || length == 0 || (offset + length) > doc.getLength())
			return null;

		//System.out.println("Scanning text "+offset+" "+length);
		// now scan section
		return scanSection(offset, length - 1, sectionName);
	}

	/**
	 * This thread will attempt to parse text after some delay
	 */
	private class AutoCommit extends Thread {
		private boolean stop = false;
		private int count = -1;
		private int total, sleep_time = 100;

		public AutoCommit() {
			super("AutoCommit");
			total = AUTO_COMMIT / sleep_time;
		}

		// this is where magic happens
		public void run() {
			while (!stop) {
				if (count >= 0)
					count++;

				try {
					sleep(sleep_time);
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}

				if (count > total) {
					flushText();
				}
			}
		}

		public void init() {
			count = 0;
		}

		public void reset() {
			count = -1;
		}

		public void stopThread() {
			stop = true;
		}
	}
}
