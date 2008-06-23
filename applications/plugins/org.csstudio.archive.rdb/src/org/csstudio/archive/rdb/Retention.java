package org.csstudio.archive.rdb;

import org.csstudio.utility.rdb.StringID;

/** Retention information
 *  <p>
 *  Uses the 'Name' of a string/ID pair for the retention description.
 *  @author Kay Kasemir
 */
public class Retention extends StringID
{
	public Retention(final int id, final String description)
	{
		super(id, description);
	}
	
    @Override
    @SuppressWarnings("nls")
    final public String toString()
    {
        return String.format("Retention '%s' (%d)", getName(), getId());
    }
}
