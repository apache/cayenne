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


package org.apache.cayenne.jpa.map;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.EntityResult;

import org.apache.cayenne.util.TreeNodeChild;

public class JpaEntityResult {

    protected String entityClassName;
    protected String discriminatorColumn;
    protected Collection<JpaFieldResult> fieldResults;

    public JpaEntityResult() {

    }

    public JpaEntityResult(EntityResult annotation) {
        entityClassName = annotation.entityClass().getName();
        discriminatorColumn = annotation.discriminatorColumn();

        getFieldResults();
        for (int i = 0; i < annotation.fields().length; i++) {
            fieldResults.add(new JpaFieldResult(annotation.fields()[i]));
        }
    }

    @TreeNodeChild(type=JpaFieldResult.class)
    public Collection<JpaFieldResult> getFieldResults() {
        if (fieldResults == null) {
            fieldResults = new ArrayList<JpaFieldResult>();
        }

        return fieldResults;
    }

    public String getDiscriminatorColumn() {
        return discriminatorColumn;
    }

    public void setDiscriminatorColumn(String descriminatorColumn) {
        this.discriminatorColumn = descriminatorColumn;
    }

    public String getEntityClassName() {
        return entityClassName;
    }

    public void setEntityClassName(String entityClassName) {
        this.entityClassName = entityClassName;
    }
}
