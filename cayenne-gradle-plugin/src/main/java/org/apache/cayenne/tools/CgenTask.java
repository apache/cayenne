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

import groovy.lang.Reference;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.dbsync.filter.NamePatternMatcher;
import org.apache.cayenne.dbsync.reverse.configuration.ToolsModule;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.gen.ArtifactsGenerationMode;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.CgenModule;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClientClassGenerationAction;
import org.apache.cayenne.map.DataMap;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Set;

/**
 * @since 4.0
 */
public class CgenTask extends BaseCayenneTask {

    private static final File[] NO_FILES = new File[0];

    private File additionalMaps;

    @Input
    @Optional
    private Boolean client;

    private File destDir;

    @Input
    @Optional
    private String encoding;

    @Input
    @Optional
    private String excludeEntities;

    @Input
    @Optional
    private String includeEntities;

    /**
     * @since 4.1
     */
    @Input
    @Optional
    private String excludeEmbeddables;

    @Input
    @Optional
    private Boolean makePairs;

    @Input
    @Optional
    private String mode;

    @Input
    @Optional
    private String outputPattern;

    @Input
    @Optional
    private Boolean overwrite;

    @Input
    @Optional
    private String superPkg;

    @Input
    @Optional
    private String superTemplate;

    @Input
    @Optional
    private String template;

    @Input
    @Optional
    private String embeddableSuperTemplate;

    @Input
    @Optional
    private String embeddableTemplate;

    @Input
    @Optional
    private Boolean usePkgPath;

    @Input
    @Optional
    private Boolean createPropertyNames;

    /**
     * Force run (skip check for files modification time)
     * @since 4.1
     */
    @Input
    private boolean force;

    @Input
    @Optional
    private String queryTemplate;

    @Input
    @Optional
    private String querySuperTemplate;

    /**
     * If set to <code>true</code>, will generate PK attributes as Properties.
     * Default is <code>false</code>.
     * @since 4.1
     */
    @Input
    @Optional
    private Boolean createPKProperties;

    private String destDirName;

    private DataChannelMetaData metaData;

    private boolean useConfigFromDataMap;

    @TaskAction
    public void generate() {
        File dataMapFile = getDataMapFile();

        final Injector injector = DIBootstrap.createInjector(new CgenModule(), new ToolsModule(LoggerFactory.getLogger(CgenTask.class)));
        metaData = injector.getInstance(DataChannelMetaData.class);

        CayenneGeneratorMapLoaderAction loaderAction = new CayenneGeneratorMapLoaderAction(injector);
        loaderAction.setMainDataMapFile(dataMapFile);
        try {
            loaderAction.setAdditionalDataMapFiles(convertAdditionalDataMaps());

            DataMap dataMap = loaderAction.getMainDataMap();
            ClassGenerationAction generator = this.createGenerator(dataMap);
            CayenneGeneratorEntityFilterAction filterEntityAction = new CayenneGeneratorEntityFilterAction();
            filterEntityAction.setNameFilter(NamePatternMatcher.build(getLogger(), includeEntities, excludeEntities));

            CayenneGeneratorEmbeddableFilterAction filterEmbeddableAction = new CayenneGeneratorEmbeddableFilterAction();
            filterEmbeddableAction.setNameFilter(NamePatternMatcher.build(getLogger(), null, excludeEmbeddables));
            filterEntityAction.setClient(generator.getCgenConfiguration().isClient());
            generator.setLogger(getLogger());

            if(this.force || getProject().hasProperty("force")) {
                generator.getCgenConfiguration().setForce(true);
            }
            generator.getCgenConfiguration().setTimestamp(dataMapFile.lastModified());

            if(!hasConfig() && useConfigFromDataMap) {
                generator.prepareArtifacts();
            } else {
                generator.addEntities(filterEntityAction.getFilteredEntities(dataMap));
                generator.addEmbeddables(filterEmbeddableAction.getFilteredEmbeddables(dataMap));
                generator.addQueries(dataMap.getQueryDescriptors());
            }
            generator.execute();
        } catch (Exception exception) {
            throw new GradleException("Error generating classes: ", exception);
        }
    }

