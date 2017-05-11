package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

import edu.pitt.dbmi.tutor.messages.*;
import static edu.pitt.dbmi.tutor.messages.Constants.*;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import edu.pitt.dbmi.tutor.util.OntologyHelper;

public class H3ColorInconsistency implements ProtocolScript {
	private PrintStream output;
	private ProtocolModule protocol;
	
	public void dispose() {
		}

	public String getDescription() {
		return "In H3 check if coloring the same thing with the same color produces different tutor response OR coloring w/ different color produces the same response";
	}

	public String getName() {
		return "H3 Color Response Inconsistency";
	}

	public void initialize() {
	}

	public boolean process(Session session) {
		Map<String,String> colorMap = new HashMap<String,String>();
		Map<String,ClientEvent> fcolorMap = new HashMap<String,ClientEvent>();
		Map<String,String> responseMap = new HashMap<String,String>();
		for(ClientEvent ce: session.getClientEvents()){
			if(ACTION_SELF_CHECK.equals(ce.getAction())){
				String fok = ce.getInputMap().get("fok");
				String key = ce.getObjectDescription()+"="+fok;
				String value = session.getTutorResponses(ce).get(0).getResponse();
				if(colorMap.containsKey(key)){
					// if response is not equal, shit output
					if(!value.equals(colorMap.get(key))){
						output.println(session.getUsername()+" "+OntologyHelper.getCaseName(session.getCase())+" "+ce.getObjectDescription()+" same color, different response");
					}
				}else{
					colorMap.put(key,value);
				}
				fcolorMap.put(ce.getObjectDescription(),ce);
				
				//now check that the same response is not given to the different coloring
				String key2 = ce.getObjectDescription()+"="+("sure".equals(fok)?"unsure":"sure");
				if(colorMap.containsKey(key2)){
					if(value.equals(colorMap.get(key2))){
						//output.println(session.getUsername()+" "+OntologyHelper.getCaseName(session.getCase())+" "+ce.getObjectDescription()+" different color, same response");
					}
				}
				
				String shit = responseMap.get(ce.getObjectDescription());
				String r = RESPONSE_CONFIRM;
				if("sure".equals(fok)){
					if(RESPONSE_FAILURE.equals(shit))
						r = RESPONSE_FAILURE;
				}else if("unsure".equals(fok) || "error".equals(fok)){
					if(RESPONSE_CONFIRM.equals(shit))
						r = RESPONSE_FAILURE;
				}
				// if what should is different from what is, SHIT!!!
				if(!r.equals(value)){
					//output.println(session.getUsername()+" "+OntologyHelper.getCaseName(session.getCase())+" "+ce.getObjectDescription()+" incorrect color response");
					//for(TutorResponse tr : session.getTutorResponses(ce)){
					//	updateDB(session, tr,r);
					//}
				}
			}else if(ACTION_ADDED.equals(ce.getAction()) || ACTION_REFINE.equals(ce.getAction()) || ACTION_REMOVED.equals(ce.getAction())){
				//String value = session.getTutorResponses(ce).get(0).getResponse();
				//responseMap.put(ce.getObjectDescription(),value);
				for(TutorResponse tr: session.getTutorResponses(ce)){
					String od = tr.getResponseConcept();
					responseMap.put(od,tr.getResponse());
				
					// if entry is in color map alrady that means it is after coloring
					if(RESPONSE_FAILURE.equals(tr.getResponse())){
						if(fcolorMap.containsKey(od) && od.startsWith(TYPE_DIAGNOSIS)){
							ClientEvent cce = fcolorMap.get(od);
							TutorResponse cte = session.getTutorResponses(cce).get(0);
							String fok = cce.getInputMap().get("fok");
							if("sure".equals(fok) && RESPONSE_CONFIRM.equals(cte.getResponse())){
								output.println(session.getUsername()+" "+OntologyHelper.getCaseName(session.getCase())+" "+cce.getObjectDescription()+" non-goal DX issue needs "+RESPONSE_FAILURE);
							}else if("unsure".equals(fok) && RESPONSE_FAILURE.equals(cte.getResponse())){
								output.println(session.getUsername()+" "+OntologyHelper.getCaseName(session.getCase())+" "+cce.getObjectDescription()+" non-goal DX issue needs "+RESPONSE_CONFIRM);
							}
						}
					}
				
				}
			}
		}
		
		return true;
	}

	public void setOutput(PrintStream out) {
		output = out;
	}

	public void setProtocolModule(ProtocolModule m) {
		protocol = m;
	}
	public String toString(){
		return getName();
	}

	private void updateDB(Session s, TutorResponse tr, String response){
		if(protocol instanceof DatabaseProtocolModule){
			try {
				// update tutor response
				String SQL = "UPDATE tutor_response SET response_type = ? WHERE tutor_response_id = ?";
				Connection conn = ((DatabaseProtocolModule) protocol).getConnection();
				PreparedStatement st = conn.prepareStatement(SQL);
				st.setString(1,response);
				st.setInt(2,tr.getMessageId());	
				// execute
				st.executeUpdate();
				st.close();
				
				// update  node event
				NodeEvent ne = null;
				for(NodeEvent n: s.getNodeEvents(tr)){
					ne = n;
					break;
				}
				if(ne != null){
					SQL = "UPDATE node_event SET response_type = ? WHERE node_event_id = ?";
					conn = ((DatabaseProtocolModule) protocol).getConnection();
					st = conn.prepareStatement(SQL);
					st.setString(1,response);
					st.setInt(2,ne.getMessageId());	
					// execute
					st.executeUpdate();
					st.close();
				}
				
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
}
