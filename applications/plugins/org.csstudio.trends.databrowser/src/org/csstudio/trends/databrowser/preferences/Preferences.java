package org.csstudio.trends.databrowser.preferences;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.platform.model.CentralItemFactory;
import org.csstudio.platform.model.IArchiveDataSource;
import org.csstudio.trends.databrowser.Plugin;
import org.csstudio.trends.databrowser.model.ArchiveRescale;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;

/** Data Browser preferences.
 *  <p>
 *  The list of archive server URLs is stored as one string with
 *  separators for the individual URLs.
 *  <p>
 *  The default archive data sources are such a list,
 *  but each item in turn contains name/key/url.
 *  
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
    /** Separator for the list of items.
     *  <p>
     *  Obviously, the separator must not be part of a valid URL.
     *  Might have to change this one...
     */
    private static final String LIST_SEPARATOR = "*";
    
    /** Separator for the pieces of an archive data source. */
    private static final char ARCHIVE_SEPARATOR = '|';

    /** Identifier for start time preference. */
    public static final String START_TIME_SPEC = "start_time";
    
    /** Identifier for end time preference. */
    public static final String END_TIME_SPEC = "end_time";
    
    /** Minimum for scan_period. */
    public static final double MIN_SCAN_PERIOD = 0.1;
    
    /** Identifier for scan period [seconds] preference. */
    public static final String SCAN_PERIOD = "scan_period";
    
    /** Minimum for update_period. */
    public static final double MIN_UPDATE_PERIOD = 0.5;

    /** Identifier for update period [seconds] preference. */
    public static final String UPDATE_PERIOD = "update_period";
    
    /** Minimum for live buffer sample count. */
    public static final int MIN_LIVE_BUFFER_SIZE = 10;
    
    /** Identifier for live buffer sample count preference. */
    public static final String LIVE_BUFFER_SIZE = "live_buffer_size";
    
    /** Identifier for the auto-scale preference. */
    public static final String AUTOSCALE = "autoscale";
    
    /** Identifier for the auto-scale preference. */
    public static final String ARCHIVE_RESCALE = "archive_rescale";    

    /** Identifier for preference to use default archives. */
    public static final String USE_DEFAULT_ARCHIVES = "use_default_archives";    
    
    /** Identifier for the plot bin preference. */
    public static final String PLOT_BINS = "plot_bins";
    
    /** Identifier for the Archive Server URLs preference. */
    public static final String URLS = "urls";

    /** Identifier for the Archives preference. */
    public static final String ARCHIVES = "archives";
    
    /** Preference Identifier: Show 'SampleFileImportAction' in Config View? */
    public static final String SHOW_SAMPLE_FILE_IMPORT_ACTION = "show_sample_file_import_action";
    
    /** Preference Identifier: Show "Start Time" in archives view? */
    private static final String SHOW_ARCHIVE_START_TIMES = "show_archive_start_times";

    /** @return Default start time specification. */
    static public String getStartSpecification()
    {
        final IPreferencesService prefs = Platform.getPreferencesService();
        return prefs.getString(Plugin.ID, START_TIME_SPEC, "-1 hour", null);
    }

    /** @return Default end time specification. */
    static public String getEndSpecification()
    {
        final IPreferencesService prefs = Platform.getPreferencesService();
        return prefs.getString(Plugin.ID, END_TIME_SPEC, "now", null);
    }
    
    /** @return Default scan period [seconds]. */
    static public double getScanPeriod()
    {
        final IPreferencesService prefs = Platform.getPreferencesService();
        return prefs.getDouble(Plugin.ID, SCAN_PERIOD, 1.0, null);
    }

    /** @return Default update period [seconds]. */
    static public double getUpdatePeriod()
    {
        final IPreferencesService prefs = Platform.getPreferencesService();
        return prefs.getDouble(Plugin.ID, UPDATE_PERIOD, 5.0, null);
    }

    /** @return Default update period [seconds]. */
    static public int getLiveBufferSize()
    {
        final IPreferencesService prefs = Platform.getPreferencesService();
        return prefs.getInt(Plugin.ID, LIVE_BUFFER_SIZE, 5000, null);
    }
    
    /** @return Default auto-scale setting */
    static public boolean getAutoScale()
    {
        final IPreferencesService prefs = Platform.getPreferencesService();
        return prefs.getBoolean(Plugin.ID, AUTOSCALE, false, null);
    }

    /** @return Archive rescale setting */
    static public ArchiveRescale getArchiveRescale()
    {
        final IPreferencesService prefs = Platform.getPreferencesService();
        try
        {
            return ArchiveRescale.valueOf(
                    prefs.getString(Plugin.ID, ARCHIVE_RESCALE,
                                    ArchiveRescale.NONE.name(), null));
        }
        catch (Throwable ex)
        {
            CentralLogger.getInstance().getLogger(Preferences.class)
                .error(ex.getMessage());
        }
        return ArchiveRescale.NONE;
    }
    
    /** @return Number of plot bins */
    static public int getPlotBins()
    {
        final IPreferencesService prefs = Platform.getPreferencesService();
        return prefs.getInt(Plugin.ID, PLOT_BINS, 500, null);
    }
    
    /** @return <code>true</code> when "Start Time" should show in archives view */
    static public boolean getShowArchiveStartTimes()
    {
        final IPreferencesService prefs = Platform.getPreferencesService();
        return prefs.getBoolean(Plugin.ID, SHOW_ARCHIVE_START_TIMES, false, null);
    }
    
    /** @return Use 'default' archives instead of configuration file info? */
    static public boolean getUseDefaultArchives()
    {
        final IPreferencesService prefs = Platform.getPreferencesService();
        return prefs.getBoolean(Plugin.ID, USE_DEFAULT_ARCHIVES, true, null);
    }
    
    /** @return Default setting for 'show sample file import action'. */
    static public boolean getShowSampleFileImportAction()
    {
        final IPreferencesService prefs = Platform.getPreferencesService();
        return prefs.getBoolean(Plugin.ID, SHOW_SAMPLE_FILE_IMPORT_ACTION, false, null);
    }

    /** @return Default archive server URLs. */
    static public String[] getArchiveServerURLs()
    {
        final IPreferencesService prefs = Platform.getPreferencesService();
        String concat = prefs.getString(Plugin.ID, URLS, "", null);
        return splitListItems(concat);
    }

    /** @return Default archive data sources. */
    static public IArchiveDataSource[] getArchiveDataSources()
    {
        final IPreferencesService prefs = Platform.getPreferencesService();
        String concat = prefs.getString(Plugin.ID, ARCHIVES, "", null);
        String encoded_sources[] = splitListItems(concat);
        IArchiveDataSource sources[] = new IArchiveDataSource[encoded_sources.length];
        for (int i = 0; i < sources.length; ++i)
            sources[i] = parseArchiveDataSource(encoded_sources[i]);
        return sources;
    }
    
    /** Concatenate list of items into one String.
     *  @see #splitListItems(String)
     */
    static String concatListItems(String[] urls)
    {
        StringBuffer concat = new StringBuffer();
        for (int i = 0; i < urls.length; i++)
        {
            concat.append(urls[i]);
            concat.append(LIST_SEPARATOR);
        }
        return concat.toString();
    }

    /** Split List items from string.
     *  @see #concatListItems(String[])
     */
    static String[] splitListItems(String concat_URLs)
    {
        StringTokenizer st = new StringTokenizer(concat_URLs, LIST_SEPARATOR);
        ArrayList<String> urls = new ArrayList<String>();
        while (st.hasMoreElements())
            urls.add((String)st.nextElement());
        return urls.toArray(new String[urls.size()]);
    }
    
    /** Concatenate the pieces of an archive data source into one String.
     *  @see #parseArchiveDataSource(String)
     */
    static String encodeArchiveDataSource(String url, String name, int key)
    {
        return name + ARCHIVE_SEPARATOR + key + ARCHIVE_SEPARATOR + url;
    }

    /** Parse an archive data source from a concatenated string.
     *  @see #encodeArchiveDataSource(String, String, int)
     */
    static IArchiveDataSource parseArchiveDataSource(String text)
    {
        text = text.trim();
        final int i1 = text.indexOf(ARCHIVE_SEPARATOR);
        if (i1 < 0)
            return null;
        final String name = text.substring(0, i1);
        final int i2 = text.indexOf(ARCHIVE_SEPARATOR, i1+1);
        if (i2 < 1) // need some minimum length
            return null;
        int key;
        try
        {
            key = Integer.parseInt(text.substring(i1+1, i2));
        }
        catch (Exception e)
        {
            return null;
        }
        final String url = text.substring(i2 + 1);
        if (url.length() < 1)
            return null;
        return CentralItemFactory.createArchiveDataSource(url, key, name);
    }
}   
