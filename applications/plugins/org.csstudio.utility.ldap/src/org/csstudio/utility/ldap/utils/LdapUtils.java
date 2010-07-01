/*
 * Copyright (c) 2008 Stiftung Deutsches Elektronen-Synchrotron,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */
package org.csstudio.utility.ldap.utils;


import static org.csstudio.utility.ldap.utils.LdapFieldsAndAttributes.FIELD_ASSIGNMENT;
import static org.csstudio.utility.ldap.utils.LdapFieldsAndAttributes.FIELD_WILDCARD;
import static org.csstudio.utility.ldap.utils.LdapFieldsAndAttributes.FORBIDDEN_SUBSTRINGS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.naming.InvalidNameException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.log4j.Logger;
import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.platform.model.pvs.ControlSystemEnum;
import org.csstudio.platform.model.pvs.ProcessVariableAdressFactory;
import org.csstudio.platform.util.StringUtil;

/**
 * Constants class for LDAP entries.
 *
 * @author bknerr
 * @version $Revision$
 * @since 11.03.2010
 */
public final class LdapUtils {

    private static final Logger LOG = CentralLogger.getInstance().getLogger(LdapUtils.class.getName());

    /**
     * Constructor.
     */
    private LdapUtils() {
        // Dont instantiate
    }

    /**
     * Returns a filter for 'any' match of the field name (e.g. '<fieldName>=*').
     * @param fieldName the field to match any
     * @return .
     */
    @Nonnull
    public static String any(@Nonnull final String fieldName) {
        return fieldName + FIELD_ASSIGNMENT + FIELD_WILDCARD;
    }

    /**
     * Returns the attributes for a new entry with the given object class and
     * name.
     *
     * @param keysAndValues an array of Strings that represent key value pairs, consecutively (1st=key, 2nd=value, 3rd=key, 4th=value, etc.)
     * @return the attributes for the new entry.
     */
    @Nonnull
    public static Attributes attributesForLdapEntry(@Nonnull final String... keysAndValues) {
        if (keysAndValues.length % 2 > 0) {
            LOG.error("Ldap Attributes: For key value pairs the length of String array has to be multiple of 2!");
            throw new IllegalArgumentException("Length of parameter keysAndValues has to be multiple of 2.");
        }

        final BasicAttributes result = new BasicAttributes();
        for (int i = 0; i < keysAndValues.length; i+=2) {
            result.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return result;
    }

    /**
     * Assembles and LDAP query from field and value pairs.
     *
     * @param fieldsAndValues an array of Strings that represent key value pairs, consecutively (1st=key, 2nd=value, 3rd=key, 4th=value, etc.)
     * @return the String with <field1>=<value1>, <field2>=<value2> assignments.
     */
    @Nonnull
    public static LdapName createLdapQuery(@Nonnull final String... fieldsAndValues) {
        if (fieldsAndValues.length % 2 > 0) {
            LOG.error("Ldap Attributes: For field and value pairs the length of String array has to be multiple of 2!");
            throw new IllegalArgumentException("Length of parameter fieldsAndValues has to be multiple of 2.");
        }

        final List<Rdn> rdns = new ArrayList<Rdn>(fieldsAndValues.length >> 1);
        for (int i = 0; i < fieldsAndValues.length; i+=2) {

            try {
                final Rdn rdn = new Rdn(fieldsAndValues[i] + FIELD_ASSIGNMENT + fieldsAndValues[i + 1]);
                rdns.add(rdn);
            } catch (final InvalidNameException e) {
                // TODO (bknerr) :
                e.printStackTrace();
            }
        }
        Collections.reverse(rdns);
        final LdapName name = new LdapName(rdns);
        return name;
    }

    /**
     * Filters for forbidden substrings {@link LdapUtils}.
     * @param recordName the name to filter
     * @return true, if the forbidden substring is contained, false otherwise (even for empty and null strings)
     */
    public static boolean filterLDAPNames(@Nonnull final String recordName) {
        if (!StringUtil.hasLength(recordName)) {
            return false;
        }
        for (final String s : FORBIDDEN_SUBSTRINGS) {
            if (recordName.contains(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts the given process variable name (<recordName>.<fieldName>) into a
     * record name (<recordName>) which can be
     * looked up in the LDAP directory. If the default control system is EPICS,
     * this will truncate everything after the first dot in the PV name.
     *
     * @param pv
     *            the name of the process variable.
     * @return the name of the record in the LDAP directory.
     */
    @Nonnull
    public static String pvNameToRecordName(@Nonnull final String pv) {
        // TODO (bknerr) : does this epics check really belong here
        if (pv.contains(".") && isEpicsDefaultControlSystem()) {
            return pv.substring(0, pv.indexOf("."));
        }
        return pv;
    }

    /**
     * Returns <code>true</code> if EPICS is the default control system.
     *
     * @return <code>true</code> if EPICS is the default control system,
     *         <code>false</code> otherwise.
     */
    private static boolean isEpicsDefaultControlSystem() {
        final ControlSystemEnum controlSystem =
            ProcessVariableAdressFactory.getInstance().getDefaultControlSystem();
        return controlSystem == ControlSystemEnum.EPICS;
        //              || controlSystem == ControlSystemEnum.DAL_EPICS;
    }
}
