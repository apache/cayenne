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
package org.apache.cayenne.gen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.tools.NamePatternMatcher;
import org.apache.commons.logging.Log;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.NullLogSystem;

public class ClassGenerationAction {
    static final String TEMPLATES_DIR_NAME = "templates/v1_2/";

    public static final String SINGLE_CLASS_TEMPLATE = TEMPLATES_DIR_NAME + "singleclass.vm";
    public static final String SUBCLASS_TEMPLATE = TEMPLATES_DIR_NAME + "subclass.vm";
    public static final String SUPERCLASS_TEMPLATE = TEMPLATES_DIR_NAME + "superclass.vm";

    public static final String EMBEDDABLE_SINGLE_CLASS_TEMPLATE = TEMPLATES_DIR_NAME + "embeddable-singleclass.vm";
    public static final String EMBEDDABLE_SUBCLASS_TEMPLATE = TEMPLATES_DIR_NAME + "embeddable-subclass.vm";
    public static final String EMBEDDABLE_SUPERCLASS_TEMPLATE = TEMPLATES_DIR_NAME + "embeddable-superclass.vm";

    public static final String DATAMAP_SINGLE_CLASS_TEMPLATE = TEMPLATES_DIR_NAME + "datamap-singleclass.vm";
    public static final String DATAMAP_SUBCLASS_TEMPLATE = TEMPLATES_DIR_NAME + "datamap-subclass.vm";
    public static final String DATAMAP_SUPERCLASS_TEMPLATE = TEMPLATES_DIR_NAME + "datamap-superclass.vm";

    public static final String SUPERCLASS_PREFIX = "_";
    private static final String WILDCARD = "*";

    protected Collection<Artifact> artifacts;

    protected String superPkg;
    protected DataMap dataMap;

    protected ArtifactsGenerationMode artifactsGenerationMode;
    protected boolean makePairs;

    protected Log logger;
    protected File destDir;
    protected boolean overwrite;
    protected boolean usePkgPath;

    protected String template;
    protected String superTemplate;
    protected String embeddableTemplate;
    protected String embeddableSuperTemplate;
    protected String queryTemplate;
    protected String querySuperTemplate;
    protected long timestamp;
    protected String outputPattern;
    protected String encoding;

    // runtime ivars
    protected VelocityContext context;
    protected Map<String, Template> templateCache;

    public ClassGenerationAction() {
        this.outputPattern = "*.java";
        this.timestamp = System.currentTimeMillis();
        this.usePkgPath = true;
        this.makePairs = true;
        this.context = new VelocityContext();
        this.templateCache = new HashMap<String, Template>(5);

        this.artifacts = new ArrayList<Artifact>();
    }

    protected String defaultTemplateName(TemplateType type) {
        switch (type) {
            case ENTITY_SINGLE_CLASS:
                return ClassGenerationAction.SINGLE_CLASS_TEMPLATE;
            case ENTITY_SUBCLASS:
                return ClassGenerationAction.SUBCLASS_TEMPLATE;
            case ENTITY_SUPERCLASS:
                return ClassGenerationAction.SUPERCLASS_TEMPLATE;
            case EMBEDDABLE_SUBCLASS:
                return ClassGenerationAction.EMBEDDABLE_SUBCLASS_TEMPLATE;
            case EMBEDDABLE_SUPERCLASS:
                return ClassGenerationAction.EMBEDDABLE_SUPERCLASS_TEMPLATE;
            case EMBEDDABLE_SINGLE_CLASS:
                return ClassGenerationAction.EMBEDDABLE_SINGLE_CLASS_TEMPLATE;
            case DATAMAP_SINGLE_CLASS:
                return ClassGenerationAction.DATAMAP_SINGLE_CLASS_TEMPLATE;
            case DATAMAP_SUPERCLASS:
                return ClassGenerationAction.DATAMAP_SUPERCLASS_TEMPLATE;
            case DATAMAP_SUBCLASS:
                return ClassGenerationAction.DATAMAP_SUBCLASS_TEMPLATE;
            default:
                throw new IllegalArgumentException("Invalid template type: " + type);
        }
    }

