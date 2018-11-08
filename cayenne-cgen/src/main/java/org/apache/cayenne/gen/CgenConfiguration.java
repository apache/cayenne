package org.apache.cayenne.gen;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.gen.xml.CgenExtension;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class CgenConfiguration implements Serializable, XMLSerializable {

    private Collection<Artifact> artifacts;
    private Collection<String> entityArtifacts;
    private Collection<String> excludeEntityArtifacts;
    private Collection<String> embeddableArtifacts;
    private Collection<String> excludeEmbeddableArtifacts;

    private String superPkg;
    private DataMap dataMap;

    private ArtifactsGenerationMode artifactsGenerationMode;
    private boolean makePairs;

    private Path rootPath;
    private Path relPath;
    private boolean overwrite;
    private boolean usePkgPath;

    private String template;
    private String superTemplate;
    private String embeddableTemplate;
    private String embeddableSuperTemplate;
    private String queryTemplate;
    private String querySuperTemplate;
    private long timestamp;
    private String outputPattern;
    private String encoding;
    private boolean createPropertyNames;
    private boolean force; // force run generator
    /**
     * @since 4.1
     */
    private boolean createPKProperties;

    private boolean client;

    public CgenConfiguration() {
        this.outputPattern = "*.java";
        this.timestamp = 0L;
        this.usePkgPath = true;
        this.makePairs = true;

        this.artifacts = new ArrayList<>();
        this.entityArtifacts = new ArrayList<>();
        this.excludeEntityArtifacts = new ArrayList<>();
        this.embeddableArtifacts = new ArrayList<>();
        this.excludeEmbeddableArtifacts = new ArrayList<>();
        this.artifactsGenerationMode = ArtifactsGenerationMode.ENTITY;

        this.template = ClassGenerationAction.SUBCLASS_TEMPLATE;
        this.superTemplate = ClassGenerationAction.SUPERCLASS_TEMPLATE;
        this.embeddableTemplate = ClassGenerationAction.EMBEDDABLE_SUBCLASS_TEMPLATE;
        this.embeddableSuperTemplate = ClassGenerationAction.EMBEDDABLE_SUPERCLASS_TEMPLATE;
        this.queryTemplate = ClassGenerationAction.DATAMAP_SUBCLASS_TEMPLATE;
        this.querySuperTemplate = ClassGenerationAction.DATAMAP_SUPERCLASS_TEMPLATE;
    }

    public void resetCollections(){
        embeddableArtifacts.clear();
        entityArtifacts.clear();
    }

    public String getSuperPkg() {
        return superPkg;
    }

    public void setSuperPkg(String superPkg) {
        this.superPkg = superPkg;
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

    public String getArtifactsGenerationMode(){
        return artifactsGenerationMode.getLabel();
    }


    public boolean isMakePairs() {
        return makePairs;
    }

    public void setMakePairs(boolean makePairs) {
        this.makePairs = makePairs;
    }

    public Path getRootPath() {
        return rootPath;
    }

    public void setRootPath(Path rootPath) {
        this.rootPath = rootPath;
    }

    public void setRelPath(Path relPath) {
        this.relPath = relPath;
    }

    public void setRelPath(String path) {
		this.relPath = rootPath != null ? rootPath.relativize(Paths.get(path)) : Paths.get(path);
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

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getSuperTemplate() {
        return superTemplate;
    }

    public void setSuperTemplate(String superTemplate) {
        this.superTemplate = superTemplate;
    }

    public String getEmbeddableTemplate() {
        return embeddableTemplate;
    }

    public void setEmbeddableTemplate(String embeddableTemplate) {
        this.embeddableTemplate = embeddableTemplate;
    }

    public String getEmbeddableSuperTemplate() {
        return embeddableSuperTemplate;
    }

    public void setEmbeddableSuperTemplate(String embeddableSuperTemplate) {
        this.embeddableSuperTemplate = embeddableSuperTemplate;
    }

    public String getQueryTemplate() {
        return queryTemplate;
    }

    public void setQueryTemplate(String queryTemplate) {
        this.queryTemplate = queryTemplate;
    }

    public String getQuerySuperTemplate() {
        return querySuperTemplate;
    }

    public void setQuerySuperTemplate(String querySuperTemplate) {
        this.querySuperTemplate = querySuperTemplate;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public boolean isCreatePKProperties() {
        return createPKProperties;
    }

    public void setCreatePKProperties(boolean createPKProperties) {
        this.createPKProperties = createPKProperties;
    }

    public String getRelPath() {
        if(relPath == null || relPath.toString().isEmpty()) {
            return ".";
        }
        return relPath.toString();
    }

    public String getDir(){
        return rootPath != null ? relPath != null ? rootPath.resolve(relPath).toAbsolutePath().normalize().toString() : rootPath.toString() : null;
    }

    public Collection<Artifact> getArtifacts() {
        return artifacts;
    }

    public Collection<String> getEntities() {
        return entityArtifacts;
    }

    public Collection<String> getEmbeddables() {
        return embeddableArtifacts;
    }

    public Path buildPath() {
		return rootPath != null ? relPath != null ? rootPath.resolve(relPath).toAbsolutePath().normalize() : rootPath : relPath;
	}

    /**
     * @since 4.1
     */
    public void loadEntity(String name) {
        entityArtifacts.add(name);
    }

    /**
     * @since 4.1
     */
    public void loadEmbeddable(String name) {
        embeddableArtifacts.add(name);
    }

    public void loadEntities(String entities) {
        excludeEntityArtifacts.addAll(Arrays.asList(entities.split(",")));
    }

    private String getExcludeEntites() {
        Collection<String> excludeEntities = dataMap.getObjEntities()
                .stream()
                .filter(entity -> !entityArtifacts.contains(entity.getName()))
                .map(ObjEntity::getName)
                .collect(Collectors.toList());
        return org.apache.commons.lang3.StringUtils.join(excludeEntities, ",");
    }

    public void loadEmbeddables(String embeddables) {
        excludeEmbeddableArtifacts.addAll(Arrays.asList(embeddables.split(",")));
    }

    private String getExcludeEmbeddables() {
        Collection<String> excludeEmbeddable = dataMap.getEmbeddables()
                .stream()
                .filter(embeddable -> !embeddableArtifacts.contains(embeddable.getClassName()))
                .map(Embeddable::getClassName)
                .collect(Collectors.toList());
        return org.apache.commons.lang3.StringUtils.join(excludeEmbeddable, ",");
    }

	public void resolveExcludeEntities() {
		entityArtifacts = dataMap.getObjEntities()
				.stream()
				.filter(entity -> !excludeEntityArtifacts.contains(entity.getName()))
				.map(ObjEntity::getName)
				.collect(Collectors.toList());
	}

	public void resolveExcludeEmbeddables() {
    	embeddableArtifacts = dataMap.getEmbeddables()
				.stream()
				.filter(embeddable -> !excludeEmbeddableArtifacts.contains(embeddable.getClassName()))
				.map(Embeddable::getClassName)
				.collect(Collectors.toList());
	}

    public boolean isClient() {
        return client;
    }

    public void setClient(boolean client) {
        this.client = client;
    }

    public void addArtifact(Artifact artifact) {
        artifacts.add(artifact);
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
        encoder.start("cgen")
                .attribute("xmlns", CgenExtension.NAMESPACE)
                .simpleTag("excludeEntities", getExcludeEntites())
                .simpleTag("excludeEmbeddables",getExcludeEmbeddables())
                .simpleTag("destDir", getRelPath())
                .simpleTag("mode", this.artifactsGenerationMode.getLabel())
                .simpleTag("template", this.template)
                .simpleTag("superTemplate", this.superTemplate)
                .simpleTag("outputPattern", this.outputPattern)
                .simpleTag("makePairs", Boolean.toString(this.makePairs))
                .simpleTag("usePkgPath", Boolean.toString(this.usePkgPath))
                .simpleTag("overwrite", Boolean.toString(this.overwrite))
                .simpleTag("createPropertyNames", Boolean.toString(this.createPropertyNames))
                .simpleTag("superPkg", this.superPkg)
                .simpleTag("createPKProperties", Boolean.toString(this.createPKProperties))
                .simpleTag("client", Boolean.toString(client))
                .end();
    }

}
