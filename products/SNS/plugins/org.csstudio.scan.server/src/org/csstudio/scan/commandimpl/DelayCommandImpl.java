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
package org.csstudio.scan.commandimpl;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.scan.command.DelayCommand;
import org.csstudio.scan.server.ScanCommandImpl;
import org.csstudio.scan.server.ScanContext;

/** {@link ScanCommandImpl} that delays the scan for some time
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DelayCommandImpl extends ScanCommandImpl<DelayCommand>
{
    /** Initialize
     *  @param command Command description
     */
    public DelayCommandImpl(final DelayCommand command)
    {
        super(command);
    }

	/** {@inheritDoc} */
	@Override
    public void execute(final ScanContext command_context) throws Exception
    {
        Logger.getLogger(getClass().getName()).log(Level.FINE, "{0}", command);
		Thread.sleep(Math.round(command.getSeconds() * 1000));
        command_context.workPerformed(1);
    }
}
