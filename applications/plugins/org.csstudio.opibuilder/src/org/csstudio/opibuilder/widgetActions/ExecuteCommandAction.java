package org.csstudio.opibuilder.widgetActions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.csstudio.opibuilder.properties.IntegerProperty;
import org.csstudio.opibuilder.properties.StringProperty;
import org.csstudio.opibuilder.properties.WidgetPropertyCategory;
import org.csstudio.opibuilder.util.ConsoleService;
import org.csstudio.opibuilder.widgetActions.WidgetActionFactory.ActionType;

/**The action executing a system command.
 * @author Xihui Chen
 *
 */
public class ExecuteCommandAction extends AbstractWidgetAction {

	private final static String PROP_COMMAND = "command";
	private final static String PROP_DIRECTORY = "command_directory";
	private final static String PROP_WAIT_TIME = "wait_time";
	
	@Override
	protected void configureProperties() {
		addProperty(new StringProperty(
				PROP_COMMAND, "Command", WidgetPropertyCategory.Basic, ""));
		addProperty(new StringProperty(
				PROP_DIRECTORY, "Command Directory[path]", WidgetPropertyCategory.Basic, "$(user.home)"));
		addProperty(new IntegerProperty(
				PROP_WAIT_TIME, "Wait Time(s)", WidgetPropertyCategory.Basic, 10, 1, Integer.MAX_VALUE));
		
	}

	@Override
	public ActionType getActionType() {
		return ActionType.EXECUTE_CMD;
	}

	@Override
	public void run() {
		ConsoleService.getInstance().writeInfo("Execute Command: " + getCommand());
		new CommandExecutor(getCommand(), getDirectory(), getWaitTime());
		
	}
	
	public String getCommand(){
		return (String)getPropertyValue(PROP_COMMAND);
	}
	
	public String getDirectory(){
		String directory = (String)getPropertyValue(PROP_DIRECTORY);
		try {
			return replaceProperties(directory);
		} catch (Exception e) {
			ConsoleService.getInstance().writeError(e.getMessage());
		}
		return  directory;
	}

	public int getWaitTime(){
		return (Integer)getPropertyValue(PROP_WAIT_TIME);
	}
	

    /** @param value Value that might contain "$(prop)"
     *  @return Value where "$(prop)" is replaced by Java system property "prop"
     *  @throws Exception on error
     */
    private static String replaceProperties(final String value) throws Exception
    {
        final Matcher matcher = Pattern.compile("\\$\\((.*)\\)").matcher(value);
        if (matcher.matches())
        {
            final String prop_name = matcher.group(1);
            final String prop = System.getProperty(prop_name);
            if (prop == null)
                throw new Exception("Property '" + prop_name + "' is not defined");
            return prop;
        }
        // Return as is
        return value;
    }
    
    @Override
    public String getDescription() {
    	return super.getDescription() + " " + getCommand(); //$NON-NLS-1$
    }
}
