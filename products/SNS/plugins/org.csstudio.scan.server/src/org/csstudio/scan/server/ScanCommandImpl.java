/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.server;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.server.internal.Scan;

/** Implementation of a command
 *
 *  <p>Wraps a {@link ScanCommand} and allows execution of the command.
 *
 *  <p>Most commands will perform one unit of work,
 *  for example set a PV.
 *  A loop on the other hand will perform one unit of work per loop
 *  iteration.
 *
 *  <p>The {@link Scan} queries each command for the number of work
 *  units that it will perform, and the command must then update
 *  the {@link ScanContext} with the number of performed work
 *  units.
 *
 *  <p>Work units are just a guess to provide a progress indication.
 *  If a command cannot estimate the number of work units
 *  because it will dynamically perform more or less work,
 *  it should return 1.
 *  Likewise, a delay of 1 second or 1 minute would each be one
 *  work unit, even though their duration differs a lot.
 *
 *  @author Kay Kasemir
 */
abstract public class ScanCommandImpl<C extends ScanCommand>
{
    final protected C command;

    /** Initialize
     *  @param command Command that is implemented
     */
    public ScanCommandImpl(final C command)
    {
        this.command = command;
    }

    /** @return {@link ScanCommand} */
    final public C getCommand()
    {
        return command;
    }

    /** Most commands will perform one unit of work,
     *  for example set a PV.
     *  A loop on the other hand will perform one unit of work per loop
     *  iteration, so derived implementations may override.
     *
     *  @return Number of work units that this command performs */
    public int getWorkUnits()
    {
        return 1;
    }

    /** @return Device (alias) names used by the command */
    public String[] getDeviceNames()
    {
        return new String[0];
    }

	/** Execute the command
	 *
	 *  <p>Should update the performed work units on the {@link ScanContext}
	 *
	 *  @param context {@link ScanContext}
	 *  @throws Exception on error
	 *
	 *  @see ScanContext#workPerformed(int)
	 */
    abstract public void execute(ScanContext context) throws Exception;

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return command.toString();
    }
}
