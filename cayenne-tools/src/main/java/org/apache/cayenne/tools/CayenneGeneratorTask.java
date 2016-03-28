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
package org.apache.cayenne.tools;

import foundrylogic.vpp.VPPConfig;
import org.apache.cayenne.access.loader.NamePatternMatcher;
import org.apache.cayenne.map.template.ArtifactsGenerationMode;
import org.apache.cayenne.map.template.ClassTemplate;
import org.apache.cayenne.map.template.TemplateType;
import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.map.template.ClassGenerationDescriptor;
import org.apache.cayenne.gen.ClientClassGenerationAction;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;
import org.apache.velocity.VelocityContext;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import static org.apache.cayenne.map.template.TemplateType.*;
import static org.apache.cayenne.map.template.TemplateType.DATAMAP_SUBCLASS;
import static org.apache.cayenne.map.template.TemplateType.DATAMAP_SUPERCLASS;

/**
 * An Ant task to perform class generation based on CayenneDataMap.
 *
 * @since 3.0
 */
public class CayenneGeneratorTask extends CayenneTask {

    protected String includeEntitiesPattern;
    protected String excludeEntitiesPattern;
    protected VPPConfig vppConfig;

    protected File map;
    protected File additionalMaps[];
    protected boolean client;
    protected File destDir;
    protected String encoding;
    protected boolean makepairs;
    protected String mode;
    protected String outputPattern;
    protected boolean overwrite;
    protected String superpkg;
    protected String supertemplate;
    protected String template;
    protected String embeddabletemplate;
    protected String embeddablesupertemplate;
    protected String querytemplate;
    protected String querysupertemplate;
    protected boolean usepkgpath;
    protected boolean createpropertynames;
    protected boolean isClassGenerationDefined;


    public CayenneGeneratorTask() {
        this.makepairs = true;
        this.mode = ArtifactsGenerationMode.ENTITY.getLabel();
        this.outputPattern = "*.java";
        this.usepkgpath = true;
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

        AntLogger logger = new AntLogger(this);
        CayenneGeneratorMapLoaderAction loadAction = new CayenneGeneratorMapLoaderAction();

        loadAction.setMainDataMapFile(map);
        loadAction.setAdditionalDataMapFiles(additionalMaps);

        CayenneGeneratorEntityFilterAction filterAction = new CayenneGeneratorEntityFilterAction();
        filterAction.setClient(client);
        filterAction.setNameFilter(NamePatternMatcher.build(logger, includeEntitiesPattern, excludeEntitiesPattern));

        try {

            DataMap dataMap = loadAction.getMainDataMap();

            ClassGenerationAction generatorAction = createGeneratorAction(dataMap);
            generatorAction.setLogger(logger);
            generatorAction.setTimestamp(map.lastModified());
            generatorAction.setDataMap(dataMap);
            generatorAction.addEntities(filterAction.getFilteredEntities(dataMap));
            generatorAction.addEmbeddables(filterAction.getFilteredEmbeddables(dataMap));
            generatorAction.addQueries(dataMap.getQueryDescriptors());
            generatorAction.execute();
        } catch (Exception e) {
            throw new BuildException(e);
        }
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
        isClassGenerationDefined = true;
        this.template = template;
    }

    /**
     * Sets <code>supertemplate</code> property.
     */
    public void setSupertemplate(String supertemplate) {
        isClassGenerationDefined = true;
        this.supertemplate = supertemplate;
    }

    /**
     * Sets <code>querytemplate</code> property.
     */
    public void setQueryTemplate(String querytemplate) {
        isClassGenerationDefined = true;
        this.querytemplate = querytemplate;
    }

