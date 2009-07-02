package org.csstudio.display.pace.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.csstudio.apputil.macros.Macro;
import org.csstudio.display.pace.Messages;
import org.csstudio.platform.data.IValue;
import org.csstudio.platform.data.ValueUtil;
import org.csstudio.platform.model.IProcessVariable;
import org.csstudio.utility.pv.PV;
import org.csstudio.utility.pv.PVFactory;
import org.csstudio.utility.pv.PVListener;

/** One cell in the model.
 *  <p>
 *  Knows about the Instance and Column where this cell resides,
 *  connects to a PV, holds the most recent value of the PV
 *  as well as an optional user value that overrides the PV's value.
 *  <p>
 *  In addition, a cell might have "meta PVs" that contain the name
 *  of the user, date, and a comment regarding the last change
 *  of the "main" PV.
 *  
 *  @author Kay Kasemir
 *  @author Delphy Nypaver Armstrong
 *  
 *   reviewed by Delphy 01/29/09
 */
public class Cell implements PVListener, IProcessVariable
{
    /** Date format used for updating the last_date_pv */
    final private static DateFormat date_format =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //$NON-NLS-1$

    final private Instance instance;

    final private Column column;
    
    /** Control system PV for 'live' value */
    final private PV pv;
    
    /** Most recent value received from PV */
    private volatile String current_value = null;

    /** Value that the user entered. */
    private volatile String user_value = null;
    
    /** Optional PVs for the name of the person who made the last change,
     *  the date of the change, and a comment.
     *  Either may be <code>null</code>
     */
    private PV last_name_pv, last_date_pv, last_comment_pv;
    
    /** Initialize
     *  @param instance Instance (row) that holds this cell
     *                  and provides the macro substitutions for the cell
     *  @param column   Column that holds this cell
     *                  and provides the macro-ized PV name
     *                  for all cells in the column
     *  @throws Exception on error in macro substitution or PV creation
     */
    public Cell(final Instance instance, final Column column) throws Exception
    {
        this.instance = instance;
        this.column = column;
        String pv_name = Macro.apply(instance.getMacros(), column.getPvWithMacros());

        //  Create the main PV and add listener
        this.pv = PVFactory.createPV(pv_name);
        pv.addListener(this);

        // Create the optional comment pvs.
        // No listener. Their value is fetched on demand.
        pv_name=Macro.apply(instance.getMacros(), column.getNamePvWithMacros());
        if (pv_name.length() <= 0)
            last_name_pv = null;
        else
            last_name_pv = PVFactory.createPV(pv_name);
        
        pv_name = Macro.apply(instance.getMacros(), column.getDatePvWithMacros());
        if (pv_name.length() <= 0)
            last_date_pv = null;
        else
            last_date_pv = PVFactory.createPV(pv_name);
        
        pv_name = Macro.apply(instance.getMacros(), column.getCommentPvWithMacros());
        if (pv_name.length() <= 0)
            last_comment_pv = null;
        else
            last_comment_pv = PVFactory.createPV(pv_name);
    }

    /** @return Instance (row) that contains this cell */
    public Instance getInstance()
    {
        return instance;
    }

    /** @return Column that contains this cell */
    public Column getColumn()
    {
        return column;
    }
    
    /** @return <code>true</code> for read-only cell */
    public boolean isReadOnly()
    {
        return column.isReadonly();
    }
    
    /** Even though a cell may be configured as writable,
     *  the underlying PV might still prohibit write access. 
     *  @return <code>true</code> for PVs that can be written.
     */
    public boolean isPVWriteAllowed()
    {
        return pv.isWriteAllowed();
    }

    /** If the user entered a value, that's it.
     *  Otherwise it's the PV's value, or UNKNOWN
     *  if we have nothing.
     *  @return Value of this cell
     */
    public String getValue()
    {
        if (user_value != null)
            return user_value;
        if (current_value != null)
            return current_value;
        return Messages.UnknownValue;
    }

    /** @return Original value of PV or <code>null</code>
     */
    public String getCurrentValue()
    {
        return current_value;
    }
    
