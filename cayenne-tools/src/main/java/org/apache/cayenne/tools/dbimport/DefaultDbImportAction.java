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
import org.apache.cayenne.dbsync.reverse.db.DbLoaderDelegate;
import org.apache.cayenne.dbsync.reverse.filters.CatalogFilter;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.dbsync.reverse.filters.PatternFilter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
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
 * A default implementation of {@link DbImportAction} that can load DB schema and merge it to a new or an existing
 * DataMap.
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
        ObjectNameGenerator objectNameGenerator = config.createNameGenerator();

        DataMap sourceDataMap;
        try (Connection connection = dataSource.getConnection()) {
            sourceDataMap = load(config, adapter, connection, objectNameGenerator);
        }

        DataMap targetDataMap = existingTargetMap(config);
        if (targetDataMap == null) {

            String path = config.getTargetDataMap() == null ? "null" : config.getTargetDataMap().getAbsolutePath() + "'";

            logger.info("");
            logger.info("Map file does not exist. Loaded db model will be saved into '" + path);

            hasChanges = true;
            targetDataMap = newTargetDataMap(config);
        }

        // transform source DataMap before merging
        transformSourceBeforeMerge(sourceDataMap, targetDataMap, config);

        MergerTokenFactory mergerTokenFactory = mergerTokenFactoryProvider.get(adapter);

        DbLoaderConfiguration loaderConfig = config.getDbLoaderConfig();
        List<MergerToken> tokens = DbMerger.builder(mergerTokenFactory)
                .filters(loaderConfig.getFiltersConfig())
                .skipPKTokens(loaderConfig.isSkipPrimaryKeyLoading())
                .skipRelationshipsTokens(loaderConfig.isSkipRelationshipsLoading())
                .build()
                .createMergeTokens(targetDataMap, sourceDataMap);

        hasChanges |= syncDataMapProperties(targetDataMap, config);
        hasChanges |= applyTokens(config.createMergeDelegate(),
                targetDataMap,
                log(sort(reverse(mergerTokenFactory, tokens))),
                objectNameGenerator,
                config.createMeaningfulPKFilter(),
                config.isUsePrimitives());
        hasChanges |= syncProcedures(targetDataMap, sourceDataMap, loaderConfig.getFiltersConfig());

        if (hasChanges) {
            saveLoaded(targetDataMap);
        }
    }


    protected void transformSourceBeforeMerge(DataMap sourceDataMap,
                                              DataMap targetDataMap,
                                              DbImportConfiguration configuration) {

        if (configuration.isForceDataMapCatalog()) {
            String catalog = targetDataMap.getDefaultCatalog();
            for (DbEntity e : sourceDataMap.getDbEntities()) {
                e.setCatalog(catalog);
            }
        }

        if (configuration.isForceDataMapSchema()) {
            String schema = targetDataMap.getDefaultSchema();
            for (DbEntity e : sourceDataMap.getDbEntities()) {
                e.setSchema(schema);
            }
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

    protected DataMap existingTargetMap(DbImportConfiguration configuration) throws IOException {

        File file = configuration.getTargetDataMap();
        if (file != null && file.exists() && file.canRead()) {
            DataMap dataMap = mapLoader.loadDataMap(new InputSource(file.getCanonicalPath()));
            dataMap.setNamespace(new EntityResolver(Collections.singleton(dataMap)));
            dataMap.setConfigurationSource(new URLResource(file.toURI().toURL()));

            return dataMap;
        }

        return null;
    }

    protected DataMap newTargetDataMap(DbImportConfiguration config) throws IOException {

        DataMap dataMap = new DataMap();

        dataMap.setName(config.getDataMapName());
        dataMap.setConfigurationSource(new URLResource(config.getTargetDataMap().toURI().toURL()));
        dataMap.setNamespace(new EntityResolver(Collections.singleton(dataMap)));

        // update map defaults

        // do not override default package of existing DataMap unless it is
        // explicitly requested by the plugin caller
        String defaultPackage = config.getDefaultPackage();
        if (defaultPackage != null && defaultPackage.length() > 0) {
            dataMap.setDefaultPackage(defaultPackage);
        }

        CatalogFilter[] catalogs = config.getDbLoaderConfig().getFiltersConfig().getCatalogs();
        if (catalogs.length > 0) {
            // do not override default catalog of existing DataMap unless it is
            // explicitly requested by the plugin caller, and the provided catalog is
            // not a pattern
            String catalog = catalogs[0].name;
            if (catalog != null && catalog.length() > 0 && catalog.indexOf('%') < 0) {
                dataMap.setDefaultCatalog(catalog);
            }

            // do not override default schema of existing DataMap unless it is
            // explicitly requested by the plugin caller, and the provided schema is
            // not a pattern
            String schema = catalogs[0].schemas[0].name;
            if (schema != null && schema.length() > 0 && schema.indexOf('%') < 0) {
                dataMap.setDefaultSchema(schema);
            }
        }

        return dataMap;
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

    private boolean syncProcedures(DataMap targetDataMap, DataMap loadedDataMap, FiltersConfig filters) {
        Collection<Procedure> procedures = loadedDataMap.getProcedures();
        if (procedures.isEmpty()) {
            return false;
        }

        boolean hasChanges = false;
        for (Procedure procedure : procedures) {
            PatternFilter proceduresFilter = filters.proceduresFilter(procedure.getCatalog(), procedure.getSchema());
            if (proceduresFilter == null || !proceduresFilter.isIncluded(procedure.getName())) {
                continue;
            }

            Procedure oldProcedure = targetDataMap.getProcedure(procedure.getName());
            // maybe we need to compare oldProcedure's and procedure's fully qualified names?
            if (oldProcedure != null) {
                targetDataMap.removeProcedure(procedure.getName());
                logger.info("Replace procedure " + procedure.getName());
            } else {
                logger.info("Add new procedure " + procedure.getName());
            }
            targetDataMap.addProcedure(procedure);
            hasChanges = true;
        }
        return hasChanges;
    }

    protected void saveLoaded(DataMap dataMap) throws FileNotFoundException {
        ConfigurationTree<DataMap> projectRoot = new ConfigurationTree<>(dataMap);
        Project project = new Project(projectRoot);
        projectSaver.save(project);
    }

    protected DataMap load(DbImportConfiguration config,
                           DbAdapter adapter,
                           Connection connection,
                           ObjectNameGenerator objectNameGenerator) throws Exception {

        DataMap dataMap = new DataMap("_import_source_");
        createDbLoader(adapter, connection, config.createLoaderDelegate(), objectNameGenerator)
                .load(dataMap, config.getDbLoaderConfig());
        return dataMap;
    }

    protected DbLoader createDbLoader(DbAdapter adapter,
                                      Connection connection,
                                      DbLoaderDelegate dbLoaderDelegate,
                                      ObjectNameGenerator objectNameGenerator) {
        return new DbLoader(connection, adapter, dbLoaderDelegate, objectNameGenerator);
    }
}