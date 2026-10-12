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

import org.apache.cayenne.RowLayout;
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
 * The positions of the columns that turn rows of one {@link RowLayout} into objects of one {@link ClassDescriptor}:
 * the primary key, the attributes and the to-one foreign keys. A resolver builds a plan for the rows it converts,
 * so the column names are looked up once per batch of rows instead of once per row. A position of -1 marks a column
 * missing from the layout, i.e. a partial snapshot.
 */
final class MergePlan {

    final RowLayout layout;
    final ClassDescriptor descriptor;

    // the primary key of the descriptor's DbEntity
    final String[] pkNames;
    final int[] pkIndexes;

    final AttributeProperty[] attributes;
    final int[] attributeIndexes;

    final ToOneProperty[] toOnes;
    // per to-one, the positions of the FK columns join by join, or null when the FK does not define the arc
    final int[][] toOneFkIndexes;
    // per to-one, the target PK column of each join
    final String[][] toOneTargetNames;

    final ToManyProperty[] toManys;

    private MergePlan(RowLayout layout, ClassDescriptor descriptor, String[] pkNames, int[] pkIndexes,
                      AttributeProperty[] attributes, int[] attributeIndexes, ToOneProperty[] toOnes,
                      int[][] toOneFkIndexes, String[][] toOneTargetNames, ToManyProperty[] toManys) {
        this.layout = layout;
        this.descriptor = descriptor;
        this.pkNames = pkNames;
        this.pkIndexes = pkIndexes;
        this.attributes = attributes;
        this.attributeIndexes = attributeIndexes;
        this.toOnes = toOnes;
        this.toOneFkIndexes = toOneFkIndexes;
        this.toOneTargetNames = toOneTargetNames;
        this.toManys = toManys;
    }

    static MergePlan of(RowLayout layout, ClassDescriptor descriptor) {

        Collection<DbAttribute> pk = descriptor.getEntity().getDbEntity().getPrimaryKeys();
        String[] pkNames = new String[pk.size()];
        int[] pkIndexes = new int[pk.size()];
        int i = 0;
        for (DbAttribute attribute : pk) {
            pkNames[i] = attribute.getName();
            pkIndexes[i] = layout.indexOf(attribute.getName());
            i++;
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

        int[] attributeIndexes = new int[attributes.size()];
        for (i = 0; i < attributeIndexes.length; i++) {
            attributeIndexes[i] = layout.indexOf(attributes.get(i).getAttribute().getDbAttributePath().value());
        }

        int[][] toOneFkIndexes = new int[toOnes.size()][];
        String[][] toOneTargetNames = new String[toOnes.size()][];
        for (i = 0; i < toOneFkIndexes.length; i++) {
            ObjRelationship relationship = toOnes.get(i).getRelationship();
            // only an FK to the target PK lets the source snapshot tell whether the arc changed
            if (relationship.isSourceIndependentFromTargetChange()) {
                continue;
            }
            List<DbJoin> joins = relationship.getDbRelationships().getFirst().getJoins();
            int[] fkIndexes = new int[joins.size()];
            String[] targetNames = new String[joins.size()];
            for (int j = 0; j < fkIndexes.length; j++) {
                fkIndexes[j] = layout.indexOf(joins.get(j).getSourceName());
                targetNames[j] = joins.get(j).getTargetName();
            }
            toOneFkIndexes[i] = fkIndexes;
            toOneTargetNames[i] = targetNames;
        }

        return new MergePlan(layout, descriptor, pkNames, pkIndexes,
                attributes.toArray(new AttributeProperty[0]), attributeIndexes,
                toOnes.toArray(new ToOneProperty[0]), toOneFkIndexes, toOneTargetNames,
                toManys.toArray(new ToManyProperty[0]));
    }
}
