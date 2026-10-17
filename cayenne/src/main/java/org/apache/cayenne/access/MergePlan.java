/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.access;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRowLayout;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyDescriptor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The columns that turn rows into objects of one {@link ClassDescriptor}: the primary key, the attributes and the
 * to-one foreign keys, by their positions in the descriptor's {@link ClassDescriptor#getSnapshotLayout() snapshot
 * layout}, and a {@link DataRowLayout.Reader} of that layout, which reads a snapshot built from an object by position
 * and looks the positions up once per result set otherwise. A resolver builds a plan per descriptor for the rows it
 * converts.
 */
final class MergePlan {

    final ClassDescriptor descriptor;

    final DataRowLayout layout;
    final DataRowLayout.Reader reader;

    // the primary key of the descriptor's DbEntity, PK i being column pkColumns[i]
    final String[] pkNames;
    final int[] pkColumns;

    // attribute i being column attributeColumns[i]
    final AttributeProperty[] attributes;
    final int[] attributeColumns;

    final ToOneProperty[] toOnes;
    // per to-one, the columns of the FK join by join, or null when the FK does not define the arc
    final int[][] toOneFkColumns;
    // per to-one, the target PK column name of each join
    final String[][] toOneTargetNames;

    final ToManyProperty[] toManys;

    private MergePlan(ClassDescriptor descriptor, DataRowLayout layout, String[] pkNames, int[] pkColumns,
                      AttributeProperty[] attributes, int[] attributeColumns, ToOneProperty[] toOnes,
                      int[][] toOneFkColumns, String[][] toOneTargetNames, ToManyProperty[] toManys) {
        this.descriptor = descriptor;
        this.layout = layout;
        this.reader = layout.reader();
        this.pkNames = pkNames;
        this.pkColumns = pkColumns;
        this.attributes = attributes;
        this.attributeColumns = attributeColumns;
        this.toOnes = toOnes;
        this.toOneFkColumns = toOneFkColumns;
        this.toOneTargetNames = toOneTargetNames;
        this.toManys = toManys;
    }

    static MergePlan of(ClassDescriptor descriptor) {

        DataRowLayout layout = descriptor.getSnapshotLayout();

        Collection<DbAttribute> pk = descriptor.getEntity().getDbEntity().getPrimaryKeys();
        String[] pkNames = new String[pk.size()];
        int[] pkColumns = new int[pk.size()];
        int i = 0;
        for (DbAttribute attribute : pk) {
            pkNames[i] = attribute.getName();
            pkColumns[i++] = column(layout, attribute.getName());
        }

        List<AttributeProperty> attributes = new ArrayList<>();
        List<ToOneProperty> toOnes = new ArrayList<>();
        List<ToManyProperty> toManys = new ArrayList<>();
        for (PropertyDescriptor property : descriptor.getProperties()) {
            switch (property) {
                case AttributeProperty attribute -> attributes.add(attribute);
                case ToOneProperty toOne -> toOnes.add(toOne);
                case ToManyProperty toMany -> toManys.add(toMany);
                default -> {
                }
            }
        }

        int[] attributeColumns = new int[attributes.size()];
        for (i = 0; i < attributeColumns.length; i++) {
            attributeColumns[i] = column(layout, attributes.get(i).getAttribute().getDbAttributePath().value());
        }

        int[][] toOneFkColumns = new int[toOnes.size()][];
        String[][] toOneTargetNames = new String[toOnes.size()][];
        for (i = 0; i < toOneFkColumns.length; i++) {
            ObjRelationship relationship = toOnes.get(i).getRelationship();
            // only an FK on the source lets the source snapshot tell whether the arc changed
            if (!relationship.isFromFK()) {
                continue;
            }
            List<DbJoin> joins = relationship.getDbRelationships().getFirst().getJoins();
            int[] fkColumns = new int[joins.size()];
            String[] targetNames = new String[joins.size()];
            for (int j = 0; j < fkColumns.length; j++) {
                fkColumns[j] = column(layout, joins.get(j).getSourceName());
                targetNames[j] = joins.get(j).getTargetName();
            }
            toOneFkColumns[i] = fkColumns;
            toOneTargetNames[i] = targetNames;
        }

        return new MergePlan(descriptor, layout, pkNames, pkColumns,
                attributes.toArray(new AttributeProperty[0]), attributeColumns,
                toOnes.toArray(new ToOneProperty[0]), toOneFkColumns, toOneTargetNames,
                toManys.toArray(new ToManyProperty[0]));
    }

    private static int column(DataRowLayout layout, String name) {
        int column = layout.indexOf(name);
        if (column < 0) {
            throw new CayenneRuntimeException("Column '%s' is not in the snapshot layout %s", name, layout);
        }
        return column;
    }
}
