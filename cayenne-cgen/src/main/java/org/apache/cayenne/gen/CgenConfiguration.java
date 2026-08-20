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

package org.apache.cayenne.gen;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.gen.internal.Utils;
import org.apache.cayenne.gen.xml.CgenExtension;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;
import org.apache.cayenne.validation.ValidationException;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stores configuration for the code generation tool.
 * CgenConfiguration is stored in the dataMap xml file and used by Modeler and CLI tools (Maven, Gradle and Ant).
 *
 * @since 4.1
 */
public class CgenConfiguration implements Serializable, XMLSerializable {

    /**
     * Absolute directory this configuration is anchored to, normally the one holding the Cayenne
     * project XML. Null when cgen is driven directly by a tool (Ant, Maven or Gradle).
     */
    private Path rootProjectPath;

    /**
     * Target directory for the generated classes, stored exactly as it was supplied - either absolute,
     * or relative to the {@code rootProjectPath}. Never null: an empty path means "the root directory"
     * when a root is known, and "not configured" when it isn't.
     */
    private Path cgenOutputPath = Path.of("");

    private final Collection<Artifact> artifacts;
    private Set<String> entityArtifacts;
    private final Collection<String> excludedEntityArtifacts;
    private Set<String> embeddableArtifacts;
    private final Collection<String> excludedEmbeddableArtifacts;

    private String name;
    private String superPkg;
    private DataMap dataMap;

    private ArtifactsGenerationMode artifactsGenerationMode;
    private boolean makePairs;
    private boolean overwrite;
    private boolean usePkgPath;

    private CgenTemplate template;
    private CgenTemplate superTemplate;
    private CgenTemplate embeddableTemplate;
    private CgenTemplate embeddableSuperTemplate;
    /**
     * @since 5.0 renamed from queryTemplate
     */
    private CgenTemplate dataMapTemplate;
    /**
     * @since 5.0 renamed from querySuperTemplate
     */
    private CgenTemplate dataMapSuperTemplate;
    private String outputPattern;
    private String encoding;
    private boolean createPropertyNames;
    /**
     * @since 4.1
     */
    private boolean createPKProperties;

    /**
     * @since 4.2
     */
    private String externalToolConfig;

    public CgenConfiguration() {
        this.name = CgenConfigList.DEFAULT_CONFIG_NAME;

        // isDefault() method should be in sync with the following values
        this.outputPattern = "*.java";
        this.usePkgPath = true;
        this.makePairs = true;
        this.createPKProperties = true;
        this.artifactsGenerationMode = ArtifactsGenerationMode.ENTITY;

        this.artifacts = new ArrayList<>();
        this.entityArtifacts = new HashSet<>();
        this.excludedEntityArtifacts = new ArrayList<>();
        this.embeddableArtifacts = new HashSet<>();
        this.excludedEmbeddableArtifacts = new ArrayList<>();

        this.template = TemplateType.ENTITY_SUBCLASS.defaultTemplate();
        this.superTemplate = TemplateType.ENTITY_SUPERCLASS.defaultTemplate();

        this.dataMapTemplate = TemplateType.DATAMAP_SUBCLASS.defaultTemplate();
        this.dataMapSuperTemplate = TemplateType.DATAMAP_SUPERCLASS.defaultTemplate();

        this.embeddableTemplate = TemplateType.EMBEDDABLE_SUBCLASS.defaultTemplate();
        this.embeddableSuperTemplate = TemplateType.EMBEDDABLE_SUPERCLASS.defaultTemplate();
    }

    /**
     * Builds a default configuration for a DataMap that has no stored cgen config, generating every
     * non-generic entity and embeddable into the given output directory.
     *
     * @param outputDir directory to generate classes into, or {@code null} to derive a default
     * @since 5.0
     */
    public static CgenConfiguration createDefault(DataMap dataMap, Path outputDir) {
        CgenConfiguration config = new CgenConfiguration();
        config.setDataMap(dataMap);
        dataMap.getObjEntities().forEach(config::loadEntity);
        dataMap.getEmbeddables().forEach(config::loadEmbeddable);

        Path root = Utils.rootPathForDataMap(dataMap).orElse(null);
        if (root != null) {
            config.setRootPath(root);
        }
        if (outputDir != null) {
            config.setOutputDir(outputDir);
        } else if (root != null) {
            Utils.getMavenSrcPathForPath(root).map(Path::of).ifPresent(config::setOutputDir);
        }
        return config;
    }

