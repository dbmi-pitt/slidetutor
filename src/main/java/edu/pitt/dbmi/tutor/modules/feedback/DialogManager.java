package edu.pitt.dbmi.tutor.modules.feedback;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.Properties;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.html.HTMLDocument;

import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.ScenarioSet;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.model.*;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;


public class DialogManager implements FeedbackModule, ActionListener {
	private UIHelper.HTMLPanel outtext;
	private Container dialogInputPanel,currentInput,noInput,scaleInput;
	private ButtonGroup answerGroup,scaleGroup;
	private AbstractButton nothing,send;
    private JTextArea intext;
	private Tutor tutor;
	private Properties defaultConfig;
	private boolean interactive;
    private boolean isActiveDialog = true,enabled;
    private String questionText;
	private JSplitPane split;
	private Color inputColor;
	private JPanel component;
	
			
	/**
     * Get dialog input component
     * @param type
     * @param answers
     * @return
     */
    private Container getInputComponent(String type,String [] answers){
        Container comp = null;
        answerGroup = null;
        // plain question with text anser
        if(type.equalsIgnoreCase("text")){
           if(intext == null){
               intext = new JTextArea(3,100);
               intext.setBorder(new LineBorder(Color.black));
               intext.setLineWrap(true);
               intext.setWrapStyleWord(true);
               intext.setFont(intext.getFont().deriveFont(Font.PLAIN,11));
           }
           comp = intext; 
        // multiple choice question    
        }else if(type.equalsIgnoreCase("choice") && answers != null){
            JPanel panel = new JPanel();
            //GridLayout layout = new GridLayout(answers.length,1,0,0);
            BoxLayout layout = new BoxLayout(panel,BoxLayout.Y_AXIS);
            /*
            if(answers[0].length() < 25){
                grid.setColumns(2);
                grid.setRows((int)Math.ceil((double)answers.length/2.0));
            }*/
            //panel.setForeground(Color.white);
            panel.setBackground(Color.white);
            panel.setLayout(layout);
            panel.setBorder(new LineBorder(Color.black));
            FontRenderContext fontContext = new FontRenderContext(null,false, false);
            answerGroup = new ButtonGroup();
            for(int i=0;i<answers.length;i++){
                JRadioButton bt = new JRadioButton();
                bt.setVerticalTextPosition(SwingConstants.TOP);
                bt.setFont(bt.getFont().deriveFont(Font.PLAIN,11));
                Rectangle2D qr = bt.getFont().getStringBounds(answers[i],fontContext);
                Rectangle2D lr = bt.getFont().getStringBounds("a",fontContext);
                String str = answers[i];
                // see if anser needs to be wrapped
                if(qr.getWidth()+20 > noInput.getSize().getWidth()){
                    str = TextHelper.formatString(answers[i],(int)((noInput.getSize().getWidth()-20)/lr.getWidth()));
                }
                bt.setText(str);
                bt.setActionCommand(answers[i]);
                bt.setOpaque(false);
                answerGroup.add(bt);
                panel.add(bt);
            }
            comp = new JScrollPane(panel);
        // question with scale    
        }else if(type.equalsIgnoreCase("scale")){
            if(scaleInput == null){
                scaleInput = new JPanel();
                GridBagLayout grid = new GridBagLayout();
                GridBagConstraints c = new GridBagConstraints();
                c.anchor = GridBagConstraints.CENTER;
                //GridLayout grid = new GridLayout();
                //grid.setHgap(0);
                //grid.setVgap(0);
                scaleInput.setLayout(grid);
                scaleInput.setBackground(Color.white);
                ((JPanel)scaleInput).setBorder(new LineBorder(Color.black));
                scaleGroup = new ButtonGroup();
                
                // this is a default button to clear others
                nothing = new JButton();
                
                // place infomration label
                JLabel lbl = new JLabel("Please rate your answer on the scale of 1 (least) to 10 (most)");
                Font fnt = lbl.getFont().deriveFont(Font.PLAIN);
                lbl.setFont(fnt);
                c.gridwidth = 10;
                grid.setConstraints(lbl,c);
                scaleInput.add(lbl);
                c.gridwidth = 1;
                c.gridy = 1;
                c.gridx = 0;
                
                for(int i=1;i<=10;i++){
                    JRadioButton bt = new JRadioButton(""+i);
                    bt.setFont(bt.getFont().deriveFont(Font.PLAIN));
                    bt.setActionCommand(""+i);
                    bt.setOpaque(false);
                    scaleGroup.add(bt);
                    grid.setConstraints(bt,c);
                    scaleInput.add(bt);
                    c.gridx ++;
                }
            }else
                scaleGroup.setSelected(nothing.getModel(),true);
            
            comp = scaleInput;
        // retoric question, no answer needed    
        }else {
            if(noInput == null){
                noInput = new JPanel();
                //noInput.setPreferredSize(new Dimension(300,100));
            }
            comp = noInput;
        }
        return comp;
    }
    
    
    /**
     * ask user a question
     * @param type - question type: text, choice, scale, remark
     * @param question - text of the question
     * @param answers - if question is choice, then list of answers
     */
    public void askQuestion(String question,String type, java.util.List answers){
        askQuestion(type,question,(String []) answers.toArray(new String [0]),false);
    }
    
    
    /**
     * ask user a question
     * @param type - question type: text, choice, scale, remark
     * @param question - text of the question
     * @param answers - if question is choice, then list of answers
     */
    private void askQuestion(String type, String question, String [] answers, boolean first){
        //String s = (first)?"":"<br>";
        questionText = question;
        
        // add question
        //outtext.append(s+"<b><font color='red'>tutor</font> :</b> "+question);
        if(question.length() > 0)
            outtext.append("<b><font color='red'>tutor</font> :</b> "+question+"<br>");
        // add diffent componet
        if(currentInput != null)
            dialogInputPanel.remove(currentInput);
        currentInput = getInputComponent(type,answers);
        dialogInputPanel.add(currentInput,BorderLayout.CENTER);
        dialogInputPanel.validate();
        split.revalidate();
        split.repaint();
        send.setEnabled(!type.equalsIgnoreCase("remark")); 
        // flash first question
        if(question.length() > 0)     //!isActiveDialog && 
            flashPanel();
        
        isActiveDialog = !first;
    }
	
