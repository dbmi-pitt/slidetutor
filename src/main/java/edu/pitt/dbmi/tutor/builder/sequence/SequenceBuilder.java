package edu.pitt.dbmi.tutor.builder.sequence;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.*;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.modules.expert.DomainExpertModule;
import edu.pitt.dbmi.tutor.ui.CaseSelectorPanel;
import edu.pitt.dbmi.tutor.ui.DomainSelectorPanel;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;


public class SequenceBuilder implements ActionListener {
	private final String DEFAULT_CONDITION = "";
	private Component component,listComponent;
	private JToolBar toolbar;
	//private List<String> caseSequenceMap.get(getCondition());
	private JList sequenceList;
	private UIHelper.ListModel sequenceModel;
	private File file;
	private CaseSelectorPanel caseSelectorPanel;
	private ExpertModule expert;
	private String condition;
	private Map<String,List<String>> caseSequenceMap;
	private JComboBox conditionSelector;

	public SequenceBuilder(ExpertModule expert){
		//caseSequenceMap.get(getCondition()) = new ArrayList<String>();
		caseSequenceMap = new LinkedHashMap<String, List<String>>();
		this.expert = expert;
	}
	
	/**
	 * 
	 * @return
	 */
	public Component getListComponent(){
		if(listComponent == null){
			//sequenceModel = new UIHelper.ListModel(caseSequenceMap.get(getCondition()));
			sequenceList = new JList();
			sequenceList.setDragEnabled(true);
			sequenceList.addMouseListener(new MouseAdapter(){
				public void mouseClicked(MouseEvent e) {
					if(e.getClickCount() == 2){
						Object obj = sequenceList.getSelectedValue();
						if(obj != null){
							doEdit(obj.toString());
						}
					}
				}
				
			});
			sequenceList.setCellRenderer(new DefaultListCellRenderer(){
				public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
					JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
					lbl.setText(OntologyHelper.getCaseName((String)value));
					return lbl;
				}
				
			});
			switchCondition(getCondition());
			new DropTarget(sequenceList,new DropTargetAdapter() {
				public void drop(DropTargetDropEvent dtde) {
					Object obj = sequenceList.getSelectedValue();
					if(obj != null){
						int i = sequenceList.locationToIndex(dtde.getLocation());
						if(i > -1 ){
							caseSequenceMap.get(getCondition()).remove(obj);
							caseSequenceMap.get(getCondition()).add(i,(String)obj);
							sync(sequenceList);
							sequenceList.setSelectedIndex(i);
						}	
					}
				}
			});
			
			
			//task
			JScrollPane s = new JScrollPane(sequenceList);
			s.setPreferredSize(new Dimension(500,500));
			
			JPanel p3 = new JPanel();
			p3.setLayout(new BorderLayout());
			p3.add(createToolBar("Tutor Case Sequence","sequence"),BorderLayout.NORTH);
			p3.add(s,BorderLayout.CENTER);
			listComponent = p3;
		}
		return listComponent;
	}
	
	public JList getSequenceList(){
		return sequenceList;
	}
	
	/**
	 * get component 
	 * @return
	 */
	public Component getComponent(){
		if(component == null){
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			panel.add(getToolBar(),BorderLayout.NORTH);
			panel.add(getListComponent(),BorderLayout.CENTER);
			component = panel;
		}
		return component;
	}
	
	
	public String getCondition() {
		if(condition == null)
			condition = DEFAULT_CONDITION;
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
		switchCondition(condition);
	}

	
	/**
	 * create tool bar
	 * @return
	 */
	private JToolBar createToolBar(String title, String action){
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.setBackground(Color.white);
		toolbar.add(UIHelper.createButton("add-"+action,"Add to "+title, 
		UIHelper.getIcon(Config.getProperty("icon.toolbar.add"),16),this));
		toolbar.add(UIHelper.createButton("remove-"+action,"Remove from "+title,
		UIHelper.getIcon(Config.getProperty("icon.toolbar.rem"),16),this));
		toolbar.addSeparator();
		toolbar.add(new JLabel(title));
		return toolbar;
	}
	
	/**
	 * get toolbar for component
	 * @return
	 */
	public JToolBar getToolBar(){
		if(toolbar == null){
			toolbar = new JToolBar();
			toolbar.add(UIHelper.createButton("new","Create New Sequence File",
					UIHelper.getIcon(Config.getProperty("icon.toolbar.new"),24),this));
			toolbar.add(UIHelper.createButton("open","Open Sequence File",
					UIHelper.getIcon(Config.getProperty("icon.toolbar.open"),24),this));
			toolbar.add(UIHelper.createButton("save","Save Sequence File",
					UIHelper.getIcon(Config.getProperty("icon.toolbar.save"),24),this));
			toolbar.addSeparator();
			toolbar.add(UIHelper.createButton("save-as","Save Sequnce File As",
					UIHelper.getIcon(Config.getProperty("icon.toolbar.save.as"),24),this));
			
			toolbar.addSeparator(new Dimension(30,30));
			
			DefaultComboBoxModel model = new DefaultComboBoxModel();
			for(String c: caseSequenceMap.keySet()){
				model.addElement(c);
			}
				
			conditionSelector = new JComboBox(model);
			conditionSelector.setToolTipText("Select a Condition Associated with a Sequence");
			conditionSelector.addActionListener(this);
			toolbar.add(conditionSelector);
			
			toolbar.add(UIHelper.createButton("add-condition","Add Condition",UIHelper.getIcon(Config.getProperty("icon.toolbar.add"),24),this));
			toolbar.add(UIHelper.createButton("remove-condition","Remove Condition",UIHelper.getIcon(Config.getProperty("icon.toolbar.rem"),24),this));
			
		}
		return toolbar;
	}
	
	

	public void actionPerformed(ActionEvent e) {
		if("new".equals(e.getActionCommand())){
			doNew();
		}else if("open".equals(e.getActionCommand())){
			doOpen();
		}else if("save".equals(e.getActionCommand())){
			doSave();
		}else if("save-as".equals(e.getActionCommand())){
			doSaveAs();
		}else if("add-sequence".equals(e.getActionCommand())){
			doAddCase();
		}else if("remove-sequence".equals(e.getActionCommand())){
			if(caseSequenceMap.get(getCondition()) != null){
				for(Object o: sequenceList.getSelectedValues()){
					caseSequenceMap.get(getCondition()).remove(o);
				}
				sync(sequenceList);
			}
		}else if("add-condition".equals(e.getActionCommand())){
			doAddCondition();
		}else if("remove-condition".equals(e.getActionCommand())){
			doRemoveCondition();
		}else if(e.getSource() == conditionSelector){
			setCondition(""+conditionSelector.getSelectedItem());
		}
	}
	
	

	private void doAddCase(){
		if(caseSelectorPanel == null){
			caseSelectorPanel = new CaseSelectorPanel(expert);
		}
		// show component
		caseSelectorPanel.showDialog(getComponent());
		for(String s: caseSelectorPanel.getSelectedCases()){
			caseSequenceMap.get(getCondition()).add(s);
		}
		sync(sequenceList);
	}
	
	private void doAddCondition(){
		String text = JOptionPane.showInputDialog(getComponent(),"Add Condition","Condition",JOptionPane.QUESTION_MESSAGE);
		if(text != null && text.length() > 0){
			final String cond = text;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if(caseSequenceMap.containsKey(DEFAULT_CONDITION)){
						// replace default condition
						caseSequenceMap.put(cond,caseSequenceMap.get(DEFAULT_CONDITION));
						caseSequenceMap.remove(DEFAULT_CONDITION);
					}else{
						// replace default condition
						caseSequenceMap.put(cond,new ArrayList<String>());
					}
					((DefaultComboBoxModel)conditionSelector.getModel()).addElement(cond);
					((DefaultComboBoxModel)conditionSelector.getModel()).removeElement(DEFAULT_CONDITION);
					conditionSelector.revalidate();
					setCondition(cond);
				}
			});
		}
	}
	
	private void doRemoveCondition() {
		int r = JOptionPane.showConfirmDialog(getComponent(),"Are you sure you want to delete this condition and all of the associated cases?","Question",
				JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
		if(JOptionPane.YES_OPTION == r){
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					String c = ""+conditionSelector.getSelectedItem();
					caseSequenceMap.remove(c);
					((DefaultComboBoxModel)conditionSelector.getModel()).removeElement(c);
					conditionSelector.revalidate();
					setCondition(""+conditionSelector.getSelectedItem());
				}
			});
		}
	}
	
	/**
	 * sync list content
	 * @param l
	 */
	private void sync(JList l){
		final JList list = l;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				((UIHelper.ListModel)list.getModel()).sync(list);
			}
		});
	}
	
	public void doNew(){
		caseSequenceMap.clear();
		conditionSelector.setModel(new DefaultComboBoxModel(new String [] {DEFAULT_CONDITION}));
		setCondition(getCondition());
	}
	
	private void switchCondition(String cond){
		List<String> list = caseSequenceMap.get(cond);
		if(list == null){
			list = new ArrayList<String>();
			caseSequenceMap.put(cond,list);
		}
		if(sequenceList != null){
			sequenceModel = new UIHelper.ListModel(list);
			sequenceList.setModel(sequenceModel);
			sync(sequenceList);
		}
		if(conditionSelector != null)
			conditionSelector.setSelectedItem(cond);
	}
	
	
	private void doOpen(){
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(file);
		chooser.setFileFilter(new FileFilter(){
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith(".lst");
			}
			public String getDescription() {
				return "Sequence File File (.lst)";
			}
		});
		int r = chooser.showOpenDialog(getComponent());
		if(r == JFileChooser.APPROVE_OPTION){
			file = chooser.getSelectedFile();
			load(file.getAbsolutePath());
		}
	}
	
	
	public void doSaveAs(){
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(file);
		chooser.setFileFilter(new FileFilter(){
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith(".lst");
			}
			public String getDescription() {
				return "Sequence File File (.lst)";
			}
		});
		int r = chooser.showSaveDialog(getComponent());
		if(r == JFileChooser.APPROVE_OPTION){
			file = chooser.getSelectedFile();
			if(!file.getName().endsWith(".lst"))
				file = new File(file.getAbsolutePath()+".lst");
			try{
				save(file);
			}catch(Exception ex){
				ex.printStackTrace();
				JOptionPane.showMessageDialog(getComponent(),
						"Could not save help file "+file.getAbsolutePath(),
						"Error",JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	public void doSave(){
		if(file != null){
			try{
				save(file);
			}catch(Exception ex){
				ex.printStackTrace();
				JOptionPane.showMessageDialog(getComponent(),
						"Could not save help file "+file.getAbsolutePath(),
						"Error",JOptionPane.ERROR_MESSAGE);
			}
		}else{
			doSaveAs();
		}
	}
	
	/**
	 * preview case
	 * @param c
	 */
	private void doEdit(String c){
		if(caseSelectorPanel == null){
			caseSelectorPanel = new CaseSelectorPanel(expert);
		}
		
		// is it a URL???
		if(!UIHelper.isURL(c)){
			doAddOntology();
			if(UIHelper.isURL(""+sequenceList.getSelectedValue())){
				doEdit(""+sequenceList.getSelectedValue());
			}
			return;
		}
		
		// display case
		caseSelectorPanel.displayCase(c);
		
	}
	
	private void updateSequence(List<String> seq, String url){
		List<String> n = new ArrayList<String>(seq);
		seq.clear();
		for(String name: n)
			seq.add(""+OntologyHelper.getCaseURL(name,URI.create(url)));
	}
	
	/**
	 * add ontology to the sequence
	 */
	private void doAddOntology() {
		String msg = "It looks like you do not have domain ontology selected for a a given tutor case sequence.\nWould you like to select it now?";
		int r = JOptionPane.showConfirmDialog(getComponent(),msg,"Select Domain Ontology?",JOptionPane.YES_NO_OPTION);
		if(JOptionPane.YES_OPTION == r){
			if(expert instanceof DomainExpertModule){
				DomainSelectorPanel d = (DomainSelectorPanel) ((DomainExpertModule)expert).getComponent();
				d.showChooserDialog();
				Object o = d.getSelectedObject();
				if(o != null && UIHelper.isURL(o.toString())){
					// update immediate sequence
					updateSequence(caseSequenceMap.get(getCondition()),""+o);
					// update file
					if(caseSequenceMap != null){
						for(String c: caseSequenceMap.keySet()){
							updateSequence(caseSequenceMap.get(c),""+o);
						}
					}
					sync(sequenceList);
				}
			}
		}
	}

	/**
	 * load behavior from location
	 * @param location
	 * @throws FileNotFoundException 
	 */
	public void load(String location) {
		File f = new File(location);
		
		// it could be a non existing file then just create a new one
		if(f == null || (f.getParentFile().isDirectory() && !f.exists())){
			doNew();
			//setFile(f);
			return;
		}
		
		Reader in = null;
		// is it a file?
		try{
			in = new FileReader(f);
			setFile(f);
		}catch(Exception ex){
			try{
				URL u = new URL(location);
				in = new InputStreamReader(u.openStream());
			}catch(Exception ex1){
				try{
					in = new InputStreamReader(getClass().getResourceAsStream(location));
				}catch(Exception ex2){
					Config.getLogger().severe(TextHelper.getErrorMessage(ex2));
					in = null;
				}
			}
		}
		if(in != null){
			try{
				caseSequenceMap.clear();
				
				BufferedReader reader = new BufferedReader(in);
				List<String> cases = new ArrayList<String>();
				for(String line= reader.readLine();line != null; line = reader.readLine()){
					line = line.trim();
					if(line.length() == 0)
						continue;
					if(line.startsWith("[") && line.endsWith("]")){
						String cond = line.substring(1,line.length()-1).trim();
						cases = new ArrayList<String>();
						caseSequenceMap.put(cond,cases);
					}else{
						cases.add(line);
					}
				}
				reader.close();
				
				// put default condition
				if(!cases.isEmpty() && caseSequenceMap.isEmpty())
					caseSequenceMap.put(DEFAULT_CONDITION,cases);
			
				// load conditions
				if(conditionSelector != null){
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							DefaultComboBoxModel model = new DefaultComboBoxModel();
							for(String c: caseSequenceMap.keySet()){
								model.addElement(c);
							}
							conditionSelector.setModel(model);
							conditionSelector.revalidate();
							
						}
					});
				}
				
				// if we have condition
				if(!caseSequenceMap.containsKey(getCondition()) && !caseSequenceMap.isEmpty())
					setCondition(caseSequenceMap.keySet().iterator().next());
				else
					setCondition(getCondition());
			}catch(Exception ex){
				Config.getLogger().severe(TextHelper.getErrorMessage(ex));
			}
		}else{
			String msg = "Unable to load sequence file "+location;
			JOptionPane.showMessageDialog(getComponent(),msg,"Error",JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * save tasks
	 * @param os
	 * @throws Exception
	 */
	private void save(File file) throws Exception {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		// prepend stuff for other conditions
		for(String c : caseSequenceMap.keySet()){
			// skip current condition it is handled after
			if(!DEFAULT_CONDITION.equals(c))
				writer.write("["+c+"]\n");
			for(String s: caseSequenceMap.get(c))
				writer.write(s+"\n");
			
		}
		// write out current sequence at the end 
		writer.close();
	}
	
	public File getFile(){
		return file;
	}
	
	public void setFile(File f){
		file = f;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SequenceBuilder hb = new SequenceBuilder(new DomainExpertModule());
		
		// display
		JFrame frame = new JFrame("Tutor Case Sequence Builder");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(hb.getComponent());
		frame.pack();
		frame.setVisible(true);

	}


}
