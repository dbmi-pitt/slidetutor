package edu.pitt.dbmi.tutor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import static edu.pitt.dbmi.tutor.util.OntologyHelper.*;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.UIHelper;


public class ConfigSelectorPanel extends JPanel implements ItemListener, ListSelectionListener, ActionListener {
	private JList study,configs;
	private JComboBox institution;
	private JTextField preview;
	private JProgressBar progress;
	private JButton okButton,cancelButton;
	private JDialog dialog;
	private JOptionPane op;
	private Frame frame;
	private boolean ok;
	private Map<String,Vector<String>> configMap;
	private Vector<String> studyList,institutionList;
	private String baseURL;
	private URL server;
	
	public ConfigSelectorPanel(){
		baseURL = DEFAULT_HOST_URL+"/"+CURRICULUM_ROOT+"/"+CONFIG_FOLDER+"/";
		try{
			server = new URL(DEFAULT_FILE_MANAGER_SERVLET);
		}catch(MalformedURLException ex){
			Config.getLogger().severe("Could not parse default file manager URL "+DEFAULT_FILE_MANAGER_SERVLET);
		}
		createInterface();
		//load();
	}
	
	/**
	 * create UI
	 */
	private void createInterface(){
		setLayout(new BorderLayout());
		
		institution = new JComboBox(new DefaultComboBoxModel());
		institution.setBorder(new TitledBorder("Institiution"));
		institution.addItemListener(this);
		
		study = new JList(new DefaultListModel());
		study.addListSelectionListener(this);
		study.setVisibleRowCount(15);
		configs = new JList();
		configs.setVisibleRowCount(15);
		configs.addListSelectionListener(this);
		configs.setCellRenderer(new DefaultListCellRenderer(){
			public Component getListCellRendererComponent(JList list, Object value, int i, boolean s,boolean f) {
				JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, i, s, f);
				String val = ""+value;
				if(val.endsWith(CONFIG_SUFFIX)){
					try{
						lbl.setText(val.substring(val.lastIndexOf("/")+1,val.lastIndexOf(".")));
					}catch(Exception ex){
						
					}
				}
				return lbl;
			}
			
		});
		
		preview = new JTextField();
		preview.setEditable(false);
		preview.setForeground(Color.blue);
		JScrollPane s1 = new JScrollPane(study);
		s1.setPreferredSize(new Dimension(200,200));
		s1.setBorder(new TitledBorder("Study"));
		JScrollPane s2 = new JScrollPane(configs);
		s2.setBorder(new TitledBorder("Configuration"));
		s2.setPreferredSize(new Dimension(250,200));
		add(institution,BorderLayout.NORTH);
		add(s1,BorderLayout.WEST);
		add(s2,BorderLayout.CENTER);
		add(preview,BorderLayout.SOUTH);
		
		progress = new JProgressBar();
		progress.setIndeterminate(true);
		progress.setString("Please Wait ...");
		progress.setStringPainted(true);
		
		
		// create popup
		okButton = new JButton("OK");
		okButton.addActionListener(this);
		okButton.setEnabled(false);
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		op = new JOptionPane(this,JOptionPane.PLAIN_MESSAGE,
		JOptionPane.OK_CANCEL_OPTION,null,new Object []{okButton,cancelButton});
		
