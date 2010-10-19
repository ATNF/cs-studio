/* 
 * Copyright (c) 2008 Stiftung Deutsches Elektronen-Synchrotron, 
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
package org.csstudio.sds.ui.internal.runmode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.platform.ui.util.CustomMediaFactory;
import org.csstudio.sds.internal.runmode.RunModeBoxInput;
import org.csstudio.sds.ui.SdsUiPlugin;
import org.csstudio.sds.ui.internal.actions.OpenScreenshotAction;
import org.csstudio.sds.ui.runmode.RunModeService;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;

/**
 * A box that manages a shell, which uses a GEF graphical viewer to display SDS
 * displays.
 * 
 * @author Sven Wende
 * @version $Revision: 1.26 $
 */
public final class ShellRunModeBox extends AbstractRunModeBox {

	private EditPartViewerProxy _editPartViewerProxy;

	private RunModeContextMenuProvider _contextMenuProvider;

	/**
	 * The width of the ScrollBars.
	 */
	private static final int SCROLLBAR_WIDTH = 17;

	/**
	 * The shell.
	 */
	private Shell _shell;

	
	/**
	 * Constructor.
	 * 
	 * @param input
	 *            the input
	 */
	public ShellRunModeBox(RunModeBoxInput input) {
		super(input);
	}

