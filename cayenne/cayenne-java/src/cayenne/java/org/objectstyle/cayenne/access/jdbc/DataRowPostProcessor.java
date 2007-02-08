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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.access.trans.SelectTranslator;
import org.objectstyle.cayenne.access.types.ExtendedType;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.EntityInheritanceTree;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;

/**
 * Deals with DataRow type conversion in inheritance situations.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataRowPostProcessor {

    private EntityInheritanceTree inheritanceTree;
    private Map columnOverrides;
    private Collection defaultOverrides;

    // factory method
    static DataRowPostProcessor createPostProcessor(SelectTranslator translator) {
        Map attributeOverrides = translator.getAttributeOverrides();
        if (attributeOverrides.isEmpty()) {
            return null;
        }

        ColumnDescriptor[] columns = translator.getResultColumns();

        Map columnOverrides = new HashMap(2);

        Iterator it = attributeOverrides.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();

            ObjAttribute attribute = (ObjAttribute) entry.getKey();
            Entity entity = attribute.getEntity();

            String key = null;
            int jdbcType = TypesMapping.NOT_DEFINED;
            int index = -1;
            for (int i = 0; i < columns.length; i++) {
                if (columns[i] == entry.getValue()) {

                    // if attribute type is the same as column, there is no conflict
                    if (!attribute.getType().equals(columns[i].getJavaClass())) {
                        // note that JDBC index is "1" based
                        index = i + 1;
                        jdbcType = columns[i].getJdbcType();
                        key = columns[i].getLabel();
                    }

                    break;
                }
            }

            if (index < 1) {
                continue;
            }

            ExtendedType converter = translator
                    .getAdapter()
                    .getExtendedTypes()
                    .getRegisteredType(attribute.getType());

            Collection overrides = null;

            if (columnOverrides == null) {
                columnOverrides = new HashMap(2);
            }
            else {
                overrides = (Collection) columnOverrides.get(entity.getName());
            }

            if (overrides == null) {
                overrides = new ArrayList(3);
                columnOverrides.put(entity.getName(), overrides);
            }

            overrides.add(new ColumnOverride(index, key, converter, jdbcType));
        }

        // inject null post-processor
        return columnOverrides != null ? new DataRowPostProcessor(translator
                .getRootInheritanceTree(), columnOverrides) : null;
    }

    private DataRowPostProcessor(EntityInheritanceTree inheritanceTree,
            Map columnOverrides) {

        if (inheritanceTree != null && inheritanceTree.getChildren().size() > 0) {
            this.inheritanceTree = inheritanceTree;
            this.columnOverrides = columnOverrides;
        }
        else {
            if (columnOverrides.size() != 1) {
                throw new IllegalArgumentException(
                        "No inheritance - there must be only one override set");
            }

            defaultOverrides = (Collection) columnOverrides.values().iterator().next();
        }
    }

    void postprocessRow(ResultSet resultSet, DataRow row) throws Exception {

        Collection overrides = getOverrides(row);

        if (overrides != null) {
            Iterator it = overrides.iterator();
            while (it.hasNext()) {
                ColumnOverride override = (ColumnOverride) it.next();

                Object newValue = override.converter.materializeObject(
                        resultSet,
                        override.index,
                        override.jdbcType);
                row.put(override.key, newValue);
            }
        }
    }

    private final Collection getOverrides(DataRow row) {
        if (defaultOverrides != null) {
            return defaultOverrides;
        }
        else {
            ObjEntity entity = inheritanceTree.entityMatchingRow(row);
            return entity != null
                    ? (Collection) columnOverrides.get(entity.getName())
                    : null;
        }
    }

    static final class ColumnOverride {

        int index;
        int jdbcType;
        String key;
        ExtendedType converter;

        ColumnOverride(int index, String key, ExtendedType converter, int jdbcType) {
            this.index = index;
            this.key = key;
            this.converter = converter;
            this.jdbcType = jdbcType;
        }
    }
}
