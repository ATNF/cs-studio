/* 
 * Copyright (c) 2006 Stiftung Deutsches Elektronen-Synchroton, 
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

package org.csstudio.opibuilder.editor;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import org.csstudio.opibuilder.actions.ChangeOrderAction;
import org.csstudio.opibuilder.actions.CopyPropertiesAction;
import org.csstudio.opibuilder.actions.CopyWidgetsAction;
import org.csstudio.opibuilder.actions.CutWidgetsAction;
import org.csstudio.opibuilder.actions.DistributeWidgetsAction;
import org.csstudio.opibuilder.actions.PastePropertiesAction;
import org.csstudio.opibuilder.actions.PasteWidgetsAction;
import org.csstudio.opibuilder.actions.PrintDisplayAction;
import org.csstudio.opibuilder.actions.RunOPIAction;
import org.csstudio.opibuilder.actions.ShowMacrosAction;
import org.csstudio.opibuilder.actions.ChangeOrderAction.OrderType;
import org.csstudio.opibuilder.actions.DistributeWidgetsAction.DistributeType;
import org.csstudio.opibuilder.commands.SetWidgetPropertyCommand;
import org.csstudio.opibuilder.editparts.AbstractBaseEditPart;
import org.csstudio.opibuilder.editparts.ExecutionMode;
import org.csstudio.opibuilder.editparts.WidgetEditPartFactory;
import org.csstudio.opibuilder.model.DisplayModel;
import org.csstudio.opibuilder.model.RulerModel;
import org.csstudio.opibuilder.palette.OPIEditorPaletteFactory;
import org.csstudio.opibuilder.palette.WidgetCreationFactory;
import org.csstudio.opibuilder.persistence.XMLUtil;
import org.csstudio.opibuilder.preferences.PreferencesHelper;
import org.csstudio.opibuilder.runmode.OPIRunner;
import org.csstudio.opibuilder.util.ConsoleService;
import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.platform.ui.dialogs.SaveAsDialog;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.AutoexposeHelper;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ViewportAutoexposeHelper;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.rulers.RulerProvider;
import org.eclipse.gef.ui.actions.ActionBarContributor;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.AlignmentAction;
import org.eclipse.gef.ui.actions.CopyTemplateAction;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.gef.ui.actions.DirectEditAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.MatchHeightAction;
import org.eclipse.gef.ui.actions.MatchWidthAction;
import org.eclipse.gef.ui.actions.ToggleGridAction;
import org.eclipse.gef.ui.actions.ToggleRulerVisibilityAction;
import org.eclipse.gef.ui.actions.ToggleSnapToGeometryAction;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.properties.UndoablePropertySheetEntry;
import org.eclipse.gef.ui.rulers.RulerComposite;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;


/**The OPI Editor.
 * @author Xihui Chen, Sven Wende & Alexander Will (part of the code is copied from SDS)
 *
 */
public class OPIEditor extends GraphicalEditorWithFlyoutPalette {
	
	/**
	 * The file extension for OPI files.
	 */
	public static final String OPI_FILE_EXTENSION = "opi"; //$NON-NLS-1$
	public static final String ID = "org.csstudio.opibuilder.OPIEditor"; //$NON-NLS-1$

	private PaletteRoot paletteRoot;
	
	/** the undoable <code>IPropertySheetPage</code> */
	private PropertySheetPage undoablePropertySheetPage;
	
	private DisplayModel displayModel;

	private RulerComposite rulerComposite;

	private KeyHandler sharedKeyHandler;

	private OverviewOutlinePage overviewOutlinePage;
	
	private Clipboard clipboard;

	
	public OPIEditor() {
		if(getPalettePreferences().getPaletteState() <= 0)
			getPalettePreferences().setPaletteState(FlyoutPaletteComposite.STATE_PINNED_OPEN);
		setEditDomain(new DefaultEditDomain(this));				
	}
	
