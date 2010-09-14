/*
 * Copyright (c) 2010 Stiftung Deutsches Elektronen-Synchrotron,
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
package org.csstudio.alarm.service.declaration;

import static org.csstudio.alarm.service.declaration.AlarmTreeLdapConstants.ECOM_FIELD_NAME;
import static org.csstudio.alarm.service.declaration.AlarmTreeLdapConstants.ECON_FIELD_NAME;
import static org.csstudio.alarm.service.declaration.AlarmTreeLdapConstants.EFAN_FIELD_NAME;
import static org.csstudio.alarm.service.declaration.AlarmTreeLdapConstants.EREN_FIELD_NAME;
import static org.csstudio.alarm.service.declaration.AlarmTreeLdapConstants.ESCO_FIELD_NAME;
import static org.csstudio.utility.ldap.treeconfiguration.LdapFieldsAndAttributes.ORGANIZATION_UNIT_FIELD_NAME;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.csstudio.utility.treemodel.ITreeNodeConfiguration;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


/**
 * The tree configuration of the alarm tree items. The enumeration constants defined in this
 * class store information about the name of the object class in the directory,
 * which attribute to use to construct the name of a directory entry, and the
 * value of the attributes in the directory.
 *
 * @author Bastian Knerr
 */
public enum LdapEpicsAlarmcfgConfiguration implements ITreeNodeConfiguration<LdapEpicsAlarmcfgConfiguration> {

    /**
     * The root (invisible in the alarm tree view).
     */
    UNIT("organizationUnit",
         ORGANIZATION_UNIT_FIELD_NAME,
         ImmutableSet.<String>builder().build()),

    /**
     * The facility object class (efan).
     */
    FACILITY("epicsFacility",
             EFAN_FIELD_NAME,
             AlarmTreeNodePropertyId.getLdapAttributes()),

    /**
     * The component object class (ecom).
     */
    COMPONENT("epicsComponent",
              ECOM_FIELD_NAME,
              AlarmTreeNodePropertyId.getLdapAttributes()),

    @Deprecated
    IOC("epicsIOC",
        ECON_FIELD_NAME,
        AlarmTreeNodePropertyId.getLdapAttributes()),
    @Deprecated
    SUBCOMPONENT("epicsSubComponent",
                 ESCO_FIELD_NAME,
                 AlarmTreeNodePropertyId.getLdapAttributes()),

    /**
     * The record object class (eren).
     */
    RECORD("epicsRecord",
           EREN_FIELD_NAME,
           AlarmTreeNodePropertyId.getLdapAttributes());


    private static final Map<String, LdapEpicsAlarmcfgConfiguration> CACHE_BY_NAME =
        Maps.newHashMapWithExpectedSize(values().length);

    static {
        RECORD._nestedClasses = EnumSet.noneOf(LdapEpicsAlarmcfgConfiguration.class);

        // FIXME (bknerr) : this structure is obsolete
        IOC._nestedClasses = EnumSet.of(RECORD);
        SUBCOMPONENT._nestedClasses = EnumSet.of(IOC, RECORD, SUBCOMPONENT, COMPONENT);
        COMPONENT._nestedClasses = EnumSet.of(IOC, SUBCOMPONENT);


        COMPONENT._nestedClasses.add(RECORD);
        COMPONENT._nestedClasses.add(COMPONENT);

        FACILITY._nestedClasses = EnumSet.noneOf(LdapEpicsAlarmcfgConfiguration.class);
        FACILITY._nestedClasses.addAll(COMPONENT._nestedClasses);

        UNIT._nestedClasses = EnumSet.of(FACILITY);

        for (final LdapEpicsAlarmcfgConfiguration oc : values()) {
            CACHE_BY_NAME.put(oc.getNodeTypeName(), oc);
        }
    }

    /**
     * The name of this object class in the directory.
     */
    private final String _description;

    /**
     * The name of the attribute to use for the RDN of entries of this class in
     * the directory.
     */
    private final String _nodeName;


    private final ImmutableSet<String> _attributes;


    /**
     * The tree items that are nested into a container of this class.
     */
    private Set<LdapEpicsAlarmcfgConfiguration> _nestedClasses;



    /**
     * Creates a new object class.
     *
     * @param description
     *            the name of this object class in the directory.
     * @param nodeName
     *            the name of the attribute to use for the RDN.
     * @param cssType
     *            the value for the epicsCssType attribute in the directory.
     *
     * CHECKSTYLE:Jsr305Annotations:OFF
     */
    private LdapEpicsAlarmcfgConfiguration(final String description,
                                           final String nodeName,
                                           final ImmutableSet<String> attributes) {
        _description = description;
        _nodeName = nodeName;
        _attributes = attributes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public String getDescription() {
        return _description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public String getNodeTypeName() {
        return _nodeName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public ImmutableSet<LdapEpicsAlarmcfgConfiguration> getNestedContainerTypes() {
        return Sets.immutableEnumSet(_nestedClasses);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @CheckForNull
    public LdapEpicsAlarmcfgConfiguration getNodeTypeByNodeTypeName(@Nonnull final String name) {
        return getNodeTypeByNodeNameStatic(name);
    }

    @CheckForNull
    private static LdapEpicsAlarmcfgConfiguration getNodeTypeByNodeNameStatic(@Nonnull final String name) {
        return CACHE_BY_NAME.get(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRootTypeValue() {
        return AlarmTreeLdapConstants.EPICS_ALARM_CFG_FIELD_VALUE;
    }

    /**
     * Getter.
     * @return the immutable set of permitted attributes.
     */
    @Override
    @Nonnull
    public ImmutableSet<String> getAttributes() {
        return _attributes;
    }
}
