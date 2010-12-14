/*
 * Copyright (c) 2010 Stiftung Deutsches Elektronen-Synchrotron,
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
 *
 * $Id: DesyKrykCodeTemplates.xml,v 1.7 2010/04/20 11:43:22 bknerr Exp $
 */
package org.csstudio.config.ioconfig.view.actions;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.csstudio.config.ioconfig.model.IOConifgActivator;
import org.csstudio.config.ioconfig.model.FacilityDBO;
import org.csstudio.config.ioconfig.model.IocDBO;
import org.csstudio.config.ioconfig.model.pbmodel.ProfibusSubnetDBO;
import org.csstudio.config.ioconfig.model.siemens.ProfibusConfigWinModGenerator;
import org.csstudio.config.ioconfig.view.ProfiBusTreeView;
import org.csstudio.platform.logging.CentralLogger;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

/**
 * TODO (hrickens) : 
 * 
 * @author hrickens
 * @author $Author: $
 * @since 08.10.2010

 */
public class CreateWinModAction extends Action {

	private static final Logger LOG = CentralLogger.getInstance().getLogger(
			CreateWinModAction.class);
	private final ProfiBusTreeView _pbtv;
	
	public CreateWinModAction(@Nullable String text, @Nonnull ProfiBusTreeView pbtv) {
		super(text);
		_pbtv = pbtv;
	}

	private void makeXMLFile(@Nonnull final File path,@Nonnull final ProfibusSubnetDBO subnet) {
	    ProfibusConfigWinModGenerator cfg = new ProfibusConfigWinModGenerator(subnet.getName());
	    cfg.setSubnet(subnet);
	    File xmlFile = new File(path, subnet.getName() + ".cfg");
	    File txtFile = new File(path, subnet.getName() + ".txt");
	    if (xmlFile.exists()) {
	        MessageBox box = new MessageBox(Display.getDefault().getActiveShell(),
	                                        SWT.ICON_WARNING | SWT.YES | SWT.NO);
	        box.setMessage("The file " + xmlFile.getName() + " exist! Overwrite?");
	        int erg = box.open();
	        if (erg == SWT.YES) {
	            try {
	                cfg.getXmlFile(xmlFile);
	                cfg.getTxtFile(txtFile);
	            } catch (IOException e) {
	                MessageBox abortBox = new MessageBox(Display.getDefault()
	                                                     .getActiveShell(), SWT.ICON_WARNING | SWT.ABORT);
	                abortBox.setMessage("The file " + xmlFile.getName()
	                                    + " can not created!");
	                abortBox.open();
	            }
	        }
	    } else {
	        try {
	            xmlFile.createNewFile();
	            cfg.getXmlFile(xmlFile);
	        } catch (IOException e) {
	            MessageBox abortBox = new MessageBox(Display.getDefault().getActiveShell(),
	                                                 SWT.ICON_WARNING | SWT.ABORT);
	            abortBox.setMessage("The file " + xmlFile.getName() + " can not created!");
	            abortBox.open();
	        }
	    }
	}

	@Override
	public void run() {
	    // TODO: Multi Selection Siemens Create.
	    final String filterPathKey = "FilterPath";
	    IEclipsePreferences pref = new DefaultScope().getNode(IOConifgActivator.PLUGIN_ID);
	    String filterPath = pref.get(filterPathKey, "");
	    DirectoryDialog dDialog = new DirectoryDialog(_pbtv.getShell());
	    dDialog.setFilterPath(filterPath);
	    filterPath = dDialog.open();
	    File path = new File(filterPath);
	    pref.put(filterPathKey, filterPath);
	    Object selectedNode = _pbtv.getSelectedNodes().getFirstElement();
	    if (selectedNode instanceof ProfibusSubnetDBO) {
	        ProfibusSubnetDBO subnet = (ProfibusSubnetDBO) selectedNode;
	        LOG.info("Create XML for Subnet: " + subnet);
	        makeXMLFile(path, subnet);

	    } else if (selectedNode instanceof IocDBO) {
	        IocDBO ioc = (IocDBO) selectedNode;
	        LOG.info("Create XML for Ioc: " + ioc);
	        for (ProfibusSubnetDBO subnet : ioc.getProfibusSubnets()) {
	            makeXMLFile(path, subnet);
	        }
	    } else if (selectedNode instanceof FacilityDBO) {
	        FacilityDBO facility = (FacilityDBO) selectedNode;
	        LOG.info("Create XML for Facility: " + facility);
	        for (IocDBO ioc : facility.getIoc()) {
	            for (ProfibusSubnetDBO subnet : ioc.getProfibusSubnets()) {
	                makeXMLFile(path, subnet);
	            }
	        }
	    }
	}

}