    /**
     * Sets <code>querysupertemplate</code> property.
     */
    public void setQuerySupertemplate(String querysupertemplate) {
        isClassGenerationDefined = true;
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

    protected ClassGenerationAction createGeneratorAction(DataMap dataMap)
            throws UnsupportedEncodingException, MalformedURLException {

        ClassGenerationAction action;
        if (client) {
            action = new ClientClassGenerationAction();
        } else {
            action = new ClassGenerationAction();
        }

        if (isClassGenerationDefined) {
            createGenerator(action);
        } else {
            if (dataMap.getClassGenerationDescriptor() != null) {
                createGeneratorFromMap(action, dataMap.getClassGenerationDescriptor());
            }
        }

        action.setDestDir(destDir);
        action.setEncoding(encoding);
        action.setMakePairs(makepairs);
        action.setOutputPattern(outputPattern);
        action.setOverwrite(overwrite);
        action.setSuperPkg(superpkg);
        action.setUsePkgPath(usepkgpath);
        action.setCreatePropertyNames(createpropertynames);

        return action;
    }

    protected void createGenerator(ClassGenerationAction action) {
        action.setArtifactsGenerationMode(mode);
        action.setSuperTemplate(supertemplate);
        action.setTemplate(template);
        action.setEmbeddableSuperTemplate(embeddablesupertemplate);
        action.setEmbeddableTemplate(embeddabletemplate);
    }

    abstract static class GeneratorByTemplate {

        public static Map<TemplateType, GeneratorByTemplate> GENERATORS = new HashMap<>();

        public static void setTemplate_(ClassGenerationAction action, ClassTemplate template, File map)
                throws UnsupportedEncodingException, MalformedURLException {

            GeneratorByTemplate generator = GENERATORS.get(template.getType());
            if (generator == null) {
                throw new IllegalArgumentException("Invalid template type: " + template.getType());
            }
            Injector injector = DIBootstrap.createInjector(new ServerModule());
            ConfigurationNameMapper nameMapper = injector.getInstance(ConfigurationNameMapper.class);
            String templateLocation = nameMapper.configurationLocation(ClassTemplate.class, template.getName());
            Resource templateResource = new URLResource(map.toURI().toURL()).getRelativeResource(templateLocation);
            template.setConfigurationSource(templateResource);
            generator.setTemplate(action, template);
        }

        static {
            GENERATORS.put(ENTITY_SINGLE_CLASS, new GeneratorByTemplate() {
                @Override
                void setTemplate(ClassGenerationAction action, ClassTemplate template) throws UnsupportedEncodingException {
                    if (!isTemplateDefined) {
                        String path = URLDecoder.decode(template.getConfigurationSource().getURL().getPath(), "UTF-8");
                        action.setTemplate(path);
                        isTemplateDefined = true;
                    } else {
                        throw new IllegalStateException("Entity template defined more than one time in datamap. " +
                                "Delete redundant ones or use mvn plugin");
                    }
                }
            });

            GENERATORS.put(ENTITY_SUPERCLASS, new GeneratorByTemplate() {
                @Override
                void setTemplate(ClassGenerationAction action, ClassTemplate template) throws UnsupportedEncodingException {
                    if (!isTemplateDefined) {
                        String path = URLDecoder.decode(template.getConfigurationSource().getURL().getPath(), "UTF-8");
                        action.setSuperTemplate(path);
                        isTemplateDefined = true;
                    } else {
                        throw new IllegalStateException("Entity super template defined more than one time in datamap. " +
                                "Delete redundant ones or use mvn plugin");
                    }
                }
            });

            GENERATORS.put(ENTITY_SUBCLASS, new GeneratorByTemplate() {
                @Override
                void setTemplate(ClassGenerationAction action, ClassTemplate template) {
                    if (!isTemplateDefined) {
                        isTemplateDefined = true;
                    } else {
                        throw new IllegalStateException("Entity sub template defined more than one time in datamap. " +
                                "Delete redundant ones or use mvn plugin");
                    }
                }
            });

            GENERATORS.put(EMBEDDABLE_SINGLE_CLASS, new GeneratorByTemplate() {
                @Override
                void setTemplate(ClassGenerationAction action, ClassTemplate template) throws UnsupportedEncodingException {
                    if (!isTemplateDefined) {
                        String path = URLDecoder.decode(template.getConfigurationSource().getURL().getPath(), "UTF-8");
                        action.setEmbeddableTemplate(path);
                        isTemplateDefined = true;
                    } else {
                        throw new IllegalStateException("Embeddable template defined more than one time in datamap. " +
                                "Delete redundant ones or use mvn plugin");
                    }
                }
            });

            GENERATORS.put(EMBEDDABLE_SUPERCLASS, new GeneratorByTemplate() {
                @Override
                void setTemplate(ClassGenerationAction action, ClassTemplate template) throws UnsupportedEncodingException {
                    if (!isTemplateDefined) {
                        String path = URLDecoder.decode(template.getConfigurationSource().getURL().getPath(), "UTF-8");
                        action.setEmbeddableSuperTemplate(path);
                        isTemplateDefined = true;
                    } else {
                        throw new IllegalStateException("Embeddable super template defined more than one time in datamap. " +
                                "Delete redundant ones or use mvn plugin");
                    }
                }
            });

            GENERATORS.put(EMBEDDABLE_SUBCLASS, new GeneratorByTemplate() {
                @Override
                void setTemplate(ClassGenerationAction action, ClassTemplate template) {
                    if (!isTemplateDefined) {
                        isTemplateDefined = true;
                    } else {
                        throw new IllegalStateException("Embeddable sub template defined more than one time in datamap. " +
                                "Delete redundant ones or use mvn plugin");
                    }
                }
            });

            GENERATORS.put(DATAMAP_SINGLE_CLASS, new GeneratorByTemplate() {
                @Override
                void setTemplate(ClassGenerationAction action, ClassTemplate template) throws UnsupportedEncodingException {
                    if (!isTemplateDefined) {
                        String path = URLDecoder.decode(template.getConfigurationSource().getURL().getPath(), "UTF-8");
                        action.setQueryTemplate(path);
                        isTemplateDefined = true;
                    } else {
                        throw new IllegalStateException("Query template defined more than one time in datamap. " +
                                "Delete redundant ones or use mvn plugin");
                    }
                }
            });

            GENERATORS.put(DATAMAP_SUPERCLASS, new GeneratorByTemplate() {
                @Override
                void setTemplate(ClassGenerationAction action, ClassTemplate template) throws UnsupportedEncodingException {
                    if (!isTemplateDefined) {
                        String path = URLDecoder.decode(template.getConfigurationSource().getURL().getPath(), "UTF-8");
                        action.setQuerySuperTemplate(path);
                        isTemplateDefined = true;
                    } else {
                        throw new IllegalStateException("Query super template defined more than one time in datamap. " +
                                "Delete redundant ones or use mvn plugin");
                    }
                }
            });

            GENERATORS.put(DATAMAP_SUBCLASS, new GeneratorByTemplate() {
                @Override
                void setTemplate(ClassGenerationAction action, ClassTemplate template) {
                    if (!isTemplateDefined) {
                        isTemplateDefined = true;
                    } else {
                        throw new IllegalStateException("Query sub template defined more than one time in datamap. " +
                                "Delete redundant ones or use mvn plugin");
                    }
                }
            });
        }

        boolean isTemplateDefined = false;

        abstract void setTemplate(ClassGenerationAction action, ClassTemplate template) throws UnsupportedEncodingException;

    }

    protected void createGeneratorFromMap(ClassGenerationAction action, ClassGenerationDescriptor descriptor)
            throws UnsupportedEncodingException, MalformedURLException {
        if (descriptor.getArtifactsGenerationMode() != null) {
            action.setArtifactsGenerationMode(descriptor.getArtifactsGenerationMode().getLabel());
        } else {
            action.setArtifactsGenerationMode(mode);
        }
        for (ClassTemplate template : descriptor.getTemplates().values()) {
            GeneratorByTemplate.setTemplate_(action, template, map);
        }
    }
}
