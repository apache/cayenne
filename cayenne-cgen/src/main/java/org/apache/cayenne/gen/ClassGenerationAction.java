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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.apache.velocity.runtime.resource.util.StringResourceRepositoryImpl;
import org.apache.velocity.tools.ToolManager;
import org.apache.velocity.tools.config.ConfigurationUtils;
import org.apache.velocity.tools.config.FactoryConfiguration;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class ClassGenerationAction {

    public static final String SUPERCLASS_PREFIX = "_";
    private static final String WILDCARD = "*";
    private static final String CUSTOM_TEMPLATE_REPO = "customTemplateRepo";

    protected CgenConfiguration configuration;
    protected Logger logger;

    protected Context context;
    protected final Map<String, Template> templateCache;

    private ToolsUtilsFactory utilsFactory;
    private MetadataUtils metadataUtils;

    /**
     * Optionally allows user-defined tools besides {@link ImportUtils} for working with velocity templates.<br/>
     * To use this feature, either set the java system property {@code -Dorg.apache.velocity.tools=tools.properties}
     * or set the {@code externalToolConfig} property to "tools.properties" in {@code CgenConfiguration}. Then
     * create the file "tools.properties" in the working directory or in the root of the classpath with content
     * like this:
     * <pre>
     * tools.toolbox = application
     * tools.application.myTool = com.mycompany.MyTool</pre>
     * Then the methods in the MyTool class will be available for use in the template like ${myTool.myMethod(arg)}
     */
    public ClassGenerationAction(CgenConfiguration configuration) {
        this.configuration = configuration;
        String toolConfigFile = configuration.getExternalToolConfig();

        if (System.getProperty("org.apache.velocity.tools") != null || toolConfigFile != null) {
            ToolManager manager = new ToolManager(true, true);
            if (toolConfigFile != null) {
                FactoryConfiguration config = ConfigurationUtils.find(toolConfigFile);
                manager.getToolboxFactory().configure(config);
            }
            this.context = manager.createContext();
        } else {
            this.context = new VelocityContext();
        }
        this.templateCache = new HashMap<>(5);
    }

    protected void resetContextForArtifact(Artifact artifact) {
        StringUtils stringUtils = StringUtils.getInstance();

        String qualifiedClassName = artifact.getQualifiedClassName();
        String packageName = stringUtils.stripClass(qualifiedClassName);
        String className = stringUtils.stripPackageName(qualifiedClassName);

        String qualifiedBaseClassName = artifact.getQualifiedBaseClassName();
        String basePackageName = stringUtils.stripClass(qualifiedBaseClassName);
        String baseClassName = stringUtils.stripPackageName(qualifiedBaseClassName);

        String superClassName = SUPERCLASS_PREFIX + stringUtils.stripPackageName(qualifiedClassName);

        String superPackageName = configuration.getSuperPkg();
        if (superPackageName == null || superPackageName.isEmpty()) {
            superPackageName = packageName + ".auto";
        }

        context.put(Artifact.BASE_CLASS_KEY, baseClassName);
        context.put(Artifact.BASE_PACKAGE_KEY, basePackageName);

        context.put(Artifact.SUB_CLASS_KEY, className);
        context.put(Artifact.SUB_PACKAGE_KEY, packageName);

        context.put(Artifact.SUPER_CLASS_KEY, superClassName);
        context.put(Artifact.SUPER_PACKAGE_KEY, superPackageName);

        context.put(Artifact.OBJECT_KEY, artifact.getObject());
        context.put(Artifact.STRING_UTILS_KEY, stringUtils);

        context.put(Artifact.CREATE_PROPERTY_NAMES, configuration.isCreatePropertyNames());
        context.put(Artifact.CREATE_PK_PROPERTIES, configuration.isCreatePKProperties());
    }

    /**
     * VelocityContext initialization method called once per each artifact and
     * template type combination.
     */
    void resetContextForArtifactTemplate(Artifact artifact) {
        ImportUtils importUtils = utilsFactory.createImportUtils();
        context.put(Artifact.IMPORT_UTILS_KEY, importUtils);
        context.put(Artifact.PROPERTY_UTILS_KEY, utilsFactory.createPropertyUtils(logger, importUtils));
        context.put(Artifact.METADATA_UTILS_KEY, metadataUtils);
        artifact.postInitContext(context);
    }

    /**
     * Adds entities to the internal entity list.
     *
     * @param entities collection
     * @since 4.0 throws exception
     */
    public void addEntities(Collection<ObjEntity> entities) {
        if (entities != null) {
            for (ObjEntity entity : entities) {
                configuration.addArtifact(new EntityArtifact(entity));
            }
        }
    }

    public void addEmbeddables(Collection<Embeddable> embeddables) {
        if (embeddables != null) {
            for (Embeddable embeddable : embeddables) {
                configuration.addArtifact(new EmbeddableArtifact(embeddable));
            }
        }
    }

    /**
     * @param dataMap to add to the list of artifacts to generate
     * @since 5.0 replaces removed {@code addQueries()} method
     */
    public void addDataMap(DataMap dataMap) {
        // data map should be used only in ArtifactsGenerationMode.ALL
        if (!configuration.getArtifactsGenerationMode().equals(ArtifactsGenerationMode.ALL.getLabel())) {
            return;
        }

        Artifact artifact = new DataMapArtifact(configuration.getDataMap(), dataMap.getQueryDescriptors());
        if (!configuration.getArtifacts().contains(artifact)) {
            configuration.addArtifact(artifact);
        }
    }

    /**
     * @since 4.1
     */
    public void prepareArtifacts() {
        configuration.getArtifacts().clear();
        addEntities(configuration.getEntities().stream()
                .map(entity -> configuration.getDataMap().getObjEntity(entity))
                .collect(Collectors.toList()));
        addEmbeddables(configuration.getEmbeddables().stream()
                .map(embeddable -> configuration.getDataMap().getEmbeddable(embeddable))
                .collect(Collectors.toList()));
        addDataMap(configuration.getDataMap());
    }

    /**
     * Executes class generation once per each artifact.
     */
    public void execute() throws Exception {

        validateAttributes();

        try {
            for (Artifact artifact : configuration.getArtifacts()) {
                execute(artifact);
            }
        } finally {
            // must reset engine at the end of class generator run to avoid memory leaks and stale templates
            templateCache.clear();
        }
    }

    /**
     * Executes class generation for a single artifact.
     */
    protected void execute(Artifact artifact) throws Exception {

        resetContextForArtifact(artifact);

        ArtifactGenerationMode artifactMode = configuration.isMakePairs()
                ? ArtifactGenerationMode.GENERATION_GAP
                : ArtifactGenerationMode.SINGLE_CLASS;

        TemplateType[] templateTypes = artifact.getTemplateTypes(artifactMode);
        for (TemplateType type : templateTypes) {
            try (Writer out = openWriter(type)) {
                if (out != null) {
                    resetContextForArtifactTemplate(artifact);
                    getTemplate(type).merge(context, out);
                }
            }
        }
    }

    protected Template getTemplate(TemplateType type) {
        Properties props = new Properties();
        initVelocityProperties(props, type);
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.init(props);
        return velocityEngine.getTemplate(configuration.getTemplateByType(type).getName());
    }

    protected void initVelocityProperties(Properties props, TemplateType type) {
        CgenTemplate template = configuration.getTemplateByType(type);
        if (template.isFile()) {
            props.put(RuntimeConstants.RESOURCE_LOADERS, "cayenne");
            props.put("resource.loader.cayenne.class", ClassGeneratorResourceLoader.class.getName());
            props.put("resource.loader.cayenne.cache", "false");
            if (configuration.getRootPath() != null) {
                props.put("resource.loader.cayenne.root", configuration.getRootPath());
            }
        } else {
            props.put(RuntimeConstants.RESOURCE_LOADERS, "string");
            props.put("resource.loader.string.class", StringResourceLoader.class.getName());
            props.put("resource.loader.string.repository.name", CUSTOM_TEMPLATE_REPO);

            StringResourceRepository repo = new StringResourceRepositoryImpl();
            repo.putStringResource(template.getName(), template.getData());
            StringResourceLoader.setRepository(CUSTOM_TEMPLATE_REPO, repo);
        }
    }

    /**
     * Validates the state of this class generator.
     * Throws CayenneRuntimeException if it is in an inconsistent state.
     * Called internally from "execute".
     */
    protected void validateAttributes() {
        Path dir = configuration.buildOutputPath();
        if (dir == null) {
            throw new CayenneRuntimeException("Output directory is not set.");
        }
        if (Files.notExists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new CayenneRuntimeException("Can't create output directory '%s'", dir);
            }
        }

        if (!Files.isDirectory(dir)) {
            throw new CayenneRuntimeException("'%s' is not a directory.", dir);
        }

        if (!Files.isWritable(dir)) {
            throw new CayenneRuntimeException("No write permission for the output directory '%s'", dir);
        }
    }

    /**
     * Opens a Writer to write generated output. Returned Writer is mapped to a
     * filesystem file (although subclasses may override that). File location is
     * determined from the current state of VelocityContext and the TemplateType
     * passed as a parameter. Writer encoding is determined from the value of
     * the "encoding" property.
     */
    protected Writer openWriter(TemplateType templateType) throws Exception {

        File outFile = (templateType.isSuperclass()) ? fileForSuperclass() : fileForClass();
        if (outFile == null) {
            return null;
        }

        return new GeneratedFileWriter(outFile, templateType);
    }

    /**
     * A callback invoked after a generated file was written to disk. Files whose generated contents match
     * what is already on disk are left alone, so this is only called for files that actually changed.
     *
     * @since 5.0
     */
    protected void fileWritten(File file, TemplateType templateType) {
    }

    /**
     * Returns a target file where a generated superclass must be saved. Superclasses are always
     * regenerated, overwriting any previous version.
     */
    private File fileForSuperclass() throws Exception {

        String packageName = (String) context.get(Artifact.SUPER_PACKAGE_KEY);
        String className = (String) context.get(Artifact.SUPER_CLASS_KEY);

        File dir = mkpath(configuration.buildOutputPath().toFile(), packageName);
        String fileName = StringUtils
                .getInstance()
                .replaceWildcardInStringWithString(WILDCARD, configuration.getOutputPattern(), className);

        return new File(dir, fileName);
    }

    /**
     * Returns a target file where a generated class must be saved. If null is
     * returned, class shouldn't be generated.
     */
    private File fileForClass() throws Exception {

        String packageName = (String) context.get(Artifact.SUB_PACKAGE_KEY);
        String className = (String) context.get(Artifact.SUB_CLASS_KEY);

        String filename = StringUtils.getInstance().replaceWildcardInStringWithString(WILDCARD, configuration.getOutputPattern(), className);
        File dest = new File(mkpath(configuration.buildOutputPath().toFile(), packageName), filename);

        if (dest.exists()) {
            // no overwrite of subclasses
            if (configuration.isMakePairs()) {
                return null;
            }

            // skip if said so
            if (!configuration.isOverwrite()) {
                return null;
            }
        }

        return dest;
    }

    /**
     * Returns a File object corresponding to a directory where files that
     * belong to <code>pkgName</code> package should reside. Creates any missing
     * diectories below <code>dest</code>.
     */
    private File mkpath(File dest, String pkgName) throws Exception {

        if (!configuration.isUsePkgPath() || pkgName == null) {
            return dest;
        }

        String path = pkgName.replace('.', File.separatorChar);
        File fullPath = new File(dest, path);
        if (!fullPath.isDirectory() && !fullPath.mkdirs()) {
            throw new Exception("Error making path: " + fullPath);
        }

        return fullPath;
    }

    /**
     * Injects an optional logger that will be used to trace generated files at
     * the info level.
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * @since 4.1
     */
    public CgenConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Sets an optional shared VelocityContext. Useful with tools like VPP that
     * can set custom values in the context, not known to Cayenne.
     */
    public void setContext(Context context) {
        this.context = context;
    }

    public ToolsUtilsFactory getUtilsFactory() {
        return utilsFactory;
    }

    public void setUtilsFactory(ToolsUtilsFactory utilsFactory) {
        this.utilsFactory = utilsFactory;
    }

    public void setMetadataUtils(MetadataUtils metadataUtils) {
        this.metadataUtils = metadataUtils;
    }

    public MetadataUtils getMetadataUtils() {
        return metadataUtils;
    }

    /**
     * Collects generated class text in memory, and on close writes it to the target file, but only if it
     * differs from what the file already contains.
     */
    private class GeneratedFileWriter extends Writer {

        private final File file;
        private final TemplateType templateType;
        private final StringBuilder buffer;

        GeneratedFileWriter(File file, TemplateType templateType) {
            this.file = file;
            this.templateType = templateType;
            this.buffer = new StringBuilder();
        }

        @Override
        public void write(char[] chars, int offset, int length) {
            buffer.append(chars, offset, length);
        }

        @Override
        public void flush() {
            // nothing to flush - the file is written on close
        }

        @Override
        public void close() throws IOException {

            String encoding = configuration.getEncoding();
            Charset charset = encoding != null ? Charset.forName(encoding) : Charset.defaultCharset();
            byte[] generated = buffer.toString().getBytes(charset);

            if (file.isFile()
                    && file.length() == generated.length
                    && Arrays.equals(generated, Files.readAllBytes(file.toPath()))) {
                return;
            }

            Files.write(file.toPath(), generated);

            if (logger != null) {
                String label = templateType.isSuperclass() ? "superclass" : "class";
                logger.info("Generating {} file: {}", label, file.getCanonicalPath());
            }

            fileWritten(file, templateType);
        }
    }
}
