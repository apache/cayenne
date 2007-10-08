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
package org.objectstyle.cayenne.access.types;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.validation.ValidationResult;

/**
 * Handles <code>java.lang.Boolean</code> mapping. Note that "materialize*" methods
 * return either Boolean.TRUE or Boolean.FALSE, instead of creating new Boolean instances
 * using contructor. This makes possible identity comparison such as
 * <code>object.getBooleanProperty() == Boolean.TRUE</code>.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class BooleanType implements ExtendedType {

    public String getClassName() {
        return Boolean.class.getName();
    }

    public boolean validateProperty(
            Object source,
            String property,
            Object value,
            DbAttribute dbAttribute,
            ValidationResult validationResult) {
        return AbstractType.validateNull(
                source,
                property,
                value,
                dbAttribute,
                validationResult);
    }

    public void setJdbcObject(
            PreparedStatement st,
            Object val,
            int pos,
            int type,
            int precision) throws Exception {

        if (val == null) {
            st.setNull(pos, type);
        }
        else if (type == Types.BIT || type == Types.BOOLEAN) {
            boolean flag = Boolean.TRUE.equals(val);
            st.setBoolean(pos, flag);
        }
        else {
            st.setObject(pos, val, type);
        }
    }

    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
        boolean b = rs.getBoolean(index);
        return (rs.wasNull()) ? null : b ? Boolean.TRUE : Boolean.FALSE;
    }

    public Object materializeObject(CallableStatement st, int index, int type)
            throws Exception {
        boolean b = st.getBoolean(index);
        return (st.wasNull()) ? null : b ? Boolean.TRUE : Boolean.FALSE;
    }
}
