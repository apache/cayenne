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
package org.apache.cayenne.configuration;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.dbsync.CreateIfNoSchemaStrategy;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.access.dbsync.ThrowOnPartialSchemaStrategy;
import org.apache.cayenne.dba.DbAdapter;

import javax.sql.DataSource;

/**
 * Contains {@link DataNode} configuration.
 *
 * @since 5.0
 */
public record DataNodeDescriptor(
        String name,
        DataSource dataSource,
        Class<? extends DbAdapter> adapterType,
        Class<? extends SchemaUpdateStrategy> schemaUpdateStrategyType) {

    /**
     * @since 5.0
     */
    public static Builder of(String name) {
        return new Builder(name);
    }

    /**
     * @since 5.0
     */
    public static class Builder {

        private final String name;
        private DataSource dataSource;
        private Class<? extends DbAdapter> adapterType;
        private Class<? extends SchemaUpdateStrategy> schemaUpdateStrategyType;

        public Builder(String name) {
            this.name = name;
        }

        public Builder adapter(Class<? extends DbAdapter> adapterType) {
            this.adapterType = adapterType;
            return this;
        }

        public Builder dataSource(DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        public Builder schemaUpdateStrategy(Class<? extends SchemaUpdateStrategy> schemaUpdateStrategyType) {
            this.schemaUpdateStrategyType = schemaUpdateStrategyType;
            return this;
        }

        public Builder createSchemaIfNeeded() {
            return schemaUpdateStrategy(CreateIfNoSchemaStrategy.class);
        }

        public Builder throwOnPartialSchema() {
            return schemaUpdateStrategy(ThrowOnPartialSchemaStrategy.class);
        }

        public DataNodeDescriptor build() {
            return new DataNodeDescriptor(
                    name,
                    dataSource,
                    adapterType,
                    schemaUpdateStrategyType);
        }
    }
}
