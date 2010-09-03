/*
 * Copyright (c) 2007 Stiftung Deutsches Elektronen-Synchrotron,
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
/*
 * $Id: ChannelConfigDialog.java,v 1.2 2010/08/20 13:32:59 hrickens Exp $
 */
package org.csstudio.config.ioconfig.config.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;

import org.csstudio.config.ioconfig.config.view.helper.DocumentationManageView;
import org.csstudio.config.ioconfig.config.view.helper.ProfibusHelper;
import org.csstudio.config.ioconfig.model.DBClass;
import org.csstudio.config.ioconfig.model.IDocumentable;
import org.csstudio.config.ioconfig.model.Repository;
import org.csstudio.config.ioconfig.model.pbmodel.DataType;
import org.csstudio.config.ioconfig.model.pbmodel.GSDModule;
import org.csstudio.config.ioconfig.model.pbmodel.Module;
import org.csstudio.config.ioconfig.model.pbmodel.ModuleChannelPrototype;
import org.csstudio.config.ioconfig.model.pbmodel.SlaveCfgData;
import org.csstudio.config.ioconfig.model.pbmodel.gsdParser.GsdModuleModel;
import org.csstudio.platform.security.SecurityFacade;
import org.csstudio.platform.security.User;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * @author hrickens
 * @author $Author: hrickens $
 * @version $Revision: 1.2 $
 * @since 22.09.2008
 */
public class ChannelConfigDialog extends Dialog implements IHasDocumentableObject {

    private final GsdModuleModel _moduleModel;
    private boolean[] _ioTypeArray;
    private String _parameter;
    private final GSDModule _gsdModule;
    private static int _dirty;
    /**
     * The configuration Table for the input Channels.
     */
    private TableViewer _inputTableViewer;
    /**
     * The configuration Table for the output Channels.
     */
    private TableViewer _outputTableViewer;
    /**
     * A list of all input Channel.
     */
    private final ArrayList<ModuleChannelPrototype> _inputChannelPrototypeModelList;
    /**
     * A list of all output Channel.
     */
    private final ArrayList<ModuleChannelPrototype> _outputChannelPrototypeModelList;
    /**
     * The Tab folder for the I/O Configuration Tabel's.
     */
    private TabFolder _ioTabFolder;

    /**
     * Have this prototype input fields.
     */
    private boolean _inputs = false;
    /**
     * Have this prototype output fields.
     */
    private boolean _outputs = false;
    /**
     * If the data length of prototype word.
     */
    private boolean _word = true;
    private DocumentationManageView _documentationManageView;

    /**
     *
     * @param parentShell
     *            The parent shell for the dialog.
     * @param gsdModuleModel
     *            the GSD Module Model.
     * @param gsdModule
     *            the GSD Module.
     * @param module
     *            the Parent Module
     */
    public ChannelConfigDialog(final Shell parentShell, final GsdModuleModel gsdModuleModel,
            final GSDModule gsdModule, final Module module) {
        super(parentShell);
        setShellStyle(SWT.MODELESS | SWT.CLOSE | SWT.MAX | SWT.TITLE | SWT.BORDER | SWT.RESIZE);
        _moduleModel = gsdModuleModel;
        _gsdModule = gsdModule;
        _inputChannelPrototypeModelList = new ArrayList<ModuleChannelPrototype>();
        _outputChannelPrototypeModelList = new ArrayList<ModuleChannelPrototype>();
        if ((_moduleModel != null) && (_gsdModule != null)
                && (_gsdModule.getModuleChannelPrototypeNH() != null)) {
            for (ModuleChannelPrototype moduleChannelPrototype : _gsdModule
                    .getModuleChannelPrototypeNH()) {
                if (moduleChannelPrototype.isInput()) {
                    _inputChannelPrototypeModelList.add(moduleChannelPrototype);
                } else {
                    _outputChannelPrototypeModelList.add(moduleChannelPrototype);
                }
            }
        }
    }

    /**
     * @return the Configuration Data.
     */
    public final String getConfigurationData() {
        return Arrays.toString(_ioTypeArray);
    }

