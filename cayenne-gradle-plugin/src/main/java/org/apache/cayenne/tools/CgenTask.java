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
import java.io.FilenameFilter;
import java.util.Set;

import groovy.lang.Reference;
import org.apache.cayenne.dbsync.filter.NamePatternMatcher;
import org.apache.cayenne.dbsync.reverse.configuration.ToolsModule;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
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
    private boolean makePairs = true;

    @Input
    private String mode = "entity";

    @Input
    private String outputPattern = "*.java";

    @Input
    private boolean overwrite;

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
    private boolean usePkgPath = true;

    @Input
    private boolean createPropertyNames;

    private String destDirName;

    @TaskAction
    public void generate() {
        File dataMapFile = getDataMapFile();

        Injector injector = DIBootstrap.createInjector(new ToolsModule(LoggerFactory.getLogger(CgenTask.class)));

        CayenneGeneratorMapLoaderAction loaderAction = new CayenneGeneratorMapLoaderAction(injector);
        loaderAction.setMainDataMapFile(dataMapFile);

        CayenneGeneratorEntityFilterAction filterAction = new CayenneGeneratorEntityFilterAction();
        filterAction.setClient(client);
        filterAction.setNameFilter(NamePatternMatcher.build(getLogger(), includeEntities, excludeEntities));

        try {
            loaderAction.setAdditionalDataMapFiles(convertAdditionalDataMaps());

            ClassGenerationAction generator = this.createGenerator();
            DataMap dataMap = loaderAction.getMainDataMap();

            generator.setLogger(getLogger());
            generator.setTimestamp(dataMapFile.lastModified());
            generator.setDataMap(dataMap);
            generator.addEntities(filterAction.getFilteredEntities(dataMap));
            generator.addEmbeddables(dataMap.getEmbeddables());
            generator.addQueries(dataMap.getQueryDescriptors());
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

        FilenameFilter mapFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name != null && name.toLowerCase().endsWith(".map.xml");
            }

        };
        return additionalMaps.listFiles(mapFilter);
    }

    ClassGenerationAction newGeneratorInstance() {
        return client ? new ClientClassGenerationAction() : new ClassGenerationAction();
    }

    ClassGenerationAction createGenerator() {
        ClassGenerationAction action = newGeneratorInstance();

        action.setDestDir(getDestDirFile());
        action.setEncoding(encoding);
        action.setMakePairs(makePairs);
        action.setArtifactsGenerationMode(mode);
        action.setOutputPattern(outputPattern);
        action.setOverwrite(overwrite);
        action.setSuperPkg(superPkg);
        action.setSuperTemplate(superTemplate);
        action.setTemplate(template);
        action.setEmbeddableSuperTemplate(embeddableSuperTemplate);
        action.setEmbeddableTemplate(embeddableTemplate);
        action.setUsePkgPath(usePkgPath);
        action.setCreatePropertyNames(createPropertyNames);

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
        return makePairs;
    }

    public void setMakePairs(boolean makePairs) {
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

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
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

    public boolean isUsePkgPath() {
        return usePkgPath;
    }

    public void setUsePkgPath(boolean usePkgPath) {
        this.usePkgPath = usePkgPath;
    }

    public void usePkgPath(boolean usePkgPath) {
        setUsePkgPath(usePkgPath);
    }

    public boolean isCreatePropertyNames() {
        return createPropertyNames;
    }

    public void setCreatePropertyNames(boolean createPropertyNames) {
        this.createPropertyNames = createPropertyNames;
    }

    public void createPropertyNames(boolean createPropertyNames) {
        setCreatePropertyNames(createPropertyNames);
    }

}