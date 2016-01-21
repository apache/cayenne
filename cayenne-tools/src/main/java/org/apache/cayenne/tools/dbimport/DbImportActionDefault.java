/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.tools.dbimport;

import org.apache.cayenne.access.DbLoader;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.DbAdapterFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.merge.AbstractToModelToken;
import org.apache.cayenne.merge.AddRelationshipToDb;
import org.apache.cayenne.merge.DbMerger;
import org.apache.cayenne.merge.ExecutingMergerContext;
import org.apache.cayenne.merge.MergerContext;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.merge.MergerToken;
import org.apache.cayenne.merge.ModelMergeDelegate;
import org.apache.cayenne.merge.ProxyModelMergeDelegate;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.validation.SimpleValidationFailure;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;
import org.apache.commons.logging.Log;
import org.xml.sax.InputSource;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * A thin wrapper around {@link DbLoader} that encapsulates DB import logic for
 * the benefit of Ant and Maven db importers.
 *
 * @since 4.0
 */
public class DbImportActionDefault implements DbImportAction {

    private final ProjectSaver projectSaver;
    private final Log logger;
    private final DataSourceFactory dataSourceFactory;
    private final DbAdapterFactory adapterFactory;
    private final MapLoader mapLoader;

    public DbImportActionDefault(@Inject Log logger,
                                 @Inject ProjectSaver projectSaver,
                                 @Inject DataSourceFactory dataSourceFactory,
                                 @Inject DbAdapterFactory adapterFactory,
                                 @Inject MapLoader mapLoader) {
        this.logger = logger;
        this.projectSaver = projectSaver;
        this.dataSourceFactory = dataSourceFactory;
        this.adapterFactory = adapterFactory;
        this.mapLoader = mapLoader;
    }

    public void execute(DbImportConfiguration config) throws Exception {

        if (logger.isDebugEnabled()) {
            logger.debug("DB connection: " + config.getDataSourceInfo());
        }

        if (logger.isDebugEnabled()) {
            logger.debug(config);
        }

        DataNodeDescriptor dataNodeDescriptor = config.createDataNodeDescriptor();
        DataSource dataSource = dataSourceFactory.getDataSource(dataNodeDescriptor);
        DbAdapter adapter = adapterFactory.createAdapter(dataNodeDescriptor, dataSource);

        DataMap loadedFomDb = load(config, adapter, dataSource.getConnection());
        if (loadedFomDb == null) {
            logger.info("Nothing was loaded from db.");
            return;
        }

        DataMap existing = loadExistingDataMap(config.getDataMapFile());
        if (existing == null) {
            logger.info("");
            File file = config.getDataMapFile();
            logger.info("Map file does not exist. Loaded db model will be saved into '"
                    + (file == null ? "null" : file.getAbsolutePath() + "'"));

            saveLoaded(config.initializeDataMap(loadedFomDb));
        } else {
            MergerFactory mergerFactory = adapter.mergerFactory();

            List<MergerToken> mergeTokens = new DbMerger(mergerFactory)
                    .createMergeTokens(existing, loadedFomDb, config.getDbLoaderConfig());
            if (mergeTokens.isEmpty()) {
                logger.info("");
                logger.info("Detected changes: No changes to import.");
                return;
            }

            if (!isBlank(config.getDefaultPackage())) {
                existing.setDefaultPackage(config.getDefaultPackage());
            }

            final Collection<ObjEntity> loadedObjEntities = new LinkedList<>();
            DataMap executed = execute(new ProxyModelMergeDelegate(config.createMergeDelegate()) {
                @Override
                public void objEntityAdded(ObjEntity ent) {
                    loadedObjEntities.add(ent);

                    super.objEntityAdded(ent);
                }

            }, existing, log(sort(reverse(mergerFactory, mergeTokens))));

            DbLoader.flattenManyToManyRelationships(executed, loadedObjEntities, config.getNameGenerator());

            relationshipsSanity(executed);


            saveLoaded(executed);
        }
    }

