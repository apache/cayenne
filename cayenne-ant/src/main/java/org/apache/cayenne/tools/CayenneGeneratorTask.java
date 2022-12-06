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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.dbsync.filter.NamePatternMatcher;
import org.apache.cayenne.dbsync.reverse.configuration.ToolsModule;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.gen.ArtifactsGenerationMode;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.CgenConfigList;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClassGenerationActionFactory;
import org.apache.cayenne.gen.CgenTemplate;
import org.apache.cayenne.gen.TemplateType;
import org.apache.cayenne.map.DataMap;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;
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

    protected File map;
    protected File additionalMaps[];
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
    protected String datamaptemplate;
    protected String datamapsupertemplate;
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
            for (ClassGenerationAction generatorAction : createGenerators(dataMap)) {
                CayenneGeneratorEntityFilterAction filterEntityAction = new CayenneGeneratorEntityFilterAction();
                filterEntityAction.setNameFilter(NamePatternMatcher.build(logger, includeEntitiesPattern, excludeEntitiesPattern));

                CayenneGeneratorEmbeddableFilterAction filterEmbeddableAction = new CayenneGeneratorEmbeddableFilterAction();
                filterEmbeddableAction.setNameFilter(NamePatternMatcher.build(logger, null, excludeEmbeddablesPattern));
                generatorAction.setLogger(logger);
                if (force) {
                    // will (re-)generate all files
                    generatorAction.getCgenConfiguration().setForce(true);
                }
                generatorAction.getCgenConfiguration().setTimestamp(map.lastModified());
                if (!hasConfig() && useConfigFromDataMap) {
                    generatorAction.prepareArtifacts();
                } else {
                    generatorAction.addEntities(filterEntityAction.getFilteredEntities(dataMap));
                    generatorAction.addEmbeddables(filterEmbeddableAction.getFilteredEmbeddables(dataMap));
                    generatorAction.addQueries(dataMap.getQueryDescriptors());
                }
                generatorAction.execute();
            }
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(loader);
        }
    }

    private List<ClassGenerationAction> createGenerators(DataMap dataMap) {
        List<ClassGenerationAction> actions = new ArrayList<>();
        for (CgenConfiguration configuration : buildConfigurations(dataMap)) {
            actions.add(injector.getInstance(ClassGenerationActionFactory.class).createAction(configuration));
        }
        return actions;
    }

    private boolean hasConfig() {
        return destDir != null || encoding != null || excludeEntitiesPattern != null || excludeEmbeddablesPattern != null || includeEntitiesPattern != null ||
                makepairs != null || mode != null || outputPattern != null || overwrite != null || superpkg != null ||
                supertemplate != null || template != null || embeddabletemplate != null || embeddablesupertemplate != null ||
                usepkgpath != null || createpropertynames != null || datamaptemplate != null ||
                datamapsupertemplate != null || createpkproperties != null || force || externaltoolconfig != null;
    }

    List<CgenConfiguration> buildConfigurations(DataMap dataMap) {
        CgenConfigList cgenConfigList = injector.getInstance(DataChannelMetaData.class).get(dataMap, CgenConfigList.class);
        if (hasConfig()) {
            logger.info("Using cgen config from pom.xml");
            return Collections.singletonList(cgenConfigFromPom(dataMap));
        } else if (cgenConfigList != null) {
            logger.info("Using cgen config from dataMap");
            useConfigFromDataMap = true;
            return cgenConfigList.getAll();
        } else {
            logger.info("Using default cgen config.");
            CgenConfiguration cgenConfiguration = new CgenConfiguration();
            cgenConfiguration.setDataMap(dataMap);
            return Collections.singletonList(cgenConfiguration);
        }
    }

    private CgenConfiguration cgenConfigFromPom(DataMap dataMap){
        CgenConfiguration cgenConfiguration = new CgenConfiguration();
        cgenConfiguration.setDataMap(dataMap);
        if(destDir != null) {
            cgenConfiguration.setRelativePath(destDir.toPath());
        }
        cgenConfiguration.setEncoding(encoding != null ? encoding : cgenConfiguration.getEncoding());
        cgenConfiguration.setMakePairs(makepairs != null ? makepairs : cgenConfiguration.isMakePairs());
        if(mode != null && mode.equals("datamap")) {
            replaceDatamapGenerationMode();
        }
        cgenConfiguration.setArtifactsGenerationMode(mode != null ? mode : cgenConfiguration.getArtifactsGenerationMode());
        cgenConfiguration.setOutputPattern(outputPattern != null ? outputPattern : cgenConfiguration.getOutputPattern());
        cgenConfiguration.setOverwrite(overwrite != null ? overwrite : cgenConfiguration.isOverwrite());
        cgenConfiguration.setSuperPkg(superpkg != null ? superpkg : cgenConfiguration.getSuperPkg());
        cgenConfiguration.setSuperTemplate(supertemplate != null ? new CgenTemplate(supertemplate,true,TemplateType.ENTITY_SUPERCLASS) : cgenConfiguration.getSuperTemplate());
        cgenConfiguration.setTemplate(template != null ? new CgenTemplate(template,true,TemplateType.ENTITY_SUBCLASS) :  cgenConfiguration.getTemplate());
        cgenConfiguration.setEmbeddableSuperTemplate(embeddablesupertemplate != null ? new CgenTemplate(embeddablesupertemplate,true,TemplateType.EMBEDDABLE_SUPERCLASS) : cgenConfiguration.getEmbeddableSuperTemplate());
        cgenConfiguration.setEmbeddableTemplate(embeddabletemplate != null ? new CgenTemplate(embeddabletemplate,true,TemplateType.EMBEDDABLE_SUBCLASS) : cgenConfiguration.getEmbeddableTemplate());
        cgenConfiguration.setUsePkgPath(usepkgpath != null ? usepkgpath : cgenConfiguration.isUsePkgPath());
        cgenConfiguration.setCreatePropertyNames(createpropertynames != null ? createpropertynames : cgenConfiguration.isCreatePropertyNames());
        cgenConfiguration.setDataMapTemplate(datamaptemplate != null ? new CgenTemplate(datamaptemplate,true,TemplateType.DATAMAP_SUBCLASS) : cgenConfiguration.getDataMapTemplate());
        cgenConfiguration.setDataMapSuperTemplate(datamapsupertemplate != null ? new CgenTemplate(datamapsupertemplate,true,TemplateType.DATAMAP_SUPERCLASS) : cgenConfiguration.getDataMapSuperTemplate());
        cgenConfiguration.setCreatePKProperties(createpkproperties != null ? createpkproperties : cgenConfiguration.isCreatePKProperties());
        cgenConfiguration.setExternalToolConfig(externaltoolconfig != null ? externaltoolconfig : cgenConfiguration.getExternalToolConfig());
        if(!cgenConfiguration.isMakePairs()) {
            if(template == null) {
                cgenConfiguration.setTemplate(TemplateType.ENTITY_SINGLE_CLASS.defaultTemplate());
            }
            if(embeddabletemplate == null) {
                cgenConfiguration.setEmbeddableTemplate(TemplateType.EMBEDDABLE_SINGLE_CLASS.defaultTemplate());
            }
            if(datamaptemplate == null) {
                cgenConfiguration.setDataMapTemplate(TemplateType.DATAMAP_SINGLE_CLASS.defaultTemplate());
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
        String[] additionalMapFilenames = additionalMapsPath.list();
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
     * Sets <code>datamaptemplate</code> property.
     * @since 5.0 querytemplate renamed to datamaptemplate
     */
    public void setDataMapTemplate(String datamaptemplate) {
        this.datamaptemplate = datamaptemplate;
    }

    /**
     * Sets <code>datamapsupertemplate</code> property.
     * @since 5.0 querysupertemplate renamed to datamapsupertemplate
     */
    public void setDataMapSupertemplate(String datamapsupertemplate) {
        this.datamapsupertemplate = datamapsupertemplate;
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

}
