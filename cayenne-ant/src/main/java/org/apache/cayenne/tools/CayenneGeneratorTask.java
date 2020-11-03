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
package org.apache.cayenne.tools;

import java.io.File;

import foundrylogic.vpp.VPPConfig;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.dbsync.filter.NamePatternMatcher;
import org.apache.cayenne.dbsync.reverse.configuration.ToolsModule;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.gen.ArtifactsGenerationMode;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClassGenerationActionFactory;
import org.apache.cayenne.gen.ClientClassGenerationAction;
import org.apache.cayenne.map.DataMap;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;
import org.apache.velocity.VelocityContext;
import org.slf4j.LoggerFactory;

/**
 * An Ant task to perform class generation based on CayenneDataMap.
 * 
 * @since 3.0
 */
public class CayenneGeneratorTask extends CayenneTask {

    private AntLogger logger;

    protected String includeEntitiesPattern;
    protected String excludeEntitiesPattern;
    /**
     * @since 4.1
     */
    protected String excludeEmbeddablesPattern;
    protected VPPConfig vppConfig;

    protected File map;
    protected File additionalMaps[];
    protected Boolean client;
    protected File destDir;
    protected String encoding;
    protected Boolean makepairs;
    protected String mode;
    protected String outputPattern;
    protected Boolean overwrite;
    protected String superpkg;
    protected String supertemplate;
    protected String template;
    protected String embeddabletemplate;
    protected String embeddablesupertemplate;
    protected String querytemplate;
    protected String querysupertemplate;
    protected Boolean usepkgpath;
    protected Boolean createpropertynames;

    /**
     * @since 4.1
     */
    private boolean force;

    private boolean useConfigFromDataMap;

    private transient Injector injector;

    /**
     * Create PK attributes as Properties
     *
     * @since 4.1
     */
    protected Boolean createpkproperties;

    /**
     * Optional path (classpath or filesystem) to external velocity tool configuration file
     * @since 4.2 
     */
    protected String externaltoolconfig;

    public CayenneGeneratorTask() {
    }

    protected VelocityContext getVppContext() {
        initializeVppConfig();
        return vppConfig.getVelocityContext();
    }

    /**
     * Executes the task. It will be called by ant framework.
     */
    @Override
    public void execute() throws BuildException {
        validateAttributes();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        injector = new ToolsInjectorBuilder()
                .addModule(new ToolsModule(LoggerFactory.getLogger(CayenneGeneratorTask.class)))
                .create();

        logger = new AntLogger(this);
        CayenneGeneratorMapLoaderAction loadAction = new CayenneGeneratorMapLoaderAction(injector);

        loadAction.setMainDataMapFile(map);
        loadAction.setAdditionalDataMapFiles(additionalMaps);
        try {
            Thread.currentThread().setContextClassLoader(CayenneGeneratorTask.class.getClassLoader());

            DataMap dataMap = loadAction.getMainDataMap();

            ClassGenerationAction generatorAction = createGenerator(dataMap);
            CayenneGeneratorEntityFilterAction filterEntityAction = new CayenneGeneratorEntityFilterAction();
            filterEntityAction.setNameFilter(NamePatternMatcher.build(logger, includeEntitiesPattern, excludeEntitiesPattern));

            CayenneGeneratorEmbeddableFilterAction filterEmbeddableAction = new CayenneGeneratorEmbeddableFilterAction();
            filterEmbeddableAction.setNameFilter(NamePatternMatcher.build(logger, null, excludeEmbeddablesPattern));
            filterEntityAction.setClient(generatorAction.getCgenConfiguration().isClient());
            generatorAction.setLogger(logger);
            if(force) {
                // will (re-)generate all files
                generatorAction.getCgenConfiguration().setForce(true);
            }
            generatorAction.getCgenConfiguration().setTimestamp(map.lastModified());
            if(!hasConfig() && useConfigFromDataMap) {
                generatorAction.prepareArtifacts();
            } else {
                generatorAction.addEntities(filterEntityAction.getFilteredEntities(dataMap));
                generatorAction.addEmbeddables(filterEmbeddableAction.getFilteredEmbeddables(dataMap));
                generatorAction.addQueries(dataMap.getQueryDescriptors());
            }
            generatorAction.execute();
        }
        catch (Exception e) {
            throw new BuildException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(loader);
        }
    }

    private ClassGenerationAction createGenerator(DataMap dataMap) {
        CgenConfiguration cgenConfiguration = buildConfiguration(dataMap);
        return injector.getInstance(ClassGenerationActionFactory.class).createAction(cgenConfiguration);
    }

