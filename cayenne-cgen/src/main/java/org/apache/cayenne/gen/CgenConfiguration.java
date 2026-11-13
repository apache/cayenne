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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.project.cgen.CgenDescriptor;
import org.apache.cayenne.project.cgen.CgenPaths;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stores configuration for the code generation tool, used by Modeler and CLI tools (Maven, Gradle and Ant). Wraps a
 * {@link CgenDescriptor} that holds the settings stored in the DataMap.
 *
 * @since 4.1
 */
public class CgenConfiguration {

    private final CgenDescriptor descriptor;

    private Path rootProjectPath;

    // if no root path is set, use the DataMap directory, following the DataMap when it is moved
    private boolean rootPathFromDataMap;

    private final Collection<Artifact> artifacts;
    private Set<String> entityArtifacts;
    private Set<String> embeddableArtifacts;

    private DataMap dataMap;

    // the templates that can't be rebuilt from the path or text stored in the descriptor, null otherwise
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
    private String encoding;

    public CgenConfiguration() {
        this(new CgenDescriptor(), false);
    }

    /**
     * Creates a configuration for the stored settings of a DataMap, selecting all entities and embeddables that are not
     * excluded.
     *
     * @since 6.0
     */
    public CgenConfiguration(CgenDescriptor descriptor, DataMap dataMap) {
        this(descriptor, true);
        setDataMap(dataMap);
        resolveExcludedEntities();
        resolveExcludedEmbeddables();
    }

    private CgenConfiguration(CgenDescriptor descriptor, boolean rootPathFromDataMap) {
        this.descriptor = Objects.requireNonNull(descriptor);
        this.rootPathFromDataMap = rootPathFromDataMap;
        this.artifacts = new ArrayList<>();
        this.entityArtifacts = new HashSet<>();
        this.embeddableArtifacts = new HashSet<>();
    }

    /**
     * Builds a default configuration for a DataMap that has no stored cgen config, generating every
     * non-generic entity and embeddable into the given output directory.
     *
     * @param outputDir directory to generate classes into, or {@code null} to derive a default
     * @since 5.0
     */
    public static CgenConfiguration createDefault(DataMap dataMap, Path outputDir) {
        CgenConfiguration config = new CgenConfiguration(new CgenDescriptor(), true);
        config.setDataMap(dataMap);
        dataMap.getObjEntities().forEach(config::loadEntity);
        dataMap.getEmbeddables().forEach(config::loadEmbeddable);

        Path root = config.getRootPath();
        if (outputDir != null) {
            config.setOutputDir(outputDir);
        } else if (root != null) {
            CgenPaths.mavenSourceDir(root).ifPresent(config::setOutputDir);
        }
        return config;
    }

    /**
     * @since 6.0
     */
    public CgenDescriptor getDescriptor() {
        return descriptor;
    }

    public void resetCollections() {
        embeddableArtifacts.clear();
        entityArtifacts.clear();
    }

    public String getSuperPkg() {
        return descriptor.getSuperPkg();
    }

    public void setSuperPkg(String superPkg) {
        descriptor.setSuperPkg(superPkg);
    }

    public String getName() {
        return descriptor.getName();
    }

    public void setName(String name) {
        descriptor.setName(name);
    }

    public DataMap getDataMap() {
        return dataMap;
    }

    public void setDataMap(DataMap dataMap) {
        this.dataMap = dataMap;
    }

    // makes this configuration behave as if created for the stored settings of the DataMap
    void attachTo(DataMap dataMap) {
        if (this.dataMap != dataMap || !rootPathFromDataMap) {
            // a relative output directory of the stored settings is relative to the DataMap directory, so the one
            // resolved against the root path is made absolute
            if (rootProjectPath != null) {
                outputDirectory().ifPresent(this::storeOutputDir);
            }
            setDataMap(dataMap);
            rootPathFromDataMap = true;
            resolveExcludedEntities();
            resolveExcludedEmbeddables();
        }
    }

    public void setArtifactsGenerationMode(String mode) {
        descriptor.setMode(mode);
    }