    private File[] convertAdditionalDataMaps() throws Exception {
        if (additionalMaps == null) {
            return NO_FILES;
        }

        if (!additionalMaps.isDirectory()) {
            throw new GradleException("'additionalMaps' must be a directory.");
        }

        return additionalMaps.listFiles(
                (dir, name) -> name != null && name.toLowerCase().endsWith(".map.xml")
        );
    }

    ClassGenerationAction createGenerator(DataMap dataMap) {
        CgenConfiguration cgenConfiguration = buildConfiguration(dataMap);
        return cgenConfiguration.isClient() ? new ClientClassGenerationAction(cgenConfiguration) :
                new ClassGenerationAction(cgenConfiguration);
    }

    CgenConfiguration buildConfiguration(DataMap dataMap) {
        CgenConfiguration cgenConfiguration;
        if(hasConfig()) {
            getLogger().info("Using cgen config from pom.xml");
            return cgenConfigFromPom(dataMap);
        } else if(metaData != null && metaData.get(dataMap, CgenConfiguration.class) != null) {
            getLogger().info("Using cgen config from " + dataMap.getName());
            useConfigFromDataMap = true;
            cgenConfiguration = metaData.get(dataMap, CgenConfiguration.class);
            return cgenConfiguration;
        } else {
            getLogger().info("Using default cgen config.");
            cgenConfiguration = new CgenConfiguration();
            cgenConfiguration.setRelPath(getDestDirFile().getPath());
            cgenConfiguration.setDataMap(dataMap);
            return cgenConfiguration;
        }
    }

    private CgenConfiguration cgenConfigFromPom(DataMap dataMap){
        CgenConfiguration cgenConfiguration = new CgenConfiguration();
        cgenConfiguration.setDataMap(dataMap);
        cgenConfiguration.setRelPath(getDestDirFile() != null ? getDestDirFile().toPath() : cgenConfiguration.getRelPath());
        cgenConfiguration.setEncoding(encoding != null ? encoding : cgenConfiguration.getEncoding());
        cgenConfiguration.setMakePairs(makePairs != null ? makePairs : cgenConfiguration.isMakePairs());
        if(mode != null && mode.equals("datamap")) {
            replaceDatamapGenerationMode();
        }
        cgenConfiguration.setArtifactsGenerationMode(mode != null ? mode : cgenConfiguration.getArtifactsGenerationMode());
        cgenConfiguration.setOutputPattern(outputPattern != null ? outputPattern : cgenConfiguration.getOutputPattern());
        cgenConfiguration.setOverwrite(overwrite != null ? overwrite : cgenConfiguration.isOverwrite());
        cgenConfiguration.setSuperPkg(superPkg != null ? superPkg : cgenConfiguration.getSuperPkg());
        cgenConfiguration.setSuperTemplate(superTemplate != null ? superTemplate : cgenConfiguration.getSuperTemplate());
        cgenConfiguration.setTemplate(template != null ? template :  cgenConfiguration.getTemplate());
        cgenConfiguration.setEmbeddableSuperTemplate(embeddableSuperTemplate != null ? embeddableSuperTemplate : cgenConfiguration.getEmbeddableSuperTemplate());
        cgenConfiguration.setEmbeddableTemplate(embeddableTemplate != null ? embeddableTemplate : cgenConfiguration.getEmbeddableTemplate());
        cgenConfiguration.setUsePkgPath(usePkgPath != null ? usePkgPath : cgenConfiguration.isUsePkgPath());
        cgenConfiguration.setCreatePropertyNames(createPropertyNames != null ? createPropertyNames : cgenConfiguration.isCreatePropertyNames());
        cgenConfiguration.setQueryTemplate(queryTemplate != null ? queryTemplate : cgenConfiguration.getQueryTemplate());
        cgenConfiguration.setQuerySuperTemplate(querySuperTemplate != null ? querySuperTemplate : cgenConfiguration.getQuerySuperTemplate());
        cgenConfiguration.setCreatePKProperties(createPKProperties != null ? createPKProperties : cgenConfiguration.isCreatePKProperties());
        cgenConfiguration.setClient(client != null ? client : cgenConfiguration.isClient());
        if(!cgenConfiguration.isMakePairs()) {
            if(template == null) {
                cgenConfiguration.setTemplate(cgenConfiguration.isClient() ? ClientClassGenerationAction.SINGLE_CLASS_TEMPLATE : ClassGenerationAction.SINGLE_CLASS_TEMPLATE);
            }
            if(embeddableTemplate == null) {
                cgenConfiguration.setEmbeddableTemplate(ClassGenerationAction.EMBEDDABLE_SINGLE_CLASS_TEMPLATE);
            }
            if(queryTemplate == null) {
                cgenConfiguration.setQueryTemplate(cgenConfiguration.isClient() ? ClientClassGenerationAction.DATAMAP_SINGLE_CLASS_TEMPLATE : ClassGenerationAction.DATAMAP_SINGLE_CLASS_TEMPLATE);
            }
        }
        return cgenConfiguration;
    }