    private boolean hasConfig() {
        return destDir != null || encoding != null || client != null || excludeEntitiesPattern != null || excludeEmbeddablesPattern != null || includeEntitiesPattern != null ||
                makepairs != null || mode != null || outputPattern != null || overwrite != null || superpkg != null ||
                supertemplate != null || template != null || embeddabletemplate != null || embeddablesupertemplate != null ||
                usepkgpath != null || createpropertynames != null || querytemplate != null ||
                querysupertemplate != null || createpkproperties != null || force || externaltoolconfig != null;
    }

    private CgenConfiguration buildConfiguration(DataMap dataMap) {
        CgenConfiguration cgenConfiguration = injector.getInstance(DataChannelMetaData.class).get(dataMap, CgenConfiguration.class);
        if(hasConfig()) {
            logger.info("Using cgen config from pom.xml");
            return cgenConfigFromPom(dataMap);
        } else if(cgenConfiguration != null) {
            logger.info("Using cgen config from " + cgenConfiguration.getDataMap().getName());
            useConfigFromDataMap = true;
            return cgenConfiguration;
        } else {
            logger.info("Using default cgen config.");
            cgenConfiguration = new CgenConfiguration(false);
            cgenConfiguration.setDataMap(dataMap);
            return cgenConfiguration;
        }
    }

    private CgenConfiguration cgenConfigFromPom(DataMap dataMap){
        CgenConfiguration cgenConfiguration = new CgenConfiguration(client != null ? client : false);
        cgenConfiguration.setDataMap(dataMap);
        cgenConfiguration.setRelPath(destDir != null ? destDir.toPath() : cgenConfiguration.getRelPath());
        cgenConfiguration.setEncoding(encoding != null ? encoding : cgenConfiguration.getEncoding());
        cgenConfiguration.setMakePairs(makepairs != null ? makepairs : cgenConfiguration.isMakePairs());
        if(mode != null && mode.equals("datamap")) {
            replaceDatamapGenerationMode();
        }
        cgenConfiguration.setArtifactsGenerationMode(mode != null ? mode : cgenConfiguration.getArtifactsGenerationMode());
        cgenConfiguration.setOutputPattern(outputPattern != null ? outputPattern : cgenConfiguration.getOutputPattern());
        cgenConfiguration.setOverwrite(overwrite != null ? overwrite : cgenConfiguration.isOverwrite());
        cgenConfiguration.setSuperPkg(superpkg != null ? superpkg : cgenConfiguration.getSuperPkg());
        cgenConfiguration.setSuperTemplate(supertemplate != null ? supertemplate : cgenConfiguration.getSuperTemplate());
        cgenConfiguration.setTemplate(template != null ? template :  cgenConfiguration.getTemplate());
        cgenConfiguration.setEmbeddableSuperTemplate(embeddablesupertemplate != null ? embeddablesupertemplate : cgenConfiguration.getEmbeddableSuperTemplate());
        cgenConfiguration.setEmbeddableTemplate(embeddabletemplate != null ? embeddabletemplate : cgenConfiguration.getEmbeddableTemplate());
        cgenConfiguration.setUsePkgPath(usepkgpath != null ? usepkgpath : cgenConfiguration.isUsePkgPath());
        cgenConfiguration.setCreatePropertyNames(createpropertynames != null ? createpropertynames : cgenConfiguration.isCreatePropertyNames());
        cgenConfiguration.setQueryTemplate(querytemplate != null ? querytemplate : cgenConfiguration.getQueryTemplate());
        cgenConfiguration.setQuerySuperTemplate(querysupertemplate != null ? querysupertemplate : cgenConfiguration.getQuerySuperTemplate());
        cgenConfiguration.setCreatePKProperties(createpkproperties != null ? createpkproperties : cgenConfiguration.isCreatePKProperties());
        cgenConfiguration.setExternalToolConfig(externaltoolconfig != null ? externaltoolconfig : cgenConfiguration.getExternalToolConfig());
        if(!cgenConfiguration.isMakePairs()) {
            if(template == null) {
                cgenConfiguration.setTemplate(cgenConfiguration.isClient() ? ClientClassGenerationAction.SINGLE_CLASS_TEMPLATE : ClassGenerationAction.SINGLE_CLASS_TEMPLATE);
            }
            if(embeddabletemplate == null) {
                cgenConfiguration.setEmbeddableTemplate(ClassGenerationAction.EMBEDDABLE_SINGLE_CLASS_TEMPLATE);
            }
            if(querytemplate == null) {
                cgenConfiguration.setQueryTemplate(cgenConfiguration.isClient() ? ClientClassGenerationAction.DATAMAP_SINGLE_CLASS_TEMPLATE : ClassGenerationAction.DATAMAP_SINGLE_CLASS_TEMPLATE);
            }
        }
        return cgenConfiguration;
    }