    public String getArtifactsGenerationMode() {
        return descriptor.getMode();
    }

    public boolean isMakePairs() {
        return descriptor.isMakePairs();
    }

    public void setMakePairs(boolean makePairs) {
        descriptor.setMakePairs(makePairs);
    }

    /**
     * Returns the root path set for this configuration, or the DataMap directory for a configuration of a DataMap.
     *
     * @see #setRootPath(Path)
     */
    public Path getRootPath() {
        if (rootProjectPath == null && rootPathFromDataMap && dataMap != null) {
            return CgenPaths.dataMapDir(dataMap).orElse(null);
        }
        return rootProjectPath;
    }

    /**
     * Sets the root path, that relative template paths and a relative output directory are resolved against. The
     * stored settings of a DataMap keep a relative output directory relative to the DataMap directory, as they are
     * saved, so for them the output directory is made absolute instead, keeping its effective location.
     *
     * @param rootProjectPath root path for the Cayenne project this config relates to
     * @see #setOutputDir(Path)
     */
    public void setRootPath(Path rootProjectPath) {
        if (!Objects.requireNonNull(rootProjectPath).isAbsolute()) {
            throw new ValidationException("Project root path '%s' should be absolute.", rootProjectPath);
        }

        if (rootPathFromDataMap) {
            storeOutputDir(outputDirectory().orElseGet(() -> rootProjectPath.resolve(storedOutputDir()).normalize()));
        }
        this.rootProjectPath = rootProjectPath;
    }

    /**
     * Stores the output directory as supplied, with "/" separators. Setting the output directory and setting
     * the root path are independent operations that may happen in any order; they are combined lazily by
     * {@link #outputDirectory()}. The exception is a configuration of the stored settings of a DataMap with a root path
     * set, that stores a relative directory resolved against the root path (see {@link #setRootPath(Path)}).
     *
     * @param dir absolute directory, or a directory relative to the root path
     * @see #setRootPath(Path)
     * @see #outputDirectory()
     * @since 5.0 replaces {@code updateOutputPath()}
     */
    public void setOutputDir(Path dir) {
        Objects.requireNonNull(dir, "Null output directory");
        storeOutputDir(rootPathFromDataMap && rootProjectPath != null && !dir.isAbsolute()
                ? rootProjectPath.resolve(dir).normalize()
                : dir);
    }

    private void storeOutputDir(Path dir) {
        String path = dir.toString();
        descriptor.setDestDir(path.isEmpty() ? null : Util.substBackslashes(path));
    }

    // an empty path means "the root directory" when a root is known, and "not configured" when it isn't
    private Path storedOutputDir() {
        String destDir = descriptor.getDestDir();
        if (destDir == null) {
            return Path.of("");
        }

        // a DataMap may come from another OS, so its output directory is only checked here
        try {
            return Path.of(destDir);
        } catch (InvalidPathException e) {
            throw new ValidationException("Invalid output directory '%s': %s", destDir, e.getMessage());
        }
    }

