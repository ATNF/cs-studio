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

package org.csstudio.alarm.treeView.model;

import java.util.HashMap;
import java.util.Map;

/**
 * The object class of a tree item. The enumeration constants defined in this
 * class store information about the name of the object class in the directory,
 * which attribute to use to construct the name of a directory entry, and the
 * value of the epicsCssType attribute in the directory.
 *
 * @author Joerg Rathlev
 */
public enum LdapObjectClass {

	/**
	 * The facility object class (efan).
	 */
	FACILITY("epicsFacility", "efan", "facility"),

	/**
	 * The component object class (ecom).
	 */
	COMPONENT("epicsComponent", "ecom", "component"),

	/**
	 * The subcomponent object class (esco).
	 */
	SUBCOMPONENT("epicsSubComponent", "esco", "subComponent"),

	/**
	 * The IOC object class (econ).
	 */
	IOC("epicsController", "econ", "ioc"),

	/**
	 * The record object class (eren).
	 */
	RECORD("epicsRecord", "eren", "record");


	private static final Map<String, LdapObjectClass> CACHE_BY_RDN =
	    new HashMap<String, LdapObjectClass>();

	static {
		// Initialize the _nestedClass attribute
		FACILITY._nestedClass = COMPONENT;
		COMPONENT._nestedClass = SUBCOMPONENT;
		SUBCOMPONENT._nestedClass = SUBCOMPONENT;

        for (final LdapObjectClass oc : LdapObjectClass.values()) {
            CACHE_BY_RDN.put(oc.getRdnAttribute(), oc);
        }
	}

	/**
	 * The name of this object class in the directory.
	 */
	private final String _objectClass;

	/**
	 * The name of the attribute to use for the RDN of entries of this class in
	 * the directory.
	 */
	private final String _rdn;

	/**
	 * The value for the epicsCssType attribute for entries of this class in the
	 * directory.
	 */
	private final String _cssType;

	/**
	 * The object class of a container nested within a container of this object
	 * class. <code>null</code> if this object class is not a container or if
	 * there is no standard nested class for this class.
	 */
	private LdapObjectClass _nestedClass;

	/**
	 * Creates a new object class.
	 *
	 * @param objectClass
	 *            the name of this object class in the directory.
	 * @param rdn
	 *            the name of the attribute to use for the RDN.
	 * @param cssType
	 *            the value for the epicsCssType attribute in the directory.
	 */
	private LdapObjectClass(final String objectClass,
	                        final String rdn,
	                        final String cssType) {
		_objectClass = objectClass;
		_rdn = rdn;
		_cssType = cssType;
	}

	/**
	 * Returns the name of this object class in the directory.
	 * @return the name of this object class in the directory.
	 */
	public String getObjectClassName() {
		return _objectClass;
	}

	/**
	 * Returns the name of the attribute to use for the RDN.
	 * @return the name of the attribute to use for the RDN.
	 */
	public String getRdnAttribute() {
		return _rdn;
	}

	/**
	 * Returns the value to use for the epicsCssType attribute of entries of
	 * this object class.
	 * @return the value to use for the epicsCssType attribute.
	 */
	public String getCssType() {
		return _cssType;
	}

	/**
	 * Returns the object class that a container entry nested within this an
	 * entry of this object class should have. If this object class is not a
	 * container class or if there is no recommended class for nested
	 * containers, this method returns <code>null</code>.
	 *
	 * @return the recommended object class for a container within a container
	 *         of this object class. <code>null</code> if there is no
	 *         recommended class.
	 */
	public LdapObjectClass getNestedContainerClass() {
		return _nestedClass;
	}

    /**
     * Returns the object class of an LDAP rdn attribute (efan, eren, ...).
     *
     * @param name
     *            the name.
     * @return the object class.
     */
    public static LdapObjectClass getObjectClassByRdn(final String rdn) {
        return CACHE_BY_RDN.get(rdn);
    }
}
