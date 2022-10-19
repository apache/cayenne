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

import groovy.lang.Reference;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.dbsync.filter.NamePatternMatcher;
import org.apache.cayenne.dbsync.reverse.configuration.ToolsModule;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.gen.ArtifactsGenerationMode;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClassGenerationActionFactory;
import org.apache.cayenne.gen.CgenTemplate;
import org.apache.cayenne.gen.TemplateType;
import org.apache.cayenne.map.DataMap;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.*;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Set;

/**
 * @since 4.0
 */
public class CgenTask extends BaseCayenneTask {

    private static final File[] NO_FILES = new File[0];

    /**
     * Path to additional DataMap XML files to use for class generation.
     */
    private File additionalMaps;

    /**
     * Destination directory for Java classes (ignoring their package names).
     */
    private File destDir;

    /**
     * Specify generated file encoding if different from the default on current
     * platform. Target encoding must be supported by the JVM running Maven
     * build. Standard encodings supported by Java on all platforms are
     * US-ASCII, ISO-8859-1, UTF-8, UTF-16BE, UTF-16LE, UTF-16. See Sun Java
     * Docs for java.nio.charset.Charset for more information.
     */
    @Input
    @Optional
    private String encoding;

    /**
     * Entities (expressed as a perl5 regex) to exclude from template
     * generation. (Default is to include all entities in the DataMap).
     */
    @Input
    @Optional
    private String excludeEntities;

    /**
     * Entities (expressed as a perl5 regex) to include in template generation.
     * (Default is to include all entities in the DataMap).
     */
    @Input
    @Optional
    private String includeEntities;

    /**
     * @since 4.1
     * Embeddables (expressed as a perl5 regex) to exclude from template
     * generation. (Default is to include all embeddables in the DataMap).
     */
    @Input
    @Optional
    private String excludeEmbeddables;

    /**
     * If set to <code>true</code>, will generate subclass/superclass pairs,
     * with all generated code included in superclass (default is
     * <code>true</code>).
     */
    @Input
    @Optional
    private Boolean makePairs;

    /**
     * Specifies generator iteration target. &quot;entity&quot; performs one
     * iteration for each selected entity. &quot;datamap&quot; performs one
     * iteration per datamap (This is always one iteration since cgen currently
     * supports specifying one-and-only-one datamap).
     * (Default is &quot;entity&quot;)
     */
    @Input
    @Optional
    private String mode;

    /**
     * Name of file for generated output. (Default is &quot;*.java&quot;)
     */
    @Input
    @Optional
    private String outputPattern;

    /**
     * If set to <code>true</code>, will overwrite older versions of generated
     * classes. Ignored unless makepairs is set to <code>false</code>.
     */
    @Input
    @Optional
    private Boolean overwrite;

    /**
     * Java package name of generated superclasses. Ignored unless
     * <code>makepairs</code> set to <code>true</code>. If omitted, each
     * superclass will be assigned the same package as subclass. Note that
     * having superclass in a different package would only make sense when
     * <code>usepkgpath</code> is set to <code>true</code>. Otherwise classes
     * from different packages will end up in the same directory.
     */
    @Input
    @Optional
    private String superPkg;

    /**
     * Location of Velocity template file for Entity superclass generation.
     * Ignored unless <code>makepairs</code> set to <code>true</code>. If
     * omitted, default template is used.
     */
    @Input
    @Optional
    private String superTemplate;

    /**
     * Location of Velocity template file for Entity class generation. If
     * omitted, default template is used.
     */
    @Input
    @Optional
    private String template;

    /**
     * Location of Velocity template file for Embeddable superclass generation.
     * Ignored unless <code>makepairs</code> set to <code>true</code>. If
     * omitted, default template is used.
     */
    @Input
    @Optional
    private String embeddableSuperTemplate;

    /**
     * Location of Velocity template file for Embeddable class generation. If
     * omitted, default template is used.
     */
    @Input
    @Optional
    private String embeddableTemplate;

    /**
     * If set to <code>true</code> (default), a directory tree will be generated
     * in "destDir" corresponding to the class package structure, if set to
     * <code>false</code>, classes will be generated in &quot;destDir&quot;
     * ignoring their package.
     */
    @Input
    @Optional
    private Boolean usePkgPath;

    /**
     * If set to <code>true</code>, will generate String Property names.
     * Default is <code>false</code>.
     */
    @Input
    @Optional
    private Boolean createPropertyNames;

