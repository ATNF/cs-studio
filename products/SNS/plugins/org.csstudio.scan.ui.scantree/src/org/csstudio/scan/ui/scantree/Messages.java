/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.scantree;

import org.eclipse.osgi.util.NLS;

/** Externalized strings
 *  @author Kay Kasemir
 */
public class Messages extends NLS
{
    private static final String BUNDLE_NAME = "org.csstudio.scan.ui.scantree.messages"; //$NON-NLS-1$
    public static String AddCommand;
    public static String AddCommandMessage;
    public static String AddCommandTitle;
    public static String BoolEdit_False;
    public static String BoolEdit_True;
    public static String CommandListTT;
    public static String DeviceListFetch;
    public static String DeviceListFetchError;
    public static String DroppedPVNameBesideCommand;
    public static String DroppedPVNameNotSupportedByCommand;
    public static String Error;
    public static String FileOpenErrorFmt;
    public static String FileSaveErrorFmt;
    public static String NotSaved;
    public static String OpenCommandList;
    public static String OpenProperties;
    public static String OpenScanTree;
    public static String OpenScanTreeErrorFmt;
    public static String OpenScanTreePerspective;
    public static String ScanSubmitErrorFmt;
    public static String ServerDisconnected;
    public static String SubmitScan;
    public static String XMLCommandErrorFmt;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
        // Prevent instantiation
    }
}
