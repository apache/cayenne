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

package org.apache.cayenne.dbsync.merge.context;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.filter.NameFilter;
import org.apache.cayenne.dbsync.filter.NamePatternMatcher;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.dbsync.naming.NoStemStemmer;
import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.dbsync.reverse.dbload.DefaultModelMergeDelegate;
import org.apache.cayenne.dbsync.reverse.dbload.ModelMergeDelegate;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.validation.ValidationResult;

import javax.sql.DataSource;
import java.util.Objects;

/**
 * An object passed as an argument to {@link MergerToken#execute(MergerContext)}s that a
 * {@link MergerToken} can do its work.
 */
public class MergerContext {

    private DataMap dataMap;
    private DataNode dataNode;
    private ValidationResult validationResult;
    private ModelMergeDelegate delegate;
    private EntityMergeSupport entityMergeSupport;
    private ObjectNameGenerator nameGenerator;

    protected MergerContext() {
    }

    /**
     * @since 4.0
     */
    public static Builder builder(DataMap dataMap) {
        return new Builder(dataMap);
    }

    /**
     * @since 4.0
     */
    public EntityMergeSupport getEntityMergeSupport() {
        return entityMergeSupport;
    }

    /**
     * Returns the DataMap that is the target of a the merge operation.
     *
     * @return the DataMap that is the target of a the merge operation.
     */
    public DataMap getDataMap() {
        return dataMap;
    }

    public DataNode getDataNode() {
        return dataNode;
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }

    /**
     * Returns a callback object that is invoked as the merge proceeds through tokens, modifying the DataMap.
     *
     * @return a callback object that is invoked as the merge proceeds through tokens, modifying the DataMap.
     * @since 4.0
     */
    public ModelMergeDelegate getDelegate() {
        return delegate;
    }

    /**
     * @since 4.0
     */
    public ObjectNameGenerator getNameGenerator() {
        return nameGenerator;
    }

    public static class Builder {

        private MergerContext context;
        private boolean usingPrimitives;
        private NameFilter meaningfulPKsFilter;

        private Builder(DataMap dataMap) {
            this.context = new MergerContext();
            this.context.dataMap = Objects.requireNonNull(dataMap);
            this.context.validationResult = new ValidationResult();
        }

        public MergerContext build() {

            // init missing defaults ...

            if (context.delegate == null) {
                delegate(new DefaultModelMergeDelegate());
            }

            if (context.dataNode == null) {
                dataNode(new DataNode());
            }

            if(context.nameGenerator == null) {
                context.nameGenerator = new DefaultObjectNameGenerator(NoStemStemmer.getInstance());
            }

            if(meaningfulPKsFilter == null) {
                meaningfulPKsFilter = NamePatternMatcher.EXCLUDE_ALL;
            }

            context.entityMergeSupport = new EntityMergeSupport(context.nameGenerator,
                    meaningfulPKsFilter,
                    true,
                    usingPrimitives);

            return context;
        }

        public Builder delegate(ModelMergeDelegate delegate) {
            context.delegate = Objects.requireNonNull(delegate);
            return this;
        }

        public Builder nameGenerator(ObjectNameGenerator nameGenerator) {
            this.context.nameGenerator = Objects.requireNonNull(nameGenerator);
            return this;
        }

        public Builder usingPrimitives(boolean flag) {
            this.usingPrimitives = flag;
            return this;
        }

        public Builder dataNode(DataNode dataNode) {
            this.context.dataNode = Objects.requireNonNull(dataNode);
            return this;
        }

        public Builder meaningfulPKFilter(NameFilter filter) {
            this.meaningfulPKsFilter = Objects.requireNonNull(filter);
            return this;
        }

        public Builder syntheticDataNode(DataSource dataSource, DbAdapter adapter) {
            DataNode dataNode = new DataNode();
            dataNode.setDataSource(dataSource);
            dataNode.setAdapter(adapter);
            return dataNode(dataNode);
        }
    }
}