    protected String customTemplateName(TemplateType type) {
        switch (type) {
            case ENTITY_SINGLE_CLASS:
                return template;
            case ENTITY_SUBCLASS:
                return template;
            case ENTITY_SUPERCLASS:
                return superTemplate;
            case EMBEDDABLE_SUBCLASS:
                return embeddableTemplate;
            case EMBEDDABLE_SUPERCLASS:
                return embeddableSuperTemplate;
            case DATAMAP_SINGLE_CLASS:
                return queryTemplate;
            case DATAMAP_SUPERCLASS:
                return querySuperTemplate;
            case DATAMAP_SUBCLASS:
                return queryTemplate;
            default:
                throw new IllegalArgumentException("Invalid template type: " + type);
        }
    }

    /**
     * Returns a String used to prefix class name to create a generated superclass.
     * Default value is "_".
     */
    protected String getSuperclassPrefix() {
        return ClassGenerationAction.SUPERCLASS_PREFIX;
    }

    /**
     * VelocityContext initialization method called once per artifact.
     */
    protected void resetContextForArtifact(Artifact artifact) {
        StringUtils stringUtils = StringUtils.getInstance();

        String qualifiedClassName = artifact.getQualifiedClassName();
        String packageName = stringUtils.stripClass(qualifiedClassName);
        String className = stringUtils.stripPackageName(qualifiedClassName);

        String qualifiedBaseClassName = artifact.getQualifiedBaseClassName();
        String basePackageName = stringUtils.stripClass(qualifiedBaseClassName);
        String baseClassName = stringUtils.stripPackageName(qualifiedBaseClassName);

        String superClassName = getSuperclassPrefix()
                + stringUtils.stripPackageName(qualifiedClassName);

        String superPackageName = this.superPkg;
        if (superPackageName == null) {
            superPackageName = packageName;
        }

        context.put(Artifact.BASE_CLASS_KEY, baseClassName);
        context.put(Artifact.BASE_PACKAGE_KEY, basePackageName);

        context.put(Artifact.SUB_CLASS_KEY, className);
        context.put(Artifact.SUB_PACKAGE_KEY, packageName);

        context.put(Artifact.SUPER_CLASS_KEY, superClassName);
        context.put(Artifact.SUPER_PACKAGE_KEY, superPackageName);

        context.put(Artifact.OBJECT_KEY, artifact.getObject());
        context.put(Artifact.STRING_UTILS_KEY, stringUtils);
    }

    /**
     * VelocityContext initialization method called once per each artifact and template
     * type combination.
     */
    protected void resetContextForArtifactTemplate(
            Artifact artifact,
            TemplateType templateType) {
        context.put(Artifact.IMPORT_UTILS_KEY, new ImportUtils());
        artifact.postInitContext(context);
    }

    /**
     * Executes class generation once per each artifact.
     */
    public void execute() throws Exception {

        validateAttributes();

        try {
            for (Artifact artifact : artifacts) {
                execute(artifact);
            }
        }
        finally {
            // must reset engine at the end of class generator run to avoid memory
            // leaks and stale templates
            this.templateCache.clear();
        }
    }

    /**
     * Executes class generation for a single artifact.
     */
    protected void execute(Artifact artifact) throws Exception {

        resetContextForArtifact(artifact);

        ArtifactGenerationMode artifactMode = makePairs
                ? ArtifactGenerationMode.GENERATION_GAP
                : ArtifactGenerationMode.SINGLE_CLASS;

        TemplateType[] templateTypes = artifact.getTemplateTypes(artifactMode);
        for (TemplateType type : templateTypes) {

            Writer out = openWriter(type);
            if (out != null) {

                resetContextForArtifactTemplate(artifact, type);
                getTemplate(type).merge(context, out);
                out.close();
            }
        }
    }

    protected Template getTemplate(TemplateType type) throws Exception {

        String templateName = customTemplateName(type);
        if (templateName == null) {
            templateName = defaultTemplateName(type);
        }

        // Velocity < 1.5 has some memory problems, so we will create a VelocityEngine
        // every time, and store templates in an internal cache, to avoid uncontrolled
        // memory leaks... Presumably 1.5 fixes it.

        Template template = templateCache.get(templateName);

        if (template == null) {

            Properties props = new Properties();

            // null logger that will prevent velocity.log from being generated
            props.put(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, NullLogSystem.class
                    .getName());
            props.put("resource.loader", "cayenne");
            props.put("cayenne.resource.loader.class", ClassGeneratorResourceLoader.class
                    .getName());
            props.put("cayenne.resource.loader.cache", "false");

            VelocityEngine velocityEngine = new VelocityEngine();
            velocityEngine.init(props);

            template = velocityEngine.getTemplate(templateName);
            templateCache.put(templateName, template);
        }

        return template;
    }