    /**
     * Force run (skip check for files modification time)
     *
     * @since 4.1
     */
    @Input
    private boolean force;

    /**
     * Location of Velocity template file for DataMap class generation.
     * DataMap class provides utilities for usage of the Cayenne queries stored in the DataMap.
     * If omitted, default template is used.
     *
     * @since 4.3 renamed from queryTemplate
     */
    @Input
    @Optional
    private String dataMapTemplate;

    /**
     * Location of Velocity template file for DataMap superclass generation.
     * DataMap class provides utilities for usage of the Cayenne queries stored in the DataMap.
     * If omitted, default template is used.
     * Ignored unless <code>makepairs</code> set to <code>true</code>.
     *
     * @since 4.3 renamed from querySuperTemplate
     */
    @Input
    @Optional
    private String dataMapSuperTemplate;

    /**
     * If set to <code>true</code>, will generate PK attributes as Properties.
     * Default is <code>false</code>.
     *
     * @since 4.1
     */
    @Input
    @Optional
    private Boolean createPKProperties;

    /**
     * Optional path (classpath or filesystem) to external velocity tool configuration file
     *
     * @since 4.2
     */
    @Input
    @Optional
    private String externalToolConfig;


    private String destDirName;

    private DataChannelMetaData metaData;

    private boolean useConfigFromDataMap;

    private transient Injector injector;

