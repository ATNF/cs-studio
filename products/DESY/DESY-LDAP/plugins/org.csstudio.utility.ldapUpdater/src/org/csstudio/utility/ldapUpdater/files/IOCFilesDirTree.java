package org.csstudio.utility.ldapUpdater.files;
/*
 * Copyright (c) Ian F. Darwin, http://www.darwinsys.com/, 1996-2002.
 * All rights reserved. Software written by Ian F. Darwin and others.
 * $Id$
 *
 * Modified for settable recursion depth by Klaus Valett - DESY Hamburg
 */

import static org.csstudio.utility.ldapUpdater.preferences.LdapUpdaterPreferenceKey.IOC_DBL_DUMP_PATH;
import static org.csstudio.utility.ldapUpdater.preferences.LdapUpdaterPreferences.getValueFromPreferences;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;
import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.utility.ldap.model.IOC;
import org.csstudio.utility.ldap.model.Record;
import org.csstudio.utility.ldapUpdater.LdapAccess;


/**
 * DirTree - directory lister, like UNIX ls or DOS/VMS dir
 *
 * TODO (valett) : extract generic dir tree scanner
 *
 * @author Ian Darwin, http://www.darwinsys.com/
 * @version $Id$
 * modified by kv (Klaus Valett) - DESY - for recursion depth
 *
 */
public final class IOCFilesDirTree {

    private static Logger LOG = CentralLogger.getInstance().getLogger(IOCFilesDirTree.class.getName());

    private static final String RECORDS_FILE_SUFFIX = ".records";

    /**
     * Don't instantiate.
     */
    private IOCFilesDirTree() {
        // Empty
    }

    /**
     * doFile - process one regular file.
     *
     * @param f
     *            the current file to be analysed
     * @param iocMap
     * */
    private static void doFile(@Nonnull final File f, @Nonnull final Map<String, IOC> iocMap) {
        final String fileName = f.getName();

        if (fileName.endsWith(RECORDS_FILE_SUFFIX)) {
            final GregorianCalendar dateTime = findLastModifiedDateForFile(fileName);

            final String iocName = fileName.replace(RECORDS_FILE_SUFFIX, "");
            iocMap.put(iocName, new IOC(iocName, dateTime));
            LOG.debug("File found for IOC: " + iocName);
        }
    }

    /**
     * Finds all IOC files under in the directory traversing up to a given tree level depth.
     * @param iocDblDumpPath the directory under which to look
     * @param recursiveDepth .
     * @return the map of identified files
     */
    @Nonnull
    public static Map<String, IOC> findIOCFiles(@Nonnull final String iocDblDumpPath, final int recursiveDepth) {
        final int currentDepth = 0;

        final Map<String, IOC> iocMap = new HashMap<String, IOC>();

        final IOCFilesDirTree dt = new IOCFilesDirTree();

        dt.doDir(currentDepth, recursiveDepth, iocDblDumpPath, iocMap);

        return iocMap;
    }

    @Nonnull
    private static GregorianCalendar findLastModifiedDateForFile(@Nonnull final String iocFileName) {
        final GregorianCalendar dateTime = new GregorianCalendar(TimeZone.getTimeZone("ETC"));
        final String prefFileName = getValueFromPreferences(IOC_DBL_DUMP_PATH);
        final File filePath = new File(prefFileName);
        dateTime.setTimeInMillis(new File(filePath,iocFileName).lastModified());
        return dateTime;
    }

    /**
     * doDir - handle one filesystem object by name
     *
     * @param iocMap
     *            the list of iocs to be filled by this recursive method
     * */
    private void doDir(final int currentDepth,
                       final int recDepth,
                       @Nonnull final String s,
                       @Nonnull final Map<String, IOC> iocMap) {

        int depth = currentDepth;

        final File f = new File(s);
        if (!f.exists()) {
            LOG.warn(f.getName() + " does not exist.");
            return;
        }
        if (f.isFile()) {
            doFile(f, iocMap);
        } else if (f.isDirectory()) {
            if (depth >= recDepth) {
                return;
            }
            depth++;

            final String[] filePaths = f.list();
            for (final String filePath : filePaths) {
                doDir(depth, recDepth, s + File.separator + filePath, iocMap);
            }
        } else {
            LOG.warn(f.getAbsolutePath() + " is neither file nor directory.");
        }
    }

    /**
     * TODO (bknerr) : should be encapsulated in a file access class - does not belong here.
     * @param pathToFile the file with records
     */
    @Nonnull
    public static Set<Record> getRecordsFromFile(@Nonnull final String pathToFile) {
        final Set<Record> records = new HashSet<Record>();
        try {
            final BufferedReader br = new BufferedReader(new FileReader(pathToFile + RECORDS_FILE_SUFFIX));
            String strLine;
            while ((strLine = br.readLine()) != null)   {
                records.add(new Record(strLine));
            }
            br.close();
            return records;
        } catch (final FileNotFoundException e) {
            LdapAccess.LOG.error("Could not find file: " + pathToFile + "\n" + e.getMessage());
        } catch (final IOException e) {
            LdapAccess.LOG.error("Error while reading from file: " + e.getMessage());
        }
        return Collections.emptySet();
    }
}
