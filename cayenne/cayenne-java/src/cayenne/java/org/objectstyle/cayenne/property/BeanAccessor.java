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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

/**
 * A property accessor that uses set/get methods following JavaBean naming conventions.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class BeanAccessor implements PropertyAccessor {

    protected String propertyName;
    protected Method readMethod;
    protected Method writeMethod;
    protected Object nullValue;

    public BeanAccessor(Class objectClass, String propertyName, Class propertyType) {
        if (objectClass == null) {
            throw new IllegalArgumentException("Null objectClass");
        }

        if (propertyName == null) {
            throw new IllegalArgumentException("Null propertyName");
        }

        this.propertyName = propertyName;
        this.nullValue = PropertyUtils.defaultNullValueForType(propertyType);

        try {
            PropertyDescriptor descriptor = new PropertyDescriptor(
                    propertyName,
                    objectClass);

            this.readMethod = descriptor.getReadMethod();
            this.writeMethod = descriptor.getWriteMethod();
        }
        catch (IntrospectionException e) {
            throw new PropertyAccessException(
                    "Invalid bean property: " + propertyName,
                    null,
                    e);
        }
    }

    public String getName() {
        return propertyName;
    }

    public Object readPropertyDirectly(Object object) throws PropertyAccessException {
        if (readMethod == null) {
            throw new PropertyAccessException("Property '"
                    + propertyName
                    + "' is not readable", this, object);
        }

        try {
            return readMethod.invoke(object, null);
        }
        catch (Throwable th) {
            throw new PropertyAccessException(
                    "Error reading property: " + propertyName,
                    this,
                    object,
                    th);
        }
    }

    public void writePropertyDirectly(Object object, Object oldValue, Object newValue)
            throws PropertyAccessException {

        if (writeMethod == null) {
            throw new PropertyAccessException("Property '"
                    + propertyName
                    + "' is not writable", this, object);
        }

        // this will take care of primitives.
        if (newValue == null) {
            newValue = this.nullValue;
        }

        try {
            writeMethod.invoke(object, new Object[] {
                newValue
            });
        }
        catch (Throwable th) {
            throw new PropertyAccessException(
                    "Error reading property: " + propertyName,
                    this,
                    object,
                    th);
        }
    }
}
