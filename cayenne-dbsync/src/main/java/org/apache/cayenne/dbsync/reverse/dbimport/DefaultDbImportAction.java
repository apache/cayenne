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

package org.apache.cayenne.dbsync.reverse.dbimport;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.runtime.DataSourceFactory;
import org.apache.cayenne.configuration.runtime.DbAdapterFactory;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.merge.DataMapMerger;
import org.apache.cayenne.dbsync.merge.context.MergerContext;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactoryProvider;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.merge.token.model.AbstractToModelToken;
import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.dbsync.reverse.dbload.DbLoader;
import org.apache.cayenne.dbsync.reverse.dbload.DbLoaderConfiguration;
import org.apache.cayenne.dbsync.reverse.dbload.ModelMergeDelegate;
import org.apache.cayenne.dbsync.reverse.dbload.ProxyModelMergeDelegate;
import org.apache.cayenne.dbsync.reverse.filters.CatalogFilter;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfigBuilder;
import org.apache.cayenne.dbsync.reverse.filters.SchemaFilter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.validation.SimpleValidationFailure;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;
import org.slf4j.Logger;

import static org.apache.cayenne.util.Util.isBlank;

/**
 * A default implementation of {@link DbImportAction} that can load DB schema and merge it to a new or an existing
 * DataMap.
 *
 * @since 4.0
 */
public class DefaultDbImportAction implements DbImportAction {

    private final ProjectSaver projectSaver;
    protected final Logger logger;
    private final DataSourceFactory dataSourceFactory;
    private final DbAdapterFactory adapterFactory;
    private final DataMapLoader mapLoader;
    private final MergerTokenFactoryProvider mergerTokenFactoryProvider;
    private final DataChannelDescriptorLoader dataChannelDescriptorLoader;
    private final DataChannelMetaData metaData;
    private boolean hasChanges;
    private Collection<MergerToken> tokens;
    private DataMap loadedDataMap;

    public DefaultDbImportAction(@Inject Logger logger,
                                 @Inject ProjectSaver projectSaver,
                                 @Inject DataSourceFactory dataSourceFactory,
                                 @Inject DbAdapterFactory adapterFactory,
                                 @Inject DataMapLoader mapLoader,
                                 @Inject MergerTokenFactoryProvider mergerTokenFactoryProvider,
                                 @Inject DataChannelDescriptorLoader dataChannelDescriptorLoader,
                                 @Inject DataChannelMetaData metaData) {
        this.logger = logger;
        this.projectSaver = projectSaver;
        this.dataSourceFactory = dataSourceFactory;
        this.adapterFactory = adapterFactory;
        this.mapLoader = mapLoader;
        this.mergerTokenFactoryProvider = mergerTokenFactoryProvider;
        this.metaData = metaData;
        this.dataChannelDescriptorLoader = dataChannelDescriptorLoader;
    }

    protected static List<MergerToken> sort(List<MergerToken> reverse) {
        Collections.sort(reverse);
        return reverse;
    }

    /**
     * Flattens many-to-many relationships in the generated model.
     */
    public static void flattenManyToManyRelationships(DataMap map, Collection<ObjEntity> loadedObjEntities,
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
        commit(config, loadDataMap(config));
    }

    protected DbAdapter createAdapter(DataNodeDescriptor dataNodeDescriptor, DataSource dataSource) throws Exception {
        DbAdapter adapter = adapterFactory.createAdapter(dataNodeDescriptor, dataSource);

        // Warm up the AutoAdapter by calling any method. This to avoid AutoAdapter opening a connection later in
        // the middle of import to detect the DB type. Opening two connections in the same thread causes issues with
        // some DBs (namely com.sap.cloud.db.jdbc:ngdbc:2.4.56)
        adapter.getPkGenerator();

        return adapter;
    }

    protected void commit(DbImportConfiguration config, DataMap sourceDataMap) throws Exception {
        if (hasChanges) {
            DataMap targetDataMap = loadedDataMap;

            syncDataMapProperties(targetDataMap, config);
            applyTokens(targetDataMap, tokens, config);

            saveLoaded(targetDataMap, config);
            this.loadedDataMap = null;
            this.hasChanges = false;
        }
    }

