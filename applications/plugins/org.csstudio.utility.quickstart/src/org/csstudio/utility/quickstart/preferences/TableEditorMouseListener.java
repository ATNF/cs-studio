package org.csstudio.utility.quickstart.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class TableEditorMouseListener extends MouseAdapter {



	private TableEditor _editor;
	private Table _table;


	public TableEditorMouseListener(TableEditor editor, Table table) {
		_editor = editor;
		_table = table;
	}
		
	/**
	 * Dispose the editor before selecting a new row
	 * otherwise the cell and not the row will be selected
	 * and it is not possible to move the row up and down.
	 */
	@Override
	public void mouseDown(MouseEvent event) {
        // Dispose any existing editor
        Control old = _editor.getEditor();
        if (old != null) old.dispose();
	}
	
	/**
	 * Make the selected cell editable with a
	 * double click. (Copied from an internet example)
	 */
	public void mouseDoubleClick(MouseEvent event) {
        // Dispose any existing editor
        Control old = _editor.getEditor();
        if (old != null) old.dispose();

        // Determine where the mouse was clicked
        Point pt = new Point(event.x, event.y);

        // Determine which row was selected
        final TableItem item = _table.getItem(pt);
        if (item != null) {
          // Determine which column was selected
          int column = -1;
          for (int i = 0, n = _table.getColumnCount(); i < n; i++) {
            Rectangle rect = item.getBounds(i);
            if (rect.contains(pt)) {
              // This is the selected column
              column = i;
              break;
            }
          }
          
          // Create the Text object for our editor
          final Text text = new Text(_table, SWT.NONE);
          text.setForeground(item.getForeground());

          // Transfer any text from the cell to the Text control,
          // set the color to match this row, select the text,
          // and set focus to the control
          text.setText(item.getText(column));
          text.setForeground(item.getForeground());
          text.selectAll();
          text.setFocus();

          // Recalculate the minimum width for the editor
          _editor.minimumWidth = text.getBounds().width;

          // Set the control into the editor
          _editor.setEditor(text, item, column);

          // Add a handler to transfer the text back to the cell
          // any time it's modified
          final int col = column;
          text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent event) {
              // Set the text of the editor's control back into the cell
              item.setText(col, text.getText());
            }
          });
        }
      }
    }

	