    public void resetCollections() {
        embeddableArtifacts.clear();
        entityArtifacts.clear();
    }

    public String getSuperPkg() {
        return superPkg;
    }

    public void setSuperPkg(String superPkg) {
        this.superPkg = superPkg;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DataMap getDataMap() {
        return dataMap;
    }

    public void setDataMap(DataMap dataMap) {
        this.dataMap = dataMap;
    }

    public void setArtifactsGenerationMode(String mode) {
        if (ArtifactsGenerationMode.ENTITY.getLabel().equalsIgnoreCase(mode)) {
            this.artifactsGenerationMode = ArtifactsGenerationMode.ENTITY;
        } else {
            this.artifactsGenerationMode = ArtifactsGenerationMode.ALL;
        }
    }

    public String getArtifactsGenerationMode() {
        return artifactsGenerationMode.getLabel();
    }


    public boolean isMakePairs() {
        return makePairs;
    }

    public void setMakePairs(boolean makePairs) {
        this.makePairs = makePairs;
    }

    public Path getRootPath() {
        return rootProjectPath;
    }

    /**
     * @param rootProjectPath root path for the Cayenne project this config relates to
     * @see #setOutputDir(Path)
     */
    public void setRootPath(Path rootProjectPath) {
        if (!Objects.requireNonNull(rootProjectPath).isAbsolute()) {
            throw new ValidationException("Project root path '%s' should be absolute.", rootProjectPath);
        }
        this.rootProjectPath = rootProjectPath;
    }

    /**
     * Stores the output directory exactly as supplied. Setting the output directory and setting
     * the root path are independent operations that may happen in any order; they are combined
     * lazily by {@link #outputDirectory()} and at serialization time.
     *
     * @param dir absolute directory, or a directory relative to the {@code rootProjectPath}
     * @see #setRootPath(Path)
     * @see #outputDirectory()
     * @since 5.0 replaces {@code updateOutputPath()}
     */
    public void setOutputDir(Path dir) {
        this.cgenOutputPath = Objects.requireNonNull(dir, "Null output directory");
    }

    /**
     * Calculates the effective output directory by combining the stored output directory with the {@code rootProjectPath}.
     *
     * @return the effective output directory, or an empty {@code Optional} when the stored path is
     * relative and there is no root path to resolve it against
     *
     * @see #setRootPath(Path)
     * @see #setOutputDir(Path)
     * @since 5.0 replaces {@code buildOutputPath()}
     */
    public Optional<Path> outputDirectory() {
        if (cgenOutputPath.isAbsolute()) {
            return Optional.of(cgenOutputPath.normalize());
        }
        if (rootProjectPath != null) {
            return Optional.of(rootProjectPath.resolve(cgenOutputPath).normalize());
        }
        return Optional.empty();
    }

    /**
     * @return the effective output directory
     * @throws ValidationException if no output directory has been configured
     * @see #outputDirectory()
     * @since 5.0
     */
    public Path requireOutputDirectory() {
        return outputDirectory().orElseThrow(() -> new ValidationException("Output directory is not set."));
    }

    /**
     * Moves this configuration to a new root path while keeping the effective output directory pointing
     * at the same physical location. Used when a project is saved to a new location. A configuration
     * that never had an output directory keeps following the root.
     *
     * @param newRoot new absolute root path
     * @see #setRootPath(Path)
     * @since 5.0
     */
    public void rebase(Path newRoot) {
        Path effective = outputDirectory().orElse(null);
        setRootPath(newRoot);
        if (effective != null) {
            this.cgenOutputPath = effective;
        }
    }

    /**
     * Renders the output directory for the {@code destDir} XML tag: relative to the root path whenever
     * the two can be relativized, absolute otherwise, always with Unix separators.
     *
     * @return the {@code destDir} value, never empty
     * @since 5.0
     */
    String encodedDestDir() {
        Path out = cgenOutputPath;
        if (rootProjectPath != null
                && rootProjectPath.isAbsolute()
                && out.isAbsolute()
                && Objects.equals(rootProjectPath.getRoot(), out.getRoot())) {
            out = rootProjectPath.relativize(out);
        }
        String encoded = out.toString();
        return encoded.isEmpty() ? "." : separatorsToUnix(encoded);
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public boolean isUsePkgPath() {
        return usePkgPath;
    }

    public void setUsePkgPath(boolean usePkgPath) {
        this.usePkgPath = usePkgPath;
    }

    public CgenTemplate getTemplate() {
        return template;
    }

    public void setTemplate(CgenTemplate template) {
        this.template = template;
    }

    public CgenTemplate getSuperTemplate() {
        return superTemplate;
    }

    public void setSuperTemplate(CgenTemplate superTemplate) {
        this.superTemplate = superTemplate;
    }

    public CgenTemplate getEmbeddableTemplate() {
        return embeddableTemplate;
    }

    public void setEmbeddableTemplate(CgenTemplate embeddableTemplate) {
        this.embeddableTemplate = embeddableTemplate;
    }

    public CgenTemplate getEmbeddableSuperTemplate() {
        return embeddableSuperTemplate;
    }

    public void setEmbeddableSuperTemplate(CgenTemplate embeddableSuperTemplate) {
        this.embeddableSuperTemplate = embeddableSuperTemplate;
    }

    public CgenTemplate getDataMapTemplate() {
        return dataMapTemplate;
    }

    public void setDataMapTemplate(CgenTemplate dataMapTemplate) {
        this.dataMapTemplate = dataMapTemplate;
    }

    public CgenTemplate getDataMapSuperTemplate() {
        return dataMapSuperTemplate;
    }

    public void setDataMapSuperTemplate(CgenTemplate dataMapSuperTemplate) {
        this.dataMapSuperTemplate = dataMapSuperTemplate;
    }

    public String getOutputPattern() {
        return outputPattern;
    }

    public void setOutputPattern(String outputPattern) {
        this.outputPattern = outputPattern;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public boolean isCreatePropertyNames() {
        return createPropertyNames;
    }

    public void setCreatePropertyNames(boolean createPropertyNames) {
        this.createPropertyNames = createPropertyNames;
    }

    /**
     * @return false
     * @deprecated cgen always regenerates classes, so there is nothing left to force
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public boolean isForce() {
        return true;
    }

    /**
     * does nothing
     *
     * @param force not used
     * @deprecated cgen always regenerates classes, so there is nothing left to force
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public void setForce(boolean force) {
    }

    public boolean isCreatePKProperties() {
        return createPKProperties;
    }

    public void setCreatePKProperties(boolean createPKProperties) {
        this.createPKProperties = createPKProperties;
    }

    public Collection<Artifact> getArtifacts() {
        return artifacts;
    }

    public Set<String> getEntities() {
        return entityArtifacts;
    }

    public Set<String> getEmbeddables() {
        return embeddableArtifacts;
    }

    public String getExternalToolConfig() {
        return externalToolConfig;
    }

    public void setExternalToolConfig(String config) {
        this.externalToolConfig = config;
    }

    void addArtifact(Artifact artifact) {
        artifacts.add(artifact);
    }

    public void loadEntity(ObjEntity entity) {
        if (!entity.isGeneric()) {
            entityArtifacts.add(entity.getName());
        }
    }

    public void loadEmbeddable(Embeddable embeddable) {
        embeddableArtifacts.add(embeddable.getClassName());
    }

    /**
     * @param entities comma separated list of entities to exclude
     * @since 5.0 renamed from {@code loadEntities()}
     */
    public void parseExcludedEntities(String entities) {
        excludedEntityArtifacts.addAll(Arrays.asList(entities.split(",")));
    }

    private String getExcludedEntities() {
        Set<String> existing = dataMap.getObjEntityMap().keySet();
        return excludedEntityArtifacts.stream()
                .filter(existing::contains)
                .sorted()
                .collect(Collectors.joining(","));
    }

    /**
     * @param embeddables comma separated list of embeddables to exclude
     * @since 5.0 renamed from {@code loadEmbeddables()}
     */
    public void parseExcludedEmbeddables(String embeddables) {
        excludedEmbeddableArtifacts.addAll(Arrays.asList(embeddables.split(",")));
    }

    private String getExcludedEmbeddables() {
        Set<String> existing = dataMap.getEmbeddableMap().keySet();
        return excludedEmbeddableArtifacts.stream()
                .filter(existing::contains)
                .sorted()
                .collect(Collectors.joining(","));
    }

    /**
     * @since 5.0 renamed from {@code resolveExcludeEntities()}
     */
    public void resolveExcludedEntities() {
        entityArtifacts = dataMap.getObjEntities()
                .stream()
                .map(ObjEntity::getName)
                .filter(name -> !excludedEntityArtifacts.contains(name))
                .collect(Collectors.toSet());
    }

    /**
     * @since 5.0 renamed from {@code resolveExcludeEmbeddables()}
     */
    public void resolveExcludedEmbeddables() {
        embeddableArtifacts = dataMap.getEmbeddables()
                .stream()
                .map(Embeddable::getClassName)
                .filter(className -> !excludedEmbeddableArtifacts.contains(className))
                .collect(Collectors.toSet());
    }

    /**
     * @since 5.0 renamed from {@code getExcludeEntityArtifacts()}
     */
    public Collection<String> getExcludedEntityArtifacts() {
        return excludedEntityArtifacts;
    }

    /**
     * @since 5.0 renamed from {@code getExcludeEmbeddableArtifacts()}
     */
    public Collection<String> getExcludedEmbeddableArtifacts() {
        return excludedEmbeddableArtifacts;
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor<?> delegate) {
        encoder.start("cgen")
                .attribute("xmlns", CgenExtension.NAMESPACE)
                .simpleTag("name", this.name)
                .simpleTag("excludeEntities", getExcludedEntities())
                .simpleTag("excludeEmbeddables", getExcludedEmbeddables())
                .simpleTag("destDir", encodedDestDir())
                .simpleTag("mode", this.artifactsGenerationMode.getLabel())
                .start("template").cdata(this.template.getData(), !this.template.isFile()).end()
                .start("superTemplate").cdata(this.superTemplate.getData(), !this.superTemplate.isFile()).end()
                .start("embeddableTemplate").cdata(this.embeddableTemplate.getData(), !this.embeddableTemplate.isFile()).end()
                .start("embeddableSuperTemplate").cdata(this.embeddableSuperTemplate.getData(), !this.embeddableSuperTemplate.isFile()).end()
                .start("dataMapTemplate").cdata(this.dataMapTemplate.getData(), !this.dataMapTemplate.isFile()).end()
                .start("dataMapSuperTemplate").cdata(this.dataMapSuperTemplate.getData(), !this.dataMapSuperTemplate.isFile()).end()
                .simpleTag("outputPattern", this.outputPattern)
                .simpleTag("makePairs", Boolean.toString(this.makePairs))
                .simpleTag("usePkgPath", Boolean.toString(this.usePkgPath))
                .simpleTag("overwrite", Boolean.toString(this.overwrite))
                .simpleTag("createPropertyNames", Boolean.toString(this.createPropertyNames))
                .simpleTag("superPkg", separatorsToUnix(this.superPkg))
                .simpleTag("createPKProperties", Boolean.toString(this.createPKProperties))
                .simpleTag("externalToolConfig", this.externalToolConfig)
                .end();
    }

    /**
     * @return is this configuration with all values set to the default
     */
    public boolean isDefault() {
        // this must be is sync with actual default values
        return makePairs
                && usePkgPath
                && !overwrite
                && createPKProperties
                && !createPropertyNames
                && "*.java".equals(outputPattern)
                && template.equals(TemplateType.ENTITY_SUBCLASS.defaultTemplate())
                && superTemplate.equals(TemplateType.ENTITY_SUPERCLASS.defaultTemplate())
                && (superPkg == null || superPkg.isEmpty())
                && (externalToolConfig == null || externalToolConfig.isEmpty());
    }

    private String separatorsToUnix(String path) {
        if (path != null) {
            return path.replace('\\', '/');
        }
        return null;
    }

    public CgenTemplate getTemplateByType(TemplateType type) {
        return switch (type) {
            case ENTITY_SINGLE_CLASS, ENTITY_SUBCLASS -> getTemplate();
            case ENTITY_SUPERCLASS -> getSuperTemplate();
            case EMBEDDABLE_SINGLE_CLASS, EMBEDDABLE_SUBCLASS -> getEmbeddableTemplate();
            case EMBEDDABLE_SUPERCLASS -> getEmbeddableSuperTemplate();
            case DATAMAP_SINGLE_CLASS, DATAMAP_SUBCLASS -> getDataMapTemplate();
            case DATAMAP_SUPERCLASS -> getDataMapSuperTemplate();
        };
    }
}