    private void relationshipsSanity(DataMap executed) {
        for (ObjEntity objEntity : executed.getObjEntities()) {
            List<ObjRelationship> rels = new LinkedList<>(objEntity.getRelationships());
            for (ObjRelationship rel : rels) {
                if (rel.getSourceEntity() == null || rel.getTargetEntity() == null) {
                    logger.error("Incorrect obj relationship source or target entity is null: " + rel);

                    objEntity.removeRelationship(rel.getName());
                }
            }
        }
    }

    protected static List<MergerToken> sort(List<MergerToken> reverse) {
        Collections.sort(reverse, new Comparator<MergerToken>() {
            @Override
            public int compare(MergerToken o1, MergerToken o2) {
                if (o1 instanceof AddRelationshipToDb && o2 instanceof AddRelationshipToDb) {
                    return 0;
                }

                if (!(o1 instanceof AddRelationshipToDb || o2 instanceof AddRelationshipToDb)) {
                    return o1.getClass().getSimpleName().compareTo(o2.getClass().getSimpleName());
                }

                return o1 instanceof AddRelationshipToDb ? 1 : -1;
            }
        });

        return reverse;
    }

    private Collection<MergerToken> log(List<MergerToken> tokens) {
        logger.info("");
        if (tokens.isEmpty()) {
            logger.info("Detected changes: No changes to import.");
            return tokens;
        }

        logger.info("Detected changes: ");
        for (MergerToken token : tokens) {
            logger.info(String.format("    %-20s %s", token.getTokenName(), token.getTokenValue()));
        }
        logger.info("");

        return tokens;
    }

    protected DataMap loadExistingDataMap(File dataMapFile) throws IOException {
        if (dataMapFile != null && dataMapFile.exists() && dataMapFile.canRead()) {
            DataMap dataMap = mapLoader.loadDataMap(new InputSource(dataMapFile.getCanonicalPath()));
            dataMap.setNamespace(new EntityResolver(Collections.singleton(dataMap)));
            dataMap.setConfigurationSource(new URLResource(dataMapFile.toURI().toURL()));

            return dataMap;
        }

        return null;
    }

    private List<MergerToken> reverse(MergerFactory mergerFactory, Iterable<MergerToken> mergeTokens) throws IOException {
        List<MergerToken> tokens = new LinkedList<>();
        for (MergerToken token : mergeTokens) {
            if (token instanceof AbstractToModelToken) {
                continue;
            }
            tokens.add(token.createReverse(mergerFactory));
        }
        return tokens;
    }

    /**
     * Performs configured schema operations via DbGenerator.
     */
    private DataMap execute(ModelMergeDelegate mergeDelegate, DataMap dataMap, Collection<MergerToken> tokens) {
        MergerContext mergerContext = new ExecutingMergerContext(
                dataMap, null, null, mergeDelegate);

        for (MergerToken tok : tokens) {
            try {
                tok.execute(mergerContext);
            } catch (Throwable th) {
                String message = "Migration Error. Can't apply changes from token: " + tok.getTokenName()
                        + " (" + tok.getTokenValue() + ")";

                logger.error(message, th);
                mergerContext.getValidationResult().addFailure(new SimpleValidationFailure(th, message));
            }
        }

        ValidationResult failures = mergerContext.getValidationResult();
        if (failures == null || !failures.hasFailures()) {
            logger.info("Migration Complete Successfully.");
        } else {
            logger.info("Migration Complete.");
            logger.warn("Migration finished. The following problem(s) were ignored.");
            for (ValidationFailure failure : failures.getFailures()) {
                logger.warn(failure.toString());
            }
        }

        return dataMap;
    }

    private DbLoader getLoader(DbImportConfiguration config, DbAdapter adapter, Connection connection) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return config.createLoader(adapter, connection, config.createLoaderDelegate());
    }

    protected void saveLoaded(DataMap dataMap) throws FileNotFoundException {
        ConfigurationTree<DataMap> projectRoot = new ConfigurationTree<>(dataMap);
        Project project = new Project(projectRoot);
        projectSaver.save(project);
    }

    protected DataMap load(DbImportConfiguration config, DbAdapter adapter, Connection connection) throws Exception {
        DataMap dataMap = config.createDataMap();

        try {
            DbLoader loader = config.createLoader(adapter, connection, config.createLoaderDelegate());
            loader.load(dataMap, config.getDbLoaderConfig());
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        return dataMap;
    }
}