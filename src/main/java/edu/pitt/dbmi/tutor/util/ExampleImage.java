package edu.pitt.dbmi.tutor.util;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * image represents example icon
 * @author tseytlin
 *
 */
public class ExampleImage implements Icon{
	private final int MAX_WIDTH = 200;
	private File location;
	private URL url;
	private String name;
	private ImageIcon img,full;
	
	public ExampleImage(File f){
		setFile(f);
	}
	public ExampleImage(URL u){
		this.url = u;
	}
	
	public File getFile() {
		return location;
	}
	public URL getURL(){
		return url;
	}
	public void setURL(URL u){
		this.url = u;
	}
	public String getName(){
		if(name == null){
			name = ""+url;
			int i= name.lastIndexOf("/");
			if(i > -1 && i< name.length()-1)
				name = name.substring(i+1);
		}
		return name;
	}
	public String toString(){
		return getName();
	}
	private void load(){
		if(img == null){
			full = img = new ImageIcon(url);
			if(img.getIconWidth() > MAX_WIDTH){
				int width = MAX_WIDTH;
				int height = (int) ((double)(width * img.getIconHeight()))/img.getIconWidth();
				img = new ImageIcon(img.getImage().getScaledInstance(width, height,Image.SCALE_SMOOTH));
			}
		}
	}
	public void setFile(File location) {
		this.location = location;
		try{
			this.url = location.toURI().toURL();
		}catch(MalformedURLException ex){}
	}
	public ImageIcon getImage(){
		load();
		return full;
	}
	public int getIconHeight() {
		load();
		return img.getIconHeight();
	}
	public int getIconWidth() {
		load();
		return img.getIconWidth();
	}
	public void paintIcon(Component c, Graphics g, int x, int y) {
		load();
		img.paintIcon(c,g,x,y);
	}
}
