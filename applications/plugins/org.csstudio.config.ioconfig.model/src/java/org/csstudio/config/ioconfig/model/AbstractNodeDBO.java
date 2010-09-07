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
package org.csstudio.config.ioconfig.model;

import java.awt.Image;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.csstudio.config.ioconfig.model.tools.NodeMap;
import org.csstudio.platform.security.SecurityFacade;
import org.csstudio.platform.security.User;
import org.hibernate.annotations.Cascade;

/**
 *
 * @author gerke
 * @author $Author: hrickens $
 * @version $Revision: 1.4 $
 * @since 21.03.2007
 */

@Entity
@Table(name = "ddb_node")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class AbstractNodeDBO extends NamedDBClass implements Comparable<AbstractNodeDBO>, IDocumentable, INode {

    protected static final int DEFAULT_MAX_STATION_ADDRESS = 255;

    /**
     * The highest accept station address.
     */
    @Transient
    public static final int MAX_STATION_ADDRESS = 128;

    /**
     * The Node Patent.
     */
    private AbstractNodeDBO _parent;

    /**
     * The Version of the Node.
     */
    private int _version;

    /**
     * A set of all manipulated Node from this node.
     */
    private final Set<AbstractNodeDBO> _alsoChanfedNodes = new HashSet<AbstractNodeDBO>();

    private Set<AbstractNodeDBO> _children = new HashSet<AbstractNodeDBO>();

    /**
     * A collection of documents that relate to this node.
     */
    private Set<DocumentDBO> _documents = new HashSet<DocumentDBO>();

    private String _description;

    private NodeImageDBO _icon;

    /**
     * Default Constructor needed by Hibernate.
     */
    public AbstractNodeDBO() {
        // Do nothing
    }

    /**
     *
     * @param parent
     *            set the Parent of this Node
     */
    public void setParent(final AbstractNodeDBO parent) {
        this._parent = parent;
    }

    /**
     *
     * @return The parent of this Node.
     */
    @Override
    @ManyToOne
    public AbstractNodeDBO getParent() {
        return _parent;
    }


    /**
     *
     * @param id
     *            set the Node key ID.
     */
    @Override
    public void setId(final int id) {
        super.setId(id);
        NodeMap.put(id, this);
    }

    /**
     *
     * @return the Children of this node.
     */
    @OneToMany(mappedBy = "parent", targetEntity = AbstractNodeDBO.class, fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE })
    @Cascade( { org.hibernate.annotations.CascadeType.SAVE_UPDATE,
            org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    public Set<? extends AbstractNodeDBO> getChildren() {
//        CentralLogger.getInstance().info(this, "Id\t"+getId()+"\tTime\t"+System.currentTimeMillis()+"\tClass\t"+
//                this.getClass().getSimpleName()+"");
        return _children;
    }

    /**
     * Set the Children to this node.
     *
     * @param children
     *            The Children for this node.
     */
    public void setChildren(final Set<AbstractNodeDBO> children) {
        _children = children;
    }

    /**
     * Add the Child to this node.
     *
     * @param <T> The Type of the Children.
     * @param child the Children to add.
     * @return null or the old Node for the SortIndex Position.
     */
    public <T extends AbstractNodeDBO> AbstractNodeDBO addChild(final T child) {
        int sortIndex = child.getSortIndex();
        AbstractNodeDBO oldNode = getChildrenAsMap().get(sortIndex);

        if(oldNode!=null&&oldNode.equals(child)) {
            return null;
        }
        child.setParent(this);
        child.setSortIndexNonHibernate(sortIndex);
        _children.add(child);

        while (oldNode != null ) {
            final AbstractNodeDBO node = oldNode;
            sortIndex++;
            oldNode = getChildrenAsMap().get(sortIndex);
            node.setSortIndexNonHibernate(sortIndex);
        }
        return oldNode;
    }

    /**
     * Clear all children of this node.
     */
    protected void clearChildren() {
        _children.clear();
    }

    /**
     * Remove a children from this Node.
     *
     * @param child
     *            the children that remove.
     */
    public void removeChild(final AbstractNodeDBO child) {
        _children.remove(child);
    }

    /**
     * Remove a children from this Node.
     */
    public void removeAllChild() {
        clearChildren();
    }

    /**
     * Get the Children of the Node as Map. The Key is the Sort Index.
     * @return the children as map.
     */
    @Transient
    public Map<Short, ? extends AbstractNodeDBO> getChildrenAsMap() {
        final Map<Short, AbstractNodeDBO> nodeMap = new TreeMap<Short, AbstractNodeDBO>();
        for (final AbstractNodeDBO child : getChildren()) {
            nodeMap.put(child.getSortIndex(), child);
        }
        return nodeMap;
    }

    /**
     *
     * @param maxStationAddress
     *            the maximum Station Address.
     * @return the first free Station Address.
     */
    @Transient
    public short getfirstFreeStationAddress(final int maxStationAddress) {
        final Map<Short, ? extends AbstractNodeDBO> children = getChildrenAsMap();
        Short nextKey = 0;
        if (!children.containsKey(nextKey)) {
            return nextKey;
        }
        final Set<Short> descendingKeySet = children.keySet();
        for (final Short key : descendingKeySet) {
            if (key - nextKey > 1) {
                return (short) (nextKey + 1);
            }
            if(key>=0) {
                nextKey = key;
            }
        }
        return (short) (nextKey + 1);
    }

    /**
     *
     * @return have this Node one or more children then return true else false.
     */
    public final boolean hasChildren() {
        return _children.size() > 0;
    }


    /**
     *  Die Tabellen MIME_FILES und MIME_FILES_DDB_NODE liegen auf einer anderen DB.
     *  Daher wird hier mit einem Link gearbeitet der folgenden Rechte ben�tigt.
     *  -  F�r MIME_FILES ist das Grand: select.
     *  -  F�r MIME_FILES_DDB_NODE ist das Grand: select, insert, update, delete.
     *
     * @return Documents for the Node.
     */
    @Override
    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REFRESH })
    @JoinTable(name = "MIME_FILES_DDB_NODES_LINK", joinColumns = @JoinColumn(name = "docs_id", referencedColumnName = "id", unique = true), inverseJoinColumns = @JoinColumn(name = "nodes_id", referencedColumnName = "id"))