    /**
     * @return the Parameter String.
     */
    public final String getParameter() {
        return _parameter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDocumentable getDocumentableObject() {
        return _gsdModule;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final Control createDialogArea(final Composite parent) {
        getShell().setText("Module: " + _moduleModel.getName());
        Composite dialogAreaComposite = (Composite) super.createDialogArea(parent);
        createInfo(dialogAreaComposite);
        _ioTabFolder = new TabFolder(dialogAreaComposite, SWT.TOP);
        _ioTabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        if (_inputs) {
            TabItem inputTabItem = new TabItem(_ioTabFolder, SWT.NONE);
            inputTabItem.setText("Input");
            _inputTableViewer = createChannelTable(_ioTabFolder, _inputChannelPrototypeModelList);
            inputTabItem.setControl(_inputTableViewer.getTable());
        }
        if (_outputs) {
            TabItem outputTabItem = new TabItem(_ioTabFolder, SWT.NONE);
            outputTabItem.setText("Output");
            _outputTableViewer = createChannelTable(_ioTabFolder, _outputChannelPrototypeModelList);
            outputTabItem.setControl(_outputTableViewer.getTable());
        }

        createDocumetView(new TabItem(_ioTabFolder, SWT.NONE));
        parent.layout();
        return dialogAreaComposite;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void createButtonsForButtonBar(final Composite parent) {
        ((GridLayout) parent.getLayout()).numColumns = 2;
        ((GridData) parent.getLayoutData()).horizontalAlignment = SWT.FILL;
        GridData data;
        GridLayout gridLayout;

        // Button Left side
        Composite left = new Composite(parent, SWT.NONE);
        data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);
        data.grabExcessHorizontalSpace = true;
        left.setLayoutData(data);
        gridLayout = new GridLayout(0, true);
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        left.setLayout(gridLayout);
        Button addButton = createButton(left, IDialogConstants.NEXT_ID, "Add", false);
        addButton.addSelectionListener(new AddSelectionListener());
        Button removeButton = createButton(left, IDialogConstants.BACK_ID, "Remove", false);
        removeButton.addSelectionListener(new RemoveSelectionListener());

        // Button Left side
        Composite right = new Composite(parent, SWT.NONE);
        data = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_CENTER);
        right.setLayoutData(data);
        gridLayout = new GridLayout(0, true);
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        right.setLayout(gridLayout);
        super.createButtonsForButtonBar(right);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void okPressed() {
        if (_inputTableViewer != null) {
            _inputTableViewer.getTable().setFocus();
            // finish last edit
            try {
                for (CellEditor editor : _inputTableViewer.getCellEditors()) {
                    if (editor != null) {
                        editor.deactivate();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (_outputTableViewer != null) {
            _outputTableViewer.getTable().setFocus();
            // finish last edit
            try {
                for (CellEditor editor : _outputTableViewer.getCellEditors()) {
                    if (editor != null) {
                        editor.deactivate();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        _gsdModule.setDocuments(_documentationManageView.getDocuments());
        super.okPressed();
    }

    /**
     * @param tabItem
     */
    private void createDocumetView(@Nonnull final TabItem item) {
        String head = "Documents";
        item.setText(head);
        //TODO: (hrickens) Documents f�r Prototypen erm�glichen
        _documentationManageView = new DocumentationManageView(_ioTabFolder, SWT.NONE, this);
        item.setControl(_documentationManageView);
        _ioTabFolder.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(final SelectionEvent e) {
                docTabSelectionAction(e);
            }

            public void widgetSelected(final SelectionEvent e) {
                docTabSelectionAction(e);
            }

            private void docTabSelectionAction(final SelectionEvent e) {
                if (e.item.equals(item)) {
                    _documentationManageView.onActivate();
                }
            }

        });
    }

    /**
     *
     * @param dialogArea
     */
    private void createInfo(final Composite dialogArea) {
        final int size = 12;
        final int leftUperCorner = 0;

        final SlaveCfgData slaveCfgData = new SlaveCfgData(_moduleModel.getValue());
        _parameter = "";
        Composite box = new Composite(dialogArea, SWT.NONE);
        box.setLayout(new GridLayout(4, true));
        box.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

        String dataFormat;
        if (slaveCfgData.isWordSize()) {
            _word &= true;
            dataFormat = "Word's: ";
        } else {
            _word &= false;
            dataFormat = "Byte's: ";
        }
        _ioTypeArray = new boolean[slaveCfgData.getNumber()];
        final short[] bitArray = new short[slaveCfgData.getNumber() * slaveCfgData.getWordSize()];
        if ((_gsdModule != null) && (_gsdModule.getConfigurationData() != null)) {
            String[] ioType = _gsdModule.getConfigurationData().replaceAll("[\\[\\]]", "").split(
                    ",");
            for (int i = 0; (i < ioType.length) && (i < _ioTypeArray.length); i++) {
                _ioTypeArray[i] = Boolean.parseBoolean(ioType[i].trim());
            }

            String parameter = _gsdModule.getParameter();
            if (parameter != null) {
                String[] para = parameter.replaceAll("[\\[\\]]", "").split(",");
                for (int i = 0; (i < para.length) && (i < bitArray.length); i++) {
                    bitArray[i] = Short.parseShort(para[i].trim());
                }
            }

        }
        new Label(box, SWT.NONE).setText("Anzahl " + dataFormat + (slaveCfgData.getNumber()));
        _inputs |= slaveCfgData.isInput();
        new Label(box, SWT.NONE).setText("Input: " + slaveCfgData.isInput());
        _outputs = slaveCfgData.isOutput();
        new Label(box, SWT.NONE).setText("Output: " + slaveCfgData.isOutput());
        new Label(box, SWT.NONE).setText("Para: " + _moduleModel.getValue());

        for (int i = 0; i < slaveCfgData.getNumber(); i++) {
            final int position = i;

            // ioType == true => analog else digital
            // default io type is digital. Contain the Name an AI or AO set
            // it to analog.
            if ((_moduleModel.getName().contains("AI") || _moduleModel.getName().contains("AO"))
                    && !(_moduleModel.getName().contains("DI") || _moduleModel.getName().contains(
                            "DO"))) {
                for (int j = 0; j < _ioTypeArray.length; j++) {
                    _ioTypeArray[j] = true;
                }
            }
            final Canvas canvas = new Canvas(box, SWT.NONE);
            GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false, slaveCfgData
                    .getWordSize() / 8, 1);
            gridData.widthHint = size * slaveCfgData.getWordSize() + 15;
            gridData.heightHint = 2 * size + 5;
            canvas.setLayoutData(gridData);

            canvas.addPaintListener(new PaintListener() {
                public void paintControl(final PaintEvent e) {
                    int x0 = 0;
                    int x1 = size * slaveCfgData.getWordSize();
                    e.gc.drawRectangle(x0, leftUperCorner, x1, size);
                    e.gc.drawRectangle(x0, leftUperCorner + size, x1, size);
                    String type = "Digital";
                    if ((position < _ioTypeArray.length) && _ioTypeArray[position]) {
                        type = "Analog";
                    }
                    Point stringExtent = e.gc.stringExtent(type);
                    e.gc.drawString(type, (x1 - stringExtent.x) / 2, leftUperCorner, true);
                    for (int j = 1; j <= slaveCfgData.getWordSize(); j++) {
                        int x2 = x0 + j * size;
                        int pos = position * slaveCfgData.getWordSize() + (j - 1);
                        if ((slaveCfgData.isInput() && !slaveCfgData.isOutput())
                                || ((pos < bitArray.length) && (bitArray[pos] == 1))) {
                            stringExtent = e.gc.stringExtent("I");
                            bitArray[pos] = 1;
                            int newX = x2 + 1 - (size + stringExtent.x) / 2;
                            e.gc.drawString("I", newX, leftUperCorner + size, true);
                        } else if ((!slaveCfgData.isInput() && slaveCfgData.isOutput())
                                || ((pos < bitArray.length) && (bitArray[pos] == 2))) {
                            bitArray[pos] = 2;
                            stringExtent = e.gc.stringExtent("O");
                            int newX = x2 + 1 - (size + stringExtent.x) / 2;
                            e.gc.drawString("O", newX, leftUperCorner + size, true);
                        } else if (!(slaveCfgData.isInput() && slaveCfgData.isOutput())
                                || ((pos < bitArray.length) && (bitArray[pos] == 3))) {
                            bitArray[pos] = 3;
                            e.gc.drawLine(x2, leftUperCorner + size, x2 - size, leftUperCorner + 2
                                    * size);
                            e.gc.drawLine(x2 - size, leftUperCorner + size, x2, leftUperCorner + 2
                                    * size);
                        }
                        e.gc.drawLine(x2, leftUperCorner + size, x2, leftUperCorner + 2 * size);
                        _parameter = Arrays.toString(bitArray);
                    }
                }

            });
            canvas.addMouseListener(new MouseListener() {

                public void mouseDoubleClick(final MouseEvent e) {
                }

                public void mouseDown(final MouseEvent e) {
                    if (e.button == 1) {
                        int xPosition = (e.x) / size;
                        int arrayPos = xPosition + (position * slaveCfgData.getWordSize());
                        if (!(slaveCfgData.isInput() ^ slaveCfgData.isOutput()) && (xPosition >= 0)
                                && (arrayPos < bitArray.length) && (e.y > (size))
                                && (e.y < (2 * size) + 1)) {
                            bitArray[arrayPos] = (short) (++bitArray[arrayPos] % 4);
                            if (_ioTypeArray[position]) {
                                for (int j = position * slaveCfgData.getWordSize(); j < position
                                        * slaveCfgData.getWordSize() + slaveCfgData.getWordSize(); j++) {
                                    bitArray[j] = bitArray[arrayPos];
                                }
                            }
                            canvas.redraw();
                        } else if ((e.y > (0)) && (e.y < size)) {
                            _ioTypeArray[position] = !_ioTypeArray[position];
                            canvas.redraw();
                        }
                    }
                }

                public void mouseUp(final MouseEvent e) {
                }

            });

        }
    }

    /**
     *
     * @param tableParent
     *            the composite for ModuleChannelPrototypeModel table
     * @param channelPrototypeModelList
     * @return
     */
    private static TableViewer createChannelTable(final Composite tableParent,
            final ArrayList<ModuleChannelPrototype> channelPrototypeModelList) {
        int style = SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION
                | SWT.HIDE_SELECTION;

        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd.minimumHeight = 100;
        Table table = new Table(tableParent, style);
        table.setLayoutData(gd);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        addTableColumn(table, SWT.RIGHT, 45, ChannelPrototypConfigColumn.OFFSET.getText());
        addTableColumn(table, SWT.LEFT, 120, ChannelPrototypConfigColumn.NAME.getText());
        addTableColumn(table, SWT.LEFT, 75, ChannelPrototypConfigColumn.TYPE.getText());
        addTableColumn(table, SWT.RIGHT, 45, ChannelPrototypConfigColumn.SIZE.getText());
        addTableColumn(table, SWT.RIGHT, 45, ChannelPrototypConfigColumn.STRUCT.getText());
        addTableColumn(table, SWT.RIGHT, 55, ChannelPrototypConfigColumn.STATUS.getText());
        addTableColumn(table, SWT.RIGHT, 35, ChannelPrototypConfigColumn.MIN.getText());
        addTableColumn(table, SWT.RIGHT, 35, ChannelPrototypConfigColumn.MAX.getText());
        addTableColumn(table, SWT.LEFT, 55, ChannelPrototypConfigColumn.ORDER.getText());
        final TableViewer tableViewer = new TableViewer(table);
        tableViewer.setLabelProvider(new ChannelPrototypeConfigTableLabelProvider());
        tableViewer.setContentProvider(new ChannelTableContentProvider());
        ICellEditorValidator cellEditorValidator = new ICellEditorValidator() {

            public String isValid(final Object value) {
                if (value instanceof String) {
                    String stringValue = (String) value;
                    try {
                        Integer.parseInt(stringValue);
                        return null;
                    } catch (Exception e) {
                        return "Error_No_Integer";
                    }
                }
                return "Error_No_String";
            }

        };
        final CellEditor[] editors = new CellEditor[9];
        // Offset
        editors[0] = new TextCellEditor(table);
        editors[0].setValidator(cellEditorValidator);
        editors[0].activate();
        // Name
        editors[1] = new TextCellEditor(table);
        editors[1].activate();
        editors[1].addPropertyChangeListener(new IPropertyChangeListener() {

            public void propertyChange(final PropertyChangeEvent event) {
                String oldValue = (String) event.getOldValue();
                String newValue = (String) event.getNewValue();
                if (((oldValue == null) || (oldValue.length() == 0)) && (newValue != null)
                        && (newValue.length() > 0)) {
                    ChannelConfigDialog._dirty--;
                } else if ((oldValue != null) && (oldValue.length() > 0)
                        && ((newValue == null) || (newValue.length() < 1))) {
                    ChannelConfigDialog._dirty++;
                }
            }

        });

        // Type
        editors[2] = new ComboBoxCellEditor(table, DataType.getNames(), SWT.DROP_DOWN
                | SWT.READ_ONLY);
        editors[2].activate();
        // Size isn't to edit
        editors[3] = null;

        // Structure
        editors[4] = new CheckboxCellEditor(table, SWT.CHECK);
        editors[4].activate();

        // Status //ehemals Shift
        editors[5] = new TextCellEditor(table);
        editors[5].setValidator(cellEditorValidator);
        editors[5].activate();

        // MIN
        editors[6] = new TextCellEditor(table);
        editors[6].setValidator(cellEditorValidator);
        editors[6].activate();

        // MAX
        editors[7] = new TextCellEditor(table);
        editors[7].setValidator(cellEditorValidator);
        editors[7].activate();

        // Byte Order
        editors[8] = new TextCellEditor(table);
        editors[8].setValidator(cellEditorValidator);
        editors[8].activate();

        tableViewer.setColumnProperties(ChannelPrototypConfigColumn.getStringValues());
        tableViewer.setCellEditors(editors);
        tableViewer.setCellModifier(new ChannelConfigCellModifier(tableViewer));
        tableViewer.setInput(channelPrototypeModelList);
        return tableViewer;
    }

    /**
     *
     * @param table
     *            The parent table for the new column.
     * @param style
     *            the style of control to construct.
     * @param width
     *            Sets the new width of the receiver.
     * @param header
     *            The new column header text.
     */
    private static void addTableColumn(final Table table, final int style, final int width,
            final String header) {
        TableColumn tc = new TableColumn(table, style);
        tc.setText(header);
        tc.setResizable(true);
        tc.setWidth(width);
    }

    /**
     *
     * @author hrickens
     * @author $Author: hrickens $
     * @version $Revision: 1.2 $
     * @since 03.06.2009
     */
    private final class RemoveSelectionListener implements SelectionListener {
        public void widgetDefaultSelected(final SelectionEvent e) {
            removeItem();
        }

        public void widgetSelected(final SelectionEvent e) {
            removeItem();
        }

        @SuppressWarnings("unchecked")
        private void removeItem() {
            IStructuredSelection selection;
            if (_ioTabFolder.getSelection()[0].getText().equals("Input")) {
                selection = (IStructuredSelection) _inputTableViewer.getSelection();
                if (selection.size() > 0) {
                    List<ModuleChannelPrototype> list = selection.toList();
                    _inputChannelPrototypeModelList.removeAll(list);
                    _gsdModule.removeModuleChannelPrototype(list);
                    for (Object object : list) {
                        if (object instanceof DBClass) {
                            DBClass dbClass = (DBClass) object;
                            try {
                                Repository.removeNode(dbClass);
                            } catch (Exception e) {
                                ProfibusHelper.openErrorDialog(getShell(), "Data Base Error",
                                        "Device Data Base (DDB) Error\n"
                                                + "Can't delete the %1s (ID: %3s)", dbClass, e);
                                return;
                            }
                        }
                    }
                } else {
                    ModuleChannelPrototype remove = _inputChannelPrototypeModelList
                            .remove(_inputChannelPrototypeModelList.size() - 1);
                    Repository.removeNode(remove);
                }
                _inputTableViewer.refresh();
            } else {
                selection = (IStructuredSelection) _outputTableViewer.getSelection();
                if (selection.size() > 0) {
                    List<ModuleChannelPrototype> list = selection.toList();
                    _outputChannelPrototypeModelList.removeAll(list);
                    _gsdModule.removeModuleChannelPrototype(list);
                    for (Object object : list) {
                        if (object instanceof DBClass) {
                            DBClass dbClass = (DBClass) object;
                            Repository.removeNode(dbClass);
                        }
                    }
                    Repository.update(_gsdModule);

                } else {
                    ModuleChannelPrototype remove = _outputChannelPrototypeModelList
                            .remove(_outputChannelPrototypeModelList.size() - 1);
                    Repository.removeNode(remove);
                }
                _outputTableViewer.refresh();
            }
        }
    }

    /**
     *
     * @author hrickens
     * @author $Author: hrickens $
     * @version $Revision: 1.2 $
     * @since 03.06.2009
     */
    private final class AddSelectionListener implements SelectionListener {
        private Button _button;

        public void widgetDefaultSelected(final SelectionEvent e) {
            addItem();
        }

        public void widgetSelected(final SelectionEvent e) {
            addItem();
        }

        private void addItem() {
            _button = getButton(IDialogConstants.OK_ID);
            _button.setEnabled(true);
            ModuleChannelPrototype lastModuleChannelPrototypeModel;
            int offset = 0;
            DataType type;
            if (_word) {
                type = DataType.UINT16;
            } else {
                type = DataType.UINT8;
            }
            ModuleChannelPrototype moduleChannelPrototype = new ModuleChannelPrototype();
            User currentUser = SecurityFacade.getInstance().getCurrentUser();
            String user = "Unkown";
            if ((currentUser != null) && (currentUser.getUsername() != null)) {
                user = SecurityFacade.getInstance().getCurrentUser().getUsername();
            }
            moduleChannelPrototype.setCreatedBy(user);
            moduleChannelPrototype.setUpdatedBy(user);
            Date date = new Date();
            moduleChannelPrototype.setCreatedOn(date);
            moduleChannelPrototype.setUpdatedOn(date);
            moduleChannelPrototype.setName("");

            moduleChannelPrototype.setGSDModule(_gsdModule);
            if (_ioTabFolder.getSelection()[0].getText().equals("Input")) {
                if (!_inputChannelPrototypeModelList.isEmpty()) {
                    lastModuleChannelPrototypeModel = _inputChannelPrototypeModelList
                            .get(_inputChannelPrototypeModelList.size() - 1);
                    offset = lastModuleChannelPrototypeModel.getOffset();
                    offset += lastModuleChannelPrototypeModel.getSize();
                    type = lastModuleChannelPrototypeModel.getType();
                }
                moduleChannelPrototype.setOffset(offset);
                moduleChannelPrototype.setType(type);
                moduleChannelPrototype.setInput(true);
                moduleChannelPrototype.setGSDModule(_gsdModule);
                _gsdModule.addModuleChannelPrototype(moduleChannelPrototype);
                _inputChannelPrototypeModelList.add(moduleChannelPrototype);
                _inputTableViewer.refresh();
            } else {
                if (!_outputChannelPrototypeModelList.isEmpty()) {
                    lastModuleChannelPrototypeModel = _outputChannelPrototypeModelList
                            .get(_outputChannelPrototypeModelList.size() - 1);
                    offset = lastModuleChannelPrototypeModel.getOffset();
                    offset += lastModuleChannelPrototypeModel.getSize();
                    type = lastModuleChannelPrototypeModel.getType();
                }
                moduleChannelPrototype.setOffset(offset);
                moduleChannelPrototype.setType(type);
                moduleChannelPrototype.setInput(false);
                _gsdModule.addModuleChannelPrototype(moduleChannelPrototype);
                _outputChannelPrototypeModelList.add(moduleChannelPrototype);
                _outputTableViewer.refresh();
            }
        }
    }

    /**
     *
     * @author hrickens
     * @author $Author: hrickens $
     * @version $Revision: 1.2 $
     * @since 13.05.2009
     */
    private static final class ChannelConfigCellModifier implements ICellModifier {
        private final TableViewer _tableViewer;

        private ChannelConfigCellModifier(final TableViewer tableViewer) {
            _tableViewer = tableViewer;
        }

        public boolean canModify(final Object element, final String property) {
            ChannelPrototypConfigColumn column = ChannelPrototypConfigColumn.valueOf(property);
            switch (column) {
                case OFFSET:
                case NAME:
                case TYPE:
                case SHIFT:
                case STRUCT:
                case STATUS:
                case MIN:
                case MAX:
                case ORDER:
                    return true;
                case IO:
                case SIZE:
                default:
                    return false;
            }
        }

        public Object getValue(final Object element, final String property) {
            Object result = null;
            ModuleChannelPrototype channel = (ModuleChannelPrototype) element;

            switch (ChannelPrototypConfigColumn.valueOf(property)) {
                case OFFSET:
                    result = channel.getOffset() + "";
                    break;
                case NAME:
                    result = channel.getName();
                    if (result == null) {
                        result = "";
                    }
                    break;
                case TYPE:
                    result = channel.getType().ordinal();
                    break;
                case SHIFT:
                    result = channel.getShift() + "";
                    break;
                case IO:
                    result = channel.isInput();
                    break;
                case STRUCT:
                    result = channel.isStructure();
                    break;
                case STATUS:
                    result = channel.getShift() + "";
                    break;
                case MIN:
                    if (channel.getMinimum() == null) {
                        return "";
                    }
                    result = channel.getMinimum().toString();
                    break;
                case MAX:
                    if (channel.getMaximum() == null) {
                        return "";
                    }
                    result = channel.getMaximum().toString();
                    break;
                case ORDER:
                    if (channel.getByteOrdering() == null) {
                        return "";
                    }
                    result = channel.getByteOrdering().toString();
                    break;
                default:
                    break;
            }
            //            assert result != null : "result!=null"; //$NON-NLS-1$;
            return result;
        }

        public void modify(final Object element, final String property, final Object value) {
            ModuleChannelPrototype channel;
            if (element instanceof Item) {
                channel = (ModuleChannelPrototype) ((Item) element).getData();
            } else {
                channel = (ModuleChannelPrototype) element;
            }

            switch (ChannelPrototypConfigColumn.valueOf(property)) {
                case OFFSET:
                    int offset = 0;
                    if (value instanceof String) {
                        try {
                            offset = Integer.parseInt((String) value);
                        } catch (NumberFormatException nfe) {
                            offset = 0;
                        }
                    } else if (value instanceof Integer) {
                        offset = (Integer) value;
                    }
                    channel.setOffset(offset);
                    break;
                case NAME:
                    channel.setName((String) value);
                    break;
                case TYPE:
                    DataType dt = DataType.BIT;
                    if (value instanceof String) {
                        dt = DataType.valueOf((String) value);
                    } else if (value instanceof Integer) {
                        Integer pos = (Integer) value;
                        if (pos < DataType.values().length) {
                            dt = DataType.values()[pos];
                        }
                    }
                    channel.setType(dt);
                    break;
                case SHIFT:
                    int shift = 0;
                    if (value instanceof String) {
                        shift = Integer.parseInt((String) value);
                    } else if (value instanceof Integer) {
                        shift = (Integer) value;
                    }
                    channel.setShift(shift);
                    break;
                case STRUCT:
                    if (value instanceof String) {
                        String io = (String) value;
                        channel.setStructure(io.equals("yes"));
                    } else if (value instanceof Boolean) {
                        channel.setStructure((Boolean) value);
                    }
                    break;
                case STATUS:
                    int status = 0;
                    if (value instanceof String) {
                        status = Integer.parseInt((String) value);
                    } else if (value instanceof Integer) {
                        status = (Integer) value;
                    }
                    channel.setShift(status);
                    break;
                case MIN:
                    Integer min = null;
                    if (value instanceof String) {
                        min = Integer.parseInt((String) value);
                    }
                    if (value instanceof Integer) {
                        min = (Integer) value;
                    }
                    channel.setMinimum(min);
                    break;
                case MAX:
                    Integer max = null;
                    if (value instanceof String) {
                        max = Integer.parseInt((String) value);
                    }
                    if (value instanceof Integer) {
                        max = (Integer) value;
                    }
                    channel.setMaximum(max);
                    break;
                case ORDER:
                    Integer order = null;
                    if (value instanceof String) {
                        order = Integer.parseInt((String) value);
                    }
                    if (value instanceof Integer) {
                        order = (Integer) value;
                    }
                    channel.setByteOrdering(order);
                    break;
                default:
                    break;
            }

            _tableViewer.refresh(channel);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSaveButtonSaved() {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSavebuttonEnabled(final String event, final boolean enabled) {
        // TODO Auto-generated method stub

    }

}
