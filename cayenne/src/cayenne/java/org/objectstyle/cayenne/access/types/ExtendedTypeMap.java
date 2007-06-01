/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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

package org.objectstyle.cayenne.access.types;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** 
 * Contains a map of ExtendedType objects, that serve as handlers for converting
 * values between Java application and JDBC layer.
 * 
 * <p>Class uses singleton model, since mapping is usually shared within the
 * application.</p>
 * 
 * @author Andrei Adamchik
 */
public class ExtendedTypeMap {
    protected Map typeMap = new HashMap();
    protected DefaultType defaultType = new DefaultType();

    public ExtendedTypeMap() {
        this.initDefaultTypes();
    }

    /** 
     * Registers default extended types. This method is called from
     * constructor and exists mainly for the benefit of subclasses that can
     * override it and configure their own extended types.
     */
    protected void initDefaultTypes() {
        // register default types
        Iterator it = DefaultType.defaultTypes();
        while (it.hasNext()) {
            registerType(new DefaultType((String) it.next()));
        }
    }

    /** Adds new type to the list of registered types. */
    public void registerType(ExtendedType type) {
        typeMap.put(type.getClassName(), type);
    }

    public ExtendedType getDefaultType() {
        return defaultType;
    }

    /**
     * Returns a type registered for the class name. If no such type exists,
     * returns the default type. It is guaranteed that this method returns a
     * non-null ExtendedType instance. Note that for array types class name must
     * be in the form 'MyClass[]'.
     */
    public ExtendedType getRegisteredType(String javaClassName) {
        ExtendedType type = (ExtendedType) typeMap.get(javaClassName);
        return (type != null) ? type : defaultType;
    }

    /**
      * Returns a type registered for the class name. If no such type exists,
      * returns the default type. It is guaranteed that this method returns a
      * non-null ExtendedType instance.
      */
    public ExtendedType getRegisteredType(Class javaClass) {
        String name = null;

        if (javaClass.isArray()) {
            // only support single dimensional arrays now
            name = javaClass.getComponentType() + "[]";
        } else {
            name = javaClass.getName();
        }

        ExtendedType type = (ExtendedType) typeMap.get(name);
        return (type != null) ? type : defaultType;
    }

    /** 
     * Removes registered ExtendedType object corresponding to
     * <code>javaClassName</code> parameter. 
     */
    public void unregisterType(String javaClassName) {
        typeMap.remove(javaClassName);
    }

    /** 
     * Returns array of Java class names supported by Cayenne 
     * for JDBC mapping. 
     */
    public String[] getRegisteredTypeNames() {
        Set keys = typeMap.keySet();
        int len = keys.size();
        String[] types = new String[len];

        Iterator it = keys.iterator();
        for (int i = 0; i < len; i++) {
            types[i] = (String) it.next();
        }

        return types;
    }
}
