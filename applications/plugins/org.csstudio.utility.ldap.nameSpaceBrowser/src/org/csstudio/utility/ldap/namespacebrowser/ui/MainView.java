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
package org.csstudio.utility.ldap.namespacebrowser.ui;

import org.csstudio.utility.ldap.namespacebrowser.Activator;
import org.csstudio.utility.ldap.namespacebrowser.utility.LDAP2Automat;
import org.csstudio.utility.ldap.namespacebrowser.utility.NameSpaceLDAP;
import org.csstudio.utility.ldap.reader.ErgebnisListe;
import org.csstudio.utility.nameSpaceBrowser.ui.CSSView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
/**********************************************************************************
 *
 * @author Helge Rickens
 *
 * Es wird das ergebnis aus einer LDAP-Anfrage, in einem Listeelemt dargestellt.
 * Das Ergebnis kann durch eine eingabe in das dar�berliegende Feld gefiltertert
 * werden. Wird ein Element selektiert wird eine neu LDAP anfrage nach den
 * Kindelmenten gestartet die wiederum in einer Liste (CSSView) dargestellt werden.
 * Die Strucktur die dazu verwendetet wird ist von der Klasse LDAPAutomat abh�ngig.
 *
 **********************************************************************************/
public class MainView extends ViewPart {
	public static final String ID = MainView.class.getName();
	private static String defaultPVFilter =""; //$NON-NLS-1$
	CSSView cssview;
	private LDAP2Automat automat;

	public MainView() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void createPartControl(Composite parent) {
		automat = new LDAP2Automat();
		ScrolledComposite sc = new ScrolledComposite(parent,SWT.H_SCROLL);
		Composite c = new Composite(sc,SWT.NONE);
		sc.setContent(c);
	    sc.setExpandVertical(true);
		c.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false, 1,1));
		c.setLayout(new GridLayout(1,false));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(
				parent.getShell(),
				Activator.PLUGIN_ID + ".nsB");


		c.getShell().addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
				if(e.keyCode==SWT.F1){
					PlatformUI.getWorkbench().getHelpSystem().displayDynamicHelp();
				}
			}

			public void keyPressed(KeyEvent e) {}
		});
		String[] headlines = {	Messages.getString("CSSView_Facility"),
//								Messages.getString("CSSView_ecom"),
								Messages.getString("CSSView_Controller"),
								Messages.getString("CSSView_Record")
		};
		// Namend the Records
		cssview = new CSSView(c, automat,new NameSpaceLDAP(), getSite(),defaultPVFilter,"ou=epicsControls", headlines, 0, new ErgebnisListe());
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
	}

	public void setDefaultPVFilter(String defaultFilter) {
		defaultPVFilter = defaultFilter;
		cssview.setDefaultFilter(defaultPVFilter);
	}

	@Override
	public void dispose(){
		automat = null;
	}
}









