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

import java.io.File;
import java.util.Set;

import com.sun.org.apache.xpath.internal.operations.Bool;
import groovy.lang.Reference;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.dbsync.filter.NamePatternMatcher;
import org.apache.cayenne.dbsync.reverse.configuration.ToolsModule;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.gen.CgenModule;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClientClassGenerationAction;
import org.apache.cayenne.map.DataMap;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0
 */
public class CgenTask extends BaseCayenneTask {

    private static final File[] NO_FILES = new File[0];

    private File additionalMaps;

    @Input
    private boolean client;

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

    @Input
    @Optional
    private String makePairs;

    @Input
    @Optional
    private String mode;

    @Input
    @Optional
    private String outputPattern;

    @Input
    @Optional
    private String overwrite;

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
    private String usePkgPath;

    @Input
    @Optional
    private String createPropertyNames;

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
    private boolean createPKProperties;

    private String destDirName;

    private DataChannelMetaData metaData;

    @TaskAction
    public void generate() {
        File dataMapFile = getDataMapFile();

        final Injector injector = DIBootstrap.createInjector(new CgenModule(), new ToolsModule(LoggerFactory.getLogger(CgenTask.class)));
        metaData = injector.getInstance(DataChannelMetaData.class);

        CayenneGeneratorMapLoaderAction loaderAction = new CayenneGeneratorMapLoaderAction(injector);
        loaderAction.setMainDataMapFile(dataMapFile);

        CayenneGeneratorEntityFilterAction filterAction = new CayenneGeneratorEntityFilterAction();
        filterAction.setClient(client);
        filterAction.setNameFilter(NamePatternMatcher.build(getLogger(), includeEntities, excludeEntities));

        try {
            loaderAction.setAdditionalDataMapFiles(convertAdditionalDataMaps());

            DataMap dataMap = loaderAction.getMainDataMap();
            ClassGenerationAction generator = this.createGenerator(dataMap);

            generator.setLogger(getLogger());

            if(this.force || getProject().hasProperty("force")) {
                generator.setForce(true);
            }
            generator.setTimestamp(dataMapFile.lastModified());
            generator.setDataMap(dataMap);
            if(generator.getEntities().isEmpty() && generator.getEmbeddables().isEmpty()) {
                generator.addEntities(filterAction.getFilteredEntities(dataMap));
                generator.addEmbeddables(dataMap.getEmbeddables());
                generator.addQueries(dataMap.getQueryDescriptors());
            } else {
                generator.prepareArtifacts();
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

    ClassGenerationAction newGeneratorInstance() {
        return client ? new ClientClassGenerationAction() : new ClassGenerationAction();
    }

    ClassGenerationAction createGenerator(DataMap dataMap) {
        ClassGenerationAction action = this.newGeneratorInstance();

        if(metaData != null && metaData.get(dataMap, ClassGenerationAction.class) != null){
            action = metaData.get(dataMap, ClassGenerationAction.class);
        }

        action.setDestDir(getDestDirFile());
        action.setEncoding(encoding != null ? encoding : action.getEncoding());
        action.setMakePairs(makePairs != null ? Boolean.valueOf(makePairs) : action.isMakePairs());
        action.setArtifactsGenerationMode(mode != null ? mode : action.getArtifactsGenerationMode());
        action.setOutputPattern(outputPattern != null ? outputPattern : action.getOutputPattern());
        action.setOverwrite(overwrite != null ? Boolean.valueOf(overwrite) : action.isOverwrite());
        action.setSuperPkg(superPkg != null ? superPkg : action.getSuperPkg());
        action.setSuperTemplate(superTemplate != null ? superTemplate : action.getSuperclassTemplate());
        action.setTemplate(template != null ? template : action.getTemplate());
        action.setEmbeddableSuperTemplate(embeddableSuperTemplate != null ? embeddableSuperTemplate : action.getEmbeddableSuperTemplate());
        action.setEmbeddableTemplate(embeddableTemplate != null ? embeddableTemplate : action.getEmbeddableTemplate());
        action.setUsePkgPath(usePkgPath != null ? Boolean.valueOf(usePkgPath) : action.isUsePkgPath());
        action.setCreatePropertyNames(createPropertyNames != null ? Boolean.valueOf(createPropertyNames) : action.isCreatePropertyNames());
        action.setQueryTemplate(queryTemplate != null ? queryTemplate : action.getQueryTemplate());
        action.setQuerySuperTemplate(querySuperTemplate != null ? querySuperTemplate : action.getQuerySuperTemplate());
        return action;
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

    public boolean isClient() {
        return client;
    }

    public void setClient(boolean client) {
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

    public boolean isMakePairs() {
        return Boolean.valueOf(makePairs);
    }

    public void setMakePairs(boolean makePairs) {
        this.makePairs = String.valueOf(makePairs);
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

    public boolean isOverwrite() {
        return Boolean.valueOf(overwrite);
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = String.valueOf(overwrite);
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

    public boolean isUsePkgPath() {
        return Boolean.valueOf(usePkgPath);
    }

    public void setUsePkgPath(boolean usePkgPath) {
        this.usePkgPath = String.valueOf(usePkgPath);
    }

    public void usePkgPath(boolean usePkgPath) {
        setUsePkgPath(usePkgPath);
    }

    public boolean isCreatePropertyNames() {
        return Boolean.valueOf(createPropertyNames);
    }

    public void setCreatePropertyNames(boolean createPropertyNames) {
        this.createPropertyNames = String.valueOf(createPropertyNames);
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

    public void setCreatePKProperties(boolean createPKProperties) {
        this.createPKProperties = createPKProperties;
    }

    public void createPKProperties(boolean createPKProperties) {
        setCreatePKProperties(createPKProperties);
    }

}