    // flash input panel
    private void flashPanel(){
        final Component comp = (currentInput instanceof JScrollPane)?
                ((JScrollPane) currentInput).getViewport().getComponent(0):currentInput;
        inputColor = comp.getBackground();
        javax.swing.Timer timer = new javax.swing.Timer( 800, new ActionListener() {
               public void actionPerformed( ActionEvent evt ) {
                   comp.setBackground(inputColor);
                   outtext.setBackground(Color.white);
                   comp.repaint();
                   outtext.repaint();
               }
           }
        );
        comp.setBackground( Color.GREEN );
        outtext.setBackground(Color.GREEN);
        comp.repaint();
        outtext.repaint();
        timer.setRepeats(false);
        timer.start();
    }
    
    /**
     * envoke after send
     */
    private void finishQuestion(){
        // add diffent componet
        if(currentInput != null)
            dialogInputPanel.remove(currentInput);
        currentInput = noInput;
        dialogInputPanel.add(currentInput,BorderLayout.CENTER);
        dialogInputPanel.validate();
        split.revalidate();
        split.repaint();
        send.setEnabled(false); 
    }
    
	
	public int getFeedbackMode() {
		return 0;
	}

	public int getLevelCount() {
		return 0;
	}

	public void load() {
		// TODO Auto-generated method stub

	}

	public void requestHint() {}

	public void requestLevel(int offset) {
	}

	public void setCaseEntry(CaseEntry problem) {}
	public void setExpertModule(ExpertModule module) {	}
	public void setFeedackMode(int mode) {}
	public void setStudentModule(StudentModule module) {}

	public void sync(FeedbackModule tm) {
		// TODO Auto-generated method stub

	}

