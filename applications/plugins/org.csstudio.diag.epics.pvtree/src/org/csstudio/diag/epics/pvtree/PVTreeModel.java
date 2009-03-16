/**
 * 
 */
package org.csstudio.diag.epics.pvtree;

import java.util.HashMap;
import java.util.List;

import org.csstudio.platform.logging.CentralLogger;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

/** The PV Tree Model
 *  <p>
 *  Unfortunately, this is not a generic model of the PV Tree data.
 *  It is tightly coupled to the TreeViewer, acting as the content provider,
 *  and directly updating/refreshing the tree GUI.
 *  <p>
 *  Note that most of the logic is actually inside the PVTreeItem.
 *  @see PVTreeItem
 *  @author Kay Kasemir
 */
class PVTreeModel implements IStructuredContentProvider, ITreeContentProvider
{
    /** The view to which we are connected. */
    final private TreeViewer viewer;

    private PVTreeItem root;

    final private HashMap<String, List<String>> field_info;
    
    /** @param view 
     *  @throws Exception on error in preferences
     */
    PVTreeModel(final TreeViewer viewer) throws Exception
    {
        this.viewer = viewer;
        field_info = Preferences.getFieldInfo();
        root = null;
    }

    /** @return Field info for all record types
     *  @see FieldParser
     */
    HashMap<String, List<String>> getFieldInfo()
    {
        return field_info;
    }
    
    /** Re-initialize the model with a new root PV. */
    public void setRootPV(final String name)
    {
        if (root != null)
        {
            root.dispose();
            root = null;
        }        
        root = new PVTreeItem(this, null, Messages.PV, name);
        itemChanged(root);
    }
    
    /** @return Returns a model item with given PV name or <code>null</code>. */
    public PVTreeItem findPV(final String pv_name)
    {
        return findPV(pv_name, root);
    }

    /** Searches for item from given item on down. */
    private PVTreeItem findPV(final String pv_name, final PVTreeItem item)
    {
        // Dead end?
        if (item == null)
            return null;
        // Is it this one?
        if (item.getName().equals(pv_name))
            return item;
        // Check each child recursively
        for (PVTreeItem child : item.getLinks())
        {
            final PVTreeItem found = findPV(pv_name, child);
            if (found != null)
                return found;
        }
        return null;
    }
    
    // IStructuredContentProvider
    public void inputChanged(Viewer v, Object oldInput, Object newInput)
    {
        // NOP
    }

    public void dispose()
    {
        if (root != null)
        {
            CentralLogger.getInstance().getLogger(this)
                .debug("PVTreeModel disposed"); //$NON-NLS-1$
            root.dispose();
            root = null;
        }
    }

    // IStructuredContentProvider
    public Object[] getElements(final Object parent)
    {
        if (parent instanceof PVTreeItem)
            return getChildren(parent);
        if (root != null)
            return new Object[] { root };
        return new Object[0];
    }

    // ITreeContentProvider
    public Object getParent(final Object child)
    {
        if (child instanceof PVTreeItem)
            return ((PVTreeItem) child).getParent();
        return null;
    }
    
    // ITreeContentProvider
    public Object[] getChildren(final Object parent)
    {
        if (parent instanceof PVTreeItem)
            return ((PVTreeItem) parent).getLinks();
        return new Object[0];
    }

    // ITreeContentProvider
    public boolean hasChildren(final Object parent)
    {
        if (parent instanceof PVTreeItem)
            return ((PVTreeItem) parent).hasLinks();
        return false;
    }

    /** Used by item to fresh the tree from the item on down. */
    public void itemUpdated(final PVTreeItem item)
    {
        if (viewer.getTree().isDisposed())
            return;
        viewer.update(item, null);
    }

    /** Used by item to refresh the tree from the item on down. */
    public void itemChanged(final PVTreeItem item)
    {
        if (viewer.getTree().isDisposed())
            return;
        if (item == root)
            viewer.refresh();
        else
            viewer.refresh(item);
        viewer.expandAll();
    }
}