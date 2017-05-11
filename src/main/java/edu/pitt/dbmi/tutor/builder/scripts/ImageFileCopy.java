package edu.pitt.dbmi.tutor.builder.scripts;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Set;

import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.SlideEntry;
import edu.pitt.dbmi.tutor.modules.expert.DomainExpertModule;
import edu.pitt.dbmi.tutor.modules.pedagogic.StaticCaseSequence;
import edu.pitt.dbmi.tutor.util.Config;

public class ImageFileCopy {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final char   FS = '\\';
		final String COMMAND = "replace";
		final String SOURCE = "E:"+FS+"slideimages"+FS;
		final String DESTINATION = "C:"+FS+"Data"+FS+"slideimages"+FS;
		final String SWITCHES = "/a";
		
		
		//final String DOMAIN = "http://slidetutor.upmc.edu/curriculum/owl/skin/PITT/VesicularDermatitis.owl";
		final String DOMAIN = "http://slidetutor.upmc.edu/curriculum/owl/skin/PITT/PerivascularDermatitis.owl";
		//final String DOMAIN = "http://slidetutor.upmc.edu/curriculum/owl/skin/PITT/NodularDiffuseDermatitis.owl";
		final String SEQUENCE = "http://slidetutor.upmc.edu/curriculum/config/PITT/STIM-H3/Sequence.lst";
		//final String SEQUENCE = "http://slidetutor.upmc.edu/curriculum/config/PITT/STIM-H5/CaseSequence.lst";
		//final String CONDITION_PREFIX = "Vesicular";
		//final String CONDITION_PREFIX = "Nodular";
		final String CONDITION_PREFIX = "D";
		
		
		Config.setProperty("tutor.case.sequence",SEQUENCE);
		Config.setProperty("StaticCaseSequence.loop.sequence","false");
		Config.setProperty("StaticCaseSequence.prompt.at.end","false");
		
		// load domain
		DomainExpertModule expert = new DomainExpertModule();
		expert.openDomain(DOMAIN);
		
		// load sequence
		StaticCaseSequence seq = new StaticCaseSequence();
		seq.setExpertModule(expert);
		seq.load();
		
		for(String condition: seq.getSequenceKeys()){
			if(condition.startsWith(CONDITION_PREFIX))
				continue;
			
			Config.setProperty("tutor.condition",condition);
			
			// get list of problems for conditions
			String problem = null;
			do {
				problem = seq.getNextCase();
				if(problem != null){
					try{
						CaseEntry entry = expert.getCaseEntry(problem);
						for(SlideEntry slide: entry.getSlides()){
							String fileName = slide.getSlidePath();
							fileName = fileName.replace('/',FS);
		
							// get all of the files
							int x = fileName.lastIndexOf(".");
							if(x > -1){
								if(slide.getSlideName().startsWith("HA_"))
									fileName = fileName.substring(0,x)+"*";
								else
									fileName = fileName.substring(0,x)+".*";
							}
							
							// write out the copy command
							System.out.println(COMMAND+" "+SOURCE+fileName+" "+DESTINATION+slide.getPath().replace('/',FS)+" "+SWITCHES);
						}
					}catch(Exception ex){
						continue;
					}
				}
				
			}while(problem != null);
			seq.reset();
		}
		
	}

}