	public Component getComponent() {
        if(component == null){
        	component = new JPanel();
        	component.setLayout(new BorderLayout());
            
    		// out text
            outtext = new UIHelper.HTMLPanel();
            ((HTMLDocument) outtext.getDocument()).getStyleSheet().addRule(
                    "body { font-family: sans-serif; font-size: 11;");
            outtext.setPreferredSize(new Dimension(300,200));
            outtext.setEditable(false);
            
            // set text
            currentInput = getInputComponent("remark",null);
            
            // input text
            send = new JButton("send",UIHelper.getIcon(this,"icon.send"));
            send.setMargin(new Insets(2,2,2,2));
            send.setVerticalTextPosition(AbstractButton.BOTTOM);
            send.setHorizontalTextPosition(SwingConstants.CENTER);
            //send.setMinimumSize(new Dimension(60,100));
            send.setPreferredSize(new Dimension(60,100));
            send.addActionListener(this);
            send.setEnabled(false);
           
            
            dialogInputPanel = new JPanel();
            dialogInputPanel.setLayout(new BorderLayout());
            dialogInputPanel.add(currentInput,BorderLayout.CENTER);
            dialogInputPanel.add(send,BorderLayout.EAST);
            dialogInputPanel.setPreferredSize(new Dimension(300,300));
            
            split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            split.setBorder(new EmptyBorder(6,6,6,6));
            split.setTopComponent(new JScrollPane(outtext));
            split.setBottomComponent(dialogInputPanel);
            split.setResizeWeight(0.5);
            
            component.add(split,BorderLayout.CENTER);
        }
		
		return component;
	}

	public ImageIcon getScreenshot() {
		return Config.getScreenshot(getClass());
	}

	public Tutor getTutor() {
		return tutor;
	}


	public boolean isInteractive() {
		return interactive;
	}

	public void setEnabled(boolean b) {
		send.setEnabled(b);
		enabled = b;
	}

	public void setInteractive(boolean b) {
		interactive = b;
	}

	public void setTutor(Tutor t) {
		tutor = t;
	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public Properties getDefaultConfiguration() {
		if(defaultConfig == null){
			defaultConfig = Config.getDefaultConfiguration(getClass());
		}
		return defaultConfig;
	}

	public String getDescription() {
		return "Manages Dialog interaction with the tutor";
	}

	public String getName() {
		return "Dialog Manager";
	}

	public Action[] getSupportedActions() {
		return new Action [0];
	}

	public Message[] getSupportedMessages() {
		return new Message [0];
	}

	public String getVersion() {
		return "1.0";
	}

	public void receiveMessage(Message msg) {
		// TODO Auto-generated method stub

	}

	public void reset() {
        // clear text
        if(outtext != null)
        	outtext.setText("");
        // ask default question
        askQuestion("remark",Config.getProperty(this,"introduction.remark.text"),null,true);
	}

	public void resolveAction(Action action) {
		// TODO Auto-generated method stub

	}


	public void actionPerformed(ActionEvent e) {
		 if(e.getSource() == send){
             //check if answer has been given
             boolean answerGiven = false;
             
             
             String s = "<b><font color='blue'>"+Config.getUsername()+"</font> :</b> ";
             String txt = "";
             // if short answer question, then
             if(currentInput == intext){
                 txt = intext.getText();
                 intext.setText("");
                 answerGiven = txt.trim().length() > 0;
             // if scale given    
             }else if(currentInput == scaleInput){
                 ButtonModel model = scaleGroup.getSelection();
                 if(model != null){
                     txt = model.getActionCommand();
                     answerGiven = (txt != null);
                 }
             // else multiple choice
             }else if(answerGroup != null){
                 ButtonModel model = answerGroup.getSelection();
                 if(model != null){
                     txt = model.getActionCommand();
                     answerGiven = (txt != null);
                 }
             }
             
             if(!answerGiven){
                 JOptionPane.showMessageDialog(Config.getMainFrame(),"You should answer the question.","Error",JOptionPane.ERROR_MESSAGE);
                 return;
             }
             
             outtext.append(s+txt+"<br>");
             split.revalidate();
             outtext.setCaretPosition(outtext.getDocument().getEndPosition().getOffset()-1);
             finishQuestion();
             //notifyDialog(questionText,txt);
            
             //send.setEnabled(false); 
		 }
		
	}


	public void reconfigure() {
		// TODO Auto-generated method stub
		
	}
	/**
	 * get a manual page that represents this module
	 * @return
	 */
	public URL getManual(){
		return Config.getManual(getClass());
	}

	public JMenu getDebugMenu() {
		return null;
	}


	public ScenarioSet getScenarioSet() {
		return null;
	}

	public boolean isEnabled() {
		return enabled;
	}
}
