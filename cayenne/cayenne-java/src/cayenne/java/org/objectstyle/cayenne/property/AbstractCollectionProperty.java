/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.property;

import java.util.Collection;

import org.objectstyle.cayenne.Fault;
import org.objectstyle.cayenne.ValueHolder;

/**
 * A generic superclass of CollectionProperty implementations.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public abstract class AbstractCollectionProperty extends AbstractSingleObjectArcProperty
        implements CollectionProperty {

    public AbstractCollectionProperty(ClassDescriptor owner,
            ClassDescriptor targetDescriptor, PropertyAccessor accessor,
            String reverseName) {
        super(owner, targetDescriptor, accessor, reverseName);
    }

    public Object readProperty(Object object) throws PropertyAccessException {
        return ensureCollectionValueHolderSet(object);
    }

    /**
     * Wraps list in a value holder that performs lazy faulting.
     */
    public void writePropertyDirectly(Object object, Object oldValue, Object newValue)
            throws PropertyAccessException {

        // must resolve value holder...
        ValueHolder holder = (ValueHolder) readProperty(object);
        holder.setValueDirectly(newValue);
    }

    public void addTarget(Object source, Object target, boolean setReverse) {
        if (target == null) {
            throw new NullPointerException("Attempt to add null object.");
        }

        // TODO, Andrus, 2/9/2006 - CayenneDataObject differences:
        // * invokes "willConnect"
        // * has a callback to ObjectStore to handle flattened
        // * has a callback to ObjectStore to retain snapshot
        // * changes object state to modified

        // Now do the rest of the normal handling (regardless of whether it was
        // flattened or not)
        Collection collection = (Collection) readProperty(source);
        collection.add(target);

        if (target != null && setReverse) {
            setReverse(source, null, target);
        }
    }

    public void removeTarget(Object source, Object target, boolean setReverse) {

        // TODO, Andrus, 2/9/2006 - CayenneDataObject differences:
        // * has a callback to ObjectStore to handle flattened
        // * changes object state to modified

        // Now do the rest of the normal handling (regardless of whether it was
        // flattened or not)
        Collection collection = (Collection) readProperty(source);
        collection.remove(target);

        if (target != null && setReverse) {
            setReverse(source, target, null);
        }
    }

    public boolean visit(PropertyVisitor visitor) {
        return visitor.visitCollectionArc(this);
    }

    public void shallowMerge(Object from, Object to) throws PropertyAccessException {
        // noop
    }

    /**
     * Injects a List in the object if it hasn't been done yet.
     */
    public void injectValueHolder(Object object) throws PropertyAccessException {
        ensureCollectionValueHolderSet(object);
    }

    /**
     * Checks that an object's List field described by this property is set, injecting a
     * List if needed.
     */
    protected ValueHolder ensureCollectionValueHolderSet(Object object)
            throws PropertyAccessException {

        Object value = accessor.readPropertyDirectly(object);

        if (value == null || value instanceof Fault) {
            value = createCollectionValueHolder(object);
            accessor.writePropertyDirectly(object, null, value);
        }

        return (ValueHolder) value;
    }

    /**
     * Creates a Collection for an object.
     */
    protected abstract ValueHolder createCollectionValueHolder(Object object)
            throws PropertyAccessException;
}
