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

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.validation.ValidationResult;

/**
 * An ExtendedType that handles an enum class. If Enum is mapped to a character column,
 * its name is used as persistent value; if it is mapped to a numeric column, its ordinal
 * (i.e. a position in enum class) is used.
 * <p>
 * <i>Requires Java 1.5 or newer</i>
 * </p>
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class EnumType implements ExtendedType {

    protected Class enumClass;
    protected Object[] values;

    public EnumType(Class enumClass) {
        if (enumClass == null) {
            throw new IllegalArgumentException("Null enum class");
        }

        this.enumClass = enumClass;

        try {
            Method m = enumClass.getMethod("values");
            this.values = (Object[]) m.invoke(null);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Class "
                    + enumClass.getName()
                    + " is not an Enum", e);
        }
    }

    public String getClassName() {
        return enumClass.getName();
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
            PreparedStatement statement,
            Object value,
            int pos,
            int type,
            int precision) throws Exception {

        if (value instanceof Enum) {

            Enum e = (Enum) value;

            if (TypesMapping.isNumeric(type)) {
                statement.setInt(pos, e.ordinal());
            }
            else {
                statement.setString(pos, e.name());
            }
        }
        else {
            statement.setNull(pos, type);
        }
    }

    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
        if (TypesMapping.isNumeric(type)) {
            int i = rs.getInt(index);
            return (rs.wasNull() || index < 0) ? null : values[i];
        }
        else {
            String string = rs.getString(index);
            return string != null ? Enum.valueOf(enumClass, string) : null;
        }
    }

    public Object materializeObject(CallableStatement rs, int index, int type)
            throws Exception {

        if (TypesMapping.isNumeric(type)) {
            int i = rs.getInt(index);
            return (rs.wasNull() || index < 0) ? null : values[i];
        }
        else {
            String string = rs.getString(index);
            return string != null ? Enum.valueOf(enumClass, string) : null;
        }
    }
}
