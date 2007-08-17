package org.csstudio.platform.ui.workbench;

import java.net.URI;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
* Implements an IEditorInput instance appropriate for 
* <code>IFileStore</code> elements that represent files
* that are not part of the current workspace.
* 
* @since 3.3
*
*/
public class FileStoreEditorInput implements IEditorInput {

	/**
	 * The workbench adapter which simply provides the label.
	 *
	 * @since 3.3
	 */
	private static class WorkbenchAdapter implements IWorkbenchAdapter {
		/*
		 * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
		 */
		public Object[] getChildren(Object o) {
			return null;
		}

		/*
		 * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
		 */
		public ImageDescriptor getImageDescriptor(Object object) {
			return null;
		}

		/*
		 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
		 */
		public String getLabel(Object o) {
			return ((FileStoreEditorInput) o).getName();
		}

		/*
		 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
		 */
		public Object getParent(Object o) {
			return null;
		}
	}

	private IFileStore fileStore;
	private WorkbenchAdapter workbenchAdapter = new WorkbenchAdapter();

	/**
	 * @param fileStore
	 */
	public FileStoreEditorInput(IFileStore fileStore) {
		Assert.isNotNull(fileStore);
		this.fileStore = fileStore;
		workbenchAdapter = new WorkbenchAdapter();
	}

	/*
	 * @see org.eclipse.ui.IEditorInput#exists()
	 */
	public boolean exists() {
		return fileStore.fetchInfo().exists();
	}

	/*
	 * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
	 */
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	/*
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	public String getName() {
		return fileStore.getName();
	}

	/*
	 * @see org.eclipse.ui.IEditorInput#getPersistable()
	 */
	public IPersistableElement getPersistable() {
		return null;
	}

	/*
	 * @see org.eclipse.ui.IEditorInput#getToolTipText()
	 */
	public String getToolTipText() {
		return fileStore.toString();
	}

	/*
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		if (IWorkbenchAdapter.class.equals(adapter))
			return workbenchAdapter;
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (o instanceof FileStoreEditorInput) {
			FileStoreEditorInput input = (FileStoreEditorInput) o;
			return fileStore.equals(input.fileStore);
		}

		return false;
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return fileStore.hashCode();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IURIEditorInput#getURI()
	 */
	public URI getURI() {
		return fileStore.toURI();
	}
	
	public IPath getPath() {
		return URIUtil.toPath(getURI());
	}
	
}