    /** Set a user-specified value.
     *  <p>
     *  If this value matches the PV's value, we revert to the PV's value.
     *  Otherwise this defines a new value that the user entered to
     *  replace the original value of the PV.
     *  @param value Value that the user entered for this cell
     */
    public void setUserValue(final String value)
    {
        if (value.equals(current_value))
            user_value = null;
        else
            user_value = value;
        instance.getModel().fireCellUpdate(this);
    }

    /** @return Value that user entered to replace the original value,
     *          or <code>null</code>
     */
    public String getUserValue()
    {
        return user_value;
    }

    /** Clear a user-specified value, revert to the PV's original value. */
    public void clearUserValue()
    {
        user_value = null;
        instance.getModel().fireCellUpdate(this);
    }
    
    /** Save value entered by user to PV
     *  @param user_name Name of the user to be logged for cells with
     *                   a last user meta PV
     *  @throws Exception on error
     */
    public void saveUserValue(final String user_name) throws Exception
    {
        if (!isEdited())
            return;
        pv.setValue(user_value);
        if (last_name_pv != null)
            last_name_pv.setValue(user_name);
        if (last_date_pv != null)
            last_date_pv.setValue(date_format.format(new Date()));
    }

    /** @return <code>true</code> if user entered a value */
    public boolean isEdited()
    {
        return user_value != null;
    }


    /** @return <code>true</code> if the cell has meta information about
     *  the last change
     *  @see #getLastComment()
     *  @see #getLastDate()
     *  @see #getLastUser()
     */
    public boolean hasMetaInformation()
    {
        return last_name_pv != null || last_date_pv != null ||
              last_comment_pv != null;
    }
    
    /** @return User name for last change to the main PV */
    public String getLastUser()
    { 
        return getOptionalValue(last_name_pv);
    }
    
    /** @return Date of last change to the main PV */
    public String getLastDate()
    { 
        return getOptionalValue(last_date_pv);
    }
    
    /** @return Comment for last change to the main PV */
    public String getLastComment()
    { 
        return getOptionalValue(last_comment_pv);
    }
    
    /** Get value of optional PV
     *  @param optional_pv PV to check, may be <code>null</code>
     *  @return Last value, never <code>null</code>
     */
    private String getOptionalValue(final PV optional_pv)
    {
        if (optional_pv == null)
            return Messages.UnknownValue;
        final IValue value = optional_pv.getValue();
        if (value == null)
            return Messages.UnknownValue;
        return ValueUtil.getString(value);
    }

    /** Start the PV connection */
    public void start() throws Exception
    {
        pv.start();
        if (last_name_pv != null)
            last_name_pv.start();
        if (last_date_pv != null)
            last_date_pv.start();
        if (last_comment_pv != null)
            last_comment_pv.start();
    }

    /** Stop the PV connection */
    public void stop()
    {
        if (last_comment_pv != null)
            last_comment_pv.stop();
        if (last_date_pv != null)
            last_date_pv.stop();
        if (last_name_pv != null)
            last_name_pv.stop();
        pv.stop();
    }

    // PVListener
    public void pvDisconnected(final PV pv)
    {
        current_value = null;
        instance.getModel().fireCellUpdate(this);
    }

    // PVListener
    public void pvValueUpdate(final PV pv)
    {
        current_value = ValueUtil.getString(pv.getValue());
        instance.getModel().fireCellUpdate(this);
    }

    // IProcessVariable
    @SuppressWarnings("unchecked")
    public Object getAdapter(final Class adapter)
    {
        return null;
    }

    /** @return PV name
     *  @see IProcessVariable
     */
    public String getName()
    {
        return pv.getName();
    }

    /** @return Name of comment PV or "" */
    public String getCommentPVName()
    {
        if (last_comment_pv == null)
            return ""; //$NON-NLS-1$
        return last_comment_pv.getName();
    }

    // IProcessVariable
    public String getTypeId()
    {
        return IProcessVariable.TYPE_ID;
    }

    /** @return String representation for debugging */
    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        return "Cell " + pv.getName() + " = " + getValue();
    }
    
}
