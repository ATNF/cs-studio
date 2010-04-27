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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.eclipse.core.runtime.IAdaptable;

/**
 * A tree node that is the root node of a subtree.
 * 
 * @author Joerg Rathlev
 */
public class SubtreeNode extends AbstractAlarmTreeNode implements IAdaptable, IAlarmTreeNode {
	
	/**
	 * This node's children.
	 */
	private List<IAlarmTreeNode> _children;
	
	/**
	 * This node's parent node.
	 */
	private SubtreeNode _parent;
	
	/**
	 * The name of this node.
	 */
	private String _name;
	
	/**
	 * The object class of this node in the directory.
	 */
	private LdapObjectClass _objectClass;
	
	/**
	 * The highest severity of the child nodes.
	 */
	private Severity _highestChildSeverity;
	
	/**
	 * The highest unacknowledged severity of the child nodes.
	 */
	private Severity _highestUnacknowledgedChildSeverity;

	/**
	 * Creates a new node with the specified parent. The node will register
	 * itself as a child at the parent node.
	 * @param parent the parent node.
	 * @param name the name of this node.
	 * @param objectClass the object class of this node.
	 */
	public SubtreeNode(final SubtreeNode parent, final String name, final LdapObjectClass objectClass) {
		if (name == null) {
			throw new NullPointerException("name must not be null");
		}
		
		this._parent = parent;
		this._name = name;
		this._objectClass = objectClass;
		_children = new ArrayList<IAlarmTreeNode>();
		_highestChildSeverity = Severity.NO_ALARM;
		_highestUnacknowledgedChildSeverity = Severity.NO_ALARM;
		if (parent != null) {
			parent.addChild(this);
		}
	}
	
	/**
	 * Creates a new node that does not have a parent node.
	 * @param name the name of this node.
	 */
	public SubtreeNode(final String name){
		this(null, name, null);
	}
	