    private void replaceDatamapGenerationMode() {
        this.mode = ArtifactsGenerationMode.ALL.getLabel();
        this.excludeEntitiesPattern = "*";
        this.excludeEmbeddablesPattern = "*";
        this.includeEntitiesPattern = "";
    }

    /**
     * Validates attributes that are not related to internal DefaultClassGenerator. Throws
     * BuildException if attributes are invalid.
     */
    protected void validateAttributes() throws BuildException {
        if (map == null && this.getProject() == null) {
            throw new BuildException("either 'map' or 'project' is required.");
        }
    }

    /**
     * Sets the map.
     * 
     * @param map The map to set
     */
    public void setMap(File map) {
        this.map = map;
    }

    /**
     * Sets the additional DataMaps.
     * 
     * @param additionalMapsPath The additional DataMaps to set
     */
    public void setAdditionalMaps(Path additionalMapsPath) {
        String additionalMapFilenames[] = additionalMapsPath.list();
        this.additionalMaps = new File[additionalMapFilenames.length];

        for (int i = 0; i < additionalMapFilenames.length; i++) {
            additionalMaps[i] = new File(additionalMapFilenames[i]);
        }
    }

    /**
     * Sets the destDir.
     */
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Sets <code>overwrite</code> property.
     */
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    /**
     * Sets <code>makepairs</code> property.
     */
    public void setMakepairs(boolean makepairs) {
        this.makepairs = makepairs;
    }

    /**
     * Sets <code>template</code> property.
     */
    public void setTemplate(String template) {
        this.template = template;
    }

    /**
     * Sets <code>supertemplate</code> property.
     */
    public void setSupertemplate(String supertemplate) {
        this.supertemplate = supertemplate;
    }

    /**
     * Sets <code>querytemplate</code> property.
     */
    public void setQueryTemplate(String querytemplate) {
        this.querytemplate = querytemplate;
    }

    /**
     * Sets <code>querysupertemplate</code> property.
     */
    public void setQuerySupertemplate(String querysupertemplate) {
        this.querysupertemplate = querysupertemplate;
    }

    /**
     * Sets <code>usepkgpath</code> property.
     */
    public void setUsepkgpath(boolean usepkgpath) {
        this.usepkgpath = usepkgpath;
    }

    /**
     * Sets <code>superpkg</code> property.
     */
    public void setSuperpkg(String superpkg) {
        this.superpkg = superpkg;
    }

    /**
     * Sets <code>client</code> property.
     */
    public void setClient(boolean client) {
        this.client = client;
    }

    /**
     * Sets <code>encoding</code> property that allows to generate files using non-default
     * encoding.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Sets <code>excludeEntitiesPattern</code> property.
     */
    public void setExcludeEntities(String excludeEntitiesPattern) {
        this.excludeEntitiesPattern = excludeEntitiesPattern;
    }

    /**
     * Sets <code>includeEntitiesPattern</code> property.
     */
    public void setIncludeEntities(String includeEntitiesPattern) {
        this.includeEntitiesPattern = includeEntitiesPattern;
    }

    /**
     * Sets <code>excludeEmbeddablesPattern</code> property.
     */
    public void setExcludeEmbeddablesPattern(String excludeEmbeddablesPattern) {
        this.excludeEmbeddablesPattern = excludeEmbeddablesPattern;
    }

    /**
     * Sets <code>outputPattern</code> property.
     */
    public void setOutputPattern(String outputPattern) {
        this.outputPattern = outputPattern;
    }

    /**
     * Sets <code>mode</code> property.
     */
    public void setMode(String mode) {
        this.mode = mode;
    }

    /**
     * Sets <code>createpropertynames</code> property.
     */
    public void setCreatepropertynames(boolean createpropertynames) {
        this.createpropertynames = createpropertynames;
    }

    public void setEmbeddabletemplate(String embeddabletemplate) {
        this.embeddabletemplate = embeddabletemplate;
    }

    public void setEmbeddablesupertemplate(String embeddablesupertemplate) {
        this.embeddablesupertemplate = embeddablesupertemplate;
    }

    /**
     * @since 4.1
     */
    public void setCreatepkproperties(boolean createpkproperties) {
        this.createpkproperties = createpkproperties;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    /**
     * @since 4.2
     */
    public void setExternaltoolconfig(String externaltoolconfig) {
    	this.externaltoolconfig = externaltoolconfig;
    }
    
    /**
     * Provides a <code>VPPConfig</code> object to configure. (Written with createConfig()
     * instead of addConfig() to avoid run-time dependency on VPP).
     */
    public Object createConfig() {
        this.vppConfig = new VPPConfig();
        return this.vppConfig;
    }

    /**
     * If no VppConfig element specified, use the default one.
     */
    private void initializeVppConfig() {
        if (vppConfig == null) {
            vppConfig = VPPConfig.getDefaultConfig(getProject());
        }
    }
}
