package org.csstudio.opibuilder.script;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.opibuilder.properties.ScriptProperty;

/**The value type definition for {@link ScriptProperty}, which describes the input
 * for a Script Property.
 * @author Xihui Chen
 *
 */
public class ScriptsInput {

	private List<ScriptData> scriptList;
	
	public ScriptsInput(List<ScriptData> scriptDataList) {
		scriptList = scriptDataList;
	}
	
	public ScriptsInput() {
		scriptList = new ArrayList<ScriptData>();
	}

	/**
	 * @return the scriptList
	 */
	public List<ScriptData> getScriptList() {
		return scriptList;
	}
	
	/**
	 * @return a total contents copy of this ScriptsInput.
	 */
	public ScriptsInput getCopy(){
		ScriptsInput copy = new ScriptsInput();
		for(ScriptData data : scriptList){
			copy.getScriptList().add(data.getCopy());
		}
		return copy;
	}
	
	@Override
	public String toString() {
		if(scriptList.size() ==0){
			return "no script attached";
		}
		if(scriptList.size() == 1){
			return scriptList.get(0).getPath().toString();
		}
		return scriptList.size() + " scripts attached";
	}
	
	
}