	/**
	 * Removes the given child from this node. If the given node is not a direct
	 * child of this subtree, does nothing. The child node must not have any
	 * children itself. If the child node has children, this method does
	 * nothing.
	 * 
	 * @param child
	 *            the child node to remove.
	 */
	public final void remove(final IAlarmTreeNode child) {
		if (!_children.contains(child)) {
			return;
		}
		if (child instanceof SubtreeNode
				&& ((SubtreeNode) child).hasChildren()) {
			return;
		}
		_children.remove(child);
		refreshSeverities();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public final LdapObjectClass getObjectClass() {
		return _objectClass;
	}
	
	/**
	 * Returns the object class recommended for a <code>SubtreeNode</code>
	 * that is a child of this node.
	 * 
	 * @return the object class recommended for subtree children of this node.
	 *         <code>null</code> if there is no recommended class.
	 */
	public final LdapObjectClass getRecommendedChildSubtreeClass() {
		return _objectClass.getNestedContainerClass();
	}

	/**
	 * Returns the name of object in the directory which this node represents.
	 * The name depends on the object class and name of this node and the object
	 * classes and names of this node's parent nodes.
	 * 
	 * @return the name of this node in the directory.
	 * @deprecated this method does not work correctly if the name of this node
	 *             or one of its parent nodes contains special characters that
	 *             need escaping. Use {@link #getLdapName()} instead.
	 */
	@Deprecated
	public final String getDirectoryName() {
		StringBuilder result = new StringBuilder();
		result.append(getObjectClass().getRdnAttribute());
		result.append("=");
		result.append(_name);
		if (_parent._objectClass != null) {
			// If the parent has an object class (i.e., it is not the root node
			// of the tree), its directory name must be appended to the result
			result.append(",");
			result.append(_parent.getDirectoryName());
		}
		return result.toString();
	}
	
	/**
	 * Returns the name of this node in the LDAP directory.
	 * 
	 * @return the name of this node in the directory.
	 */
	public final LdapName getLdapName() {
		try {
			if (_objectClass == null) {
				return new LdapName("");
			}
			
			LdapName result = new LdapName(Collections.singletonList(
					new Rdn(_objectClass.getRdnAttribute(), _name)));
			if (_parent != null) {
				result.addAll(0, _parent.getLdapName());
			}
			return result;
		} catch (InvalidNameException e) {
			return null;
		}
	}
	
	/**
	 * Adds the specified child node to the list of this node's children. Note:
	 * it is not checked whether the parent node of the child is correctly set
	 * to this node. This method is intended to be called only by constructors
	 * of nodes.
	 * 
	 * @param child the child node to add. 
	 */
	final void addChild(final IAlarmTreeNode child) {
		_children.add(child);
		childSeverityChanged(child);
	}
	
	/**
	 * Returns the highest severity of the alarms in the subtree below this
	 * node.
	 * 
	 * @return the alarm severity for this node.
	 */
	public final Severity getAlarmSeverity() {
		return _highestChildSeverity;
	}
	
	/**
	 * Returns the highest severity of the unacknowledged alarms in the subtree
	 * below this node.
	 * 
	 * @return the unacknowledged alarm severity for this node.
	 */
	public final Severity getUnacknowledgedAlarmSeverity() {
		return _highestUnacknowledgedChildSeverity;
	}
	
	/**
	 * Returns whether this node has any children.
	 * @return {@code true} if this node has children, {@code false} otherwise.
	 */
	public final boolean hasChildren() {
		return _children.size() > 0;
	}
	
	/**
	 * Returns the children of this node.
	 * @return the children of this node.
	 */
	public final IAlarmTreeNode[] getChildren() {
		return _children.toArray(new IAlarmTreeNode[_children.size()]);		
	}

	/**
	 * {@inheritDoc}
	 */
	public final String getName() {
		return _name;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public final void setName(String name) {
		_name = name;
	}

	/**
	 * {@inheritDoc}
	 */
	public final SubtreeNode getParent() {
		return _parent;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final String toString() {
		return _name;
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean hasAlarm() {
		return _highestChildSeverity.isAlarm() || _highestUnacknowledgedChildSeverity.isAlarm();
	}

	/**
	 * Signals to this node that the alarm severity of one of its children
	 * changed. This method must be called by children of this node when their
	 * severity changes. If the change causes a severity change for this node,
	 * this node will in turn notify its parent, so that the highest severity
	 * is propagated to the root of the tree.
	 * 
	 * @param child the child node whose severity status has changed.
	 */
	final void childSeverityChanged(final IAlarmTreeNode child) {
		boolean thisNodeChanged = false;
		
		// If the severity is higher than the current highest severity, simply
		// set it as the new highest severity and propagate it to the parent.
		// Otherwise, there might still be other children with a higher
		// severity. So we look for the highest severity of the children, and
		// propagate that upwards (if it is different from the current
		// severity).
		
		// Start with the active alarm severity
		Severity active = child.getAlarmSeverity();
		if (active.compareTo(_highestChildSeverity) > 0) {
			_highestChildSeverity = active;
			thisNodeChanged = true;
		} else {
			thisNodeChanged |= refreshActiveSeverity();
		}
		// Now the highest unacknowledged severity
		Severity unack = child.getUnacknowledgedAlarmSeverity();
		if (unack.compareTo(_highestUnacknowledgedChildSeverity) > 0) {
			_highestUnacknowledgedChildSeverity = unack;
			thisNodeChanged = true;
		} else {
			thisNodeChanged |= refreshHighestUnacknowledgedSeverity();
		}
		
		// Notify parent if this node changed
		if (thisNodeChanged && _parent != null) {
			_parent.childSeverityChanged(this);
		}
	}
	
	/**
	 * Refreshes the severites of this node by searching its children for the
	 * highest severities.
	 */
	private void refreshSeverities() {
		boolean thisNodeChanged = false;
		thisNodeChanged |= refreshActiveSeverity();
		thisNodeChanged |= refreshHighestUnacknowledgedSeverity();
		
		if (thisNodeChanged && _parent != null) {
			_parent.childSeverityChanged(this);
		}
	}

	/**
	 * Refreshes the active severity of this node by searching its children for
	 * the highest severity.
	 * 
	 * @return <code>true</code> if the severity of this node was changed,
	 *         <code>false</code> otherwise.
	 */
	private boolean refreshActiveSeverity() {
		Severity s = findHighestChildSeverity();
		if (!s.equals(_highestChildSeverity)) {
			_highestChildSeverity = s;
			return true;
		}
		return false;
	}

	/**
	 * Refreshes the highest unacknowledged severity of this node by searching
	 * its children for the highest unacknowledged severity.
	 * 
	 * @return <code>true</code> if the severity of this node was changed,
	 *         <code>false</code> otherwise.
	 */
	private boolean refreshHighestUnacknowledgedSeverity() {
		Severity s = findHighestUnacknowledgedChildSeverity();
		if (!s.equals(_highestUnacknowledgedChildSeverity)) {
			_highestUnacknowledgedChildSeverity = s;
			return true;
		}
		return false;
	}

	/**
	 * Returns the highest severity of the children of this node.
	 * @return the highest severity of the children of this node.
	 */
	private Severity findHighestChildSeverity() {
		Severity highest = Severity.NO_ALARM;
		for (IAlarmTreeNode node : _children) {
			if (node.getAlarmSeverity().compareTo(highest) > 0) {
				highest = node.getAlarmSeverity();
			}
		}
		return highest;
	}
	
	/**
	 * Returns the highest unacknowledged severity of the children of this node.
	 * @return the highest unacknowledged severity of the children of this node.
	 */
	private Severity findHighestUnacknowledgedChildSeverity() {
		Severity highest = Severity.NO_ALARM;
		for (IAlarmTreeNode node : _children) {
			if (node.getUnacknowledgedAlarmSeverity().compareTo(highest) > 0) {
				highest = node.getUnacknowledgedAlarmSeverity();
			}
		}
		return highest;
	}

	/**
	 * Returns the direct child with the specified name. If no such child
	 * exists, returns {@code null}.
	 * @param name the name of the child.
	 * @return the direct child with the specified name.
	 */
	public final IAlarmTreeNode getChild(final String name) {
		for (IAlarmTreeNode child : _children) {
			if (child.getName().equals(name)) {
				return child;
			}
		}
		// not found
		return null;
	}
	
	/**
	 * Finds all process variable nodes with the given name below this node. If
	 * no nodes are found, returns an empty list.
	 * @param name the name of the nodes.
	 * @return a list of the nodes.
	 */
	public final List<ProcessVariableNode> findProcessVariableNodes(
			final String name) {
		List<ProcessVariableNode> result = new ArrayList<ProcessVariableNode>();
		collectProcessVariableNodes(name, result);
		return result;
	}
	
	/**
	 * Recursively searches this subtree for process variable nodes with the
	 * given name and adds them to the given list.
	 * @param name the name of the nodes.
	 * @param nodes the list to which the nodes will be added.
	 */
	private void collectProcessVariableNodes(final String name,
			final List<ProcessVariableNode> nodes) {
		for (IAlarmTreeNode child : _children) {
			if (child instanceof ProcessVariableNode
					&& child.getName().equals(name)) {
				nodes.add((ProcessVariableNode) child);
			} else if (child instanceof SubtreeNode) {
				((SubtreeNode) child).collectProcessVariableNodes(name, nodes);
			}
		}
	}

	/**
	 * Returns a collection of the PV nodes (leaf nodes) below this subtree
	 * node that have unacknowledged alarms.
	 * 
	 * @return a collection of the PV nodes with unacknowledged alarms.
	 */
	public final Collection<ProcessVariableNode> collectUnacknowledgedAlarms() {
		Collection<ProcessVariableNode> result = new ArrayList<ProcessVariableNode>();
		this.recurseCollectUnack(result);
		return result;
	}

	/**
	 * Recursively collects nodes with unacknowledged alarms into the given
	 * collection.
	 * 
	 * @param result the collection to which the nodes will be added.
	 */
	private void recurseCollectUnack(final Collection<ProcessVariableNode> result) {
		for (IAlarmTreeNode child : _children) {
			if (child instanceof SubtreeNode) {
				((SubtreeNode) child).recurseCollectUnack(result);
			} else if (child instanceof ProcessVariableNode) {
				if (child.getUnacknowledgedAlarmSeverity() != Severity.NO_ALARM) {
					result.add((ProcessVariableNode) child);
				}
			}
		}
	}
}
