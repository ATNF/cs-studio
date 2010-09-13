/*
 * Copyright (c) 2007 Stiftung Deutsches Elektronen-Synchrotron,
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
package org.csstudio.utility.ldap.service.impl;


import static org.csstudio.utility.ldap.treeconfiguration.LdapFieldsAndAttributes.ATTR_FIELD_OBJECT_CLASS;
import static org.csstudio.utility.ldap.utils.LdapNameUtils.removeRdns;
import static org.csstudio.utility.ldap.utils.LdapUtils.any;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.log4j.Logger;
import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.utility.ldap.connection.LDAPConnector;
import org.csstudio.utility.ldap.model.builder.LdapContentModelBuilder;
import org.csstudio.utility.ldap.reader.LDAPReader;
import org.csstudio.utility.ldap.service.ILdapContentModelBuilder;
import org.csstudio.utility.ldap.service.ILdapReadCompletedCallback;
import org.csstudio.utility.ldap.service.ILdapSearchParams;
import org.csstudio.utility.ldap.service.ILdapSearchResult;
import org.csstudio.utility.ldap.service.ILdapService;
import org.csstudio.utility.ldap.utils.LdapNameUtils.Direction;
import org.csstudio.utility.ldap.utils.LdapSearchParams;
import org.csstudio.utility.ldap.utils.LdapSearchResult;
import org.csstudio.utility.treemodel.ContentModel;
import org.csstudio.utility.treemodel.CreateContentModelException;
import org.csstudio.utility.treemodel.INodeComponent;
import org.csstudio.utility.treemodel.ISubtreeNodeComponent;
import org.csstudio.utility.treemodel.ITreeNodeConfiguration;
import org.eclipse.core.runtime.jobs.Job;


/**
 * Service implementation for the LDAP access.
 *
 * @author bknerr
 * @author $Author$
 * @version $Revision$
 * @since 09.04.2010
 */
public final class LdapServiceImpl implements ILdapService {

    static final Logger LOG = CentralLogger.getInstance().getLogger(LdapServiceImpl.class);

    /**
     * DirContext Holder to prevent accidental direct access to DirContext field.
     *
     * @author bknerr
     * @author $Author: bknerr $
     * @version $Revision: 1.7 $
     * @since 08.09.2010
     */
    private enum DirContextHolder {

        INSTANCE;

        private DirContext _context;

        @CheckForNull DirContext get() {
            if (_context == null) {
                LDAPConnector ldapConnector = null;
                try {
                    ldapConnector = new LDAPConnector();
                } catch (final NamingException e) {
                    LOG.fatal("Engine.run - connection to LDAP server failed", e);
                    return null;
                }

                _context = ldapConnector.getDirContext();

                if (_context != null) {
                    LOG.info("Engine.run - successfully connected to LDAP server");
                } else {
                    LOG.fatal("Engine.run - context creation for LDAP server failed");
                }
            }
            return _context;
        }


