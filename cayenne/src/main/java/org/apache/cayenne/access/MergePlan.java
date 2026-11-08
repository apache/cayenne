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
import org.apache.cayenne.map.compiled.CompiledColumnPair;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledEmbeddedAttribute;
import org.apache.cayenne.map.compiled.CompiledAttribute;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.map.compiled.CompiledRelationship;
import org.apache.cayenne.map.compiled.CompiledToManyRelationship;
import org.apache.cayenne.map.compiled.CompiledToOneRelationship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The columns that turn rows into objects of one {@link CompiledEntity}: the primary key, the attributes and the
 * to-one foreign keys, by their positions in the entity's {@link CompiledEntity#snapshotLayout() snapshot
 * layout}, and a {@link DataRowLayout.Reader} of that layout, which reads a snapshot built from an object by position
 * and looks the positions up once per result set otherwise. A resolver builds a plan per entity for the rows it
 * converts.
 */
final class MergePlan {

    final CompiledEntity entity;

    final DataRowLayout layout;
    final DataRowLayout.Reader reader;

    // the primary key of the entity's DbEntity, PK i being column pkColumns[i]
    final String[] pkNames;
    final int[] pkColumns;

    // attribute i being column attributeColumns[i]
    final CompiledAttribute[] attributes;
    final int[] attributeColumns;

    final CompiledToOneRelationship[] toOnes;
    // per to-one, the columns of the FK join by join, or null when the FK does not define the arc
    final int[][] toOneFkColumns;
    // per to-one, the target PK column name of each join
    final String[][] toOneTargetNames;

    final CompiledToManyRelationship[] toManys;

    // the embedded attributes, whose embeddable objects must exist before the attributes are written
    final CompiledEmbeddedAttribute[] embeddedAttributes;

    private MergePlan(CompiledEntity entity, DataRowLayout layout, String[] pkNames, int[] pkColumns,
                      CompiledAttribute[] attributes, int[] attributeColumns, CompiledToOneRelationship[] toOnes,
                      int[][] toOneFkColumns, String[][] toOneTargetNames, CompiledToManyRelationship[] toManys,
                      CompiledEmbeddedAttribute[] embeddedAttributes) {
        this.entity = entity;
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
        this.embeddedAttributes = embeddedAttributes;
    }

    static MergePlan of(CompiledEntity entity) {

        DataRowLayout layout = entity.snapshotLayout();

        Collection<CompiledColumn> pk = entity.table().primaryKey();
        String[] pkNames = new String[pk.size()];
        int[] pkColumns = new int[pk.size()];
        int i = 0;
        for (CompiledColumn column : pk) {
            pkNames[i] = column.name();
            pkColumns[i++] = column(layout, column.name());
        }

        List<CompiledAttribute> attributes = new ArrayList<>(entity.attributes());
        List<CompiledToOneRelationship> toOnes = new ArrayList<>();
        List<CompiledToManyRelationship> toManys = new ArrayList<>();
        for (CompiledRelationship relationship : entity.relationships()) {
            switch (relationship) {
                case CompiledToOneRelationship toOne -> toOnes.add(toOne);
                case CompiledToManyRelationship toMany -> toManys.add(toMany);
            }
        }

        int[] attributeColumns = new int[attributes.size()];
        for (i = 0; i < attributeColumns.length; i++) {
            attributeColumns[i] = column(layout, attributes.get(i).dbPath().value());
        }

        int[][] toOneFkColumns = new int[toOnes.size()][];
        String[][] toOneTargetNames = new String[toOnes.size()][];
        for (i = 0; i < toOneFkColumns.length; i++) {
            CompiledToOneRelationship toOne = toOnes.get(i);
            // only an FK on the source lets the source snapshot tell whether the arc changed
            if (!toOne.isForeignKey()) {
                continue;
            }
            List<CompiledColumnPair> joins = toOne.joins().getFirst().columnPairs();
            int[] fkColumns = new int[joins.size()];
            String[] targetNames = new String[joins.size()];
            for (int j = 0; j < fkColumns.length; j++) {
                fkColumns[j] = column(layout, joins.get(j).source().name());
                targetNames[j] = joins.get(j).target().name();
            }
            toOneFkColumns[i] = fkColumns;
            toOneTargetNames[i] = targetNames;
        }

        return new MergePlan(entity, layout, pkNames, pkColumns,
                attributes.toArray(new CompiledAttribute[0]), attributeColumns,
                toOnes.toArray(new CompiledToOneRelationship[0]), toOneFkColumns, toOneTargetNames,
                toManys.toArray(new CompiledToManyRelationship[0]),
                entity.embeddedAttributes().toArray(new CompiledEmbeddedAttribute[0]));
    }


    private static int column(DataRowLayout layout, String name) {
        int column = layout.indexOf(name);
        if (column < 0) {
            throw new CayenneRuntimeException("Column '%s' is not in the snapshot layout %s", name, layout);
        }
        return column;
    }
}
