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
 */
package org.csstudio.config.ioconfig.editorparts;

import java.util.Set;

import javax.annotation.Nonnull;

import org.csstudio.config.ioconfig.config.view.helper.ConfigHelper;
import org.csstudio.config.ioconfig.config.view.helper.DocumentationManageView;
import org.csstudio.config.ioconfig.model.DocumentDBO;
import org.csstudio.config.ioconfig.model.FacilityDBO;
import org.csstudio.config.ioconfig.model.pbmodel.GSDFileDBO;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

/**
 * TODO (hrickens) :
 *
 * @author hrickens
 * @author $Author: hrickens $
 * @version $Revision: 1.2 $
 * @since 31.03.2010
 */
public class FacilityEditor extends AbstractNodeEditor{

    public static final String ID = "org.csstudio.config.ioconfig.view.editor.facility";

    /**
     * The Facility Object.
     */
    private FacilityDBO _facility;

    /**
     *
     * Constructor.
     */
    public FacilityEditor() {
        //  nothing to do.
    }

    /**
     * Constructor.
     */
    public FacilityEditor(@Nonnull final Composite parent,final short sortIndex) {
        super(true);
        getProfiBusTreeView().getTreeViewer().setSelection(null);
        newNode();
        getNode().moveSortIndex(sortIndex);
        buildGui();
    }

    /**
     * @param parent
     *            The Parent Composite.
     * @param facility
     *            to Configure. Is NULL create a new one.
     */
    public FacilityEditor(@Nonnull final Composite parent,@Nonnull final FacilityDBO facility) {
        super(facility == null);
        _facility = facility;
        buildGui();
        getTabFolder().setSelection(0);
    }


    @Override
    public void createPartControl(@Nonnull final Composite parent) {
        super.createPartControl(parent);
        _facility = (FacilityDBO) getNode();
        buildGui();
        getTabFolder().setSelection(0);
    }

    /**
     * (@inheritDoc)
     */
    @Override
    public void doSave(final IProgressMonitor monitor) {
        super.doSave(monitor);
        // Main
        _facility.setName(getNameWidget().getText());
        getNameWidget().setData(getNameWidget().getText());

        getIndexSpinner().setData(_facility.getSortIndex());

        // Document
        Set<DocumentDBO> docs = getDocumentationManageView().getDocuments();
        _facility.setDocuments(docs);

        save();
//        getProfiBusTreeView().refresh(getNode());
        getProfiBusTreeView().refresh();
    }

    @Override
    public void setFocus() {
        // TODO Auto-generated method stub

    }




    //----------------------------------
    // Facility EditView

    private void buildGui() {
        setSavebuttonEnabled(null, getNode().isPersistent());
        main("Facility");
        getProfiBusTreeView().refresh(getNode());
        // _tabFolder.pack();
    }

    /**
     * Generate the Main IOC configuration Tab.
     *
     * @param head
     *            The headline of the tab.
     */
    private void main(@Nonnull final String head) {
        Composite comp = ConfigHelper.getNewTabItem(head, getTabFolder(), 5,300,260);
        comp.setLayout(new GridLayout(4, false));

        Group gName = new Group(comp, SWT.NONE);
        gName.setText("Name");
        gName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1));
        gName.setLayout(new GridLayout(3, false));

        Text nameText = new Text(gName, SWT.BORDER | SWT.SINGLE);
        setText(nameText, _facility.getName(), 255);
        nameText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        setNameWidget(nameText);
        setIndexSpinner(ConfigHelper.getIndexSpinner(gName, _facility, getMLSB(), "Index",
                getProfiBusTreeView()));

        makeDescGroup(comp,3);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.csstudio.config.ioconfig.config.view.NodeConfig#fill(org.csstudio
     * .config.ioconfig.model.pbmodel.GSDFile)
     */
    @Override
    public boolean fill(final GSDFileDBO gsdFile) {
        return false;
    }

    @Override
    public void cancel() {
        super.cancel();
        if (_facility != null) {
            Object data = getParent().getData("version");
            if (data != null) {
                if (data instanceof Text) {
                    Text text = (Text) data;
                    text.setText("");
                }
            }
            getIndexSpinner().setSelection((Short) getIndexSpinner().getData());
            getNameWidget().setText((String) getNameWidget().getData());
        }
        DocumentationManageView dMV = getDocumentationManageView();
        if (dMV != null) {
            dMV.cancel();
        }
        setSaveButtonSaved();
    }

    /**
     * Have no GSD File.
     *
     * @return null.
     */
    @Override
    public GSDFileDBO getGSDFile() {
        return null;
    }
}
