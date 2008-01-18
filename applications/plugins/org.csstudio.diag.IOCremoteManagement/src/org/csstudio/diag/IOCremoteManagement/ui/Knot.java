/*
 * Copyright (c) 2006 Stiftung Deutsches Elektronen-Synchrotron,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */
package org.csstudio.diag.IOCremoteManagement.ui;
/**
 * @author Albert Kagarmanov
 *
 */
import org.eclipse.jface.viewers.TreeViewer;
public class Knot extends  org.csstudio.diag.IOCremoteManagement.ui.Node {
	private final static String attrNextLevel= "nextLevel";
	private final static String attrYes = "yes";
	private final static String attrNextAttr = "nextAttrToAdd";
	private final static String tagNameStateSet = "ssName";
	private final static String tagNameVariable = "varName";
	
	private final static String treeAttrName = "tree";
	private final static String leafAttrValue = "leaf";
	private final static String branchAttrValue = "branch";
	
	public Knot(String name,String host,Object parent,TreeViewer viewer,PropertyPart property,Request req,XMLDataSingle data, typeOfHost type ) {super(name,host,parent,viewer,property,req,data,type);}		
	protected Request createNewRequest(Request req, XMLDataSingle data) {return new Request(req,data);}
	protected  typeOfHost nextLevelType(XMLDataSingle data) {
		String s;
		if((s=data.searchAtr(attrNextAttr)) != null) {
			if((tagNameStateSet.compareTo(s)==0)) return typeOfHost.finalSatateSet;
			if((tagNameVariable.compareTo(s)==0)) return typeOfHost.finalVar;
		}
		if((s=data.searchAtr(treeAttrName)) != null) {
			if((leafAttrValue.compareTo(s)==0)) return typeOfHost.otherLeaf;
			if((branchAttrValue.compareTo(s)==0)) return typeOfHost.knot;
		}
		return null;
	}	
}//EOClass