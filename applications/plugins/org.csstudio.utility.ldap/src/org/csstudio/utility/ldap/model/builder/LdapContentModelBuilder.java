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
 *
 * $Id$
 */
package org.csstudio.utility.ldap.model.builder;

import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.log4j.Logger;
import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.utility.ldap.LdapFieldsAndAttributes;
import org.csstudio.utility.ldap.LdapNameUtils;
import org.csstudio.utility.ldap.LdapNameUtils.Direction;
import org.csstudio.utility.ldap.reader.LdapSearchResult;
import org.csstudio.utility.treemodel.ContentModel;
import org.csstudio.utility.treemodel.CreateContentModelException;
import org.csstudio.utility.treemodel.ISubtreeNodeComponent;
import org.csstudio.utility.treemodel.ITreeNodeConfiguration;
import org.csstudio.utility.treemodel.TreeNodeComponent;
import org.csstudio.utility.treemodel.builder.AbstractContentModelBuilder;

/**
 * Builds a content model from LDAP.
 *
 * @author bknerr
 * @author $Author$
 * @version $Revision$
 * @since 21.05.2010
 * @param <T> the object class type for which a tree shall be created
 */
public final class LdapContentModelBuilder<T extends Enum<T> & ITreeNodeConfiguration<T>> extends AbstractContentModelBuilder<T> {

    private static final Logger LOG = CentralLogger.getInstance().getLogger(LdapContentModelBuilder.class);

    private LdapSearchResult _searchResult;
    private final T _objectClassRoot;

    /**
     * Constructor.
     * @param searchResult the search result to build the model from
     * @param objectClassRoot the model type
     */
    public LdapContentModelBuilder(@Nonnull final T objectClassRoot,
                                   @Nonnull final LdapSearchResult searchResult) {
        _searchResult = searchResult;
        _objectClassRoot = objectClassRoot;
    }

    /**
     * Constructor for builder that enriches an already existing model.
     * @param model the already filled model
     */
    public LdapContentModelBuilder(@Nonnull final ContentModel<T> model) {
        _objectClassRoot = model.getRoot().getType();
        setModel(model);
    }

    public void setSearchResult(@Nonnull final LdapSearchResult result) {
        _searchResult = result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @CheckForNull
    protected ContentModel<T> createContentModel() throws CreateContentModelException {

        // Generate new model only if there isn't any model set
        final ContentModel<T> model = getModel();

        try {
            return addSearchResult(model == null ? new ContentModel<T>(_objectClassRoot)
                                                 : model,
                                   _searchResult);
        } catch (final InvalidNameException e) {
            throw new CreateContentModelException("Error creating content model from LDAP.", e);
        }

    }

    /**
     * Adds a given search result to the current LDAP content model.
     *
     * @param searchResult the search result .
     * @return the enriched model
     */
    @Nonnull
    private ContentModel<T> addSearchResult(@Nonnull final ContentModel<T> model,
                                            @Nullable final LdapSearchResult searchResult) {

        if (searchResult != null) {
            final ISubtreeNodeComponent<T> root = model.getRoot();

            final Set<SearchResult> answerSet = searchResult.getAnswerSet();
            try {
                for (final SearchResult row : answerSet) {

                    final Attributes attributes = row.getAttributes();
                    createLdapComponent(model,
                                        attributes == null ? new BasicAttributes() : attributes,
                                        LdapNameUtils.parseSearchResult(row),
                                        root);
                }
            } catch (final IndexOutOfBoundsException iooe) {
                LOG.error("Tried to remove a name component with index out of bounds.", iooe);
            } catch (final InvalidNameException ie) {
                LOG.error("Search result row could not be parsed by NameParser or removal of name component violates the syntax rules.", ie);
            } catch (final NamingException e) {
                LOG.error("NameParser could not be obtained for LDAP Engine and CompositeName.", e);
            }
        }

        return model;
    }

    private void createLdapComponent(@Nonnull final ContentModel<T> model,
                                     @Nonnull final Attributes attributes,
                                     @Nonnull final LdapName fullName,
                                     @Nonnull final ISubtreeNodeComponent<T> root) throws InvalidNameException {
        ISubtreeNodeComponent<T> parent = root;

        final LdapName partialName = LdapNameUtils.removeRdns(fullName,
                                                              LdapFieldsAndAttributes.EFAN_FIELD_NAME,
                                                              Direction.FORWARD);

        final LdapName currentPartialName = new LdapName("");


        for (int i = 0; i < partialName.size(); i++) {

            final Rdn rdn = partialName.getRdn(i);
            currentPartialName.add(rdn);

            // Check whether this component exists already
            final ISubtreeNodeComponent<T> childByLdapName = model.getChildByLdapName(currentPartialName.toString());
            if (childByLdapName != null) {
                if (i < partialName.size() - 1) { // another name component follows => has children
                    parent = childByLdapName;
                }
                continue; // YES
            }
            // NO

            final T oc = _objectClassRoot.getNodeTypeByNodeTypeName(rdn.getType());

            final ISubtreeNodeComponent<T> newChild =
                new TreeNodeComponent<T>((String) rdn.getValue(),
                                        oc,
                                        parent,
                                        attributes,
                                        currentPartialName);
            model.addChild(parent, newChild);

            parent = newChild;
        }
    }

}