    private void replaceDatamapGenerationMode() {
        this.mode = ArtifactsGenerationMode.ALL.getLabel();
        this.excludeEntities = "*";
        this.excludeEmbeddables = "*";
        this.includeEntities = "";
    }

    private boolean hasConfig() {
        return destDir != null || destDirName != null || encoding != null || client != null || excludeEntities != null || excludeEmbeddables != null || includeEntities != null ||
                makePairs != null || mode != null || outputPattern != null || overwrite != null || superPkg != null ||
                superTemplate != null || template != null || embeddableTemplate != null || embeddableSuperTemplate != null ||
                usePkgPath != null || createPropertyNames != null || force || queryTemplate != null ||
                querySuperTemplate != null || createPKProperties != null;
    }

    @OutputDirectory
    protected File getDestDirFile() {
        final Reference<File> javaSourceDir = new Reference<>(null);

        if (destDir != null) {
            javaSourceDir.set(destDir);
        } else if (destDirName != null) {
            javaSourceDir.set(getProject().file(destDirName));
        } else {
            getProject().getPlugins().withType(JavaPlugin.class, new Action<JavaPlugin>() {
                @Override
                public void execute(final JavaPlugin plugin) {
                    SourceSetContainer sourceSets = (SourceSetContainer)getProject().getProperties().get("sourceSets");

                    Set<File> sourceDirs = sourceSets.getByName("main").getJava().getSrcDirs();
                    if (sourceDirs != null && !sourceDirs.isEmpty()) {
                        // find java directory, if there is no such dir, take first
                        for(File dir : sourceDirs) {
                            if(dir.getName().endsWith("java")) {
                                javaSourceDir.set(dir);
                                break;
                            }
                        }

                        if (javaSourceDir.get() == null) {
                            javaSourceDir.set(sourceDirs.iterator().next());
                        }
                    }
                }
            });
        }

        if (javaSourceDir.get() == null) {
            throw new InvalidUserDataException("cgen.destDir is not set and there is no Java source sets found.");
        }

        if (!javaSourceDir.get().exists()) {
            javaSourceDir.get().mkdirs();
        }

        return javaSourceDir.get();
    }

    @InputFile
    public File getDataMapFile() {
        return super.getDataMapFile();
    }

    @Optional
    @OutputDirectory
    public File getDestDir() {
        return destDir;
    }

    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    public void setDestDir(String destDir) {
        this.destDirName = destDir;
    }

    public void destDir(String destDir) {
        setDestDir(destDir);
    }

    public void destDir(File destDir) {
        setDestDir(destDir);
    }

    @Optional
    @InputDirectory
    public File getAdditionalMaps() {
        return additionalMaps;
    }

    public void setAdditionalMaps(File additionalMaps) {
        this.additionalMaps = additionalMaps;
    }

    public void additionalMaps(File additionalMaps) {
        setAdditionalMaps(additionalMaps);
    }

    public Boolean isClient() {
        return client;
    }

    public void setClient(Boolean client) {
        this.client = client;
    }