    @TaskAction
    public void generate() {
        File dataMapFile = getDataMapFile();

        injector = new ToolsInjectorBuilder()
                .addModule(new ToolsModule(LoggerFactory.getLogger(CgenTask.class)))
                .create();

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
            generator.setLogger(getLogger());

            if (this.force || getProject().hasProperty("force")) {
                generator.getCgenConfiguration().setForce(true);
            }
            generator.getCgenConfiguration().setTimestamp(dataMapFile.lastModified());

            if (!hasConfig() && useConfigFromDataMap) {
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

    /**
     * Loads and returns DataMap based on <code>cgenConfiguration</code> attribute.
     */
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

    /**
     * Factory method to create internal class generator. Called from
     * constructor.
     */
    ClassGenerationAction createGenerator(DataMap dataMap) {
        CgenConfiguration cgenConfiguration = buildConfiguration(dataMap);
        return injector.getInstance(ClassGenerationActionFactory.class).createAction(cgenConfiguration);
    }

    CgenConfiguration buildConfiguration(DataMap dataMap) {
        CgenConfiguration cgenConfiguration;
        if (hasConfig()) {
            getLogger().info("Using cgen config from pom.xml");
            return cgenConfigFromPom(dataMap);
        } else if (metaData != null && metaData.get(dataMap, CgenConfiguration.class) != null) {
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

    private CgenConfiguration cgenConfigFromPom(DataMap dataMap) {
        CgenConfiguration cgenConfiguration = new CgenConfiguration();
        cgenConfiguration.setDataMap(dataMap);
        cgenConfiguration.setRelPath(getDestDirFile() != null ? getDestDirFile().toPath() : cgenConfiguration.getRelPath());
        cgenConfiguration.setEncoding(encoding != null ? encoding : cgenConfiguration.getEncoding());
        cgenConfiguration.setMakePairs(makePairs != null ? makePairs : cgenConfiguration.isMakePairs());
        if (mode != null && mode.equals("datamap")) {
            replaceDatamapGenerationMode();
        }
        cgenConfiguration.setArtifactsGenerationMode(mode != null ? mode : cgenConfiguration.getArtifactsGenerationMode());
        cgenConfiguration.setOutputPattern(outputPattern != null ? outputPattern : cgenConfiguration.getOutputPattern());
        cgenConfiguration.setOverwrite(overwrite != null ? overwrite : cgenConfiguration.isOverwrite());
        cgenConfiguration.setSuperPkg(superPkg != null ? superPkg : cgenConfiguration.getSuperPkg());
        cgenConfiguration.setSuperTemplate(superTemplate != null ? new CgenTemplate(superTemplate, true,TemplateType.ENTITY_SUPERCLASS) : cgenConfiguration.getSuperTemplate());
        cgenConfiguration.setTemplate(template != null ? new CgenTemplate(template, true,TemplateType.ENTITY_SUBCLASS) : cgenConfiguration.getTemplate());
        cgenConfiguration.setEmbeddableSuperTemplate(embeddableSuperTemplate != null ? new CgenTemplate(embeddableSuperTemplate,true,TemplateType.EMBEDDABLE_SUPERCLASS) : cgenConfiguration.getEmbeddableSuperTemplate());
        cgenConfiguration.setEmbeddableTemplate(embeddableTemplate != null ? new CgenTemplate(embeddableTemplate,true,TemplateType.EMBEDDABLE_SUBCLASS) : cgenConfiguration.getEmbeddableTemplate());
        cgenConfiguration.setUsePkgPath(usePkgPath != null ? usePkgPath : cgenConfiguration.isUsePkgPath());
        cgenConfiguration.setCreatePropertyNames(createPropertyNames != null ? createPropertyNames : cgenConfiguration.isCreatePropertyNames());
        cgenConfiguration.setDataMapTemplate(dataMapTemplate != null ? new CgenTemplate(dataMapTemplate,true,TemplateType.DATAMAP_SUBCLASS) : cgenConfiguration.getDataMapTemplate());
        cgenConfiguration.setDataMapSuperTemplate(dataMapSuperTemplate != null ? new CgenTemplate(dataMapSuperTemplate,true,TemplateType.DATAMAP_SUPERCLASS) : cgenConfiguration.getDataMapSuperTemplate());
        cgenConfiguration.setCreatePKProperties(createPKProperties != null ? createPKProperties : cgenConfiguration.isCreatePKProperties());
        cgenConfiguration.setExternalToolConfig(externalToolConfig != null ? externalToolConfig : cgenConfiguration.getExternalToolConfig());
        if (!cgenConfiguration.isMakePairs()) {
            if (template == null) {
                cgenConfiguration.setTemplate(TemplateType.ENTITY_SINGLE_CLASS.defaultTemplate());
            }
            if (embeddableTemplate == null) {
                cgenConfiguration.setEmbeddableTemplate(TemplateType.EMBEDDABLE_SINGLE_CLASS.defaultTemplate());
            }
            if (dataMapTemplate == null) {
                cgenConfiguration.setDataMapTemplate(TemplateType.DATAMAP_SINGLE_CLASS.defaultTemplate());
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
        return destDir != null || destDirName != null || encoding != null || excludeEntities != null || excludeEmbeddables != null || includeEntities != null ||
                makePairs != null || mode != null || outputPattern != null || overwrite != null || superPkg != null ||
                superTemplate != null || template != null || embeddableTemplate != null || embeddableSuperTemplate != null ||
                usePkgPath != null || createPropertyNames != null || force || dataMapTemplate != null ||
                dataMapSuperTemplate != null || createPKProperties != null || externalToolConfig != null;
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
                    SourceSetContainer sourceSets = (SourceSetContainer) getProject().getProperties().get("sourceSets");

                    Set<File> sourceDirs = sourceSets.getByName("main").getJava().getSrcDirs();
                    if (sourceDirs != null && !sourceDirs.isEmpty()) {
                        // find java directory, if there is no such dir, take first
                        for (File dir : sourceDirs) {
                            if (dir.getName().endsWith("java")) {
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

    public Boolean getCreatePKProperties() {
        return createPKProperties;
    }

    public String getExternalToolConfig() {
        return externalToolConfig;
    }

    public void setDataMapTemplate(String dataMapTemplate) {
        this.dataMapTemplate = dataMapTemplate;
    }

    public void dataMapTemplate(String dataMapTemplate) {
        this.dataMapTemplate = dataMapTemplate;
    }

    public String getDataMapTemplate() {
        return dataMapTemplate;
    }

    public void setDataMapSuperTemplate(String dataMapSuperTemplate) {
        this.dataMapSuperTemplate = dataMapSuperTemplate;
    }

    public void dataMapSuperTemplate(String dataMapSuperTemplate) {
        this.dataMapSuperTemplate = dataMapSuperTemplate;
    }

    public String getDataMapSuperTemplate() {
        return dataMapSuperTemplate;
    }

    /**
     * @param excludeEmbeddables pattern to use for embeddable exclusion
     * @since 4.1
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

    public void setExternalToolConfig(String externalToolConfig) {
        this.externalToolConfig = externalToolConfig;
    }

    public void externalToolConfig(String externalToolConfig) {
        setExternalToolConfig(externalToolConfig);
    }
}