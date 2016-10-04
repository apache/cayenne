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

import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.DbAdapterFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.filter.NameFilter;
import org.apache.cayenne.dbsync.merge.AbstractToModelToken;
import org.apache.cayenne.dbsync.merge.AddRelationshipToDb;
import org.apache.cayenne.dbsync.merge.DbMerger;
import org.apache.cayenne.dbsync.merge.MergerContext;
import org.apache.cayenne.dbsync.merge.MergerToken;
import org.apache.cayenne.dbsync.merge.ModelMergeDelegate;
import org.apache.cayenne.dbsync.merge.ProxyModelMergeDelegate;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactoryProvider;
import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.dbsync.reverse.db.DbLoader;
import org.apache.cayenne.dbsync.reverse.db.DbLoaderConfiguration;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
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

/**
 * A thin wrapper around {@link DbLoader} that encapsulates DB import logic for
 * the benefit of Ant and Maven db importers.
 *
 * @since 4.0
 */
public class DefaultDbImportAction implements DbImportAction {

    private final ProjectSaver projectSaver;
    private final Log logger;
    private final DataSourceFactory dataSourceFactory;
    private final DbAdapterFactory adapterFactory;
    private final MapLoader mapLoader;
    private final MergerTokenFactoryProvider mergerTokenFactoryProvider;