//    @JoinTable(name = "MIME_FILES_DDB_NODES_LINK_TEST", joinColumns = @JoinColumn(name = "docs_id", referencedColumnName = "id", unique = true), inverseJoinColumns = @JoinColumn(name = "nodes_id", referencedColumnName = "id"))
    public Set<DocumentDBO> getDocuments() {
        return _documents;
    }

    /**
     *
     * @param documents set the Documents for this node.
     */
    @Override
    public void setDocuments(final Set<DocumentDBO> documents) {
        _documents = documents;
    }

    /**
     *
     * @param document add the Document to this node.
     * @return this Node.
     */
    public AbstractNodeDBO addDocument(final DocumentDBO document) {
        this._documents.add(document);
        return this;
    }

    /**
     *
     * @return the Version of this node.
     */
    public int getVersion() {
        return _version;
    }

    /**
     *
     * @param version
     *            the Version of this node.
     */
    public void setVersion(final int version) {
        this._version = version;
    }

    /**
     *
     * @param i
     *            set the Index to sort the node inside his parent.
     */
    public void setSortIndexNonHibernate(final int i) {
        if (getSortIndex() != i) {
            setSortIndex(i);
            if (getSortIndex() >= 0) {
                localUpdate();
            }
        }
    }

    /**
     *
     * @return the Description of the Node.
     */
    public String getDescription() {
        return _description;
    }

    /**
     *
     * @param description set the Description for this node.
     */
    public void setDescription(final String description) {
        this._description = description;
    }

    /**
     * Swap the SortIndex of two nodes. Is the given SortIndex in use the other node became the old
     * SortIndex of this node.
     *
     * @param toIndex
     *            the new sortIndex for this node.
     */
    public void moveSortIndex(final int toIndex) {
        int direction = 1;
        int index = this.getSortIndex();
        if (toIndex == index) {
            return;
        }
        if(getParent()==null) {
            setSortIndexNonHibernate(toIndex);
            return;
        }
        if (index == -1) {
            // Put a new Node in.
            if (index > toIndex) {
                direction = -1;
            }
            AbstractNodeDBO node = this;
            index = toIndex;
            do {
                final AbstractNodeDBO nextNode = getParent().getChildrenAsMap().get(index);

                node.setSortIndexNonHibernate(index);
                node = nextNode;
                index = index + direction;
            } while (node != null);
        } else {
            // Move a exist Node
            int start = index;
            final AbstractNodeDBO moveNode = getParent().getChildrenAsMap().get(index);
            if (index > toIndex) {
                direction = -1;
            }
            for (; start != toIndex; start+=direction) {
                final AbstractNodeDBO nextNode = getParent().getChildrenAsMap().get((short)(start+direction));
                if(nextNode!=null) {
                    nextNode.setSortIndexNonHibernate(start);
                }
            }
            moveNode.setSortIndexNonHibernate(toIndex);
        }
    }

    /**
     * @param oldNode
     *            a node that a manipulated.
     */
    @Transient
    public void addAlsoChangedNodes(final AbstractNodeDBO oldNode) {
        _alsoChanfedNodes.add(oldNode);
    }

    /**
     *
     */
    @Transient
    public void clearAlsoChangedNodes() {
        _alsoChanfedNodes.clear();
    }

    /**
     * {@link Comparable}.
     *
     * @param other
     *            the node to compare whit this node.
     * @return if this node equals whit the give node return 0.
     */
    @Override
    public int compareTo(final AbstractNodeDBO other) {
        if (this.getClass() != other.getClass()) {
            return -1;
        }
        int comper = getId() - other.getId();
        if (comper == 0 && getId() == 0) {
            comper = this.getSortIndex() - other.getSortIndex();
        }
        return comper;
    }

    @Deprecated
    public void setImage(final Image image) {
        if (image != null) {
            // setImageBytes(image.getImageData().data);
        }
    }