        boolean reInit(@CheckForNull final Map<String, String> ldapPrefs) {
            if (ldapPrefs == null) {
                return get() != null; // just reinitializes the context with the default prefs
            }

            final Hashtable<Object, String> env = new Hashtable<Object, String>(ldapPrefs);

            env.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory");

            try {
                _context = new InitialLdapContext(env, null);
            } catch (final NamingException e) {
                LOG.error("Re-initialization of LDAP context failed.\n" +
                          "Preferences:\n" + ldapPrefs);
                return false;
            }

            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean reInitializeLdapConnection(@CheckForNull final Map<String, String> ldapPrefs) {
        return DirContextHolder.INSTANCE.reInit(ldapPrefs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @CheckForNull
    public Job createLdapReaderJob(@Nonnull final ILdapSearchParams params,
                                   @Nullable final ILdapSearchResult result,
                                   @Nullable final ILdapReadCompletedCallback callBack) {

            final LDAPReader ldapr =
                new LDAPReader.Builder(params.getSearchRoot(), params.getFilter()).
                                       setScope(params.getScope()).
                                       setSearchResult(result).
                                       setJobCompletedCallBack(callBack).
                                       build();

            return ldapr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @CheckForNull
    public LdapSearchResult retrieveSearchResultSynchronously(@Nonnull final LdapName searchRoot,
                                                              @Nonnull final String filter,
                                                              final int searchScope) {

        final DirContext context = DirContextHolder.INSTANCE.get();
        if(context != null){
            final SearchControls ctrl = new SearchControls();
            ctrl.setSearchScope(searchScope);
            NamingEnumeration<SearchResult> answer = null;
            try {
                answer = context.search(searchRoot, filter, ctrl);

                final Set<SearchResult> answerSet = new HashSet<SearchResult>();
                while(answer.hasMore()){
                    answerSet.add(answer.next());
                }

                final LdapSearchResult result = new LdapSearchResult();
                result.setResult(new LdapSearchParams(searchRoot, filter, searchScope), answerSet);

                return result;

            } catch (final NameNotFoundException nnfe){
                LOG.info("Wrong LDAP name?" + nnfe.getExplanation());
            } catch (final NamingException e) {
                LOG.info("Wrong LDAP query. " + e.getExplanation());
            } finally {
                try {
                    if (answer != null) {
                        answer.close();
                    }
                } catch (final NamingException e) {
                    LOG.warn("Error closing search results: ", e);
                }
            }
        }
        return null;

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean createComponent(@Nonnull final LdapName newComponentName,
                                   @Nullable final Attributes attributes) {
        final DirContext context = DirContextHolder.INSTANCE.get();
        if(context == null) {
            LOG.error("LDAP context is null.");
            return false;
        }
        try {
            context.bind(newComponentName, null, attributes);
            LOG.info( "New LDAP Component: " + newComponentName.toString());
        } catch (final NamingException e) {
            LOG.warn( "Naming Exception while trying to bind: " + newComponentName.toString());
            LOG.warn(e.getExplanation());
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}}
     */
    @Override
    public boolean removeLeafComponent(@Nonnull final LdapName component) {
        final DirContext context = DirContextHolder.INSTANCE.get();
        if(context == null) {
            LOG.error("LDAP context is null.");
            return false;
        }
        try {
            LOG.info("Unbind entry from LDAP: " + component);
            context.unbind(component);
        } catch (final NamingException e) {
            LOG.warn("Naming Exception while trying to unbind: " + component);
            LOG.warn(e.getExplanation());
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}}
     * @throws InvalidNameException
     * @throws CreateContentModelException
     */
    @Override
    public <T extends Enum<T> & ITreeNodeConfiguration<T>>
        boolean removeComponent(@Nonnull final T configurationRoot,
                                @Nonnull final LdapName component) throws InvalidNameException, CreateContentModelException {

        LOG.debug("Remove entry incl. subtree:\n" + component.toString());

        final Rdn rootRdn = new Rdn(configurationRoot.getNodeTypeName(), configurationRoot.getRootTypeValue());

        // get complete subtree of 'oldLdapName' and create model
        final LdapSearchResult result =
            retrieveSearchResultSynchronously(component,
                                              any(ATTR_FIELD_OBJECT_CLASS),
                                              SearchControls.SUBTREE_SCOPE);

        if (result == null || result.getAnswerSet().isEmpty()) {
            LOG.debug("LDAP query returned empty or null result for component " + component.toString() +
                      "\nand filter " + any(ATTR_FIELD_OBJECT_CLASS));
            return false;
        }

        final LdapContentModelBuilder<T> builder =
            new LdapContentModelBuilder<T>(configurationRoot, result);
        builder.build();
        final ContentModel<T> model = builder.getModel();

        // FIXME (bknerr) : the reference to a specific name 'efan' does not belong here - the model has to work with the full name
        final LdapName nameInModel = removeRdns(component,
                                                "efan",
                                                Direction.FORWARD);

        // perform the removal of the subtree
        copyAndRemoveTreeComponent(null,
                                   model.getChildByLdapName(nameInModel.toString()),
                                   rootRdn,
                                   false);
        // perform the removal of the component itself
        removeLeafComponent(component);

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modifyAttributes(@Nonnull final LdapName name,
                                 @Nonnull final ModificationItem[] mods) throws NamingException {
        final DirContext context = DirContextHolder.INSTANCE.get();
        if(context == null) {
            LOG.error("LDAP context is null.");
            return;
        }
        LOG.debug("Modify entry for: " + name);
        context.modifyAttributes(name, mods);
    }

    /**
     * {@inheritDoc}
     * @throws NamingException
     */
    @Override
    public void rename(@Nonnull final LdapName oldLdapName,
                       @Nonnull final LdapName newLdapName) throws NamingException {
        final DirContext context = DirContextHolder.INSTANCE.get();
        if(context == null) {
            LOG.error("LDAP context is null.");
            return;
        }
        LOG.info("Rename entry from:\n" + oldLdapName.toString() + "\nto\n" + newLdapName.toString());
        context.rename(oldLdapName, newLdapName);
    }

    /**
                                                       "efan",
     * Copies (if param <code>copy</code> set to true) the given subtree and removes it from the old location.
     *
     * @param <T>
     * @param ldapParentName the name of the new parent component in LDAP (may be null if copy is false)
     * @param treeParent
     * @param rootRdn
     * @param copy if <code>true</code> a new component is created in LDAP.
     *             Otherwise only the removal is performed
     * @throws InvalidNameException
     */
    private <T extends Enum<T> & ITreeNodeConfiguration<T>>
        void copyAndRemoveTreeComponent(@CheckForNull final LdapName ldapParentName,
                                        @Nonnull final ISubtreeNodeComponent<T> treeParent,
                                        @CheckForNull final Rdn rootRdn,
                                        final boolean copy) throws InvalidNameException {

        // process contents of model and build subtree below 'newLdapName'
        for (final INodeComponent<T> child : treeParent.getDirectChildren()) {

            final LdapName newChildLdapName = ldapParentName != null ? new LdapName(ldapParentName.getRdns()) : null;
            if (copy) {
                newChildLdapName.add(new Rdn(((ITreeNodeConfiguration<T>) child.getType()).getNodeTypeName(), child.getName()));

                // generate LDAP component for child
                createComponent(newChildLdapName, child.getAttributes());
            }

            if (child.hasChildren()) {
                copyAndRemoveTreeComponent(newChildLdapName, (ISubtreeNodeComponent<T>) child, rootRdn, copy);
            }
            // leaf node, remove it from ldap

            final LdapName ldapName = new LdapName(child.getLdapName().getRdns());
            removeLeafComponent((LdapName) ldapName.add(0, rootRdn));
        }
    }





    /**
     * {@inheritDoc}
     * @throws NamingException
     */
    @Override
    @CheckForNull
    public Attributes getAttributes(@Nonnull final LdapName ldapName) throws NamingException {
        final DirContext context = DirContextHolder.INSTANCE.get();
        if(context == null) {
            LOG.error("LDAP context is null.");
            return null;
        }
        return context.getAttributes(ldapName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @CheckForNull
    public Object lookup(@Nonnull final LdapName name) throws NamingException {
        final DirContext context = DirContextHolder.INSTANCE.get();
        if(context == null) {
            LOG.error("LDAP context is null.");
            return null;
        }
        return context.lookup(name);
    }

    /**
     * {@inheritDoc}
     * @throws NamingException
     */
    @Override
    @CheckForNull
    public NameParser getLdapNameParser() throws NamingException {
        final DirContext context = DirContextHolder.INSTANCE.get();
        if(context == null) {
            LOG.error("LDAP context is null.");
            return null;
        }
        return context.getNameParser(new CompositeName());
    }

    @Override
    public <T extends Enum<T> & ITreeNodeConfiguration<T>> ILdapContentModelBuilder getLdapContentModelBuilder(@Nonnull final T objectClassRoot,
                                                                                                                  @Nonnull final ILdapSearchResult searchResult) {
        return new LdapContentModelBuilder<T>(objectClassRoot, searchResult);
    }
    @Override
    public <T extends Enum<T> & ITreeNodeConfiguration<T>> ILdapContentModelBuilder getLdapContentModelBuilder(@Nonnull final ContentModel<T> model) {
        return new LdapContentModelBuilder<T>(model);
    }
}