	@Override
	public void init(final IEditorSite site, final IEditorInput input)
			throws PartInitException {		
		//in the mode of "no edit", open OPI in runtime and close this editor immediately.
		if(PreferencesHelper.isNoEdit()){			
			IDE.openEditor(site.getPage(), input, OPIRunner.ID);
			setSite(site);
			setInput(input);
						
			Display.getDefault().asyncExec(new Runnable(){
				public void run() {
					try {
						getSite().getPage().closeEditor(OPIEditor.this, false);
					} catch (Exception e) {}	
				}
			});					
		
		}		
		else 
			super.init(site, input);	
	}

	@Override
	public void commandStackChanged(EventObject event) {
		firePropertyChange(IEditorPart.PROP_DIRTY);
		super.commandStackChanged(event);
	}
	
	
	@Override
	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();
		ScrollingGraphicalViewer viewer = (ScrollingGraphicalViewer)getGraphicalViewer();
		viewer.setEditPartFactory(new WidgetEditPartFactory(ExecutionMode.EDIT_MODE));
		ScalableFreeformRootEditPart root = new ScalableFreeformRootEditPart() {
			/**
			 * {@inheritDoc}
			 */
			@SuppressWarnings("unchecked")
			@Override
			public Object getAdapter(final Class key) {
				if (key == AutoexposeHelper.class) {
					return new ViewportAutoexposeHelper(this);
				}
				return super.getAdapter(key);
			}
		};		
		viewer.setRootEditPart(root);
		viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer).setParent(getCommonKeyHandler()));
		ContextMenuProvider cmProvider = 
			new OPIEditorContextMenuProvider(viewer, getActionRegistry());
		viewer.setContextMenu(cmProvider);
		getSite().registerContextMenu(cmProvider, viewer);		
		
		// Grid Action
		IAction action = new ToggleGridAction(getGraphicalViewer()){
			@Override
			public boolean isChecked() {
				return getDisplayModel().isShowGrid();
			}
			@Override
			public void run() {
				getCommandStack().execute(new SetWidgetPropertyCommand(displayModel,
						DisplayModel.PROP_SHOW_GRID, !isChecked()));				
			}
		};		
		
		getActionRegistry().registerAction(action);
		
		// Ruler Action
		configureRuler();
		action = new ToggleRulerVisibilityAction(getGraphicalViewer()){
			@Override
			public boolean isChecked() {
				return getDisplayModel().isShowRuler();
			}
			
			@Override
			public void run() {
				getCommandStack().execute(new SetWidgetPropertyCommand(displayModel,
						DisplayModel.PROP_SHOW_RULER, !isChecked()));	
			}
			
		};		
		getActionRegistry().registerAction(action);
		
		// Snap to Geometry Action
		IAction geometryAction = new ToggleSnapToGeometryAction(getGraphicalViewer()){
			@Override
			public boolean isChecked() {
				return getDisplayModel().isSnapToGeometry();
			}
			@Override
			public void run() {
				getCommandStack().execute(new SetWidgetPropertyCommand(displayModel,
						DisplayModel.PROP_SNAP_GEOMETRY, !isChecked()));	
			}
			
		};		
		getActionRegistry().registerAction(geometryAction);
		
		// configure zoom actions
		ZoomManager zm = root.getZoomManager();

		List<String> zoomLevels = new ArrayList<String>(3);
		zoomLevels.add(ZoomManager.FIT_ALL);
		zoomLevels.add(ZoomManager.FIT_WIDTH);
		zoomLevels.add(ZoomManager.FIT_HEIGHT);
		zm.setZoomLevelContributions(zoomLevels);

		zm.setZoomLevels(createZoomLevels());

		if (zm != null) {
			IAction zoomIn = new ZoomInAction(zm);
			IAction zoomOut = new ZoomOutAction(zm);
			getActionRegistry().registerAction(zoomIn);
			getActionRegistry().registerAction(zoomOut);
		}

		/* scroll-wheel zoom */
		getGraphicalViewer().setProperty(
				MouseWheelHandler.KeyGenerator.getKey(SWT.MOD1),
				MouseWheelZoomHandler.SINGLETON);
		
		// status line listener
		getGraphicalViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			private IStatusLineManager statusLine =
					((ActionBarContributor)getEditorSite().getActionBarContributor()).
					getActionBars().getStatusLineManager();
			public void selectionChanged(SelectionChangedEvent event) {				
				List<AbstractBaseEditPart> selectedWidgets = new ArrayList<AbstractBaseEditPart>();
				for(Object editpart : getGraphicalViewer().getSelectedEditParts()){
					if(editpart instanceof AbstractBaseEditPart)
						selectedWidgets.add((AbstractBaseEditPart) editpart);
				}
				if(selectedWidgets.size() == 1)
					statusLine.setMessage(selectedWidgets.get(0).getWidgetModel().getName() + "(" //$NON-NLS-1$
							+ selectedWidgets.get(0).getWidgetModel().getType() + ")"); //$NON-NLS-1$
				else if (selectedWidgets.size() >=1)
					statusLine.setMessage(selectedWidgets.size() + " widgets were selected");
				else
					statusLine.setMessage("No widget was selected");
			}
		});
	}
	
	/**
	 * Configure the properties for the rulers.
	 */
	private void configureRuler() {
		// Ruler properties
		RulerProvider hprovider = new OPIEditorRulerProvider(new RulerModel(true));
		RulerProvider vprovider = new OPIEditorRulerProvider(new RulerModel(false));
		getGraphicalViewer().setProperty(
				RulerProvider.PROPERTY_HORIZONTAL_RULER, hprovider);
		getGraphicalViewer().setProperty(
				RulerProvider.PROPERTY_VERTICAL_RULER, vprovider);		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void createActions() {
		super.createActions();

		ActionRegistry registry = getActionRegistry();
		IAction action;

		action = new CopyTemplateAction(this);
		registry.registerAction(action);

		action = new MatchWidthAction(this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new MatchHeightAction(this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DirectEditAction((IWorkbenchPart) this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		String id = ActionFactory.DELETE.getId();
		action = getActionRegistry().getAction(id);
		action.setActionDefinitionId("org.eclipse.ui.edit.delete"); //$NON-NLS-1$
		
		action = new PasteWidgetsAction(this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());
		
		action = new CopyWidgetsAction(this, 
				(PasteWidgetsAction) registry.getAction(ActionFactory.PASTE.getId()));
		registry.registerAction(action);
		getSelectionActions().add(action.getId());
		
		action = new CutWidgetsAction(this,
				(DeleteAction) registry.getAction(ActionFactory.DELETE.getId()),
				(PasteWidgetsAction) registry.getAction(ActionFactory.PASTE.getId()));
		registry.registerAction(action);
		getSelectionActions().add(action.getId());
		
		action = new PrintDisplayAction(this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());
	
		action = new ShowMacrosAction(this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());
		
		id = ActionFactory.SELECT_ALL.getId();
		action = getActionRegistry().getAction(id);
		action.setActionDefinitionId("org.eclipse.ui.edit.selectAll");//$NON-NLS-1$

		id = ActionFactory.UNDO.getId();
		action = getActionRegistry().getAction(id);
		action.setActionDefinitionId("org.eclipse.ui.edit.undo");//$NON-NLS-1$

		id = ActionFactory.REDO.getId();
		action = getActionRegistry().getAction(id);
		action.setActionDefinitionId("org.eclipse.ui.edit.redo");//$NON-NLS-1$

		action = new AlignmentAction((IWorkbenchPart) this,
				PositionConstants.LEFT);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentAction((IWorkbenchPart) this,
				PositionConstants.RIGHT);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentAction((IWorkbenchPart) this,
				PositionConstants.TOP);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentAction((IWorkbenchPart) this,
				PositionConstants.BOTTOM);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentAction((IWorkbenchPart) this,
				PositionConstants.CENTER);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentAction((IWorkbenchPart) this,
				PositionConstants.MIDDLE);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());
		
		for(DistributeType dt : DistributeType.values()){
			action = new DistributeWidgetsAction((IWorkbenchPart) this,
				dt);
			registry.registerAction(action);
			getSelectionActions().add(action.getId());
		}
		action = new ChangeOrderAction((IWorkbenchPart)this, OrderType.TO_FRONT);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());
		
		action = new ChangeOrderAction((IWorkbenchPart)this, OrderType.STEP_FRONT);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());
		
		action = new ChangeOrderAction((IWorkbenchPart)this, OrderType.STEP_BACK);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());
		
		action = new ChangeOrderAction((IWorkbenchPart)this, OrderType.TO_BACK);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());
		
		action = new RunOPIAction();
		registry.registerAction(action);
		
		PastePropertiesAction pastePropAction = new PastePropertiesAction(this);
		registry.registerAction(pastePropAction);
		getSelectionActions().add(pastePropAction.getId());
		
		action = new CopyPropertiesAction(this, pastePropAction);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());
		
	}
	
	@Override
	protected void createGraphicalViewer(Composite parent) {
		initDisplayModel();
		
		rulerComposite = new RulerComposite(parent, SWT.NONE);

		GraphicalViewer viewer = new PatchedScrollingGraphicalViewer();
		viewer.createControl(rulerComposite);
		setGraphicalViewer(viewer);
		configureGraphicalViewer();
		hookGraphicalViewer();
		initializeGraphicalViewer();

		rulerComposite
				.setGraphicalViewer((ScrollingGraphicalViewer) getGraphicalViewer());
			
	}

	@Override
	protected PaletteViewerProvider createPaletteViewerProvider() {
		return new PaletteViewerProvider(getEditDomain()) {
			protected void configurePaletteViewer(PaletteViewer viewer) {
				super.configurePaletteViewer(viewer);
				// create a drag source listener for this palette viewer
				// together with an appropriate transfer drop target listener, this will enable
				// model element creation by dragging a CombinatedTemplateCreationEntries 
				// from the palette into the editor
				// @see ShapesEditor#createTransferDropTargetListener()
				viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
			}
		};
	}
	
	/**
	 * Create a transfer drop target listener. When using a CombinedTemplateCreationEntry
	 * tool in the palette, this will enable model element creation by dragging from the palette.
	 * @see #createPaletteViewerProvider()
	 */
	private TransferDropTargetListener createTransferDropTargetListener() {
		return new TemplateTransferDropTargetListener(getGraphicalViewer()) {
			protected CreationFactory getFactory(Object template) {
				return (WidgetCreationFactory)template;
			}
		};
	}
	
	/**
	 * Create a double array that contains the pre-defined zoom levels.
	 * 
	 * @return A double array that contains the pre-defined zoom levels.
	 */
	private double[] createZoomLevels() {
		List<Double> zoomLevelList = new ArrayList<Double>();

		double level = 0.1;
		while (level < 1.0) {
			zoomLevelList.add(level);
			level = level + 0.05;
		}

		zoomLevelList.add(1.0);
		zoomLevelList.add(1.5);
		zoomLevelList.add(2.0);
		zoomLevelList.add(2.5);
		zoomLevelList.add(3.0);
		zoomLevelList.add(3.5);
		zoomLevelList.add(4.0);
		zoomLevelList.add(4.5);
		zoomLevelList.add(5.0);

		double[] result = new double[zoomLevelList.size()];
		for (int i = 0; i < zoomLevelList.size(); i++) {
			result[i] = zoomLevelList.get(i);
		}

		return result;
	}
	
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		if (getEditorInput() instanceof FileEditorInput
				|| getEditorInput() instanceof FileStoreEditorInput) {
			performSave();
		} else {
			doSaveAs();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doSaveAs() {
		SaveAsDialog saveAsDialog = new SaveAsDialog(getEditorSite().getShell());
		if(getEditorInput() instanceof FileEditorInput)
			saveAsDialog.setOriginalFile(((FileEditorInput)getEditorInput()).getFile());
		else if(getEditorInput() instanceof FileStoreEditorInput)
			saveAsDialog.setOriginalName(((FileStoreEditorInput)getEditorInput()).getName());
		
		int ret = saveAsDialog.open();

		try {
			if (ret == Window.OK) {
				IPath targetPath = saveAsDialog.getResult();
				IFile targetFile = ResourcesPlugin.getWorkspace().getRoot()
						.getFile(targetPath);

				if (!targetFile.exists()) {
					targetFile.create(null, true, null);
				}

				FileEditorInput editorInput = new FileEditorInput(targetFile);

				setInput(editorInput);

				setPartName(targetFile.getName());

				performSave();
			}
		} catch (CoreException e) {
			MessageDialog.openError(getSite().getShell(), "IO Error", e
					.getMessage());
			CentralLogger.getInstance().error(this, e);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Class type) {
		if(type == IPropertySheetPage.class)
			return getPropertySheetPage();
		else if (type == ZoomManager.class)
			return ((ScalableFreeformRootEditPart) getGraphicalViewer()
				.getRootEditPart()).getZoomManager();
		else if (type == IContentOutlinePage.class) 
			return getOverviewOutlinePage();
			
		return super.getAdapter(type);
	}
	
	public Clipboard getClipboard() {
		if(clipboard == null)
			clipboard = new Clipboard(getSite().getShell().getDisplay());
		return clipboard;
	}
	

	/**
	 * Returns the KeyHandler with common bindings for both the Outline and Graphical Views.
	 * For example, delete is a common action.
	 */
	protected KeyHandler getCommonKeyHandler(){
		if (sharedKeyHandler == null){
			sharedKeyHandler = new KeyHandler();
			sharedKeyHandler.put(
				KeyStroke.getPressed(SWT.F2, 0),
				getActionRegistry().getAction(GEFActionConstants.DIRECT_EDIT));
		}
		return sharedKeyHandler;
	}
	
	/**
	 * Returns the Point, which is the center of the Display.
	 * 
	 * @return Point The Point, which is the center of the Display
	 */
	public Point getDisplayCenterPosition() {
		ScalableFreeformRootEditPart root = (ScalableFreeformRootEditPart) getGraphicalViewer()
				.getRootEditPart();

		ZoomManager m = root.getZoomManager();

		Point center = m.getViewport().getBounds().getCenter();

		Rectangle x = new Rectangle(center.x, center.y, 10, 10);

		x.translate(m.getViewport().getViewLocation());
		m.getScalableFigure().translateFromParent(x);

		Point result = x.getLocation();

		return result;
	}
	
	public DisplayModel getDisplayModel(){
		return displayModel;
	}
	
	@Override
	protected Control getGraphicalControl() {
		return rulerComposite;
	}
		

	/**
	 * Returns a stream which can be used to read this editors input data.
	 * 
	 * @return a stream which can be used to read this editors input data
	 */
	private InputStream getInputStream() {
		InputStream result = null;

		IEditorInput editorInput = getEditorInput();
		if (editorInput instanceof FileEditorInput) {
			try {
				result = ((FileEditorInput) editorInput).getFile()
						.getContents();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		} else if (editorInput instanceof FileStoreEditorInput) {
			IPath path = URIUtil.toPath(((FileStoreEditorInput) editorInput)
					.getURI());
			try {
				result = new FileInputStream(path.toFile());
			} catch (FileNotFoundException e) {
				result = null;
			}
		}

		return result;
	}
	
	
	
	
	
	/**
	* Returns the overview for the outline view.
	*
	* @return the overview
	*/
	protected OverviewOutlinePage getOverviewOutlinePage()
	{
		if (null == overviewOutlinePage && null != getGraphicalViewer())
		{
			RootEditPart rootEditPart = getGraphicalViewer().getRootEditPart();
			if (rootEditPart instanceof ScalableFreeformRootEditPart)
			{
				overviewOutlinePage =
					new OverviewOutlinePage(
							(ScalableFreeformRootEditPart) rootEditPart);
			}
		}
		return overviewOutlinePage;
	}
	
	@Override
	protected PaletteRoot getPaletteRoot() {
		if(paletteRoot == null)
			paletteRoot = OPIEditorPaletteFactory.createPalette();
		return paletteRoot;
	}
	
	/**
	 * Returns the main composite of the editor.
	 * 
	 * @return the main composite of the editor
	 */
	public Composite getParentComposite() {
		return rulerComposite;
	}
	
	
	/**
	* Returns the undoable <code>PropertySheetPage</code> for
	* this editor.
	*
	* @return the undoable <code>PropertySheetPage</code>
	*/
	protected PropertySheetPage getPropertySheetPage(){
		if(undoablePropertySheetPage == null){
			undoablePropertySheetPage = new PropertySheetPage();
			undoablePropertySheetPage.setRootEntry(
					new UndoablePropertySheetEntry(getCommandStack()));
		}
		return undoablePropertySheetPage;
	}
	
	/**
	 * 
	 */
	private void initDisplayModel() {
		
		displayModel = new DisplayModel();
		try {
			XMLUtil.fillDisplayModelFromInputStream(getInputStream(), displayModel);
		} catch (Exception e) {
			String message = "Error happened when loading the OPI file! An empty OPI will be created if no widget was loaded.\n"
				+ e.getMessage();
			MessageDialog.openError(getSite().getShell(), "File Open Error",
					message);
			CentralLogger.getInstance().error(this, e);
			ConsoleService.getInstance().writeError(message);
			
		}	
		displayModel.setOpiFilePath(getOPIFilePath());
	}
	
	private IPath getOPIFilePath() {
		IEditorInput editorInput = getEditorInput();
		if (editorInput instanceof FileEditorInput) {
			
			return ((FileEditorInput) editorInput).getFile().getFullPath();						
			
		} else if (editorInput instanceof FileStoreEditorInput) {
			return URIUtil.toPath(((FileStoreEditorInput) editorInput)
					.getURI());			
		}
		return null;
	}

	@Override
	protected void initializeGraphicalViewer() {
		super.initializeGraphicalViewer();
		GraphicalViewer viewer = getGraphicalViewer();
	
		viewer.setContents(displayModel);
		
		viewer.addDropTargetListener(createTransferDropTargetListener());
		setPartName(getEditorInput().getName());

	}
	
	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}
	private void performSave() {
		
		try {			
			if (getEditorInput() instanceof FileEditorInput) {
				final PipedInputStream in = new PipedInputStream();		
				final PipedOutputStream out = new PipedOutputStream(in);
				new Thread(
						 new Runnable(){
						     public void run(){
						     	try {
						     		
									XMLUtil.WidgetToOutputStream(displayModel, out, true);
						     		out.close();
								} catch (IOException e) {
									MessageDialog.openError(getSite().getShell(),
											"Save Error", e.getMessage());
									CentralLogger.getInstance().error(this, e);
								}
						      }
						    }
				).start();
				
				((FileEditorInput) getEditorInput()).getFile().setContents(
						in, false, false, null);
				in.close();
				
			} else if (getEditorInput() instanceof FileStoreEditorInput) {
				File file = URIUtil.toPath(
						((FileStoreEditorInput) getEditorInput()).getURI())
						.toFile();
				String content = XMLUtil.WidgetToXMLString(displayModel, true);
				
					FileWriter fileWriter = new FileWriter(file, false);
					BufferedWriter writer = new BufferedWriter(fileWriter);
					writer.write(content);
					writer.flush();
					writer.close();
			}	
		} catch (Exception e) {
				MessageDialog.openError(getSite().getShell(),
						"IO Error", e.getMessage());
				CentralLogger.getInstance().error(this, e);
		}		

		getCommandStack().markSaveLocation();

		firePropertyChange(IEditorPart.PROP_DIRTY);
	
	}

	
}