    /**
     * Validates the state of this class generator. Throws CayenneRuntimeException if it
     * is in an inconsistent state. Called internally from "execute".
     */
    protected void validateAttributes() {
        if (destDir == null) {
            throw new CayenneRuntimeException("'destDir' attribute is missing.");
        }

        if (!destDir.isDirectory()) {
            throw new CayenneRuntimeException("'destDir' is not a directory.");
        }

        if (!destDir.canWrite()) {
            throw new CayenneRuntimeException("Do not have write permissions for "
                    + destDir);
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
    public void setMakePairs(boolean makePairs) {
        this.makePairs = makePairs;
    }

    /**
     * Sets <code>template</code> property.
     */
    public void setTemplate(String template) {
        this.template = template;
    }

    /**
     * Sets <code>superTemplate</code> property.
     */
    public void setSuperTemplate(String superTemplate) {
        this.superTemplate = superTemplate;
    }

    public void setQueryTemplate(String queryTemplate) {
        this.queryTemplate = queryTemplate;
    }

    public void setQuerySuperTemplate(String querySuperTemplate) {
        this.querySuperTemplate = querySuperTemplate;
    }

    /**
     * Sets <code>usepkgpath</code> property.
     */
    public void setUsePkgPath(boolean usePkgPath) {
        this.usePkgPath = usePkgPath;
    }

    /**
     * Sets <code>outputPattern</code> property.
     */
    public void setOutputPattern(String outputPattern) {
        this.outputPattern = outputPattern;
    }

    /**
     * Opens a Writer to write generated output. Returned Writer is mapped to a filesystem
     * file (although subclasses may override that). File location is determined from the
     * current state of VelocityContext and the TemplateType passed as a parameter. Writer
     * encoding is determined from the value of the "encoding" property.
     */
    protected Writer openWriter(TemplateType templateType) throws Exception {

        File outFile = (templateType.isSuperclass())
                ? fileForSuperclass()
                : fileForClass();
        if (outFile == null) {
            return null;
        }

        if (logger != null) {
            String label = templateType.isSuperclass() ? "superclass" : "class";
            logger.info("Generating " + label + " file: " + outFile.getCanonicalPath());
        }

        // return writer with specified encoding
        FileOutputStream out = new FileOutputStream(outFile);

        return (encoding != null)
                ? new OutputStreamWriter(out, encoding)
                : new OutputStreamWriter(out);
    }

    /**
     * Returns a target file where a generated superclass must be saved. If null is
     * returned, class shouldn't be generated.
     */
    protected File fileForSuperclass() throws Exception {

        String packageName = (String) context.get(Artifact.SUPER_PACKAGE_KEY);
        String className = (String) context.get(Artifact.SUPER_CLASS_KEY);

        String filename = NamePatternMatcher.replaceWildcardInStringWithString(
                WILDCARD,
                outputPattern,
                className);
        File dest = new File(mkpath(destDir, packageName), filename);

        // Ignore if the destination is newer than the map
        // (internal timestamp), i.e. has been generated after the map was
        // last saved AND the template is older than the destination file
        if (dest.exists() && !isOld(dest)) {

            if (superTemplate == null) {
                return null;
            }

            File superTemplateFile = new File(superTemplate);
            if (superTemplateFile.lastModified() < dest.lastModified()) {
                return null;
            }
        }

        return dest;
    }

    /**
     * Returns a target file where a generated class must be saved. If null is returned,
     * class shouldn't be generated.
     */
    protected File fileForClass() throws Exception {

        String packageName = (String) context.get(Artifact.SUB_PACKAGE_KEY);
        String className = (String) context.get(Artifact.SUB_CLASS_KEY);

        String filename = NamePatternMatcher.replaceWildcardInStringWithString(
                WILDCARD,
                outputPattern,
                className);
        File dest = new File(mkpath(destDir, packageName), filename);

        if (dest.exists()) {
            // no overwrite of subclasses
            if (makePairs) {
                return null;
            }

            // skip if said so
            if (!overwrite) {
                return null;
            }

            // Ignore if the destination is newer than the map
            // (internal timestamp), i.e. has been generated after the map was
            // last saved AND the template is older than the destination file
            if (!isOld(dest)) {

                if (template == null) {
                    return null;
                }

                File templateFile = new File(template);
                if (templateFile.lastModified() < dest.lastModified()) {
                    return null;
                }
            }
        }

        return dest;
    }

    /**
     * Returns true if <code>file</code> parameter is older than internal timestamp of
     * this class generator.
     */
    protected boolean isOld(File file) {
        return file.lastModified() <= timestamp;
    }

    /**
     * Returns a File object corresponding to a directory where files that belong to
     * <code>pkgName</code> package should reside. Creates any missing diectories below
     * <code>dest</code>.
     */
    protected File mkpath(File dest, String pkgName) throws Exception {

        if (!usePkgPath || pkgName == null) {
            return dest;
        }

        String path = pkgName.replace('.', File.separatorChar);
        File fullPath = new File(dest, path);
        if (!fullPath.isDirectory() && !fullPath.mkdirs()) {
            throw new Exception("Error making path: " + fullPath);
        }

        return fullPath;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Sets file encoding. If set to null, default system encoding will be used.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Sets "superPkg" property value.
     */
    public void setSuperPkg(String superPkg) {
        this.superPkg = superPkg;
    }

    /**
     * @param dataMap The dataMap to set.
     */
    public void setDataMap(DataMap dataMap) {
        this.dataMap = dataMap;
    }

    /**
     * Adds entities to the internal entity list.
     */
    public void addEntities(Collection<ObjEntity> entities) {
        if (artifactsGenerationMode == ArtifactsGenerationMode.ENTITY || 
                artifactsGenerationMode == ArtifactsGenerationMode.ALL ) {
            if (entities != null) {
                for (ObjEntity entity : entities) {
                    artifacts.add(new EntityArtifact(entity));
                }
            }
        }
    }

    public void addEmbeddables(Collection<Embeddable> embeddables) {
        if (artifactsGenerationMode == ArtifactsGenerationMode.ENTITY || 
                artifactsGenerationMode == ArtifactsGenerationMode.ALL ) {
            if (embeddables != null) {
                for (Embeddable embeddable : embeddables) {
                    artifacts.add(new EmbeddableArtifact(embeddable));
                }
            }
        }
    }

    public void addQueries(Collection<Query> queries) {
        if (artifactsGenerationMode == ArtifactsGenerationMode.DATAMAP
                || artifactsGenerationMode == ArtifactsGenerationMode.ALL) {

            // TODO: andrus 10.12.2010 - why not also check for empty query list?? Or
            // create a better API for enabling DataMapArtifact
            if (queries != null) {
                artifacts.add(new DataMapArtifact(dataMap, queries));
            }
        }
    }

    /**
     * Sets an optional shared VelocityContext. Useful with tools like VPP that can set
     * custom values in the context, not known to Cayenne.
     */
    public void setContext(VelocityContext context) {
        this.context = context;
    }

    /**
     * Injects an optional logger that will be used to trace generated files at the info
     * level.
     */
    public void setLogger(Log logger) {
        this.logger = logger;
    }

    public void setEmbeddableTemplate(String embeddableTemplate) {
        this.embeddableTemplate = embeddableTemplate;
    }

    public void setEmbeddableSuperTemplate(String embeddableSuperTemplate) {
        this.embeddableSuperTemplate = embeddableSuperTemplate;
    }

    public void setArtifactsGenerationMode(String mode) {
        if (ArtifactsGenerationMode.ENTITY.getLabel().equalsIgnoreCase(mode)) {
            this.artifactsGenerationMode = ArtifactsGenerationMode.ENTITY;
        }
        else if (ArtifactsGenerationMode.DATAMAP.getLabel().equalsIgnoreCase(mode)) {
            this.artifactsGenerationMode = ArtifactsGenerationMode.DATAMAP;
        }
        else {
            this.artifactsGenerationMode = ArtifactsGenerationMode.ALL;
        }
    }
}
