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

import javax.persistence.spi.PersistenceUnitInfo;

import org.apache.cayenne.jpa.map.JpaEntityMap;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

/**
 * A common context shared by different loaders during JPA mapping information processing.
 * 
 */
public class EntityMapLoaderContext {

    protected ValidationResult conflicts;
    protected JpaEntityMap entityMap;
    protected PersistenceUnitInfo unit;
    protected ClassLoader tempClassLoader;

    public EntityMapLoaderContext(PersistenceUnitInfo unit) {
        this.unit = unit;
        this.conflicts = new ValidationResult();
        this.entityMap = new JpaEntityMap();
        this.tempClassLoader = unit.getNewTempClassLoader();
    }

    public PersistenceUnitInfo getUnit() {
        return unit;
    }

    public JpaEntityMap getEntityMap() {
        return entityMap;
    }

    public void recordConflict(ValidationFailure conflict) {
        conflicts.addFailure(conflict);
    }

    public ValidationResult getConflicts() {
        return conflicts;
    }

    public ClassLoader getTempClassLoader() {
        return tempClassLoader;
    }
}