		// add dobulic click listener
		configs.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				if(configs.getSelectedValue() != null && e.getClickCount() == 2)
					okButton.doClick();
			}
		});
			
	}
	
	/**
	 * display busy
	 * @param b
	 */
	public void setBusy(boolean busy){
		JComponent c = this;
		if(busy){
			c.remove(preview);
			c.add(progress,BorderLayout.SOUTH);
		}else{
			progress.setString(null);
			progress.setIndeterminate(true);
			c.remove(progress);
			c.add(preview,BorderLayout.SOUTH);
		}
		c.revalidate();
		c.repaint();
	}
	
	
	public void setOwner(Frame frame) {
		this.frame = frame;
	}
	
	
	/**
	 * get list of config files
	 * @return
	 */
	private List<String> getConfigList(){
		List<String> list = new ArrayList<String>();
		//list.add(baseURL+"UPMC/Development/SlideTutor.config");
		for(String u : list(CONFIG_FOLDER)){
			if(u.endsWith(CONFIG_SUFFIX)){
				list.add(baseURL+u);
			}
		}
		Collections.sort(list);
		return list;
	}
	
	/**
	 * list content of 
	 * @param path
	 * @return
	 */
	private String [] list(String path){
		Map<String,String> map = new HashMap<String, String>();
		map.put("action","list");
		map.put("root",CURRICULUM_ROOT);
		map.put("path",path);
		map.put("recurse","true");
		try{
			String str = UIHelper.doGet(server,map);
			return str.split("\n");
		}catch(IOException ex){
			ex.printStackTrace();
		}
		return new String [0];
	}
	
	
	/**
	 * load resources
	 */
	public void load(){
		// clear everything
		studyList = new Vector<String>();
		institutionList = new Vector<String>();
		study.setListData(studyList);
		configs.setListData(new Vector());
		institution.removeAllItems();
		preview.setText("");
		
		// iterate over ontologies and create a map
		configMap = new HashMap<String,Vector<String>>();
		Pattern pt = Pattern.compile("(http://.+/config/)(.+)/(.+)/.+");
		for(String url: getConfigList()){
			// now parse url to extract relevant info
			// (it better be formated right)
			Matcher m = pt.matcher(url);
			if(m.matches()){
				baseURL = m.group(1);
				String organ = m.group(3);
				String inst  = m.group(2);
				// add to models
				if(!studyList.contains(organ))
					studyList.add(organ);
				if(!institutionList.contains(inst)){
					institution.addItem(inst);
					institutionList.add(inst);
				}
				// put into hashmap
				Vector<String> list = configMap.get(organ+"/"+inst);
				if(list == null){
					list = new Vector<String>();
					configMap.put(organ+"/"+inst,list);
				}
				list.add(url);
			}
		}
		Collections.sort(studyList);
		study.setListData(studyList);
		study.setSelectedIndex(0);
		
		// select PITT by default :)
		if(institutionList.contains("PITT"))
			institution.setSelectedItem("PITT");
		
	}
	
	public void showChooserDialog() {
		load();
		dialog = op.createDialog(frame,"Select ITS Configuration");
		dialog.setVisible(true);
		//int r = JOptionPane.showOptionDialog(frame,this,"Open Domain",
		//		JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		//ok = r == JOptionPane.OK_OPTION;
	}
	
	
	
	public Vector<String> getStudyList() {
		return studyList;
	}

	public Vector<String> getInstitutionList() {
		return institutionList;
	}

	/**
	 * load domains
	 * @param organ
	 */
	private void loadConfigs(String organ,String inst){
		Vector<String> list = configMap.get(organ+"/"+inst);
		if(list == null)
			list = new Vector();
		Collections.sort(list);
		configs.setListData(list);
		preview.setText("");
		//domains.setSelectedIndex(0);
	}
	

	public void itemStateChanged(ItemEvent e) {
		if(e.getSource().equals(institution)){
			loadConfigs(""+study.getSelectedValue(),""+institution.getSelectedItem());
		}
	}

	public void valueChanged(ListSelectionEvent e) {
		if(!e.getValueIsAdjusting()){
			if(e.getSource().equals(study)){
				loadConfigs(""+study.getSelectedValue(),""+institution.getSelectedItem());
			}else if(e.getSource().equals(configs)){
				Object obj = configs.getSelectedValue();
				preview.setText(""+obj);
				okButton.setEnabled(obj != null);
			}
		}
	}
	
	

	public void actionPerformed(ActionEvent e) {
		if(e.getSource().equals(okButton)){
			ok = true;
			if(dialog != null)
				dialog.dispose();
		}else if(e.getSource().equals(cancelButton)){
			ok = false;
			if(dialog != null)
				dialog.dispose();
		}
	}
	
	public Object getSelectedObject() {
		return configs.getSelectedValue();
	}

	public Object[] getSelectedObjects() {
		return new Object [] {getSelectedObject()};
	}

	public boolean isSelected() {
		return ok && !configs.isSelectionEmpty();
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ConfigSelectorPanel selector = new ConfigSelectorPanel();
		selector.showChooserDialog();
		//System.out.println(selector.getSelectedObject());
	}

}
