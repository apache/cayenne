/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/


package org.apache.cayenne.jpa.conf;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.apache.cayenne.jpa.JpaProviderException;

/**
 * A utility class that provides access to default JPA annotation instances.
 * 
 */
@Table
abstract class AnnotationPrototypes {

    static final Column column;
    static final JoinColumn joinColumn;
    static final Table table;

    static {

        table = AnnotationPrototypes.class.getAnnotation(Table.class);

        try {
            column = AnnotationPrototypes.class
                    .getDeclaredField("annotatedColumn")
                    .getAnnotation(Column.class);

            joinColumn = AnnotationPrototypes.class.getDeclaredField(
                    "annotatedJoinColumn").getAnnotation(JoinColumn.class);
        }
        catch (NoSuchFieldException e) {
            throw new JpaProviderException("No annotated field found", e);
        }
    }

    @Column
    Object annotatedColumn;

    @JoinColumn
    Object annotatedJoinColumn;

    public static Column getColumn() {
        return column;
    }

    public static JoinColumn getJoinColumn() {
        return joinColumn;
    }

    public static Table getTable() {
        return table;
    }
}
