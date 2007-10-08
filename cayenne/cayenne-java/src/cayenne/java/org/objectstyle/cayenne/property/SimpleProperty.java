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

/**
 * A property descriptor that provides access to a simple object property, delegating
 * property read/write operations to an accessor.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class SimpleProperty implements Property {

    protected ClassDescriptor owner;
    protected PropertyAccessor accessor;
    
    // name is derived from accessor, cached here for performance
    final String name;

    public SimpleProperty(ClassDescriptor owner, PropertyAccessor accessor) {

        if (accessor == null) {
            throw new IllegalArgumentException("Null accessor");
        }

        this.accessor = accessor;
        this.owner = owner;
        this.name = accessor.getName();
    }

    public Object readProperty(Object object) throws PropertyAccessException {
        return readPropertyDirectly(object);
    }

    public void writeProperty(Object object, Object oldValue, Object newValue)
            throws PropertyAccessException {
        writePropertyDirectly(object, oldValue, newValue);
    }

    public String getName() {
        return name;
    }

    public boolean visit(PropertyVisitor visitor) {
        return visitor.visitProperty(this);
    }

    /**
     * Does nothing.
     */
    public void injectValueHolder(Object object) throws PropertyAccessException {
        // noop
    }

    public void shallowMerge(Object from, Object to) throws PropertyAccessException {
        writePropertyDirectly(to, accessor.readPropertyDirectly(to), accessor
                .readPropertyDirectly(from));
    }

    public Object readPropertyDirectly(Object object) throws PropertyAccessException {
        return accessor.readPropertyDirectly(object);
    }

    public void writePropertyDirectly(Object object, Object oldValue, Object newValue)
            throws PropertyAccessException {
        accessor.writePropertyDirectly(object, oldValue, newValue);
    }
}
