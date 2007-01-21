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

package org.apache.cayenne.access;

import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.query.Query;

/**
 * Default implementation of DataContextDelegate that serves as a superclass for mockup
 * test delegates.
 * 
 * @author Andrus Adamchik
 */
public class MockDataContextDelegate implements DataContextDelegate {

    public Query willPerformGenericQuery(DataContext context, Query query) {
        return query;
    }

    public Query willPerformQuery(DataContext context, Query query) {
        return query;
    }

    public boolean shouldMergeChanges(DataObject object, DataRow snapshotInStore) {
        return true;
    }

    public boolean shouldProcessDelete(DataObject object) {
        return true;
    }

    public void finishedMergeChanges(DataObject object) {

    }

    public void finishedProcessDelete(DataObject object) {

    }
}
