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

package org.apache.cayenne;

import java.util.Map;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.validation.ValidationResult;

/**
 */
public class MockDataObject implements DataObject {

    protected ObjectId objectId;
    protected int persistenceState;
    protected DataContext context;
    protected ObjectContext objectContext;

    public MockDataObject() {

    }

    public MockDataObject(DataContext context, ObjectId id, int persistenceState) {
        this.context = context;
        this.objectId = id;
        this.persistenceState = persistenceState;
    }

    public DataContext getDataContext() {
        return context;
    }

    public void setDataContext(DataContext context) {
        this.context = context;
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    public void setObjectId(ObjectId objectId) {
        this.objectId = objectId;
    }

    public int getPersistenceState() {
        return persistenceState;
    }

    public void setPersistenceState(int newState) {
        this.persistenceState = newState;
    }

    public void writePropertyDirectly(String propertyName, Object val) {
    }

    public Object readPropertyDirectly(String propertyName) {
        return null;
    }

    public Object readNestedProperty(String path) {
        return null;
    }

    public Object readProperty(String propName) {
        return null;
    }

    public void writeProperty(String propName, Object val) {
    }

    public DataObject readToOneDependentTarget(String relName) {
        return null;
    }

    public void addToManyTarget(String relName, DataObject val, boolean setReverse) {
    }

    public void removeToManyTarget(String relName, DataObject val, boolean setReverse) {
    }

    public void setToOneTarget(String relName, DataObject val, boolean setReverse) {
    }

    public void setToOneDependentTarget(String relName, DataObject val) {
    }

    public Map getCommittedSnapshot() {
        return null;
    }

    public Map getCurrentSnapshot() {
        return null;
    }

    public void fetchFinished() {
    }

    public long getSnapshotVersion() {
        return 0;
    }

    public void setSnapshotVersion(long snapshotVersion) {
    }

    public void resolveFault() {
    }

    public void validateForInsert(ValidationResult validationResult) {
    }

    public void validateForUpdate(ValidationResult validationResult) {
    }

    public void validateForDelete(ValidationResult validationResult) {
    }

    public ObjectContext getObjectContext() {
        return objectContext;
    }

    public void setObjectContext(ObjectContext objectContext) {
        this.objectContext = objectContext;
    }
}
