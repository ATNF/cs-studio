/*
 * Copyright (c) 2006 Stiftung Deutsches Elektronen-Synchroton,
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

/**
 *
 */
package org.epics.css.dal.impl;

import java.util.HashMap;

import org.epics.css.dal.DynamicValueProperty;
import org.epics.css.dal.context.AbstractApplicationContext;
import org.epics.css.dal.context.PropertyFamily;
import org.epics.css.dal.group.GroupDataAccess;
import org.epics.css.dal.group.PropertyCollectionMap;
import org.epics.css.dal.group.PropertyGroupConstrain;
import org.epics.css.dal.proxy.AbstractPlug;
import org.epics.css.dal.proxy.DirectoryProxy;
import org.epics.css.dal.proxy.PropertyProxy;
import org.epics.css.dal.proxy.Proxy;
import org.epics.css.dal.spi.PropertyFactory;


/**
 * The default implementation of the {@link PropertyFamily}. This class is
 * based on the {@link PropertyCollectionMap}, which means that all properties
 * are stored within a {@link HashMap}. Note that HashMap is not synchronized
 * and therefore if multiple threads add or remove properties to this family
 * the calls should be synchronized externally.
 * 
 * @author ikriznar
 *
 */
public class PropertyFamilyImpl
	extends PropertyCollectionMap<DynamicValueProperty<?>> implements PropertyFamily
{
	PropertyFactory pf;
	static final Class<? extends DynamicValueProperty> c = DynamicValueProperty.class;
	
	/**
	 * Constructs a new PropertyFamilyImpl that belongs to the given {@link PropertyFactory}.
	 */
	public PropertyFamilyImpl(PropertyFactory pf)
	{
		super((Class<DynamicValueProperty<?>>) c);
		
		this.pf = pf;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.context.PropertyFamily#destroyAll()
	 */
	public void destroyAll()
	{
		DynamicValueProperty<?>[] props = new DynamicValueProperty[size()];
		props = toArray((DynamicValueProperty<?>[])props);
		clear();

		AbstractPlug plug = (AbstractPlug)pf.getPlug();

		for (DynamicValueProperty<?> p : props) {
			PropertyProxy<?> proxy = ((DataAccessImpl<?>)p).getProxy();
			plug.releaseProxy(proxy);
			proxy.destroy();
			if (!(proxy instanceof DirectoryProxy)) {
				DirectoryProxy dp = ((SimplePropertyImpl<?>)p).getDirectoryProxy();
				plug.releaseProxy(dp);
				dp.destroy();
			}
			
		}
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.context.PropertyFamily#destroy(org.epics.css.dal.DynamicValueProperty)
	 */
	public void destroy(DynamicValueProperty<?> prop)
	{
		if (!contains(prop)) return;
		this.remove((DynamicValueProperty<?>) prop); 
		AbstractPlug plug = (AbstractPlug)pf.getPlug();
		Proxy[] proxy = ((DataAccessImpl<?>)prop).releaseProxy(true);
		if (proxy!=null && proxy[0]!=null) {
			plug.releaseProxy(proxy[0]);
		}
		if (proxy!=null && proxy[1]!=null && proxy[0]!=proxy[1]) {
			plug.releaseProxy(proxy[1]);
		}
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.context.PropertyFamily#getApplicationContext()
	 */
	public AbstractApplicationContext getApplicationContext()
	{
		return pf.getApplicationContext();
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.group.GroupDataAccessProvider#getGroupDataAccess(java.lang.Class, java.lang.Class)
	 */
	public <T, P extends DynamicValueProperty<?>> GroupDataAccess<T, P> getGroupDataAccess(
	    Class<T> dataType, Class<P> propertyType)
	{
		// TODO later
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.group.GroupDataAccessProvider#getGroupDataAccess(java.lang.Class, java.lang.Class, org.epics.css.dal.group.PropertyGroupConstrain)
	 */
	public <T, P extends DynamicValueProperty<T>> GroupDataAccess<T, P> getGroupDataAccess(
	    Class<T> dataType, Class<P> propertyType,
	    PropertyGroupConstrain constrain)
	{
		// TODO later
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.group.PropertyCollectionMap#add(T)
	 */
	@Override
	public void add(DynamicValueProperty<?> property)
	{
		super.add(property);
	}

	/**
	 * Returns true if context contains property.
	 * 
	 * @param property the property to check
	 * 
	 * @see org.epics.css.dal.context.PropertyContext#containsProperty(java.lang.Object)
	 */
	public boolean containsProperty(Object property) {
		return contains(property);
	}

	/**
	 * Returns true if context contains <b>ANY</b> property with provided name.
	 * <p>
	 * <b>Note!</b> there could be multiple property instances with same name in family.
	 * </p>
	 * 
	 * @param name the property name to check for
	 * 
	 * @see org.epics.css.dal.context.PropertyContext#containsProperty(java.lang.String)
	 */
	public boolean containsProperty(String name) {
		return contains(name);
	}

	/**
	 * Returns first property it finds with provided name. 
	 * There is no specific order in which multiple properties are held in the family.
	 * 
	 * <p>
	 * <b>Note!</b> there could be multiple property instances with same name in family.
	 * </p>
	 * 
	 * <p>
	 * This method calls <code>getFirst(String)</code>.
	 * </p>
	 * 
	 * @param name the property name to check for
	 * 
	 * @see org.epics.css.dal.context.PropertyContext#getProperty(java.lang.String)
	 * @see PropertyCollectionMap#getFirst(String);
	 */
	public DynamicValueProperty getProperty(String name) {
		return getFirst(name);
	}
	
	/**
	 * Returns plug type string, which is distinguishing for plug which
	 * creates  proxies for particular communication layer.<p>For
	 * example plug that connects to EPICS device my return string "EPICS".</p>
	 *
	 * @return plug destingushing type name
	 */
	public String getPlugType() {
		return pf.getPlugType();
	}


}

/* __oOo__ */
