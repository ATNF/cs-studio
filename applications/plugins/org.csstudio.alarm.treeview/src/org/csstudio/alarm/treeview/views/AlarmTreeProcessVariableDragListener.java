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
 * $Id$
 */
package org.csstudio.alarm.treeview.views;

import java.util.List;

import javax.annotation.Nonnull;

import org.csstudio.alarm.treeview.model.IAlarmTreeNode;
import org.eclipse.jface.util.TransferDragSourceListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;

/**
 * Provides drag support for dragging process variable nodes from the alarm tree using the
 * process variable name transfer.
 */
public final class AlarmTreeProcessVariableDragListener implements TransferDragSourceListener {

    private final AlarmTreeView _alarmTreeView;

    /**
     * Constructor.
     * @param alarmTreeView view
     */
    public AlarmTreeProcessVariableDragListener(@Nonnull final AlarmTreeView alarmTreeView) {
        _alarmTreeView = alarmTreeView;
    }

    @Override
    @Nonnull
    public Transfer getTransfer() {
    	// TODO jhatje: implement new datatype
//        return ProcessVariableNameTransfer.getInstance();
    	return null;
    }

    @Override
    public void dragStart(@Nonnull final DragSourceEvent event) {
        final TreeViewer viewer = _alarmTreeView.getViewer();
        if (viewer != null) {
            final List<IAlarmTreeNode> selectedNodes = AlarmTreeView.selectionToNodeList(viewer.getSelection());
            event.doit = !selectedNodes.isEmpty() && AlarmTreeView.containsOnlyPVNodes(selectedNodes);
        } else {
            throw new IllegalStateException("Viewer of " + AlarmTreeView.class.getName() +
            " mustn't be null at this point.");
        }
    }

    @Override
    public void dragSetData(@Nonnull final DragSourceEvent event) {
        final TreeViewer viewer = _alarmTreeView.getViewer();
        if (viewer != null) {
            final List<IAlarmTreeNode> selectedNodes = AlarmTreeView.selectionToNodeList(viewer.getSelection());
         // TODO jhatje: implement new datatype
//            event.data = selectedNodes.toArray(new IProcessVariable[selectedNodes.size()]);
        } else {
            throw new IllegalStateException("Viewer of " + AlarmTreeView.class.getName() +
            " mustn't be null at this point.");
        }
    }

    @Override
    public void dragFinished(@Nonnull final DragSourceEvent event) {
        // EMPTY
    }
}
