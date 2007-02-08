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

import java.sql.Types;

import junit.framework.TestCase;

import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;

/**
 * @author Andrei Adamchik
 */
public class ColumnDescriptorTst extends TestCase {

    public void testName() {
        ColumnDescriptor column = new ColumnDescriptor();
        column.setName("abc");
        assertEquals("abc", column.getName());
    }

    public void testLabel() {
        ColumnDescriptor column = new ColumnDescriptor();
        column.setLabel("abc");
        assertEquals("abc", column.getLabel());
    }

    public void testDbAttributeConstructor() {
        DbEntity entity = new DbEntity("entity");
        DbAttribute a = new DbAttribute();
        a.setName("name");
        a.setType(Types.VARCHAR);
        a.setEntity(entity);

        entity.addAttribute(a);

        ColumnDescriptor column = new ColumnDescriptor(a, null);
        assertEquals("name", column.getName());
        assertEquals("name", column.getQualifiedColumnName());
        assertEquals("entity", column.getTableName());
        assertEquals(String.class.getName(), column.getJavaClass());
        assertEquals("name", column.getLabel());
        assertEquals(Types.VARCHAR, column.getJdbcType());
    }

    public void testEquals() {
        ColumnDescriptor column1 = new ColumnDescriptor();
        column1.setName("n1");
        column1.setQualifiedColumnName("np1");
        column1.setTableName("t1");
        // type should be ignored in the comparison
        column1.setJdbcType(Types.VARCHAR);

        ColumnDescriptor column2 = new ColumnDescriptor();
        column2.setName("n1");
        column2.setQualifiedColumnName("np1");
        column2.setTableName("t1");
        column2.setJdbcType(Types.BOOLEAN);

        ColumnDescriptor column3 = new ColumnDescriptor();
        column3.setName("n1");
        column3.setQualifiedColumnName("np3");
        column3.setTableName("t1");

        assertEquals(column1, column2);
        assertFalse(column1.equals(column3));
        assertFalse(column3.equals(column2));
    }

    public void testHashCode() {
        ColumnDescriptor column1 = new ColumnDescriptor();
        column1.setName("n1");
        column1.setQualifiedColumnName("np1");
        column1.setTableName("t1");
        // type should be ignored in the comparison
        column1.setJdbcType(Types.VARCHAR);

        ColumnDescriptor column2 = new ColumnDescriptor();
        column2.setName("n1");
        column2.setQualifiedColumnName("np1");
        column2.setTableName("t1");
        column2.setJdbcType(Types.BOOLEAN);

        ColumnDescriptor column3 = new ColumnDescriptor();
        column3.setName("n1");
        column3.setQualifiedColumnName("np3");
        column3.setTableName("t1");

        assertEquals(column1.hashCode(), column2.hashCode());

        // this is not really required by the hashcode contract... but just to see that
        // different columns generally end up in different buckets..
        assertTrue(column1.hashCode() != column3.hashCode());
    }
}