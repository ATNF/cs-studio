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
package org.csstudio.config.ioconfig.editorinputs;

import org.csstudio.config.ioconfig.model.AbstractNodeDBO;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * TODO (hrickens) :
 *
 * @author hrickens
 * @author $Author: hrickens $
 * @version $Revision: 1.2 $
 * @since 31.03.2010
 */
public class NodeEditorInput implements IEditorInput {

    private final AbstractNodeDBO _node;
    private final boolean _nevv;


    public NodeEditorInput(final AbstractNodeDBO node) {
        _node = node;
        _nevv = false;
    }

    /**
     * Constructor.
     */
    public NodeEditorInput(final AbstractNodeDBO node, final boolean nevv) {
        _node = node;
        _nevv = nevv;
    }

    public AbstractNodeDBO getNode() {
        return _node;
    }

    /**
     * (@inheritDoc)
     */
    @Override
    public boolean exists() {
        return false;
    }

    /**
     * (@inheritDoc)
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    /**
     * (@inheritDoc)
     */
    @Override
    public String getName() {
        return _node.getName();
    }

    /**
     * (@inheritDoc)
     */
    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    /**
     * (@inheritDoc)
     */
    @Override
    public String getToolTipText() {
        return "Test Tool Tip Text";
    }

    /**
     * (@inheritDoc)
     */
    @Override
    public Object getAdapter(final Class adapter) {
        return null;
    }

    @Override
    public boolean equals(final Object obj) {
        // TODO: Pr�fen ob der Vergeleich so ausreichend ist.
        /* Idee es w�re sch�n wenn generell nur ein Editor pro Facility ge�ffnet sein k�nnte und der
         * Editor gewechslet wird innerhalb einer Faclilty!
         */
        return hashCode()==obj.hashCode();
    }

    @Override
    public int hashCode() {
        return _node.hashCode();
    }

    /**
     * @return the nevv
     */
    public boolean isNew() {
        return _nevv;
    }
}
