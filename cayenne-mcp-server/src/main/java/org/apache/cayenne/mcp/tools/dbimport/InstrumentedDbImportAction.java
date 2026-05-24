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
package org.apache.cayenne.mcp.tools.dbimport;

import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.runtime.DataSourceFactory;
import org.apache.cayenne.configuration.runtime.DbAdapterFactory;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.merge.token.model.AddRelationshipToModel;
import org.apache.cayenne.dbsync.merge.token.model.CreateTableToModel;
import org.apache.cayenne.dbsync.merge.token.model.DropTableToModel;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactoryProvider;
import org.apache.cayenne.dbsync.reverse.dbimport.DefaultDbImportAction;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.mcp.tools.dbimport.protocol.DbImportSummary;
import org.apache.cayenne.project.ProjectSaver;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Extends {@link DefaultDbImportAction} to capture the merger token list for summary
 * reporting. Overrides {@link #log(List)} — the single protected hook that receives
 * the final, sorted token list immediately before application.
 *
 * @since 5.0
 */
public class InstrumentedDbImportAction extends DefaultDbImportAction {

    private List<MergerToken> capturedTokens = List.of();

    public InstrumentedDbImportAction(
            @Inject Logger logger,
            @Inject ProjectSaver projectSaver,
            @Inject DataSourceFactory dataSourceFactory,
            @Inject DbAdapterFactory adapterFactory,
            @Inject DataMapLoader mapLoader,
            @Inject MergerTokenFactoryProvider mergerTokenFactoryProvider,
            @Inject DataChannelDescriptorLoader dataChannelDescriptorLoader,
            @Inject DataChannelMetaData metaData) {
        super(logger, projectSaver, dataSourceFactory, adapterFactory, mapLoader,
                mergerTokenFactoryProvider, dataChannelDescriptorLoader, metaData);
    }

    @Override
    protected Collection<MergerToken> log(List<MergerToken> tokens) {
        capturedTokens = List.copyOf(tokens);
        return super.log(tokens);
    }

    /**
     * Builds a {@link DbImportSummary} from the captured token list.
     *
     * @param tokensApplied number of tokens actually applied (equals tokensConsidered on
     *                      a successful run, 0 if an exception aborted before the apply phase)
     */
    public DbImportSummary buildSummary(int tokensApplied) {
        int entitiesAdded = 0;
        int entitiesRemoved = 0;
        int relationshipsAdded = 0;
        Set<String> modifiedEntities = new HashSet<>();

        Set<String> addedEntityNames = new HashSet<>();
        for (MergerToken token : capturedTokens) {
            switch (token) {
                case CreateTableToModel ignored -> {
                    entitiesAdded++;
                    addedEntityNames.add(token.getTokenValue());
                }
                case DropTableToModel ignored -> entitiesRemoved++;
                case AddRelationshipToModel ignored -> relationshipsAdded++;
                default -> {
                    // Column-level and other entity-scoped tokens: tokenValue is "entity.column" or "entity"
                    String tv = token.getTokenValue();
                    int dot = tv.indexOf('.');
                    modifiedEntities.add(dot >= 0 ? tv.substring(0, dot) : tv);
                }
            }
        }

        // Entities added this run don't count as modified
        modifiedEntities.removeAll(addedEntityNames);

        return new DbImportSummary(
                capturedTokens.size(),
                tokensApplied,
                entitiesAdded,
                entitiesRemoved,
                modifiedEntities.size(),
                relationshipsAdded
        );
    }

    public List<MergerToken> getCapturedTokens() {
        return capturedTokens;
    }
}
