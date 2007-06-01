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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.validation.ValidationResult;

/**
 * ExtendedType that allows Java application to use java.util.Date
 * transparently for all three database date/time types: TIME, DATE, TIMESTAMP.
 * 
 * @author Andrei Adamchik
 */
public class UtilDateType extends AbstractType {

    public String getClassName() {
        return java.util.Date.class.getName();
    }

    /**
     * Always returns true indicating no validation failures. There is no date-specific
     * validations at the moment.
     * 
     * @since 1.1
     */
    public boolean validateProperty(
        Object source,
        String property,
        Object value,
        DbAttribute dbAttribute,
        ValidationResult validationResult) {
        return true;
    }

    protected Object convertToJdbcObject(Object val, int type) throws Exception {
        if (type == Types.DATE)
            return new java.sql.Date(((java.util.Date) val).getTime());
        else if (type == Types.TIME)
            return new java.sql.Time(((java.util.Date) val).getTime());
        else if (type == Types.TIMESTAMP)
            return new java.sql.Timestamp(((java.util.Date) val).getTime());
        else
            throw new IllegalArgumentException(
                "Only date/time types can be used for '" + getClassName() + "'.");
    }

    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
        Object val = null;

        switch (type) {
            case Types.TIMESTAMP :
                val = rs.getTimestamp(index);
                break;
            case Types.DATE :
                val = rs.getDate(index);
                break;
            case Types.TIME :
                val = rs.getTime(index);
                break;
            default :
                val = rs.getObject(index);
                break;
        }

        // all sql time/date classes are subclasses of java.util.Date,
        // so lets cast it to Date,
        // if it is not date, ClassCastException will be thrown,
        // which is what we want
        return (rs.wasNull())
            ? null
            : new java.util.Date(((java.util.Date) val).getTime());
    }

    public Object materializeObject(CallableStatement cs, int index, int type)
        throws Exception {
        Object val = null;

        switch (type) {
            case Types.TIMESTAMP :
                val = cs.getTimestamp(index);
                break;
            case Types.DATE :
                val = cs.getDate(index);
                break;
            case Types.TIME :
                val = cs.getTime(index);
                break;
            default :
                val = cs.getObject(index);
                // check if value was properly converted by the driver
                if (val != null && !(val instanceof java.util.Date)) {
                    String typeName = TypesMapping.getSqlNameByType(type);
                    throw new ClassCastException(
                        "Expected a java.util.Date or subclass, instead fetched '"
                            + val.getClass().getName()
                            + "' for JDBC type "
                            + typeName);
                }
                break;
        }

        // all sql time/date classes are subclasses of java.util.Date,
        // so lets cast it to Date,
        // if it is not date, ClassCastException will be thrown,
        // which is what we want
        return (cs.wasNull())
            ? null
            : new java.util.Date(((java.util.Date) val).getTime());
    }

    public void setJdbcObject(
        PreparedStatement st,
        Object val,
        int pos,
        int type,
        int precision)
        throws Exception {
        super.setJdbcObject(st, convertToJdbcObject(val, type), pos, type, precision);
    }
}