    /**
     * Calculates the effective output directory by combining the stored output directory with the root path.
     *
     * @return the effective output directory, or an empty {@code Optional} when the stored path is
     * relative and there is no root path to resolve it against
     * @throws ValidationException if the stored output directory is not a valid path
     *
     * @see #setRootPath(Path)
     * @see #setOutputDir(Path)
     * @since 5.0 replaces {@code buildOutputPath()}
     */
    public Optional<Path> outputDirectory() {
        Path out = storedOutputDir();
        if (out.isAbsolute()) {
            return Optional.of(out.normalize());
        }
        Path root = getRootPath();
        if (root != null) {
            return Optional.of(root.resolve(out).normalize());
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

    public boolean isOverwrite() {
        return descriptor.isOverwrite();
    }

    public void setOverwrite(boolean overwrite) {
        descriptor.setOverwrite(overwrite);
    }

    public boolean isUsePkgPath() {
        return descriptor.isUsePkgPath();
    }

    public void setUsePkgPath(boolean usePkgPath) {
        descriptor.setUsePkgPath(usePkgPath);
    }

    public CgenTemplate getTemplate() {
        return resolveTemplate(template, descriptor.getTemplate(), TemplateType.ENTITY_SUBCLASS);
    }

    public void setTemplate(CgenTemplate template) {
        descriptor.setTemplate(storedTemplate(template));
        this.template = keptTemplate(template, TemplateType.ENTITY_SUBCLASS);
    }

    public CgenTemplate getSuperTemplate() {
        return resolveTemplate(superTemplate, descriptor.getSuperTemplate(), TemplateType.ENTITY_SUPERCLASS);
    }

    public void setSuperTemplate(CgenTemplate superTemplate) {
        descriptor.setSuperTemplate(storedTemplate(superTemplate));
        this.superTemplate = keptTemplate(superTemplate, TemplateType.ENTITY_SUPERCLASS);
    }

    public CgenTemplate getEmbeddableTemplate() {
        return resolveTemplate(
                embeddableTemplate,
                descriptor.getEmbeddableTemplate(),
                TemplateType.EMBEDDABLE_SUBCLASS);
    }

    public void setEmbeddableTemplate(CgenTemplate embeddableTemplate) {
        descriptor.setEmbeddableTemplate(storedTemplate(embeddableTemplate));
        this.embeddableTemplate = keptTemplate(embeddableTemplate, TemplateType.EMBEDDABLE_SUBCLASS);
    }

    public CgenTemplate getEmbeddableSuperTemplate() {
        return resolveTemplate(
                embeddableSuperTemplate,
                descriptor.getEmbeddableSuperTemplate(),
                TemplateType.EMBEDDABLE_SUPERCLASS);
    }

    public void setEmbeddableSuperTemplate(CgenTemplate embeddableSuperTemplate) {
        descriptor.setEmbeddableSuperTemplate(storedTemplate(embeddableSuperTemplate));
        this.embeddableSuperTemplate = keptTemplate(embeddableSuperTemplate, TemplateType.EMBEDDABLE_SUPERCLASS);
    }

    public CgenTemplate getDataMapTemplate() {
        return resolveTemplate(dataMapTemplate, descriptor.getDataMapTemplate(), TemplateType.DATAMAP_SUBCLASS);
    }

    public void setDataMapTemplate(CgenTemplate dataMapTemplate) {
        descriptor.setDataMapTemplate(storedTemplate(dataMapTemplate));
        this.dataMapTemplate = keptTemplate(dataMapTemplate, TemplateType.DATAMAP_SUBCLASS);
    }

    public CgenTemplate getDataMapSuperTemplate() {
        return resolveTemplate(
                dataMapSuperTemplate,
                descriptor.getDataMapSuperTemplate(),
                TemplateType.DATAMAP_SUPERCLASS);
    }

    public void setDataMapSuperTemplate(CgenTemplate dataMapSuperTemplate) {
        descriptor.setDataMapSuperTemplate(storedTemplate(dataMapSuperTemplate));
        this.dataMapSuperTemplate = keptTemplate(dataMapSuperTemplate, TemplateType.DATAMAP_SUPERCLASS);
    }

    // builds the template from the stored path or text, unless the kept template matches it. The stored template is
    // normalized as it is when saved.
    private static CgenTemplate resolveTemplate(CgenTemplate kept, String stored, TemplateType type) {
        if (kept != null && kept.getData().equals(stored)) {
            return kept;
        }

        TemplateType singleClassType = singleClassType(type);
        String template = CgenDescriptor.normalizeTemplate(
                stored,
                type.pathFromSourceRoot(),
                singleClassType != null ? singleClassType.pathFromSourceRoot() : null);

        if (template.equals(type.pathFromSourceRoot())) {
            return type.defaultTemplate();
        }
        if (singleClassType != null && template.equals(singleClassType.pathFromSourceRoot())) {
            return singleClassType.defaultTemplate();
        }
        return new CgenTemplate(template, false, type);
    }

    private static CgenTemplate keptTemplate(CgenTemplate template, TemplateType type) {
        return template != null && !template.equals(resolveTemplate(null, template.getData(), type)) ? template : null;
    }

    private static String storedTemplate(CgenTemplate template) {
        return template != null ? template.getData() : null;
    }

    // the single class type that shares the template setting of the given type, null for a superclass type
    private static TemplateType singleClassType(TemplateType type) {
        return switch (type) {
            case ENTITY_SUBCLASS -> TemplateType.ENTITY_SINGLE_CLASS;
            case EMBEDDABLE_SUBCLASS -> TemplateType.EMBEDDABLE_SINGLE_CLASS;
            case DATAMAP_SUBCLASS -> TemplateType.DATAMAP_SINGLE_CLASS;
            default -> null;
        };
    }

    public String getOutputPattern() {
        return descriptor.getOutputPattern();
    }

    public void setOutputPattern(String outputPattern) {
        descriptor.setOutputPattern(outputPattern);
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public boolean isCreatePropertyNames() {
        return descriptor.isCreatePropertyNames();
    }

    public void setCreatePropertyNames(boolean createPropertyNames) {
        descriptor.setCreatePropertyNames(createPropertyNames);
    }

    public boolean isCreatePKProperties() {
        return descriptor.isCreatePKProperties();
    }

    public void setCreatePKProperties(boolean createPKProperties) {
        descriptor.setCreatePKProperties(createPKProperties);
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

    /**
     * @since 4.2
     */
    public String getExternalToolConfig() {
        return descriptor.getExternalToolConfig();
    }

    /**
     * @since 4.2
     */
    public void setExternalToolConfig(String config) {
        descriptor.setExternalToolConfig(config);
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
        descriptor.getExcludedEntities().addAll(CgenDescriptor.parseExcludedNames(entities));
    }

    /**
     * @param embeddables comma separated list of embeddables to exclude
     * @since 5.0 renamed from {@code loadEmbeddables()}
     */
    public void parseExcludedEmbeddables(String embeddables) {
        descriptor.getExcludedEmbeddables().addAll(CgenDescriptor.parseExcludedNames(embeddables));
    }

    /**
     * @since 5.0 renamed from {@code resolveExcludeEntities()}
     */
    public void resolveExcludedEntities() {
        Set<String> excluded = descriptor.getExcludedEntities();
        entityArtifacts = dataMap.getObjEntities()
                .stream()
                .map(ObjEntity::getName)
                .filter(name -> !excluded.contains(name))
                .collect(Collectors.toSet());
    }

    /**
     * @since 5.0 renamed from {@code resolveExcludeEmbeddables()}
     */
    public void resolveExcludedEmbeddables() {
        Set<String> excluded = descriptor.getExcludedEmbeddables();
        embeddableArtifacts = dataMap.getEmbeddables()
                .stream()
                .map(Embeddable::getClassName)
                .filter(className -> !excluded.contains(className))
                .collect(Collectors.toSet());
    }

    /**
     * @since 5.0 renamed from {@code getExcludeEntityArtifacts()}
     */
    public Collection<String> getExcludedEntityArtifacts() {
        return descriptor.getExcludedEntities();
    }

    /**
     * @since 5.0 renamed from {@code getExcludeEmbeddableArtifacts()}
     */
    public Collection<String> getExcludedEmbeddableArtifacts() {
        return descriptor.getExcludedEmbeddables();
    }

    /**
     * @return is this configuration with all values set to the default
     */
    public boolean isDefault() {
        // must be in sync with the CgenDescriptor defaults
        return isMakePairs()
                && isUsePkgPath()
                && !isOverwrite()
                && isCreatePKProperties()
                && !isCreatePropertyNames()
                && "*.java".equals(getOutputPattern())
                && getTemplate().equals(TemplateType.ENTITY_SUBCLASS.defaultTemplate())
                && getSuperTemplate().equals(TemplateType.ENTITY_SUPERCLASS.defaultTemplate())
                && (getSuperPkg() == null || getSuperPkg().isEmpty())
                && (getExternalToolConfig() == null || getExternalToolConfig().isEmpty());
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