//    @Transient
//    public final HashSet<Node> getChangeNodeSet() {
//        return _changeNodeSet;
//    }

    /**
     * Copy this node to the given Parent Node.
     *
     * @param parentNode
     *            the target parent node.
     * @return the copy of this node.
     */
    public AbstractNodeDBO copyThisTo(final AbstractNodeDBO parentNode) {
        String createdBy = "Unknown";
        try {
            final User user = SecurityFacade.getInstance().getCurrentUser();
            if (user != null) {
                createdBy = user.getUsername();
            }
        } catch (final NullPointerException e) {
            createdBy = "Unknown";
        }
        final AbstractNodeDBO copy = copyParameter(parentNode);
        copy.setCreatedBy(createdBy);
        copy.setUpdatedBy(createdBy);
        copy.setCreatedOn(new Date());
        copy.setUpdatedOn(new Date());
        //TODO: so umbauen das "Copy of" als prefix parameter �bergeben wird.
        copy.setName("Copy of " + getName());
//        copy.setName(getName());
        copy.setVersion(getVersion());
        if(parentNode!=null) {
            parentNode.localUpdate();
        }
        return copy;
    }


    // ---- Test Start

//    @ManyToOne
    @Transient
    public NodeImageDBO getIcon() {
        return _icon;
    }
//
//    /**
//     * Set the Children to this node.
//     *
//     * @param children
//     *            The Children for this node.
//     */
    public void setIcon(final NodeImageDBO icon) {
        _icon = icon;
    }

    // ---- Test End



    /**
     * Copy this node and set Special Parameter.
     *
     * @param parent the parent Node for the Copy.
     *
     * @return a Copy of this node.
     */
    protected abstract AbstractNodeDBO copyParameter(NamedDBClass parent);

    /**
     * Save his self.
     * @throws PersistenceException
     */
    public void localSave() throws PersistenceException {
        save();
    }

    /**
     * make the data update for his self.
     */
    protected void localUpdate() {
    }

    /**
     * Update date it self and his siblings.
     */
    public void update() {
        if (isRootNode()) {
            localUpdate();
            updateChildrenOf(this);
        } else {
            updateChildrenOf(getParent());
        }
    }

    /**
     * Update the node an his children.
     * @param parent the node to update.
     */
    protected void updateChildrenOf(final AbstractNodeDBO parent) {
        for (final AbstractNodeDBO n : parent.getChildrenAsMap().values()) {
            n.localUpdate();
            updateChildrenOf(n);
        }
    }

    /**
     *
     * @return is only true if this Node a Root Node.
     */
    @Transient
    public boolean isRootNode() {
        return getParent() == null;
    }

    /**
     * Assemble the Epics Address String of the children Channels.
     */
    @Transient
    public void assembleEpicsAddressString() throws PersistenceException {
        for (final AbstractNodeDBO node : getChildren()) {
            if (node != null) {
                node.assembleEpicsAddressString();
                if (node.isDirty()) {
                    node.save();
                }
            }
        }
    }

    /**
     * (@inheritDoc)
     */
    @Override
    public boolean equals(final Object obj) {
        if (super.equals(obj)) {
            return true;
        }
        if (obj instanceof AbstractNodeDBO ) {

            final AbstractNodeDBO node = (AbstractNodeDBO) obj;
            if(getId()==node.getId()) {
                if(getId()>0) {
                    return true;
                }
                return false;
            }
        }
        return false;
   }

    /**
     * @return Return only true when the node need to work a GSD-File!
     */
    public GSDFileTypes needGSDFile() {
        return GSDFileTypes.NONE;
    }

}