    public void client(boolean client) {
        setClient(client);
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void encoding(String encoding) {
        setEncoding(encoding);
    }

    public String getExcludeEntities() {
        return excludeEntities;
    }

    public void setExcludeEntities(String excludeEntities) {
        this.excludeEntities = excludeEntities;
    }

    public void excludeEntities(String excludeEntities) {
        setExcludeEntities(excludeEntities);
    }

    public String getIncludeEntities() {
        return includeEntities;
    }

    public void setIncludeEntities(String includeEntities) {
        this.includeEntities = includeEntities;
    }

    public void includeEntities(String includeEntities) {
        setIncludeEntities(includeEntities);
    }

    public String getExcludeEmbeddables() {
        return excludeEmbeddables;
    }

    public void setExcludeEmbeddables(String excludeEmbeddables) {
        this.excludeEmbeddables = excludeEmbeddables;
    }

    /**
     * @since 4.1
     * @param excludeEmbeddables
     */
    public void excludeEmbeddables(String excludeEmbeddables) {
        setExcludeEmbeddables(excludeEmbeddables);
    }

    public Boolean isMakePairs() {
        return makePairs;
    }

    public void setMakePairs(Boolean makePairs) {
        this.makePairs = makePairs;
    }

    public void makePairs(boolean makePairs) {
        setMakePairs(makePairs);
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void mode(String mode) {
        setMode(mode);
    }

    public String getOutputPattern() {
        return outputPattern;
    }

    public void setOutputPattern(String outputPattern) {
        this.outputPattern = outputPattern;
    }

    public void outputPattern(String outputPattern) {
        setOutputPattern(outputPattern);
    }

    public Boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(Boolean overwrite) {
        this.overwrite = overwrite;
    }

    public void overwrite(boolean overwrite) {
        setOverwrite(overwrite);
    }

    public String getSuperPkg() {
        return superPkg;
    }

    public void setSuperPkg(String superPkg) {
        this.superPkg = superPkg;
    }

    public void superPkg(String superPkg) {
        setSuperPkg(superPkg);
    }

    public String getSuperTemplate() {
        return superTemplate;
    }

    public void setSuperTemplate(String superTemplate) {
        this.superTemplate = superTemplate;
    }

    public void superTemplate(String superTemplate) {
        setSuperTemplate(superTemplate);
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public void template(String template) {
        setTemplate(template);
    }

    public String getEmbeddableSuperTemplate() {
        return embeddableSuperTemplate;
    }

    public void setEmbeddableSuperTemplate(String embeddableSuperTemplate) {
        this.embeddableSuperTemplate = embeddableSuperTemplate;
    }

    public void embeddableSuperTemplate(String embeddableSuperTemplate) {
        setEmbeddableSuperTemplate(embeddableSuperTemplate);
    }

    public String getEmbeddableTemplate() {
        return embeddableTemplate;
    }

    public void setEmbeddableTemplate(String embeddableTemplate) {
        this.embeddableTemplate = embeddableTemplate;
    }

    public void embeddableTemplate(String embeddableTemplate) {
        setEmbeddableTemplate(embeddableTemplate);
    }

    public Boolean isUsePkgPath() {
        return usePkgPath;
    }

    public void setUsePkgPath(Boolean usePkgPath) {
        this.usePkgPath = usePkgPath;
    }

    public void usePkgPath(boolean usePkgPath) {
        setUsePkgPath(usePkgPath);
    }

    public Boolean isCreatePropertyNames() {
        return createPropertyNames;
    }

    public void setCreatePropertyNames(Boolean createPropertyNames) {
        this.createPropertyNames = createPropertyNames;
    }

    public void createPropertyNames(boolean createPropertyNames) {
        setCreatePropertyNames(createPropertyNames);
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public void force(boolean force) {
        setForce(force);
    }

    public void setCreatePKProperties(Boolean createPKProperties) {
        this.createPKProperties = createPKProperties;
    }

    public void createPKProperties(boolean createPKProperties) {
        setCreatePKProperties(createPKProperties);
    }

}