package edu.pitt.dbmi.tutor.ui;

import static edu.pitt.dbmi.tutor.util.OntologyHelper.getWindowLocation;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.util.UIHelper;

public class GlossaryManager {
	private boolean showExampleImage = true;
	private List<Popup> glossaryWindows;
	
	public void setShowExampleImage(boolean b){
		showExampleImage = b;
	}
	
	/**
	 * create a closable glossary panel
	 * @param ConceptEntry entry that you want to display glossary for
	 * @param Component where this glossary is created
	 * @param
	 * @return
	 */
	public void showGlossary(ConceptEntry entry, Component comp, Point location){
		//TODO: account for information in parent finding
		// setup parameters
		String name = (entry.isResolved())?entry.getConcept().getName():entry.getText();
		String definition = (entry.isResolved())?entry.getDefinition():"no definition available";
		
		// if no descent definition is available, check the feature
		/*
		if(TextHelper.isEmpty(definition)){
			entry = entry.getFeature();
			expertModule.resolveConceptEntry(entry);
			name = (entry.isResolved())?entry.getConcept().getName():entry.getText();
			definition = (entry.isResolved())?entry.getConcept().getDefinition():"no definition available";
		}
		*/
		
		String text = "<table width=300><tr><td><b>"+name+"</b><br>"+definition+"</td></tr></table>";
		
		// adjust text to be well-formatted html with picture
		if(showExampleImage && !entry.getExampleImages().isEmpty()){
			URL pic = entry.getExampleImages().get(0);
			// load picture to see ratio
			int w=0,h=0; 
			try{
				ImageIcon img = new ImageIcon(pic);
				w = img.getIconWidth();
				h = img.getIconHeight();
			}catch(Exception ex){}
			
			// adjust width/height to fit into limit
			if(w >= h && w > 300){
				h = (h * 300)/ w;
				w = 300;
			}else if( w < h && h > 200){
				w = (w * 200)/ h;
				h = 200;
			}
			int width = (w>=h)?300:350;	
			
			//reset text
			text = "<table width="+width+"><tr><td valign=top><b>"+name+"</b><br>"+
					definition+"</td>"+((w >= h)?"</tr><tr>":"")+
					"<td><a href=\""+pic+"\"><img src=\""+pic+"\" border=1 width="+w+
					" height="+h+" ></a></td></tr></table>";
		
		}
		final String glossaryText = text;
		
		
		
		// create glossary panel
		final JEditorPane glossaryTextPanel = new UIHelper.HTMLPanel();
		glossaryTextPanel.setText(glossaryText);
		glossaryTextPanel.setEditable(false);
		((JEditorPane) glossaryTextPanel).addHyperlinkListener(new HyperlinkListener(){
			public void hyperlinkUpdate(HyperlinkEvent e){
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					String str = e.getDescription();
					if(str.equals("reset")){
						glossaryTextPanel.setText(glossaryText);
					}else{
						glossaryTextPanel.setText("<center><a href=\"reset\"><img border=1 src=\""+str+"\"></a></center>");
					}
				}
			}
		});
	
		// create scroll panel
		JScrollPane sc = new JScrollPane(glossaryTextPanel, 
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBorder(LineBorder.createGrayLineBorder());
		panel.add(sc, "Center");
		JButton ok = new JButton("OK");
	
		JPanel p = new JPanel();
		p.setLayout(new FlowLayout());
		p.add(ok);
		panel.add(p, "South");
		
		
		// setup window
		if(location != null){
			Point loc = location;
			//SwingUtilities.convertPointToScreen(loc,textPane);
			Point pt = getWindowLocation(panel,loc);
			final Popup glossaryWindow = PopupFactory.getSharedInstance().getPopup(comp,panel, pt.x, pt.y);
			glossaryWindow.show();
			if(glossaryWindows == null)
				glossaryWindows = new ArrayList<Popup>();
			glossaryWindows.add(glossaryWindow);
			
			// setup close operation
			ok.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					glossaryWindow.hide();
					glossaryWindows.remove(glossaryWindow);
				}
			});
		}
	}
	
	public void reset(){
		if(glossaryWindows != null){
			for(Popup p: glossaryWindows){
				p.hide();
			}
			glossaryWindows.clear();
		}
	}
}
