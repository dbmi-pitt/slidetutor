package edu.pitt.dbmi.tutor.modules.interventions;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalSliderUI;

import edu.pitt.dbmi.tutor.ITS;
import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.beans.Operation;
import static edu.pitt.dbmi.tutor.messages.Constants.*;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.NodeEvent;
import edu.pitt.dbmi.tutor.messages.ProblemEvent;
import edu.pitt.dbmi.tutor.messages.Query;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.model.InteractiveTutorModule;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.model.Tutor;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;


public class MetaCognitiveSkillometer implements InteractiveTutorModule {
	private Properties defaultConfig;
	private Tutor tutor;
	private JPanel component,userOKPanel;
	private JButton userOK,dialogOK;
	private JSlider userSlider;
	private Map<String,Counts> valueMap,currentValueMap;
	private Map<String,String> responseMap;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MetaCognitiveSkillometer mci = new MetaCognitiveSkillometer();
		mci.prompQuestion();
		mci.showDialog();
		System.out.println("----------------");
	}

	/**
	 * get a manual page that represents this module
	 * @return
	 */
	public URL getManual(){
		return Config.getManual(getClass());
	}
	
	/*
	private JPanel createComponent() {
		//if (component == null) {
		component = new JPanel();
		component.setLayout(new BorderLayout());

		final String TEXT = "<html><center><table bgcolor=#FFF380 width=390 cellspacing=5><tr><td>"
				+ "Please provide your <b>Self Evaluation Assessment</b>. " 
				+ "Move the slider below to the position that you feel most "
				+ "reflects the accuracy of your self assessments (sure/unsure). Then press the <b>Tutor Assessment</b> button to see how "
				+ "the tutor assessed your accuracy." + "</td></tr></table></center><br>&nbsp;</html>";

		// user slider
		userSlider = createSlider(true);
		userSlider.setValue(userSlider.getMinimum());
		userSlider.setBorder(new TitledBorder("Self Evaluation Assessment"));

		JPanel ap = new JPanel();
		ap.setLayout(new BorderLayout());
		JLabel lb = new JLabel(TEXT);
		lb.setFont(lb.getFont().deriveFont(Font.PLAIN));
		lb.setHorizontalAlignment(JLabel.CENTER);
		ap.add(lb, BorderLayout.NORTH);
		ap.add(userSlider, BorderLayout.CENTER);

		// userSlider.setEnabled(false);
		userSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				userOK.setEnabled(true);
			}
		});

		// add register button
		userOKPanel = new JPanel();

		userOK = new JButton("Tutor Assessment");
		userOK.setIcon(UIHelper.getIcon("/icons/help-manager/Nerd60.gif"));
		userOK.setHorizontalTextPosition(SwingConstants.CENTER);
		userOK.setVerticalTextPosition(SwingConstants.BOTTOM);
		userOK.setPreferredSize(new Dimension(165, 165));
		userOK.setEnabled(false);
		userOK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				component.remove(userOKPanel);
				component.add(getSkillPanel(), BorderLayout.CENTER);
				component.validate();
				component.repaint();
				userSlider.setEnabled(false);
				if(dialogOK != null)
					dialogOK.setEnabled(true);
			}
		});

		userOKPanel.setLayout(new GridBagLayout());
		userOKPanel.setPreferredSize(new Dimension(400, 300));
		userOKPanel.add(userOK, new GridBagConstraints());

		// add components
		component.add(ap, BorderLayout.NORTH);
		component.add(userOKPanel, BorderLayout.CENTER);

		//}
		return component;
	}
	*/
	
	private JPanel createSkillometerComponent() {
		//if (component == null) {
		component = new JPanel();
		component.setLayout(new BorderLayout());

		final String TEXT = "<html><center><table bgcolor=#FFF380 width=390 cellspacing=5><tr><td>"
				+ "Below is your <b>Self Evaluation Assessment</b> and the tutor's assessment of your accuracy. "
				+ "The <b>Tutor Evaluation Assessment</b> is for the current case, while individual findings and " 
				+"diagnoses that may be listed below reflect your cumulative accuracy in previous cases as well as the current case. " 
				+"Please look over the information and click <b>OK</b> when you're done.</td></tr></table></center><br>&nbsp;</html>";

		// user slider
		int val = (userSlider != null)?userSlider.getValue():0;
		userSlider = createSlider(true);
		userSlider.setValue(val);
		userSlider.setBorder(new TitledBorder("Self Evaluation Assessment"));

		// merge values
		mergeValueMap(getCurrentValueMap());
		
		// print debug message
		debugValues("CURRENT VALUES",getCurrentValueMap());
		debugValues("OVERALL VALUES",getValueMap());
		if(ITS.getInstance().isDebugEnabled())
			System.out.println("Overall Bias "+getOverallCounts().getBiasValue());
		
		// add overall confidence
		JSlider overall = createSlider(true);
		overall.setValue((int) (100 * getOverallCounts().getBiasValue()));
		overall.setEnabled(false);
		overall.setBorder(new TitledBorder("Tutor Evaluation Assessment"));
		
		JPanel caseAssesment = new JPanel();
		caseAssesment.setLayout(new BoxLayout(caseAssesment, BoxLayout.Y_AXIS));
		caseAssesment.setBorder(new TitledBorder("Case Assessment"));
		caseAssesment.add(userSlider);
		caseAssesment.add(overall);
		
		
		JPanel ap = new JPanel();
		ap.setLayout(new BorderLayout());
		JLabel lb = new JLabel(TEXT);
		lb.setFont(lb.getFont().deriveFont(Font.PLAIN));
		lb.setHorizontalAlignment(JLabel.CENTER);
		ap.add(lb, BorderLayout.NORTH);
		ap.add(caseAssesment, BorderLayout.CENTER);

		
		// add components
		component.add(ap, BorderLayout.NORTH);
		component.add(getSkillPanel(), BorderLayout.CENTER);
		component.validate();
		component.repaint();
		userSlider.setEnabled(false);
		
		if(dialogOK != null)
			dialogOK.setEnabled(true);
	
		return component;
	}

	
	
	private void debugValues(String title, Map<String,Counts> map) {
		// print out current values
		if(ITS.getInstance().isDebugEnabled()){
			System.out.println("\n"+title+"\n---------------");
			System.out.println("Concept, TP, FP, TN, FN");
			for(String a : map.keySet()){
				Counts c = map.get(a);
				System.out.println(a +", " +c.TP+", "+c.FP+", " +c.TN+", "+c.FN);
			}
			System.out.println("");
		}
	}


	private JPanel createQuestionComponent() {
		component = new JPanel();
		component.setLayout(new BorderLayout());

		final String TEXT = "<html><center><table bgcolor=#FFF380 width=390 cellspacing=5><tr><td>"
				+ "Please provide your <b>Self Evaluation Assessment</b>. " 
				+ "Move the slider below to the position that you feel most "
				+ "reflects the accuracy of your self assessments (sure/unsure). Then press the <b>OK</b> button."
				+ "</td></tr></table></center><br>&nbsp;</html>";

		// user slider
		userSlider = createSlider(true);
		userSlider.setValue(userSlider.getMinimum());
		userSlider.setBorder(new TitledBorder("Self Evaluation Assessment"));

		// userSlider.setEnabled(false);
		userSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if(dialogOK != null)
					dialogOK.setEnabled(true);
			}
		});
		
		JPanel ap = new JPanel();
		ap.setLayout(new BorderLayout());
		JLabel lb = new JLabel(TEXT);
		lb.setFont(lb.getFont().deriveFont(Font.PLAIN));
		lb.setHorizontalAlignment(JLabel.CENTER);
		ap.add(lb, BorderLayout.NORTH);
		ap.add(userSlider, BorderLayout.CENTER);

		// add components
		component.add(ap, BorderLayout.NORTH);

		return component;
	}
	

	/**
	 * get the value map for items being presented in
	 * the dialog box
	 * @return
	 */
	
	public Map<String,Counts> getValueMap(){
		if(valueMap == null){
			valueMap = new LinkedHashMap<String,Counts>();
		}
		
		// some BS for testing UI
		/*valueMap.put("Finding.Blister.Concept12",new Counts(2,0,0,0));
		valueMap.put("Finding.Nuclear_Dust",new Counts(1,2,0,1));
		valueMap.put("Finding.Neutrophils",new Counts(1,2,2,1));
		valueMap.put("Finding.Neutrophils2",new Counts(1,2,2,1));
		valueMap.put("Finding.Neutrophils3",new Counts(1,2,2,1));
		valueMap.put("Finding.Neutrophils4",new Counts(1,2,2,1));
		valueMap.put("Finding.Neutrophils5",new Counts(1,2,2,1));
		valueMap.put("Finding.Neutrophils6",new Counts(1,2,2,1));
		valueMap.put("Finding.Neutrophils7",new Counts(1,2,2,1));
		valueMap.put("Finding.Neutrophils8",new Counts(1,2,2,1));
		valueMap.put("Diagnosis.LICHEN_PLANUS.Concept2",new Counts(0,2,0,0));
		valueMap.put("Diagnosis.ACRODERMATITIS_CHRONICA_ATROPHICANS.Concept4",new Counts(0,2,0,0));
		//System.out.println(valueMap);
*/		
		return valueMap;
	}
	
	/**
	 * get the value map for items being presented in
	 * the dialog box
	 * @return
	 */
	
	private Map<String,Counts> getCurrentValueMap(){
		if(currentValueMap == null){
			currentValueMap = new LinkedHashMap<String,Counts>();
		}
		return currentValueMap;
	}
	
	/**
	 * merge values
	 */
	private void mergeValueMap(Map<String,Counts> values){
		// make a copy of the current map, collapse descriptions
		Map<String,Counts> map = new HashMap<String, Counts>();
		for(String key: values.keySet()){
			Counts value = values.get(key);
			
			// strip IDs
			int x = key.indexOf(".",key.indexOf(".")+1);
			if(x > -1)
				key = key.substring(0,x);
			
			if(map.containsKey(key)){
				map.get(key).add(value);
			}else{
				map.put(key,value);
			}
		}
		
		// put everything together
		for(String c: map.keySet()){
			if(getValueMap().containsKey(c)){
				getValueMap().get(c).add(map.get(c));
			}else{
				getValueMap().put(c,map.get(c));
			}
		}
	}
	
	/**
	 * get value map that is displayed to a her
	 * @return
	 */
	private Map<String,String> getValueMapAsInput(){
		Map<String,String> map = new LinkedHashMap<String, String>();
		
		map.put("Self Assessment",""+((double)userSlider.getValue())/100);
		map.put("Overall Assessment",""+getOverallCounts().getBiasValue());
		
		for(String desc: getValueMap().keySet()){
			if(desc.startsWith(TYPE_FINDING) || desc.startsWith(TYPE_DIAGNOSIS)){
				Counts c = getValueMap().get(desc);
				if(c.isUserful()){
					map.put(desc,""+c.getBiasValue());
				}
			}
		}
		
		return map;
	}
	
	/**
	 * get overall counts
	 * @return
	 */
	private Counts getOverallCounts(){
		Counts overall = new Counts();
		for(Counts c: getCurrentValueMap().values()){
			overall.add(c);
		}
		return overall;
	}
	
	
	private JPanel getSkillPanel() {
		JPanel skillPanel = new JPanel();
		skillPanel.setLayout(new BorderLayout());
	
		
		// fetch column maps
		List<String> fList = new ArrayList<String>();
		List<String> dList = new ArrayList<String>();
		for(String desc : getValueMap().keySet()){
			if(getValueMap().get(desc).isUserful()){
				if(desc.startsWith(TYPE_FINDING)){
					fList.add(desc);
				}else if(desc.startsWith(TYPE_DIAGNOSIS)){
					dList.add(desc);
				}
			}
		}
		
		if(fList.isEmpty() && dList.isEmpty())
			return skillPanel;
		
		// sort things
		Collections.sort(fList);
		Collections.sort(dList);
		
		// now setup a border
		skillPanel.setBorder(new CompoundBorder(new EmptyBorder(40,0,0,0),new CompoundBorder(
		new TitledBorder("Cumulative Assessment for Findings and Diagnoses"),new EmptyBorder(10,0,0,0))));
				
		// create finding panel
		JPanel fp = new JPanel();
		fp.setLayout(new GridBagLayout());
		//fp.setBorder(new TitledBorder("Finding Assessments"));
		GridBagConstraints fc = new GridBagConstraints();
		fc.gridx = fc.gridy = 0;
		fc.weightx = fc.weighty = 1.0;
		
		for(String desc : fList){
			ConceptEntry e = ConceptEntry.getConceptEntry(desc);
			fp.add(createProgressBar(e.getText(), (int) (100 * getValueMap().get(desc).getBiasValue())),fc);
			fc.gridy ++;
		}
		// make columns equal size
		for(int i=0;i< dList.size() - fList.size();i++){
			fp.add(Box.createRigidArea(new Dimension(50,50)),fc); fc.gridy++;
		}
		
		
		// creating diagnosis panel
		JPanel dp = new JPanel();
		dp.setLayout(new GridBagLayout());
		//dp.setBorder(new TitledBorder("Diagnosis Assessments"));
		GridBagConstraints dc = new GridBagConstraints();
		dc.gridx = dc.gridy = 0;
		dc.weightx = dc.weighty = 1.0;
		for(String desc : dList){
			ConceptEntry e = ConceptEntry.getConceptEntry(desc);
			dp.add(createProgressBar(e.getText(), (int) (100 * getValueMap().get(desc).getBiasValue())),dc);
			dc.gridy ++;
		}
		// make columns equal size
		for(int i=0;i< fList.size() - dList.size();i++){
			dp.add(Box.createRigidArea(new Dimension(50,50)),dc); dc.gridy++;
		}
		
		// create panel
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(0,2));
		p.add(fp);
		p.add(dp);
		
		
		JScrollPane scroll = new JScrollPane(p);
		//scroll.setBorder(new CompoundBorder(new TitledBorder("Itemized Evaluation Assesment"),new EmptyBorder(10,0,0,0)));
		if(p.getPreferredSize().height > 300)
			scroll.getViewport().setPreferredSize(new Dimension(400,300));
		scroll.setBorder(new EmptyBorder(5,0,0,5));
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		skillPanel.add(scroll,BorderLayout.CENTER);
		
		return skillPanel;
	}
	
	
	/**
	 * insert G or BIOS formala here
	 * @param c
	 * @return
	 *
	private double calculateValue(Counts c){
		//TODO: calculate the actual value from counts
		
		// calculate  some formulas
		// bias = (TP + FP) / (TP + FP + TN + FN)
		// discrimination = TP / (TP + FN) - FP / (FP + TN)
		// G = (TP*TN - FP*FN) / (TP*TN + FP*FN)
		c.normalize();
		double x = (c.TP * c.TN - c.FP*c.FN)/ (c.TP * c.TN + c.FP*c.FN);
		System.out.println(x);
		return x;
	}
	*/
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	private JPanel createProgressBar(String name, int value) {
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		JLabel lbl = new JLabel(name);
		
		// shorten a name if necessary
		FontMetrics fm =lbl.getFontMetrics(lbl.getFont());
		if(fm.stringWidth(name) > 200){
			lbl.setToolTipText(name);
			// come up with shorter name that fits
			int n = fm.stringWidth("..");
			for(;fm.stringWidth(name) > 200-n; name = name.substring(0,name.length()-1));
			lbl.setText(name+"..");
		}
		
		// add title
		p.add(lbl, BorderLayout.NORTH);
		JSlider pr = createSlider(false);
		pr.setPreferredSize(new Dimension(200, 30));
		pr.setValue(value);
		pr.setEnabled(false);
		p.add(pr, BorderLayout.CENTER);

		return p;
	}

	/**
	 * create custom slider
	 * 
	 * @return
	 */
	private JSlider createSlider(boolean labels) {
		// create labels
		Dictionary<Integer,JComponent> d = new Hashtable<Integer, JComponent>();
		d.put(-100,new JLabel("underconfident"));
		d.put(0,new JLabel("perfect"));
		d.put(100,new JLabel("overconfident"));
		
		
		JSlider slider = new JSlider(-100, 100);
		slider.setPaintTicks(true);
		slider.setMajorTickSpacing(100);
		//slider.setMinorTickSpacing(25);
		if(labels){
			slider.setLabelTable(d);
			slider.setPaintLabels(true);
		}
		slider.setUI(new SliderUI());

		return slider;
	}

	
	/**
	 * custom slider UI to show what we want
	 * @author tseytlin
	 */
	private class SliderUI extends MetalSliderUI {
		private final Color green  = new Color(0,150,0);
		private final Color yellow = new Color(250,200,0);
		
		protected int getTrackWidth (){
			 return 8;
		 }
		public void paintTrack(Graphics g) {
			g.translate(trackRect.x, trackRect.y);

			int trackLeft = 0;
			int trackTop = 0;
			int trackRight = 0;
			int trackBottom = 0;

			// Draw the track
			if (slider.getOrientation() == JSlider.HORIZONTAL) {
				trackBottom = (trackRect.height - 1) - getThumbOverhang();
				trackTop = trackBottom - (getTrackWidth() - 1);
				trackRight = trackRect.width - 1;
			}

			g.setColor(MetalLookAndFeel.getControlDarkShadow());
			g.drawRect(trackLeft, trackTop, (trackRight - trackLeft) - 1, (trackBottom - trackTop) - 1);

			g.setColor(MetalLookAndFeel.getControlHighlight());
			g.drawLine(trackLeft + 1, trackBottom, trackRight, trackBottom);
			g.drawLine(trackRight, trackTop + 1, trackRight, trackBottom);

			g.setColor(MetalLookAndFeel.getControlShadow());
			g.drawLine(trackLeft + 1, trackTop + 1, trackRight - 2, trackTop + 1);
			g.drawLine(trackLeft + 1, trackTop + 1, trackLeft + 1, trackBottom - 2);

			filledSlider = true;
			
			// Draw the fill
			if (filledSlider) {
				int middleOfThumb = 0;
				int fillTop = 0;
				int fillLeft = 0;
				int fillBottom = 0;
				int fillRight = 0;

				if (slider.getOrientation() == JSlider.HORIZONTAL) {
					middleOfThumb = thumbRect.x + (thumbRect.width / 2);
					middleOfThumb -= trackRect.x; // To compensate for the
													// g.translate()
					fillTop = !slider.isEnabled() ? trackTop : trackTop + 1;
					fillBottom = !slider.isEnabled() ? trackBottom - 1 : trackBottom - 2;

					if (!drawInverted()) {
						fillLeft = !slider.isEnabled() ? trackLeft : trackLeft + 1;
						fillRight = middleOfThumb;
					} else {
						fillLeft = middleOfThumb;
						fillRight = !slider.isEnabled() ? trackRight - 1 : trackRight - 2;
					}
				} 

				g.setColor((slider.isEnabled()?slider.getBackground():Color.gray));
				g.drawLine(fillLeft, fillTop, fillRight, fillTop);
				g.drawLine(fillLeft, fillTop, fillLeft, fillBottom);

				// draw green rectangle
				//g.setColor(green);
				//g.fillRect(fillLeft + 1, fillTop + 1, fillRight - fillLeft, fillBottom - fillTop);
				
				// draw yellow rectangle
				//int width = Math.min(fillRight - fillLeft,trackRect.width/2);
				//g.setColor(yellow);
				//g.fillRect(fillLeft + 1, fillTop + 1, width, fillBottom - fillTop);
				
				// draw BIAS gradient around center
				GradientPaint gradient = new GradientPaint(fillLeft + 1,fillTop +1,Color.white,trackRect.width/2 - 2,fillTop +1,Color.blue,true);
				((Graphics2D)g).setPaint(gradient);
				//g.setColor(Color.yellow);
				g.fillRect(fillLeft + 1,fillTop +1, trackRect.width-4,fillBottom - fillTop);
				
			}

			g.translate(-trackRect.x, -trackRect.y);
		}
	}

	public void showDialog() {
		if(!getTutor().isInteractive())
			return;
		
		//JOptionPane.showMessageDialog(null, getComponent(), "", JOptionPane.PLAIN_MESSAGE);
		final JDialog dialog = new JDialog(Config.getMainFrame(),"Skillometer");
		
		dialogOK = new JButton("OK");
		dialogOK.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				dialog.dispose();
				
				// do notify
				Communicator.getInstance().sendMessage(InterfaceEvent.createInterfaceEvent(MetaCognitiveSkillometer.this,TYPE_DIALOG,getName(),ACTION_CLOSED));
			}
		});
		dialogOK.setEnabled(false);
		
		// build dialog
		JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout());
		buttons.add(dialogOK);
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if(dialogOK.isEnabled())
					dialog.dispose();
			}
		});
		dialog.setModal(false);
		dialog.setResizable(true);
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(createSkillometerComponent(),BorderLayout.CENTER);
		panel.add(buttons,BorderLayout.SOUTH);
		panel.setBorder(new CompoundBorder(new EmptyBorder(10,10,10,10),new BevelBorder(BevelBorder.RAISED)));
		dialog.setContentPane(panel);
		dialog.pack();
		UIHelper.centerWindow(Config.getMainFrame(),dialog);
		
		// notify that dialog was opened
		InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_DIALOG,getName(),ACTION_OPENED);
		ie.setInput(getValueMapAsInput());
		Communicator.getInstance().sendMessage(ie);
		
		dialog.setVisible(true);
		
		// now wait for closing
		while(dialog.isVisible()){
			UIHelper.sleep(500);
		}
	}

	
	/**
	 * prompt self assesment question
	 */
	public void prompQuestion(){
		if(!getTutor().isInteractive())
			return;
			
		final JDialog dialog = new JDialog(Config.getMainFrame(),"Question");
		
		final InterfaceEvent ies = InterfaceEvent.createInterfaceEvent(this,TYPE_DIALOG,getName()+" Question",ACTION_OPENED);
		
		dialogOK = new JButton("OK");
		dialogOK.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				
				// do notify
				InterfaceEvent iee = InterfaceEvent.createInterfaceEvent(MetaCognitiveSkillometer.this,TYPE_DIALOG,getName()+" Question",ACTION_CLOSED);
				
				// create client event
				String text  = "Self Evaluation Assesment";
				String reply = ""+((double)userSlider.getValue())/100;
				String label = (reply != null)?text+" = "+reply:text;
				ClientEvent ce = ClientEvent.createClientEvent(MetaCognitiveSkillometer.this,TYPE_QUESTION,label,ACTION_ASK);
				ce.setSource(this.getClass().getSimpleName());
				Map<String,String> map = new LinkedHashMap<String,String>();
				map.put("Question",text);
				if(reply != null)
					map.put("Answer",reply);
				ce.setInput(map);
				//attach ce				
				ies.setClientEvent(ce);
				iee.setClientEvent(ce);
				
				// send
				Communicator.getInstance().sendMessage(ies);
				Communicator.getInstance().sendMessage(iee);
				Communicator.getInstance().sendMessage(ce);
			}
		});
		dialogOK.setEnabled(false);
		
		
		// build dialog
		JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout());
		buttons.add(dialogOK);
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if(dialogOK.isEnabled())
					dialog.dispose();
			}
		});
		dialog.setModal(true);
		dialog.setResizable(false);
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(createQuestionComponent(),BorderLayout.CENTER);
		panel.add(buttons,BorderLayout.SOUTH);
		panel.setBorder(new CompoundBorder(new EmptyBorder(10,10,10,10),new BevelBorder(BevelBorder.RAISED)));
		dialog.setContentPane(panel);
		dialog.pack();
		UIHelper.centerWindow(Config.getMainFrame(),dialog);
		dialog.setVisible(true);
	}
	
	public void resolveAction(Action action) {
		Operation oper = null;
				
		// figure out which operations to set
		if(POINTER_ACTION_SHOW_SKILLOMETER_DIALOG.equalsIgnoreCase(action.getAction())){
			oper = new Operation(){
				public void run() {
					showDialog();
				}
				public void undo(){
				}
			};
		}else if(POINTER_ACTION_SHOW_SKILLOMETER_QUESTION.equalsIgnoreCase(action.getAction())){
			oper = new Operation(){
				public void run() {
					prompQuestion();
				}
				public void undo(){
				}
			};
		}
		// set operations
		action.setOperation(oper);

	}
	

	public Action[] getSupportedActions() {
		return new Action []{
			new Action(getClass().getSimpleName(),POINTER_ACTION_SHOW_SKILLOMETER_DIALOG,"","Display a Meta Cognitive Skillometer Dialog."),
			new Action(getClass().getSimpleName(),POINTER_ACTION_SHOW_SKILLOMETER_QUESTION,"","Prompt user to estimate their <font color=green>Self Evaluation Assesment</font>.")	
				
		};
	}

	
	

	public Component getComponent() {
		return component;
	}


	public ImageIcon getScreenshot() {
		return Config.getScreenshot(getClass());
	}


	public Tutor getTutor() {
		return tutor;
	}


	public boolean isEnabled() {
		return true;
	}


	public boolean isInteractive() {
		return true;
	}


	public void reconfigure() {
	}


	public void setEnabled(boolean b) {
	}


	public void setInteractive(boolean b) {
	}


	public void setTutor(Tutor t) {
		tutor  = t;
		
	}

	public void dispose() {
	}


	public Properties getDefaultConfiguration() {
		if(defaultConfig == null){
			defaultConfig = Config.getDefaultConfiguration(getClass());
		}
		return defaultConfig;
	}


	public String getDescription() {
		return "Monitors and displays user's metacognitive performance using information collected " +
				"from the Coloring Book Interface.";
	}


	public String getName() {
		return "Meta Cognitive Skillometer";
	}

	public Message[] getSupportedMessages() {
		return new Message [0];
	}


	public String getVersion() {
		return "1.0";
	}


	public void load() {
		// just once prompt for user resources
		final ProtocolModule protocol = ITS.getInstance().getProtocolModule();
		if(protocol != null){
			// launc a separate thread
			(new Thread(new Runnable(){
				public void run(){
					
					// username can only become available once main window materealizes
					while(Config.getMainFrame() == null || !Config.getMainFrame().isShowing()){
						UIHelper.sleep4sec(1);
					}
					//long time = System.currentTimeMillis();
					// query for past user sessions under the same condition
					Query query = Query.createUsernameQuery(Config.getUsername());
					query.addCondition(Config.getCondition());
					
					// iterate over session
					for(Session s: protocol.getSessions(query)){
						
						// keep track of responses and values
						Map<String,String> responses = new HashMap<String, String>();
						Map<String,Counts> values = new HashMap<String, Counts>();
						
						// iterate over node events
						for(NodeEvent n: s.getNodeEvents()){
							processNodeEvent(n, responses, values);
						}
						
						// merge int values table
						mergeValueMap(values);
					}
					//System.out.println("Overall Value Map for "+getValueMap());
					//System.out.println(System.currentTimeMillis()-time);
					
				}
			})).start();
			
		}
	}
	


	public void receiveMessage(Message msg) {
		// catch tutor events in response to FOK measures
		if(msg instanceof NodeEvent)
			processNodeEvent((NodeEvent) msg,getResponseMap(),getCurrentValueMap());
		
		//process interface event for close during playback
		/*
		if(msg instanceof InterfaceEvent){
			if(TYPE_DIALOG.equals(msg.getType()) && ACTION_CLOSED.equals(msg.getAction()) && msg.getLabel().startsWith(getName())){
				dialogOK.setEnabled(true);
				dialogOK.doClick();
			}
		}else if(msg instanceof ClientEvent){
			if(TYPE_QUESTION.equals(msg.getType()) && ACTION_ASK.equals(msg.getAction()) && msg.getLabel().startsWith("Self Evaluation Assesment")){
				Object o = msg.getInputMap().get("Answer");
				if(o != null){
					try{
						userSlider.setValue(Integer.parseInt(""+o)*100);
					}catch(NumberFormatException ex){}
				}
			}
		}*/
		
	}
	
	/**
	 * process node event to account
	 * @param ne
	 * @param responses
	 * @param values
	 */
	private void processNodeEvent(NodeEvent ne, Map<String,String> responses, Map<String,Counts> values){
		// catch tutor events in response to FOK measures
		if(ACTION_ADDED.equals(ne.getAction()) || ACTION_REFINE.equals(ne.getAction())){
			String response = ne.getResponse();
			
			// make sure that if this is incorrect Dx that is correct now we mark it incorrect
			if(RESPONSE_CONFIRM.equals(response) && Arrays.asList(ERROR_DIAGNOSIS_IS_SUPPORTED_BUT_NOT_GOAL).contains(ne.getError()))
				response = RESPONSE_FAILURE;
			if(RESPONSE_FAILURE.equals(response) && Arrays.asList(ERROR_DIAGNOSIS_IS_CORRECT_BUT_NOT_SUPPORTED).contains(ne.getError()))
				response = RESPONSE_CONFIRM;
						
			responses.put(ne.getObjectDescription(),response);
		}else if(ACTION_SELF_CHECK.equals(ne.getAction())){
			
			// only bother of some types of nodeEvents
			if(!Arrays.asList(TYPE_FINDING,TYPE_HYPOTHESIS,TYPE_DIAGNOSIS).contains(ne.getType()))
				return;
			
			// init concept
			String entry = ne.getObjectDescription();
			String response = responses.get(entry);
				
			// now gather information about self check values
			// to count coloring only once per node, simple overwrite with most
			// recent values
			Counts c = new Counts();
			values.put(entry,c);
			
			// do the count of analyzed concepts
			Map input = (Map) ne.getInput();
			
			// shit we fucked up, lets compensate
			// hopefully this should never happen
			if(response == null){
				if(CONCEPT_SURE.equals(input.get("fok"))){
					response = RESPONSE_CONFIRM.equals(ne.getResponse())?RESPONSE_CONFIRM:RESPONSE_FAILURE;
				}else if(CONCEPT_UNSURE.equals(input.get("fok"))){
					response = RESPONSE_CONFIRM.equals(ne.getResponse())?RESPONSE_FAILURE:RESPONSE_CONFIRM;
				}
			}
			
			// now determine contingency table
			if(CONCEPT_SURE.equals(input.get("fok"))){
				if(RESPONSE_CONFIRM.equals(response)){
					c.TP ++;
				}else if(RESPONSE_FAILURE.equals(response)){
					c.FP ++;
				}
			}else if(CONCEPT_UNSURE.equals(input.get("fok"))){
				if(RESPONSE_CONFIRM.equals(response)){
					c.FN ++;
				}else if(RESPONSE_FAILURE.equals(response)){
					c.TN ++;
				}
			}
		}
	}
	

	private Map<String,String> getResponseMap(){
		if(responseMap == null)
			responseMap = new HashMap<String, String>();
		return responseMap;
	}
	

	public void reset() {
		getResponseMap().clear();
		getCurrentValueMap().clear();
	}
	
	
	/**
	 * container for TP, TN, FP, and FN values
	 * @author tseytlin
	 */
	private class Counts{
		public int TP,TN,FP,FN;
		public Counts(){}
		public Counts(int tp,int tn, int fp, int fn){
			TP = tp;
			TN = tn;
			FP = fp;
			FN = fn;
		}
		
		public void add(Counts c){
			TP += c.TP;
			FP += c.FP;
			TN += c.TN;
			FN += c.FN;
		}
		public Counts clone(){
			Counts c = new Counts();
			c.TP = TP;
			c.TN = TN;
			c.FP = FP;
			c.FN = FN;
			return c;
		}
		
		/**
		 * get G value
		 * @return
		 */
		public double getGValue(){
			Counts c = clone();
			// normalize
			if(c.TP + c.TN > 0){
				if(c.TP == 0)
					c.TP = 1;
				if(c.TN == 0)
					c.TN = 1;
			}
			if(c.FP + c.FN > 0){
				if(c.FP == 0)
					c.FP = 1;
				if(c.FN == 0)
					c.FN = 1;
			}
			
			return ((double)(c.TP * c.TN - c.FP*c.FN))/ (c.TP * c.TN + c.FP*c.FN);
		}
		
		/**
		 * get bias value
		 * @return
		 */
		public double getBiasValue(){
			return ((double)(FP - FN)) / (TP + FP + TN + FN);
		}
		
		/**
		 * insert G or BIOS formala here
		 * @param c
		 * @return
		 *
		private double calculateValue(Counts c){
			//TODO: calculate the actual value from counts
			
			// calculate  some formulas
			// bias = (FP - FN) / (TP + FP + TN + FN)
			// discrimination = TP / (TP + FN) - FP / (FP + TN)
			// G = (TP*TN - FP*FN) / (TP*TN + FP*FN)
			c.normalize();
			double x = (c.TP * c.TN - c.FP*c.FN)/ (c.TP * c.TN + c.FP*c.FN);
			System.out.println(x);
			return x;
		}
		*/
		public String toString(){
			return "TP = "+TP+", FP = "+FP+", TN = "+TN+" , FN = "+FN;
		}
		
		public int sum(){
			return TP+TN+FP+FN;
		}
		
		public boolean isUserful(){
			return sum() >= 2;
		}
	}
}
