package org.csstudio.trends.databrowser.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS
{
    private static final String BUNDLE_NAME = "org.csstudio.trends.databrowser.preferences.messages"; //$NON-NLS-1$

    public static String ArchiveFormatMessage;
    public static String ArchiveInputMessage;
    public static String ArchiveInputTitle;
    public static String EndTime;
    public static String Label_Archives;
    public static String Label_Autoscale;
    public static String Label_DefaultArchives;
    public static String Label_Show_Request_Types;

    public static String LiveBufferMsg;

    public static String LiveBufferSize;
    public static String PageTitle;

    public static String PeriodFieldEditor_MinimumMsg;

    public static String PeriodFieldEditor_ParseError;

    public static String PlotBins;

    public static String PlotBinsError;

    public static String ScanPeriod;
    public static String StartTime;

    public static String UpdatePeriod;
    public static String URLInput_Message;
    public static String URLInput_Title;
    public static String URLS_Label;
        
    static
    {   // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    { /* prevent instantiation */ }
}
