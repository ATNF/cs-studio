package org.csstudio.config.ioconfig.view.preferences;

import static org.csstudio.config.ioconfig.model.preference.PreferenceConstants.DDB_FACILITIES;
import static org.csstudio.config.ioconfig.model.preference.PreferenceConstants.DDB_LOGBOOK;
import static org.csstudio.config.ioconfig.model.preference.PreferenceConstants.DDB_LOGBOOK_MEANING;
import static org.csstudio.config.ioconfig.model.preference.PreferenceConstants.DDB_PASSWORD;
import static org.csstudio.config.ioconfig.model.preference.PreferenceConstants.DDB_TIMEOUT;
import static org.csstudio.config.ioconfig.model.preference.PreferenceConstants.DDB_USER_NAME;
import static org.csstudio.config.ioconfig.model.preference.PreferenceConstants.DIALECT;
import static org.csstudio.config.ioconfig.model.preference.PreferenceConstants.HIBERNATE_CONNECTION_DRIVER_CLASS;
import static org.csstudio.config.ioconfig.model.preference.PreferenceConstants.HIBERNATE_CONNECTION_URL;
import static org.csstudio.config.ioconfig.model.preference.PreferenceConstants.SHOW_SQL;

import org.csstudio.config.ioconfig.model.IOConifgActivator;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * 
 * @author hrickens
 * @author $Author: hrickens $
 * @version $Revision: 1.3 $
 * @since 24.04.2009
 */
public class HibernatePreferencePage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

    private final class ListEditorExtension extends ListEditor {
        private final String _titel;
        private final String _desc;

        private ListEditorExtension(String name, String labelText, Composite parent, String titel, String desc) {
            super(name, labelText, parent);
            _titel = titel;
            _desc = desc;
        }

        @Override
        protected String[] parseString(String stringList) {
            return stringList.split(",");
        }

        @Override
        protected String getNewInputObject() {
            InputDialog inputDialog = new InputDialog(getFieldEditorParent().getShell(), 
                    _titel, 
                    _desc, 
                    "", 
                    null);
            if (inputDialog.open() == Window.OK) 
            {
                return inputDialog.getValue();
            }
            return null;            }

        @Override
        protected String createList(String[] items) {
            String temp = "";
            for(int i = 0; i < items.length;i++)
                temp = temp + items[i] + ",";
            return temp;            }
    }

    public HibernatePreferencePage() {
        super(GRID);
        ScopedPreferenceStore prefStore = new ScopedPreferenceStore(new InstanceScope(), IOConifgActivator
                .getDefault().getBundle().getSymbolicName());
        setPreferenceStore(prefStore);
        setDescription("Settings for the IO Configurator.");
    }

    /**
     * Creates the field editors. Field editors are abstractions of the common GUI blocks needed to
     * manipulate various types of preferences. Each field editor knows how to save and restore
     * itself.
     */
    public void createFieldEditors() {
        TabFolder tabs = new TabFolder(getFieldEditorParent(), SWT.NONE);
        tabs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        
        makeFcilityTab(tabs);
        makeLogbookTab(tabs);
        makeHibernateDBTab(tabs);
    }
    
    private void makeFcilityTab(TabFolder tabs) {
        TabItem tabDb = new TabItem(tabs, SWT.NONE);
        tabDb.setText("Facilty Settings");
        Composite facComposite = new Composite(tabs, SWT.NONE);
        facComposite.setLayout(new GridLayout(1, true));
        facComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        tabDb.setControl(facComposite);
        
        new Label(facComposite, SWT.NONE);
        new Label(facComposite, SWT.NONE);
        Label descLabel = new Label(facComposite, SWT.NONE);
        descLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        descLabel.setText("Settings for the choosen Facility.");
        new Label(facComposite, SWT.NONE);
        addField(new ListEditorExtension(DDB_FACILITIES, "Facilities:", facComposite,"Add Facility", "Add a new Facility:"));

        
    }

    private void makeHibernateDBTab(TabFolder tabs) {
        // database settings
        TabItem tabDb = new TabItem(tabs, SWT.NONE);
        tabDb.setText("Database Settings");
        Composite dbComposite = new Composite(tabs, SWT.NONE);
        dbComposite.setLayout(new GridLayout(1, true));
        dbComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        tabDb.setControl(dbComposite);
        
        new Label(dbComposite, SWT.NONE);
        Label descLabel = new Label(dbComposite, SWT.NONE);
        descLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        descLabel.setText("Hibernate Settings for the IO Configurator to the Device Database");
        new Label(dbComposite, SWT.NONE);
        addField(new StringFieldEditor(DDB_USER_NAME, "DDB User &Name:", dbComposite));
        StringFieldEditor editor = new StringFieldEditor(DDB_PASSWORD, "DDB &Password:", dbComposite);
        addField(new IntegerFieldEditor(DDB_TIMEOUT, "DDB &Timeout:", dbComposite,3));
        editor.getTextControl(dbComposite).setEchoChar('*');
        addField(editor);
        addField(new StringFieldEditor(HIBERNATE_CONNECTION_DRIVER_CLASS,
                "Hibernate &connection driver:", dbComposite));
        addField(new StringFieldEditor(DIALECT, "&Dialect:", dbComposite));
        editor = new MultiLineStringFieldEditor(HIBERNATE_CONNECTION_URL, "&URL:", dbComposite);
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, false, true);
        layoutData.widthHint = 360;
        editor.getTextControl(dbComposite).setLayoutData(layoutData);
        addField(editor);
        addField(new BooleanFieldEditor(SHOW_SQL, "&Show SQL", dbComposite));
    }


    private void makeLogbookTab(TabFolder tabs) {
        // database settings
        TabItem tabFileAdd = new TabItem(tabs, SWT.NONE);
        tabFileAdd.setText("eLogbook Settings");
        Composite fileAddComposite = new Composite(tabs, SWT.NONE);
        fileAddComposite.setLayout(new GridLayout(1, true));
        fileAddComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        tabFileAdd.setControl(fileAddComposite);
        
        new Label(fileAddComposite, SWT.NONE);
        new Label(fileAddComposite, SWT.NONE);
        Label descLabel = new Label(fileAddComposite, SWT.NONE);
        descLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        descLabel.setText("Settings for the eLogbook to add File into the DB");
        new Label(fileAddComposite, SWT.NONE);
        addField(new ListEditorExtension(DDB_LOGBOOK, "e&Logbook:", fileAddComposite,"Add eLogbook", "Add a new eLogbook:"));
        addField(new ListEditorExtension(DDB_LOGBOOK_MEANING, "&Meaning:", fileAddComposite, "Add Meaning","Add a new eLogbook meaning:"));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }

}
