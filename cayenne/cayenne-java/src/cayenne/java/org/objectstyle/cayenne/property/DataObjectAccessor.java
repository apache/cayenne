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

import org.objectstyle.cayenne.DataObject;

/**
 * A PropertyAccessor that uses DataObject API to read/write values.
 * 
 * @author Andrus Adamchik
 */
public class DataObjectAccessor implements PropertyAccessor {

    protected String propertyName;

    public DataObjectAccessor(String propertyName) {

        if (propertyName == null) {
            throw new IllegalArgumentException("Null propertyName");
        }

        this.propertyName = propertyName;
    }

    public String getName() {
        return propertyName;
    }

    /**
     * Reads the value without disturbing DataObject state. I.e. no Fault resolving occurs
     * here.
     */
    public Object readPropertyDirectly(Object object) throws PropertyAccessException {
        try {

            DataObject dataObject = (DataObject) object;
            return dataObject.readPropertyDirectly(propertyName);
        }
        catch (ClassCastException e) {
            throw new PropertyAccessException("Object is not a DataObject: '"
                    + object.getClass().getName()
                    + "'", this, object, e);
        }
        catch (Throwable th) {
            throw new PropertyAccessException("Error reading DataObject property: "
                    + propertyName, this, object, th);
        }

        // TODO - see TODO in 'writeValue'
    }

    public void writePropertyDirectly(Object object, Object oldValue, Object newValue)
            throws PropertyAccessException {

        try {
            ((DataObject) object).writePropertyDirectly(propertyName, newValue);
        }
        catch (ClassCastException e) {
            throw new PropertyAccessException("Object is not a DataObject: '"
                    + object.getClass().getName()
                    + "'", this, object, e);
        }
        catch (Throwable th) {
            throw new PropertyAccessException("Error reading DataObject property: "
                    + propertyName, this, object, th);
        }

        // TODO, Andrus, 1/22/2006 - check for the right type? DataObject never did it
        // itself... Doing a check (and a conversion) may be an easy way to fix CAY-399
    }
}
