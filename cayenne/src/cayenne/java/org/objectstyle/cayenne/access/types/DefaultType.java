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

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.validation.ValidationResult;

/** 
 * Default implementation of ExtendedType that works exactly per JDBC
 * specification.
 * 
 * @author Andrei Adamchik
 */
public class DefaultType extends AbstractType {

    private static final Map readMethods = new HashMap();
    private static final Map procReadMethods = new HashMap();
    private static Method readObjectMethod;
    private static Method procReadObjectMethod;

    static {
        try {
            Class rsClass = ResultSet.class;
            Class[] paramTypes = new Class[] { Integer.TYPE };
            readMethods.put(
                TypesMapping.JAVA_LONG,
                rsClass.getMethod("getLong", paramTypes));
            readMethods.put(
                TypesMapping.JAVA_BIGDECIMAL,
                rsClass.getMethod("getBigDecimal", paramTypes));
            readMethods.put(
                TypesMapping.JAVA_BOOLEAN,
                rsClass.getMethod("getBoolean", paramTypes));
            readMethods.put(
                TypesMapping.JAVA_BYTE,
                rsClass.getMethod("getByte", paramTypes));
            readMethods.put(
                TypesMapping.JAVA_BYTES,
                rsClass.getMethod("getBytes", paramTypes));
            readMethods.put(
                TypesMapping.JAVA_SQLDATE,
                rsClass.getMethod("getDate", paramTypes));
            readMethods.put(
                TypesMapping.JAVA_DOUBLE,
                rsClass.getMethod("getDouble", paramTypes));
            readMethods.put(
                TypesMapping.JAVA_FLOAT,
                rsClass.getMethod("getFloat", paramTypes));
            readMethods.put(
                TypesMapping.JAVA_INTEGER,
                rsClass.getMethod("getInt", paramTypes));
            readMethods.put(
                TypesMapping.JAVA_SHORT,
                rsClass.getMethod("getShort", paramTypes));
            readMethods.put(
                TypesMapping.JAVA_STRING,
                rsClass.getMethod("getString", paramTypes));
            readMethods.put(
                TypesMapping.JAVA_TIME,
                rsClass.getMethod("getTime", paramTypes));
            readMethods.put(
                TypesMapping.JAVA_TIMESTAMP,
                rsClass.getMethod("getTimestamp", paramTypes));

            readObjectMethod = rsClass.getMethod("getObject", paramTypes);

            // init procedure read methods
            Class csClass = CallableStatement.class;
            procReadMethods.put(
                TypesMapping.JAVA_LONG,
                csClass.getMethod("getLong", paramTypes));
            procReadMethods.put(
                TypesMapping.JAVA_BIGDECIMAL,
                csClass.getMethod("getBigDecimal", paramTypes));
            procReadMethods.put(
                TypesMapping.JAVA_BOOLEAN,
                csClass.getMethod("getBoolean", paramTypes));
            procReadMethods.put(
                TypesMapping.JAVA_BYTE,
                csClass.getMethod("getByte", paramTypes));
            procReadMethods.put(
                TypesMapping.JAVA_BYTES,
                csClass.getMethod("getBytes", paramTypes));
            procReadMethods.put(
                TypesMapping.JAVA_SQLDATE,
                csClass.getMethod("getDate", paramTypes));
            procReadMethods.put(
                TypesMapping.JAVA_DOUBLE,
                csClass.getMethod("getDouble", paramTypes));
            procReadMethods.put(
                TypesMapping.JAVA_FLOAT,
                csClass.getMethod("getFloat", paramTypes));
            procReadMethods.put(
                TypesMapping.JAVA_INTEGER,
                csClass.getMethod("getInt", paramTypes));
            procReadMethods.put(
                TypesMapping.JAVA_SHORT,
                csClass.getMethod("getShort", paramTypes));
            procReadMethods.put(
                TypesMapping.JAVA_STRING,
                csClass.getMethod("getString", paramTypes));
            procReadMethods.put(
                TypesMapping.JAVA_TIME,
                csClass.getMethod("getTime", paramTypes));
            procReadMethods.put(
                TypesMapping.JAVA_TIMESTAMP,
                csClass.getMethod("getTimestamp", paramTypes));

            procReadObjectMethod = csClass.getMethod("getObject", paramTypes);
        } catch (Exception ex) {
            throw new CayenneRuntimeException(
                "Error initializing read methods.",
                ex);
        }
    }

    /** Returns an Iterator of supported default Java classes (as Strings) */
    public static Iterator defaultTypes() {
        return readMethods.keySet().iterator();
    }

    protected String className;
    protected Method readMethod;
    protected Method procReadMethod;

    /** 
     * CreatesDefaultType to read objects from ResultSet
     * using "getObject" method.
     */
    public DefaultType() {
        this.className = Object.class.getName();
        this.readMethod = readObjectMethod;
        this.procReadMethod = procReadObjectMethod;
    }

    public DefaultType(String className) {
        this.className = className;
        this.readMethod = (Method) readMethods.get(className);

        if (readMethod == null) {
            throw new CayenneRuntimeException(
                "Unsupported default class: "
                    + className
                    + ". If you want a non-standard class to map to JDBC type,"
                    + " you will need to implement ExtendedType interface yourself.");
        }

        this.procReadMethod = (Method) procReadMethods.get(className);
        if (procReadMethod == null) {
            throw new CayenneRuntimeException(
                "Unsupported default class: "
                    + className
                    + ". If you want a non-standard class to map to JDBC type,"
                    + " you will need to implement ExtendedType interface yourself.");
        }
    }

    public String getClassName() {
        return className;
    }
    
    /**
     * Always returns true indicating no validation failures. DefaultType doesn't
     * support meaningful validation. Since it is generic and works with any types
     * of Java values, it is not known how a given value will be converted to the
     * database representation.
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
    
    public Object materializeObject(ResultSet rs, int index, int type)
        throws Exception {
        Object val = readMethod.invoke(rs, new Object[] { new Integer(index)});
        return (rs.wasNull()) ? null : val;
    }

    public Object materializeObject(CallableStatement st, int index, int type)
        throws Exception {
        Object val =
            procReadMethod.invoke(st, new Object[] { new Integer(index)});
        return (st.wasNull()) ? null : val;
    }
}
