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
 package org.csstudio.sns.ldapauthorization;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.csstudio.apptuil.securestorage.SecureStorage;
import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.platform.security.IAuthorizationProvider;
import org.csstudio.platform.security.Right;
import org.csstudio.platform.security.RightSet;
import org.csstudio.platform.security.User;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.osgi.util.NLS;

/**
 * Reads a user's roles and groups from an LDAP directory, and the rights
 * associated with actions from a configuration file.
 * 
 * @author Joerg Rathlev
 * @author Xihui Chen
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LdapAuthorizationReader implements IAuthorizationProvider
{
    /** LDAP search context for users' rights */ 
    private static final String USER_RIGHTS_CONTEXT = "ou=CSSGroupRole";

    /** LDAP Attribute that has the user name */
    private static final String USER_ATTRIB = "memberUid";
    
    /** Tag in DN that's used as the 'Group' part of a Right */
    final static String RIGHT_GROUP_TAG = "ou=";

    /** Tag in DN that's used as the 'Role' part of a Right */
    final static String RIGHT_ROLE_TAG = "cn=";

    /** Map of the rights associated with actions */
	private Map<String, RightSet> actionsrights;

	/** Given a user name, determine the user's rights,
	 *  i.e. which roles the user has in which groups.
	 *  @param user authenticated user name
	 *  @see org.csstudio.platform.internal.ldapauthorization.IAuthorizationProvider#getRights(org.csstudio.platform.security.User)
	 */
    public RightSet getRights(final User user)
	{
		String username = user.getUsername();
		// If the user was authenticated via Kerberos, the username may be a
		// fully qualified name (name@EXAMPLE.COM). We only want the first
		// part of the name.
		if (username.contains("@"))
			username = username.substring(0, username.indexOf('@'));
		
		final RightSet rights = new RightSet("LDAP Rights");
		try
		{
		    final DirContext ctx = new InitialDirContext(createEnvironment());
			
			final SearchControls ctrls = new SearchControls();
			ctrls.setReturningAttributes(new String[] { USER_ATTRIB });
			ctrls.setReturningObjFlag(true);
			ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			
			final String filter = NLS.bind("({0}={1})", USER_ATTRIB, username);  
			
			final NamingEnumeration<SearchResult> results =
				ctx.search(USER_RIGHTS_CONTEXT, filter, ctrls);
			while (results.hasMore())
			{
				final SearchResult r = results.next();
				rights.addRight(parseSearchResult(r));
			}
		}
		catch (NamingException e)
		{
			CentralLogger.getInstance().getLogger(this).error(
					"Error reading authorization for user: " + username, e);
		}
		return rights;
	}


	/** Determine the rights that are required to perform a given action
	 *  @param actionId Action ID
	 *  @see org.csstudio.platform.security.IAuthorizationProvider#getRights(java.lang.String)
	 */
	public RightSet getRights(final String actionId)
	{
		synchronized (this)
		{
			if (actionsrights == null)
			    loadActionRightsFromLdap();
		}
		return actionsrights.get(actionId);
	}
	
	/**
	 * Loads the actions' rights from LDAP.
	 */
	private void loadActionRightsFromLdap()
	{
		actionsrights = new HashMap<String, RightSet>();
		
		try
		{
		    final DirContext ctx = new InitialDirContext(createEnvironment());
			
			final SearchControls ctrls = new SearchControls();
			ctrls.setReturningObjFlag(false);
			ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			
			final String filter = "(objectClass=cssAuthorizeID)";
			final NamingEnumeration<SearchResult> results =
				ctx.search("ou=CSSAuthorizeID", filter, ctrls);
			while (results.hasMore())
			{
				final SearchResult r = results.next();
				final Attributes attributes = r.getAttributes();
				final Attribute auid = attributes.get("auid");
				final String authId = (String) auid.get();
				final Attribute groupRoleAttr = attributes.get("cssGroupRole");
				
				final RightSet rights = new RightSet(authId);
				parseGroupRoleAttr(groupRoleAttr, rights);
				actionsrights.put(authId, rights);
		
			}
			CentralLogger.getInstance().debug(this, "Authorization " +
					"information successfully loaded from LDAP directory.");
			
		}
		catch (NamingException e)
		{
			CentralLogger.getInstance().error(this,
					"Error loading authorization information from LDAP directory.", e);
		}
	}
	
	/**
	 * Parses cssGroupRole attribute into rights
	 * @param groupRoleAttr
	 * @param rights
	 * @throws NamingException
	 */
	@SuppressWarnings("unchecked")
	private void parseGroupRoleAttr(Attribute groupRoleAttr, RightSet rights)
		throws NamingException 
	{	
		for(NamingEnumeration ae = groupRoleAttr.getAll(); ae.hasMore(); ) {
			String grp = (String) ae.next();
			//each cssGroupRole attribute has a format of (group, role);
			String group = grp.substring(1, grp.indexOf(",")).trim();
			String role = grp.substring(grp.indexOf(",")+1, grp.indexOf(")")).trim();
			rights.addRight(new Right(role, group));			
		}		
	}

	/**
	 * Parses a search result into a right.
	 * @param r the LDAP search result.
	 */
	private Right parseSearchResult(final SearchResult r)
	{
	    final String dn = r.getName();
	    final String group = dn.substring(dn.indexOf(RIGHT_GROUP_TAG)+3);
		final String role = dn.substring(dn.indexOf(RIGHT_ROLE_TAG)+3, dn.indexOf("," + RIGHT_GROUP_TAG));
		return new Right(role, group);
	}

	/** @return Environment for the LDAP connection,
	 *          partially obtained from preferences
	 */
	private Hashtable<String, String> createEnvironment()
	{
	    final IPreferencesService prefs = Platform.getPreferencesService();
	    final String url = prefs.getString(Activator.PLUGIN_ID, PreferenceConstants.LDAP_URL, null, null);
		String user = SecureStorage.retrieveSecureStorage(
				Activator.PLUGIN_ID, PreferenceConstants.LDAP_USER);
		if (user == null)
			user = prefs.getString(Activator.PLUGIN_ID, PreferenceConstants.LDAP_USER, null, null);
		String password = SecureStorage.retrieveSecureStorage(
				Activator.PLUGIN_ID, PreferenceConstants.LDAP_PASSWORD);
		if (password == null)
			password = prefs.getString(Activator.PLUGIN_ID, PreferenceConstants.LDAP_PASSWORD, null, null);
		
		final Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, url);
		if (user.length() <= 0  ||  password.length() <= 0)
		    env.put(Context.SECURITY_AUTHENTICATION, "none");
		else
	    {
		    env.put(Context.SECURITY_PRINCIPAL, user);
	        env.put(Context.SECURITY_CREDENTIALS, password);
	    }
		
		return env;
	}
}
