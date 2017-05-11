package edu.pitt.dbmi.tutor.modules.interfaces.report;

import javax.swing.JTextPane;
import java.awt.*;
import javax.swing.text.*;

public class ReportEditorKit extends StyledEditorKit implements ViewFactory {
	private JTextPane textPane;
	public ReportEditorKit(JTextPane t){
		textPane = t;	
	}
	
	public ViewFactory getViewFactory() {
		return this;
	}
	public View create( Element elem ) {
		if ( elem.getName().equals( AbstractDocument.ContentElementName ) ) {
			return new ReportLabelView( elem );
		}else if ( elem.getName().equals( StyleConstants.ComponentElementName ) ) {
			return new ReportComponentView( elem );
		}
		return super.getViewFactory().create( elem );
	}

	/**
	 * This code is copy/pasted from
	 * http://forum.java.sun.com/thread.jspa?forumID=57&threadID=288808
	 * posted by Stanislav Lapitsky
	 */	
	private class ReportLabelView extends LabelView {
		private Element elem;
		public ReportLabelView( Element elem ) {
			super( elem );
			this.elem = elem;
		}
		// paint component
		public void paint( Graphics g, Shape allocation ) {
			super.paint( g, allocation );
			AttributeSet attr = elem.getAttributes();
			Boolean b = (Boolean) attr.getAttribute("wavyUnderline");
			if(b != null && b.booleanValue()){
				Color c = (Color) attr.getAttribute("wavyColor");
				paintJaggedLine( g, allocation,(c!=null)?c:Color.red);
			}
		}
		
		// paint jagged line
		public void paintJaggedLine( Graphics g, Shape a,Color c) {
			int y = ( int ) ( a.getBounds().getY() + a.getBounds().getHeight() );
			int x1 = ( int ) a.getBounds().getX();
			int x2 = ( int ) ( x1 + a.getBounds().getWidth() );

			Color old = g.getColor();
			g.setColor(c);
			for ( int i = x1; i < x2-3; i += 6 ) {
				//g.drawArc( i + 3, y - 3, 3, 3, 0, 180 );
				//g.drawArc( i + 6, y - 3, 3, 3, 180, 181 );
				g.drawArc( i, y - 3, 3, 2, 0, 180 );
				g.drawArc( i+3,y - 3,3, 2, 180, 181 );
			}
			g.setColor( old );
		}
	}
	
	/**
	 * Added component view to get highlighting to work
	 *
	 */
	private class ReportComponentView extends ComponentView {
		public ReportComponentView( Element elem ) {
			super( elem );
		}
		// paint component
		public void paint( Graphics g, Shape allocation ) {
			super.paint( g,allocation);	
			
			int s = getStartOffset();
			int e = getEndOffset();
			
			Highlighter h = textPane.getHighlighter();
			Highlighter.Highlight [] hs = h.getHighlights();
			if(hs != null){
				for(int i=0;i<hs.length;i++){
					int p0 = hs[i].getStartOffset();
					int p1 = hs[i].getEndOffset();
					// if label is component withing the highlight, then it
					// must be highlighted
					//if(p0 <= s && e <= p1){
					if( (s >= p0 && s < p1) || (e > p0 && e <= p1) ){
						hs[i].getPainter().paint(g,s,e,allocation,textPane);
						break;
					}
				}
				
			}		
		}	 
	}
}