    protected DataMap loadDataMap(DbImportConfiguration config) throws Exception {

        if (logger.isDebugEnabled()) {
            logger.debug("DB connection: " + config.getDataSourceInfo());
            logger.debug(String.valueOf(config));
        }

        DataNodeDescriptor dataNodeDescriptor = config.createDataNodeDescriptor();
        DataSource dataSource = dataSourceFactory.getDataSource(dataNodeDescriptor);
        DbAdapter adapter = createAdapter(dataNodeDescriptor, dataSource);

        DataMap sourceDataMap;
        DataMap targetDataMap = existingTargetMap(config);

        ReverseEngineering dataMapReverseEngineering = metaData.get(targetDataMap, ReverseEngineering.class);
        if ((config.isUseDataMapReverseEngineering()) && (dataMapReverseEngineering != null)) {
            putReverseEngineeringToConfig(dataMapReverseEngineering, config, dataSource, adapter);
        }
        if ((dataMapReverseEngineering != null) && (!config.isUseDataMapReverseEngineering())) {
            logger.warn("Found several dbimport configs. DataMap dbimport config was skipped. " +
                    "Configuration selected from build file");
        }
        if ((dataMapReverseEngineering == null) && (config.isUseDataMapReverseEngineering())) {
            logger.warn("Missing dbimport config. Database is imported completely.");
        }

        try (Connection connection = dataSource.getConnection()) {
            sourceDataMap = load(config, adapter, connection);
        }

        if (targetDataMap == null) {

            String path = config.getTargetDataMap() == null ? "null" : config.getTargetDataMap().getAbsolutePath() + "'";

            logger.info("");
            logger.info("Map file does not exist. Loaded db model will be saved into '" + path);

            hasChanges = true;
            targetDataMap = newTargetDataMap(config);
        }
        this.loadedDataMap = targetDataMap;

        // In that moment our data map fills with source map
        // transform source DataMap before merging
        transformSourceBeforeMerge(sourceDataMap, targetDataMap, config);

        MergerTokenFactory mergerTokenFactory = mergerTokenFactoryProvider.get(adapter);

        DbLoaderConfiguration loaderConfig = config.getDbLoaderConfig();
        tokens = DataMapMerger.builder(mergerTokenFactory)
           .filters(loaderConfig.getFiltersConfig())
           .skipPKTokens(loaderConfig.isSkipPrimaryKeyLoading())
           .skipRelationshipsTokens(loaderConfig.isSkipRelationshipsLoading())
           .build()
           .createMergeTokens(targetDataMap, sourceDataMap);
        tokens = log(sort(reverse(mergerTokenFactory, tokens)));

        hasChanges |= checkDataMapProperties(targetDataMap, config);
        hasChanges |= hasTokensToImport(tokens);
        return sourceDataMap;
    }

    private void putReverseEngineeringToConfig(ReverseEngineering reverseEngineering,
                                               DbImportConfiguration config,
                                               DataSource dataSource,
                                               DbAdapter dbAdapter) throws SQLException {
        config.setSkipRelationshipsLoading(reverseEngineering.getSkipRelationshipsLoading());
        config.setSkipPrimaryKeyLoading(reverseEngineering.getSkipPrimaryKeyLoading());
        config.setStripFromTableNames(reverseEngineering.getStripFromTableNames());
        config.setTableTypes(reverseEngineering.getTableTypes());
        config.setMeaningfulPkTables(reverseEngineering.getMeaningfulPkTables());
        config.setNamingStrategy(reverseEngineering.getNamingStrategy());
        config.setFiltersConfig(new FiltersConfigBuilder(
                new ReverseEngineering(reverseEngineering))
                .dataSource(dataSource)
                .dbAdapter(dbAdapter)
                .build());
        config.setForceDataMapCatalog(reverseEngineering.isForceDataMapCatalog());
        config.setForceDataMapSchema(reverseEngineering.isForceDataMapSchema());
        config.setDefaultPackage(reverseEngineering.getDefaultPackage());
        config.setUseJava7Types(reverseEngineering.isUseJava7Types());
    }

