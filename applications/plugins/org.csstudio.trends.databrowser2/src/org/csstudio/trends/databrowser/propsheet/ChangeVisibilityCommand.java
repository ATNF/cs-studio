package org.csstudio.trends.databrowser.propsheet;

import org.csstudio.swt.xygraph.undo.IUndoableCommand;
import org.csstudio.swt.xygraph.undo.OperationsManager;
import org.csstudio.trends.databrowser.Messages;
import org.csstudio.trends.databrowser.model.ModelItem;

/** Undo-able command to change a PV item's request type
 *  @author Kay Kasemir
 */
public class ChangeVisibilityCommand implements IUndoableCommand
{
    final private ModelItem item;
    final private boolean old_visibility, new_visibility;

    /** Register and perform the command
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param item Model item to configure
     *  @param new_trace_type New value
     */
    public ChangeVisibilityCommand(final OperationsManager operations_manager,
            final ModelItem item, final boolean visible)
    {
        this.item = item;
        this.old_visibility = item.isVisible();
        this.new_visibility = visible;
        operations_manager.addCommand(this);
        redo();
    }

    /** {@inheritDoc} */
    public void redo()
    {
        item.setVisible(new_visibility);
    }

    /** {@inheritDoc} */
    public void undo()
    {
        item.setVisible(old_visibility);
    }
    
    /** @return Command name that appears in undo/redo menu */
    @Override
    public String toString()
    {
        return Messages.TraceVisibility;
    }
}
