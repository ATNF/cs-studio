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
/*
 * $Id$
 */
package org.csstudio.config.ioconfig.model;

import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * @author hrickens
 * @author $Author$
 * @version $Revision$
 * @since 16.10.2008
 */

@Entity
@Table( name="ddb_NodeImage")
public class NodeImage implements Comparable<NodeImage> {

    /**
     * Key ID.
     */
    private int _id;

    /**
     * The Image Name / Desc.
     */
    private String _name;

    /**
     * The File Name.
     */
    private String _file;
    
    
    /**
     * The Image Data. 
     */
    private byte[] _imageBytes;

    private Set<Node> _nodes;

    /**
     * Default Constructor needed by Hibernate.
     */
    public NodeImage() {
    }

    /** @return the ID. */
    @Id
    @GeneratedValue
    public int getId() {
        return _id;
    }

    /**
     * @param id Set the Image node key ID.
     */
    public void setId(int id) {
        this._id = id;
    }

    /**
     * 
     * @param name set the Name of this Node.
     */
    public void setName(String name) {
        this._name = name;
    }

    /**
     * 
     * @return the Name of this Node.
     */
    public String getName() {
        return _name;
    }
    
    /**
     * 
     * @return the Image File name. 
     */
    public String getFile() {
        return _file;
    }

    public void setFile(String file) {
        _file = file;
    }

    @Lob
    @Basic(fetch=FetchType.EAGER)
    @Column(nullable=false)
    public byte[] getImageBytes() {
        return _imageBytes;
    }

    public void setImageBytes(byte[] imageBytes) {
        _imageBytes = imageBytes;
    }
    
    /**
     * {@inheritDoc}
     */
    public int compareTo(NodeImage arg0) {
        return (getId()-arg0.getId());
    }

    
//    /**
//     * 
//     * @return the Children of this node.
//     */
//    @OneToMany(mappedBy = "id", targetEntity = Node.class, fetch = FetchType.EAGER, cascade = {
//            CascadeType.PERSIST, CascadeType.MERGE })
//    @Cascade( { org.hibernate.annotations.CascadeType.SAVE_UPDATE,
//            org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
//    public Set<? extends Node> getNodes() {
//        return _nodes;
//    }
//
//    /**
//     * Set the Children to this node.
//     * 
//     * @param children
//     *            The Children for this node.
//     */
//    public void setNodes(Set<Node> nodes) {
//        _nodes = nodes;
//    }
    
}