    protected void transformSourceBeforeMerge(DataMap sourceDataMap, DataMap targetDataMap, DbImportConfiguration configuration) {
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

    public boolean hasTokensToImport(Collection<MergerToken> tokens) {

        if (tokens.isEmpty()) {
            logger.info("");
            logger.info("Detected changes: No changes to import.");
            return false;
        }

        return true;

    }

    private boolean checkDataMapProperties(DataMap targetDataMap, DbImportConfiguration config) {
        String defaultPackage = config.getDefaultPackage();
        if (defaultPackage == null || isBlank(defaultPackage)) {
            return false;
        }

        return defaultPackage.equals(targetDataMap.getDefaultPackage());
    }

    private void syncDataMapProperties(DataMap targetDataMap, DbImportConfiguration config) {
        String defaultPackage = config.getDefaultPackage();
        if (defaultPackage == null || isBlank(defaultPackage)) {
            return;
        }

        targetDataMap.setDefaultPackage(defaultPackage);
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

    protected Collection<MergerToken> log(List<MergerToken> tokens) {
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
            URLResource configurationResource = new URLResource(file.toURI().toURL());
            DataMap dataMap = mapLoader.load(configurationResource);
            dataMap.setNamespace(new EntityResolver(Collections.singleton(dataMap)));
            dataMap.setConfigurationSource(configurationResource);
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
        if (catalogs.length == 1) {
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
            SchemaFilter[] schemas = catalogs[0].schemas;
            if(schemas.length == 1) {
                String schema = schemas[0].name;
                if (schema != null && schema.length() > 0 && schema.indexOf('%') < 0) {
                    dataMap.setDefaultSchema(schema);
                }
            }
        }

        return dataMap;
    }

    private List<MergerToken> reverse(MergerTokenFactory mergerTokenFactory, Iterable<MergerToken> mergeTokens) {

        List<MergerToken> tokens = new LinkedList<>();
        for (MergerToken token : mergeTokens) {
            if (token instanceof AbstractToModelToken) {
                continue;
            }
            tokens.add(token.createReverse(mergerTokenFactory));
        }
        return tokens;
    }

    private void applyTokens(DataMap targetDataMap, Collection<MergerToken> tokens, DbImportConfiguration config) {

        if (tokens.isEmpty()) {
            logger.info("");
            logger.info("Detected changes: No changes to import.");
        }

        final Collection<ObjEntity> loadedObjEntities = new LinkedList<>();
        ModelMergeDelegate mergeDelegate = new ProxyModelMergeDelegate(config.createMergeDelegate()) {
            @Override
            public void objEntityAdded(ObjEntity ent) {
                loadedObjEntities.add(ent);
                super.objEntityAdded(ent);
            }
        };

        ObjectNameGenerator nameGenerator = config.createNameGenerator();
        MergerContext mergerContext = MergerContext.builder(targetDataMap)
                .delegate(mergeDelegate)
                .nameGenerator(nameGenerator)
                .usingJava7Types(config.isUseJava7Types())
                .meaningfulPKFilter(config.createMeaningfulPKFilter())
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
    }

    protected void logMessages(List<String> messages) {
        messages.forEach(logger::info);
    }

    /**
     * Save imported data.
     * This can create DataMap and/or Project files.
     */
    protected void saveLoaded(DataMap dataMap, DbImportConfiguration config) throws MalformedURLException {
        ConfigurationTree<ConfigurationNode> projectRoot;
        if(config.getCayenneProject() == null) {
            // Old version of cdbimport, no Cayenne project, need to save only DataMap
            projectRoot = new ConfigurationTree<>(dataMap);
        } else {
            // Cayenne project is present
            DataChannelDescriptor dataChannelDescriptor;
            if(config.getCayenneProject().exists()) {
                // Cayenne project file exists, need to read it and push DataMap inside
                URLResource configurationResource = new URLResource(config.getCayenneProject().toURI().toURL());
                ConfigurationTree<DataChannelDescriptor> configurationTree = dataChannelDescriptorLoader.load(configurationResource);
                if(!configurationTree.getLoadFailures().isEmpty()) {
                    throw new CayenneRuntimeException("Unable to load cayenne project %s, %s", config.getCayenneProject(),
                            configurationTree.getLoadFailures().iterator().next().getDescription());
                }
                dataChannelDescriptor = configurationTree.getRootNode();
                // remove old copy of DataMap if it's there
                DataMap oldDataMap = dataChannelDescriptor.getDataMap(dataMap.getName());
                if(oldDataMap != null) {
                    dataChannelDescriptor.getDataMaps().remove(oldDataMap);
                }
            } else {
                // No project file yet, can simply create empty project with resulting DataMap
                dataChannelDescriptor = new DataChannelDescriptor();
                dataChannelDescriptor.setName(getProjectNameFromFileName(config.getCayenneProject().getName()));
                dataChannelDescriptor.setConfigurationSource(new URLResource(config.getCayenneProject().toURI().toURL()));
                logger.info("Project file does not exist. New project will be saved into '" + config.getCayenneProject().getAbsolutePath());
            }

            dataChannelDescriptor.getDataMaps().add(dataMap);
            projectRoot = new ConfigurationTree<>(dataChannelDescriptor);
        }

        Project project = new Project(projectRoot);
        projectSaver.save(project);

        logger.info("");
        logger.info("All changes saved.");
    }

    protected String getProjectNameFromFileName(String fileName) {
        int xmlExtPosition = fileName.lastIndexOf(".xml");
        String name = fileName.substring(0, xmlExtPosition == -1 ? fileName.length() : xmlExtPosition);
        if(fileName.startsWith("cayenne-")) {
            name = name.substring("cayenne-".length());
        }
        return name;
    }

    protected DataMap load(DbImportConfiguration config, DbAdapter adapter, Connection connection) throws Exception {
        return createDbLoader(adapter, connection, config).load();
    }

    protected DbLoader createDbLoader(DbAdapter adapter, Connection connection, DbImportConfiguration config) {
        return new DbLoader(adapter, connection,
                config.getDbLoaderConfig(),
                config.createLoaderDelegate(),
                config.createNameGenerator());
    }
}