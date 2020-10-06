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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.ToolManager;
import org.apache.velocity.tools.config.ConfigurationUtils;
import org.apache.velocity.tools.config.FactoryConfiguration;
import org.slf4j.Logger;

public class ClassGenerationAction {

	private static final String TEMPLATES_DIR_NAME = "templates/v4_1/";

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

	/**
	 * @since 4.1
	 */
	protected CgenConfiguration cgenConfiguration;
	protected Logger logger;

    // runtime ivars
    protected Context context;
    protected Map<String, Template> templateCache;

    private ToolsUtilsFactory utilsFactory;
	private MetadataUtils metadataUtils;

	/**
	Optionally allows user-defined tools besides {@link ImportUtils} for working with velocity templates.<br/>
	To use this feature, either set the java system property {@code -Dorg.apache.velocity.tools=tools.properties}
	or set the {@code externalToolConfig} property to "tools.properties" in {@code CgenConfiguration}. Then 
	create the file "tools.properties" in the working directory or in the root of the classpath with content 
	like this: 
	<pre>
	tools.toolbox = application
	tools.application.myTool = com.mycompany.MyTool</pre>
	Then the methods in the MyTool class will be available for use in the template like ${myTool.myMethod(arg)}
	 */
	public ClassGenerationAction(CgenConfiguration cgenConfig) {
		this.cgenConfiguration = cgenConfig;
		String toolConfigFile = cgenConfig.getExternalToolConfig();
		
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

	public String defaultTemplateName(TemplateType type) {
		switch (type) {
			case ENTITY_SINGLE_CLASS:
				return SINGLE_CLASS_TEMPLATE;
			case ENTITY_SUBCLASS:
				return SUBCLASS_TEMPLATE;
			case ENTITY_SUPERCLASS:
				return SUPERCLASS_TEMPLATE;
			case EMBEDDABLE_SUBCLASS:
				return EMBEDDABLE_SUBCLASS_TEMPLATE;
			case EMBEDDABLE_SUPERCLASS:
				return EMBEDDABLE_SUPERCLASS_TEMPLATE;
			case EMBEDDABLE_SINGLE_CLASS:
				return EMBEDDABLE_SINGLE_CLASS_TEMPLATE;
			case DATAMAP_SINGLE_CLASS:
				return DATAMAP_SINGLE_CLASS_TEMPLATE;
			case DATAMAP_SUPERCLASS:
				return DATAMAP_SUPERCLASS_TEMPLATE;
			case DATAMAP_SUBCLASS:
				return DATAMAP_SUBCLASS_TEMPLATE;
			default:
				throw new IllegalArgumentException("Invalid template type: " + type);
		}
	}

	public String customTemplateName(TemplateType type) {
		switch (type) {
			case ENTITY_SINGLE_CLASS:
				return cgenConfiguration.getTemplate();
			case ENTITY_SUBCLASS:
				return cgenConfiguration.getTemplate();
			case ENTITY_SUPERCLASS:
				return cgenConfiguration.getSuperTemplate();
			case EMBEDDABLE_SINGLE_CLASS:
				return cgenConfiguration.getEmbeddableTemplate();
			case EMBEDDABLE_SUBCLASS:
				return cgenConfiguration.getEmbeddableTemplate();
			case EMBEDDABLE_SUPERCLASS:
				return cgenConfiguration.getEmbeddableSuperTemplate();
			case DATAMAP_SINGLE_CLASS:
				return cgenConfiguration.getQueryTemplate();
			case DATAMAP_SUPERCLASS:
				return cgenConfiguration.getQuerySuperTemplate();
			case DATAMAP_SUBCLASS:
				return cgenConfiguration.getQueryTemplate();
			default:
				throw new IllegalArgumentException("Invalid template type: " + type);
		}
	}

    /**
     * VelocityContext initialization method called once per artifact.
     */
    public void resetContextForArtifact(Artifact artifact) {
        StringUtils stringUtils = StringUtils.getInstance();

        String qualifiedClassName = artifact.getQualifiedClassName();
        String packageName = stringUtils.stripClass(qualifiedClassName);
        String className = stringUtils.stripPackageName(qualifiedClassName);

        String qualifiedBaseClassName = artifact.getQualifiedBaseClassName();
        String basePackageName = stringUtils.stripClass(qualifiedBaseClassName);
        String baseClassName = stringUtils.stripPackageName(qualifiedBaseClassName);

        String superClassName = SUPERCLASS_PREFIX + stringUtils.stripPackageName(qualifiedClassName);

        String superPackageName = cgenConfiguration.getSuperPkg();
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

        context.put(Artifact.CREATE_PROPERTY_NAMES, cgenConfiguration.isCreatePropertyNames());
        context.put(Artifact.CREATE_PK_PROPERTIES, cgenConfiguration.isCreatePKProperties());
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
	 * @param entities collection
	 *
	 * @since 4.0 throws exception
	 */
	public void addEntities(Collection<ObjEntity> entities) {
		if (entities != null) {
			for (ObjEntity entity : entities) {
				cgenConfiguration.addArtifact(new EntityArtifact(entity));
			}
		}
	}

	public void addEmbeddables(Collection<Embeddable> embeddables) {
		if (embeddables != null) {
			for (Embeddable embeddable : embeddables) {
				cgenConfiguration.addArtifact(new EmbeddableArtifact(embeddable));
			}
		}
	}

	public void addQueries(Collection<QueryDescriptor> queries) {
		if (cgenConfiguration.getArtifactsGenerationMode().equals(ArtifactsGenerationMode.ALL.getLabel())) {
			// TODO: andrus 10.12.2010 - why not also check for empty query list??
			// Or create a better API for enabling DataMapArtifact
			if (queries != null) {
				Artifact artifact = new DataMapArtifact(cgenConfiguration.getDataMap(), queries);
				if(!cgenConfiguration.getArtifacts().contains(artifact)) {
					cgenConfiguration.addArtifact(artifact);
				}
			}
		}
	}

	/**
	 * @since 4.1
	 */
	public void prepareArtifacts() {
		cgenConfiguration.getArtifacts().clear();
		addEntities(cgenConfiguration.getEntities().stream()
				.map(entity -> cgenConfiguration.getDataMap().getObjEntity(entity))
				.collect(Collectors.toList()));
		addEmbeddables(cgenConfiguration.getEmbeddables().stream()
				.map(embeddable -> cgenConfiguration.getDataMap().getEmbeddable(embeddable))
				.collect(Collectors.toList()));
		addQueries(cgenConfiguration.getDataMap().getQueryDescriptors());
    }

	/**
	 * Executes class generation once per each artifact.
	 */
	public void execute() throws Exception {

		validateAttributes();

		try {
			for (Artifact artifact : cgenConfiguration.getArtifacts()) {
				execute(artifact);
			}
		} finally {
			// must reset engine at the end of class generator run to avoid
			// memory
			// leaks and stale templates
			templateCache.clear();
		}
	}

	/**
	 * Executes class generation for a single artifact.
	 */
	protected void execute(Artifact artifact) throws Exception {

		resetContextForArtifact(artifact);

		ArtifactGenerationMode artifactMode = cgenConfiguration.isMakePairs() ? ArtifactGenerationMode.GENERATION_GAP
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

	private Template getTemplate(TemplateType type) {

		String templateName = customTemplateName(type);
		if (templateName == null) {
			templateName = defaultTemplateName(type);
		}

		// Velocity < 1.5 has some memory problems, so we will create a VelocityEngine every time,
		// and store templates in an internal cache, to avoid uncontrolled memory leaks...
		// Presumably 1.5 fixes it.

		Template template = templateCache.get(templateName);

		if (template == null) {
			Properties props = new Properties();

			props.put("resource.loaders", "cayenne");
			props.put("resource.loader.cayenne.class", ClassGeneratorResourceLoader.class.getName());
			props.put("resource.loader.cayenne.cache", "false");
			if (cgenConfiguration.getRootPath() != null) {
				props.put("resource.loader.cayenne.path", cgenConfiguration.getRootPath().toString());
			}

			VelocityEngine velocityEngine = new VelocityEngine();
			velocityEngine.init(props);

			template = velocityEngine.getTemplate(templateName);
			templateCache.put(templateName, template);
		}

		return template;
	}

	/**
	 * Validates the state of this class generator.
	 * Throws CayenneRuntimeException if it is in an inconsistent state.
	 * Called internally from "execute".
	 */
	private void validateAttributes() {
		Path dir = cgenConfiguration.buildPath();
		if (dir == null) {
			throw new CayenneRuntimeException("'rootPath' attribute is missing.");
		}
		if(Files.notExists(dir)) {
			try {
				Files.createDirectories(dir);
			} catch (IOException e) {
				throw new CayenneRuntimeException("can't create directory");
			}
		}

		if (!Files.isDirectory(dir)) {
			throw new CayenneRuntimeException("'destDir' is not a directory.");
		}

		if (!Files.isWritable(dir)) {
			throw new CayenneRuntimeException("Do not have write permissions for %s", dir);
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

		if (logger != null) {
			String label = templateType.isSuperclass() ? "superclass" : "class";
			logger.info("Generating " + label + " file: " + outFile.getCanonicalPath());
		}

		// return writer with specified encoding
		FileOutputStream out = new FileOutputStream(outFile);

		return (cgenConfiguration.getEncoding() != null) ? new OutputStreamWriter(out, cgenConfiguration.getEncoding()) : new OutputStreamWriter(out);
	}

	/**
	 * Returns a target file where a generated superclass must be saved. If null
	 * is returned, class shouldn't be generated.
	 */
	private File fileForSuperclass() throws Exception {

		String packageName = (String) context.get(Artifact.SUPER_PACKAGE_KEY);
		String className = (String) context.get(Artifact.SUPER_CLASS_KEY);

		String filename = StringUtils.getInstance().replaceWildcardInStringWithString(WILDCARD, cgenConfiguration.getOutputPattern(), className);
		File dest = new File(mkpath(new File(cgenConfiguration.buildPath().toString()), packageName), filename);

		if (dest.exists() && !fileNeedUpdate(dest, cgenConfiguration.getSuperTemplate())) {
			return null;
		}

		return dest;
	}

	/**
	 * Returns a target file where a generated class must be saved. If null is
	 * returned, class shouldn't be generated.
	 */
	private File fileForClass() throws Exception {

		String packageName = (String) context.get(Artifact.SUB_PACKAGE_KEY);
		String className = (String) context.get(Artifact.SUB_CLASS_KEY);

		String filename = StringUtils.getInstance().replaceWildcardInStringWithString(WILDCARD, cgenConfiguration.getOutputPattern(), className);
		File dest = new File(mkpath(new File(Objects.requireNonNull(cgenConfiguration.buildPath()).toString()), packageName), filename);

		if (dest.exists()) {
			// no overwrite of subclasses
			if (cgenConfiguration.isMakePairs()) {
				return null;
			}

			// skip if said so
			if (!cgenConfiguration.isOverwrite()) {
				return null;
			}

			if (!fileNeedUpdate(dest, cgenConfiguration.getTemplate())) {
				return null;
			}
		}

		return dest;
	}

	/**
	 * Ignore if the destination is newer than the map
	 * (internal timestamp), i.e. has been generated after the map was
	 * last saved AND the template is older than the destination file
	 */
	protected boolean fileNeedUpdate(File dest, String templateFileName) {
		if(cgenConfiguration.isForce()) {
			return true;
		}

		if (isOld(dest)) {
            if (templateFileName == null) {
				return false;
            }

            File templateFile = new File(templateFileName);
			return templateFile.lastModified() >= dest.lastModified();
        }
		return true;
	}

	/**
	 * Is file modified after internal timestamp (usually equal to mtime of datamap file)
	 */
	protected boolean isOld(File file) {
		return file.lastModified() > cgenConfiguration.getTimestamp();
	}

	/**
	 * Returns a File object corresponding to a directory where files that
	 * belong to <code>pkgName</code> package should reside. Creates any missing
	 * diectories below <code>dest</code>.
	 */
	private File mkpath(File dest, String pkgName) throws Exception {

		if (!cgenConfiguration.isUsePkgPath() || pkgName == null) {
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
	public CgenConfiguration getCgenConfiguration() {
		return cgenConfiguration;
	}

	/**
	 * Sets an optional shared VelocityContext. Useful with tools like VPP that
	 * can set custom values in the context, not known to Cayenne.
	 */
	public void setContext(Context context) {
		this.context = context;
	}

	/**
	 * @since 4.1
	 */
	public void setCgenConfiguration(CgenConfiguration cgenConfiguration) {
		this.cgenConfiguration = cgenConfiguration;
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
}
