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
package org.objectstyle.cayenne.access.jdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.types.ExtendedType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;

/**
 * A descriptor of a result row obtained from a database.
 * 
 * @since 1.2
 * @author Andrei Adamchik
 */
// replaces 1.1 ResultDescriptor
public class RowDescriptor {

    protected ColumnDescriptor[] columns;
    protected ExtendedType[] converters;

    /**
     * Creates an empty RowDescriptor. Intended mainly for testing and use by subclasses.
     */
    protected RowDescriptor() {

    }

    /**
     * Creates a RowDescriptor for an array of columns.
     */
    public RowDescriptor(ColumnDescriptor[] columns, ExtendedTypeMap types) {
        this.columns = columns;
        indexTypes(types);
    }

    /**
     * Creates new RowDescriptor using ResultSet metadata to determine the columns.
     */
    public RowDescriptor(ResultSet resultSet, ExtendedTypeMap types) {
        this(resultSet, types, null);
    }

    /**
     * Creates new RowDescriptor using ResultSet metadata to determine the columns. Note
     * that if javaTypeOverrides array is null, default JDBC to Java types mapping is
     * used.
     */
    public RowDescriptor(ResultSet resultSet, ExtendedTypeMap types, Map javaTypeOverrides) {

        initFromResultSet(resultSet);

        if (javaTypeOverrides != null) {
            overrideJavaTypes(javaTypeOverrides);
        }

        indexTypes(types);
    }

    /**
     * Initializes converters for columns.
     */
    protected void indexTypes(ExtendedTypeMap types) {
        this.converters = new ExtendedType[columns.length];
        for (int i = 0; i < columns.length; i++) {
            converters[i] = types.getRegisteredType(columns[i].getJavaClass());
        }
    }

    /**
     * Builds columns list from ResultSet metadata.
     */
    protected void initFromResultSet(ResultSet resultSet) {
        try {
            ResultSetMetaData md = resultSet.getMetaData();
            int len = md.getColumnCount();
            if (len == 0) {
                throw new CayenneRuntimeException("No columns in ResultSet.");
            }

            this.columns = new ColumnDescriptor[len];

            for (int i = 0; i < len; i++) {
                columns[i] = new ColumnDescriptor(md, i + 1);
            }
        }
        catch (SQLException sqex) {
            throw new CayenneRuntimeException("Error reading metadata.", sqex);
        }
    }

    /**
     * Overrides Java types of result columns. Keys in the map must correspond to the
     * names of the columns.
     */
    protected void overrideJavaTypes(Map overrides) {

        for (int i = 0; i < columns.length; i++) {
            String type = (String) overrides.get(columns[i].getName());

            if (type != null) {
                columns[i].setJavaClass(type);
            }
        }
    }

    /**
     * Returns a number of columns in a row.
     */
    public int getWidth() {
        return columns.length;
    }

    /**
     * Returns column descriptors.
     */
    public ColumnDescriptor[] getColumns() {
        return columns;
    }

    /**
     * Returns extended types for columns.
     */
    public ExtendedType[] getConverters() {
        return converters;
    }
}