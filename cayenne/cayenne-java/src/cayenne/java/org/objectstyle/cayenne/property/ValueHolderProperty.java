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

import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.ValueHolder;
import org.objectstyle.cayenne.util.PersistentObjectHolder;

/**
 * Provides access to a property implemented as a ValueHolder Field. This implementation
 * hides the fact of the ValueHolder existence. I.e. it never returns it from
 * 'readPropertyDirectly', returning held value instead.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class ValueHolderProperty extends AbstractSingleObjectArcProperty {

    public ValueHolderProperty(ClassDescriptor owner, ClassDescriptor targetDescriptor,
            PropertyAccessor accessor, String reverseName) {
        super(owner, targetDescriptor, accessor, reverseName);
    }

    /**
     * Returns true if a property ValueHolder is not initialized or is itself a fault.
     */
    public boolean isFault(Object object) {
        ValueHolder holder = (ValueHolder) accessor.readPropertyDirectly(object);
        return holder == null || holder.isFault();
    }

    public Object readPropertyDirectly(Object object) throws PropertyAccessException {
        ValueHolder holder = (ValueHolder) accessor.readPropertyDirectly(object);

        // TODO: Andrus, 2/9/2006 ValueHolder will resolve an object in a call to
        // 'getValue'; this is inconsistent with 'readPropertyDirectly' contract
        return (holder != null) ? holder.getValueDirectly() : null;
    }

    public Object readProperty(Object object) throws PropertyAccessException {
        return ensureValueHolderSet(object).getValue();
    }

    public void writePropertyDirectly(Object object, Object oldValue, Object newValue)
            throws PropertyAccessException {

        ValueHolder holder = (ValueHolder) accessor.readPropertyDirectly(object);
        if (holder == null) {
            holder = createValueHolder(object);
            accessor.writePropertyDirectly(object, null, holder);
        }

        holder.setValueDirectly(newValue);
    }

    public void writeProperty(Object object, Object oldValue, Object newValue)
            throws PropertyAccessException {
        ensureValueHolderSet(object).setValueDirectly(newValue);
    }

    public void shallowMerge(Object from, Object to) throws PropertyAccessException {
        // noop
    }

    /**
     * Injects a ValueHolder in the object if it hasn't been done yet.
     */
    public void injectValueHolder(Object object) throws PropertyAccessException {
        ensureValueHolderSet(object);
    }

    /**
     * Checks that an object's ValueHolder field described by this property is set,
     * injecting a ValueHolder if needed.
     */
    protected ValueHolder ensureValueHolderSet(Object object)
            throws PropertyAccessException {

        ValueHolder holder = (ValueHolder) accessor.readPropertyDirectly(object);
        if (holder == null) {
            holder = createValueHolder(object);
            accessor.writePropertyDirectly(object, null, holder);
        }

        return holder;
    }

    /**
     * Creates a ValueHolder for an object. Default implementation requires that an object
     * implements Persistent interface.
     */
    protected ValueHolder createValueHolder(Object object) throws PropertyAccessException {
        if (!(object instanceof Persistent)) {

            throw new PropertyAccessException(
                    "ValueHolders for non-persistent objects are not supported.",
                    this,
                    object);
        }

        return new PersistentObjectHolder((Persistent) object, getName());
    }
}