    public DefaultDbImportAction(@Inject Log logger,
                                 @Inject ProjectSaver projectSaver,
                                 @Inject DataSourceFactory dataSourceFactory,
                                 @Inject DbAdapterFactory adapterFactory,
                                 @Inject MapLoader mapLoader,
                                 @Inject MergerTokenFactoryProvider mergerTokenFactoryProvider) {
        this.logger = logger;
        this.projectSaver = projectSaver;
        this.dataSourceFactory = dataSourceFactory;
        this.adapterFactory = adapterFactory;
        this.mapLoader = mapLoader;
        this.mergerTokenFactoryProvider = mergerTokenFactoryProvider;
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

    @Override
    public void execute(DbImportConfiguration config) throws Exception {

        if (logger.isDebugEnabled()) {
            logger.debug("DB connection: " + config.getDataSourceInfo());
            logger.debug(config);
        }

        boolean hasChanges = false;
        DataNodeDescriptor dataNodeDescriptor = config.createDataNodeDescriptor();
        DataSource dataSource = dataSourceFactory.getDataSource(dataNodeDescriptor);
        DbAdapter adapter = adapterFactory.createAdapter(dataNodeDescriptor, dataSource);

        DataMap loadedFomDb;
        try (Connection connection = dataSource.getConnection()) {
            loadedFomDb = load(config, adapter, connection);
        }

        if (loadedFomDb == null) {
            logger.info("Nothing was loaded from db.");
            return;
        }

        DataMap targetDataMap = loadExistingDataMap(config.getDataMapFile());
        if (targetDataMap == null) {

            hasChanges = true;
            File file = config.getDataMapFile();
            logger.info("");
            logger.info("Map file does not exist. Loaded db model will be saved into '"
                    + (file == null ? "null" : file.getAbsolutePath() + "'"));

            targetDataMap = config.createDataMap();
        }

        MergerTokenFactory mergerTokenFactory = mergerTokenFactoryProvider.get(adapter);

        DbLoaderConfiguration loaderConfig = config.getDbLoaderConfig();
        List<MergerToken> tokens = DbMerger.builder(mergerTokenFactory)
                .filters(loaderConfig.getFiltersConfig())
                .skipPKTokens(loaderConfig.isSkipPrimaryKeyLoading())
                .skipRelationshipsTokens(loaderConfig.isSkipRelationshipsLoading())
                .build()
                .createMergeTokens(targetDataMap, loadedFomDb);

        hasChanges |= syncDataMapProperties(targetDataMap, config);
        hasChanges |= applyTokens(config.createMergeDelegate(),
                targetDataMap,
                log(sort(reverse(mergerTokenFactory, tokens))),
                config.getNameGenerator(),
                config.getMeaningfulPKFilter(),
                config.isUsePrimitives());

        if (hasChanges) {
            saveLoaded(targetDataMap);
        }
    }

    private boolean syncDataMapProperties(DataMap targetDataMap, DbImportConfiguration config) {

        String defaultPackage = config.getDefaultPackage();
        if (defaultPackage == null || defaultPackage.trim().length() == 0) {
            return false;
        }

        if (defaultPackage.equals(targetDataMap.getDefaultPackage())) {
            return false;
        }

        targetDataMap.setDefaultPackage(defaultPackage);
        return true;
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

    private List<MergerToken> reverse(MergerTokenFactory mergerTokenFactory, Iterable<MergerToken> mergeTokens)
            throws IOException {

        List<MergerToken> tokens = new LinkedList<>();
        for (MergerToken token : mergeTokens) {
            if (token instanceof AbstractToModelToken) {
                continue;
            }
            tokens.add(token.createReverse(mergerTokenFactory));
        }
        return tokens;
    }

    private boolean applyTokens(ModelMergeDelegate mergeDelegate,
                                DataMap targetDataMap,
                                Collection<MergerToken> tokens,
                                ObjectNameGenerator nameGenerator,
                                NameFilter meaningfulPKFilter,
                                boolean usingPrimitives) {

        if (tokens.isEmpty()) {
            logger.info("");
            logger.info("Detected changes: No changes to import.");
            return false;
        }

        final Collection<ObjEntity> loadedObjEntities = new LinkedList<>();

        mergeDelegate = new ProxyModelMergeDelegate(mergeDelegate) {
            @Override
            public void objEntityAdded(ObjEntity ent) {
                loadedObjEntities.add(ent);
                super.objEntityAdded(ent);
            }
        };

        MergerContext mergerContext = MergerContext.builder(targetDataMap)
                .delegate(mergeDelegate)
                .nameGenerator(nameGenerator)
                .usingPrimitives(usingPrimitives)
                .meaningfulPKFilter(meaningfulPKFilter)
                .build();

        for (MergerToken token : tokens) {
            try {
                token.execute(mergerContext);
            } catch (Throwable th) {
                String message = "Migration Error. Can't apply changes from token: " + token.getTokenName()
                        + " (" + token.getTokenValue() + ")";

                logger.error(message, th);
                mergerContext.getValidationResult().addFailure(new SimpleValidationFailure(th, message));
            }
        }

        ValidationResult failures = mergerContext.getValidationResult();
        if (failures.hasFailures()) {
            logger.info("Migration Complete.");
            logger.warn("Migration finished. The following problem(s) were encountered and ignored.");
            for (ValidationFailure failure : failures.getFailures()) {
                logger.warn(failure.toString());
            }
        } else {
            logger.info("Migration Complete Successfully.");
        }

        flattenManyToManyRelationships(targetDataMap, loadedObjEntities, nameGenerator);
        relationshipsSanity(targetDataMap);
        return true;
    }

    protected void saveLoaded(DataMap dataMap) throws FileNotFoundException {
        ConfigurationTree<DataMap> projectRoot = new ConfigurationTree<>(dataMap);
        Project project = new Project(projectRoot);
        projectSaver.save(project);
    }

    protected DataMap load(DbImportConfiguration config, DbAdapter adapter, Connection connection) throws Exception {
        DataMap dataMap = config.createDataMap();
        DbLoader loader = config.createLoader(adapter, connection, config.createLoaderDelegate());
        loader.load(dataMap, config.getDbLoaderConfig());
        return dataMap;
    }

    /**
     * Flattens many-to-many relationships in the generated model.
     */
    protected static void flattenManyToManyRelationships(DataMap map, Collection<ObjEntity> loadedObjEntities,
                                                      ObjectNameGenerator objectNameGenerator) {
        if (loadedObjEntities.isEmpty()) {
            return;
        }
        Collection<ObjEntity> entitiesForDelete = new LinkedList<>();

        for (ObjEntity curEntity : loadedObjEntities) {
            ManyToManyCandidateEntity entity = ManyToManyCandidateEntity.build(curEntity);

            if (entity != null) {
                entity.optimizeRelationships(objectNameGenerator);
                entitiesForDelete.add(curEntity);
            }
        }

        // remove needed entities
        for (ObjEntity curDeleteEntity : entitiesForDelete) {
            map.removeObjEntity(curDeleteEntity.getName(), true);
        }
        loadedObjEntities.removeAll(entitiesForDelete);
    }
}