	/**
	 * {@inheritDoc}
	 */
	protected GraphicalViewer doOpen(final int x, final int y, final int width, final int height, final String title) {
		// create a shell
		_shell = new Shell();

		_shell.setText(title);
		_shell.setLocation(x, y);
		_shell.setLayout(new FillLayout());
		_shell.setImage(CustomMediaFactory.getInstance().getImageFromPlugin(SdsUiPlugin.PLUGIN_ID, "icons/sds.gif"));
		_shell.setSize(width + SCROLLBAR_WIDTH + 20, height + SCROLLBAR_WIDTH + 60);

		final ScrolledComposite scrollComposite = new ScrolledComposite(_shell, SWT.V_SCROLL | SWT.H_SCROLL);
		scrollComposite.setExpandHorizontal(true);
		scrollComposite.setExpandVertical(true);
		scrollComposite.setSize(width + SCROLLBAR_WIDTH, height + SCROLLBAR_WIDTH + 20);
		
		// create a parent composite that fills the whole shell
		final Composite parent = new Composite(scrollComposite, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginBottom = 0;
		layout.marginTop = 0;
		layout.marginLeft = 0;
		layout.marginRight = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		parent.setLayout(layout);
		parent.setSize(width, height + 20);

		// create a composite for the graphical viewer
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new FillLayout());
		c.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		// create a composite for path and navigation information
		List<RunModeBoxInput> predecessors = getPredecessors(getInput());

		if (predecessors.size() > 0) {
			Composite navigation = new Composite(parent, SWT.NONE);
			navigation.setLayout(new GridLayout(1, false));
			navigation.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

			for (int i = 0; i < predecessors.size(); i++) {
				new LinkLabel(navigation, predecessors.get(i));

				if (i < predecessors.size() - 1) {
					new SeparatorLabel(navigation);
				}
			}

			Composite filler = new Composite(navigation, SWT.NONE);
			filler.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		}

		// configure a graphical viewer
		final GraphicalViewer graphicalViewer = createGraphicalViewer(c);

		ActionRegistry actionRegistry = new ActionRegistry();
		this.createActions(actionRegistry);

		// provide a context menu, Note: We use an proxy for the EditpartViewer
		// to ensure that the real EditPartViewer can get garbage collected
		_editPartViewerProxy = new EditPartViewerProxy(graphicalViewer);
		_contextMenuProvider = new RunModeContextMenuProvider(_editPartViewerProxy, actionRegistry);

		_contextMenuProvider.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager manager) {
				manager.setRemoveAllWhenShown(true);
			}
		});
		graphicalViewer.setContextMenu(_contextMenuProvider);

		IWorkbenchPartSite site = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite();
		site.registerContextMenu("org.csstudio.sds.ui.runmode", _contextMenuProvider,
				_editPartViewerProxy);
		
		// provide a toolbar
		createToolbar(_shell, graphicalViewer);
		scrollComposite.setContent(parent);

		// add dispose listener
		_shell.addDisposeListener(new DisposeListener() {
			/**
			 * {@inheritDoc}
			 */
			public void widgetDisposed(final DisposeEvent e) {
				dispose();
			}
		});

		// open the shell
		_shell.open();

		return graphicalViewer;
	}

	/**
	 * Recursive method which returns all predecessors of the current run mode
	 * box.
	 * 
	 * @param input
	 *            the current box�s input
	 * @return the input�s of all predecessor boxes
	 */
	private List<RunModeBoxInput> getPredecessors(RunModeBoxInput input) {
		List<RunModeBoxInput> result = new ArrayList<RunModeBoxInput>();

		RunModeBoxInput predecessor = input.getPredecessorBox();

		if (predecessor != null) {
			result.addAll(getPredecessors(predecessor));
			result.add(predecessor);
		}

		return result;

	}

	/**
	 * Creates the actions for the shell.
	 * 
	 * @param actionRegistry
	 *            The {@link ActionRegistry} for the created actions
	 */
	private void createActions(final ActionRegistry actionRegistry) {
		Action closeAction = new Action() {
			@Override
			public void run() {
				Display.getCurrent().asyncExec(new Runnable() {
					public void run() {
						_shell.close();
					}
				});
			}
		};
		closeAction.setText("Close Shell");
		closeAction.setId(RunModeContextMenuProvider.CLOSE_ACTION_ID);
		actionRegistry.registerAction(closeAction);
	}

	/**
	 * Disposes the shell.
	 */
	protected void doDispose() {
		// close the shell
		if (!_shell.isDisposed()) {
			_shell.close();
			_shell = null;
			if(_contextMenuProvider!=null) {
			    _contextMenuProvider.dispose();
			    _contextMenuProvider = null;
			}
			if(_editPartViewerProxy!=null) {
			    _editPartViewerProxy.dispose();
			    _editPartViewerProxy = null;
			}
		}
	}

	/**
	 * Creates a toolbar for the graphical viewer.
	 * 
	 * @param shell
	 *            the shell
	 * @param graphicalViewer
	 *            the graphical viewer
	 */
	@SuppressWarnings("deprecation")
	protected void createToolbar(final Shell shell, final GraphicalViewer graphicalViewer) {
		// menu bar
		MenuManager menuManager = new MenuManager();

		// configure zoom actions
		RootEditPart rootEditPart = graphicalViewer.getRootEditPart();

		if (rootEditPart instanceof ScalableFreeformRootEditPart) {
			final ZoomManager zm = ((ScalableFreeformRootEditPart) rootEditPart).getZoomManager();

			final List<String> zoomLevels = new ArrayList<String>(3);
			zoomLevels.add(ZoomManager.FIT_ALL);
			zoomLevels.add(ZoomManager.FIT_WIDTH);
			zoomLevels.add(ZoomManager.FIT_HEIGHT);
			zm.setZoomLevelContributions(zoomLevels);

			if (zm != null) {
				MenuManager zoomManager = new MenuManager("Zoom");
				final IAction zoomIn = new ZoomInAction(zm);
				final IAction zoomOut = new ZoomOutAction(zm);

				zoomManager.add(zoomIn);
				zoomManager.add(zoomOut);

				menuManager.add(zoomManager);
			}

			MenuManager layerManager = new MenuManager("Layers");
			layerManager.add(new ChangeLayerVisibilityAction(graphicalViewer));
			menuManager.add(layerManager);
		}

		// Added by Markus Moeller, 2009-01-26
		// Search for the screenshot plugin
		IExtensionRegistry extReg = Platform.getExtensionRegistry();
		IConfigurationElement[] confElements = extReg.getConfigurationElementsFor("org.csstudio.utility.screenshot.ImageWorker");

		if (confElements.length > 0) {
			for (int i = 0; i < confElements.length; i++) {
				if (confElements[i].getContributor().getName().compareToIgnoreCase("org.csstudio.utility.screenshot") == 0) {
					MenuManager captureManager = new MenuManager("Screenshot");

					captureManager.add(new OpenScreenshotAction());
					menuManager.add(captureManager);
				}
			}
		}

		Menu menu = menuManager.createMenuBar(shell);
		shell.setMenuBar(menu);
	}

	/**
	 * Sets the focus on this Shell.
	 */
	public void bringToTop() {
		if (_shell != null && !_shell.isDisposed()) {
			_shell.setMinimized(false);
			_shell.setActive();
			_shell.setFocus();
		} else {
			RunModeService.getInstance().closeRunModeBox(getInput());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected void handleWindowPositionChange(final int x, final int y, final int width, final int height) {
		_shell.setBounds(x, y, width, height);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void finalize() throws Throwable {
		CentralLogger.getInstance().debug(this, "finalized()");
	}

	/**
	 * Helper class that represents a navigation separator.
	 * 
	 * @author Sven Wende
	 * 
	 */
	private static class SeparatorLabel {
		public SeparatorLabel(Composite parent) {
			Label label = new Label(parent, SWT.NONE);
			label.setImage(CustomMediaFactory.getInstance().getImageFromPlugin(SdsUiPlugin.PLUGIN_ID, "icons/arrow_right.png"));
			label.setText(" > ");
		}
	}

	/**
	 * Helper class that represents a display navigation link.
	 * 
	 * @author Sven Wende
	 */
	private static class LinkLabel extends MouseAdapter implements MouseTrackListener {
		private Label _label;
		private RunModeBoxInput _input;

		public LinkLabel(Composite parent, RunModeBoxInput input) {
			assert input != null;
			_input = input;
			_label = new Label(parent, SWT.NONE);
			_label.setText(input.getFilePath().lastSegment());
			_label.setForeground(CustomMediaFactory.getInstance().getColor(0, 0, 255));
			_label.addMouseListener(this);
			_label.addMouseTrackListener(this);
			_label
					.setToolTipText(_input.calculateFullPath() + " [" + new SimpleDateFormat("hh:mm").format(new Date(input.getTimestamp()))
							+ "]");
		}

		@Override
		public void mouseUp(MouseEvent e) {
			RunModeService.getInstance().openDisplayShellInRunMode(_input.getFilePath(), _input.getAliases());
		}

		public void mouseEnter(MouseEvent e) {
			_label.setForeground(CustomMediaFactory.getInstance().getColor(0, 255, 255));
		}

		public void mouseExit(MouseEvent e) {
			_label.setForeground(CustomMediaFactory.getInstance().getColor(0, 0, 255));
		}

		public void mouseHover(MouseEvent e) {
		}
	}

}
