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
package org.apache.cayenne.merge;

import javax.sql.DataSource;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.validation.ValidationResult;

public class ExecutingMergerContext implements MergerContext {

    private DataMap map;
    private DataNode node;
    private ValidationResult result = new ValidationResult();
    private ModelMergeDelegate delegate;

    public ExecutingMergerContext(DataMap map, DataNode node) {
        this.map = map;
        this.node = node;
        this.delegate = new DefaultModelMergeDelegate();
    }

    public ExecutingMergerContext(DataMap map, DataSource dataSource, JdbcAdapter adapter,
            ModelMergeDelegate delegate) {
        this.map = map;
        // create a fake DataNode as lots of DbAdapter/PkGenerator methods
        // take a DataNode instead of just a DataSource
        this.node = new DataNode();
        this.node.setJdbcEventLogger(adapter.getJdbcEventLogger());
        this.node.setDataSource(dataSource);
        this.node.setAdapter(adapter);
        this.delegate = delegate;
    }

    public DbAdapter getAdapter() {
        return getDataNode().getAdapter();
    }

    public DataMap getDataMap() {
        return map;
    }

    public DataNode getDataNode() {
        return node;
    }

    public ValidationResult getValidationResult() {
        return result;
    }

    public ModelMergeDelegate getModelMergeDelegate() {
        return delegate;
    }

}
