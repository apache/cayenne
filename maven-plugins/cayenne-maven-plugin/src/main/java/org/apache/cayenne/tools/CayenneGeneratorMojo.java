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

import java.io.File;

import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.dbsync.filter.NamePatternMatcher;
import org.apache.cayenne.dbsync.reverse.configuration.ToolsModule;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.gen.ArtifactsGenerationMode;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClassGenerationActionFactory;
import org.apache.cayenne.gen.ClientClassGenerationAction;
import org.apache.cayenne.map.DataMap;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maven mojo to perform class generation from data cgenConfiguration. This class is an Maven
 * adapter to DefaultClassGenerator class.
 * 
 * @since 3.0
 */
@Mojo(name = "cgen", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CayenneGeneratorMojo extends AbstractMojo {

    public static final File[] NO_FILES = new File[0];

    /**
	 * Path to additional DataMap XML files to use for class generation.
	 */
    @Parameter
	private File additionalMaps;

	/**
	 * Whether we are generating classes for the client tier in a Remote Object
	 * Persistence application. Default is <code>false</code>.
	 */
	@Parameter
	private Boolean client;

	/**
	 * Default destination directory for Java classes (ignoring their package names).
	 */
	@Parameter(defaultValue = "${project.build.sourceDirectory}")
	private File defaultDir;

	/**
	 * Destination directory for Java classes (ignoring their package names).
	 */
	@Parameter
	private File destDir;

	/**
	 * Specify generated file encoding if different from the default on current
	 * platform. Target encoding must be supported by the JVM running Maven
	 * build. Standard encodings supported by Java on all platforms are
	 * US-ASCII, ISO-8859-1, UTF-8, UTF-16BE, UTF-16LE, UTF-16. See Sun Java
	 * Docs for java.nio.charset.Charset for more information.
	 */
	@Parameter
	private String encoding;

	/**
	 * Entities (expressed as a perl5 regex) to exclude from template
	 * generation. (Default is to include all entities in the DataMap).
	 */
	@Parameter
	private String excludeEntities;

	/**
	 * Entities (expressed as a perl5 regex) to include in template generation.
	 * (Default is to include all entities in the DataMap).
	 */
	@Parameter
	private String includeEntities;

	/**
	 * @since 4.1
	 * Embeddables (expressed as a perl5 regex) to exclude from template
	 * generation. (Default is to include all embeddables in the DataMap).
	 */
	@Parameter
	private String excludeEmbeddables;

	/**
	 * If set to <code>true</code>, will generate subclass/superclass pairs,
	 * with all generated code included in superclass (default is
	 * <code>true</code>).
	 */
	@Parameter
	private Boolean makePairs;

	/**
	 * DataMap XML file to use as a base for class generation.
	 */
	@Parameter(required = true)
	private File map;

	/**
	 * Specifies generator iteration target. &quot;entity&quot; performs one
	 * iteration for each selected entity. &quot;datamap&quot; performs one
	 * iteration per datamap (This is always one iteration since cgen currently
	 * supports specifying one-and-only-one datamap). (Default is &quot;entity&quot;)
	 */
	@Parameter
	private String mode;

	/**
	 * Name of file for generated output. (Default is &quot;*.java&quot;)
	 */
	@Parameter
	private String outputPattern;

	/**
	 * If set to <code>true</code>, will overwrite older versions of generated
	 * classes. Ignored unless makepairs is set to <code>false</code>.
	 */
	@Parameter
	private Boolean overwrite;

	/**
	 * Java package name of generated superclasses. Ignored unless
	 * <code>makepairs</code> set to <code>true</code>. If omitted, each
	 * superclass will be assigned the same package as subclass. Note that
	 * having superclass in a different package would only make sense when
	 * <code>usepkgpath</code> is set to <code>true</code>. Otherwise classes
	 * from different packages will end up in the same directory.
	 */
	@Parameter
	private String superPkg;

	/**
	 * Location of Velocity template file for Entity superclass generation.
	 * Ignored unless <code>makepairs</code> set to <code>true</code>. If
	 * omitted, default template is used.
	 */
	@Parameter
	private String superTemplate;

	/**
	 * Location of Velocity template file for Entity class generation. If
	 * omitted, default template is used.
	 */
	@Parameter
	private String template;

	/**
	 * Location of Velocity template file for Embeddable superclass generation.
	 * Ignored unless <code>makepairs</code> set to <code>true</code>. If
	 * omitted, default template is used.
	 */
	@Parameter
	private String embeddableSuperTemplate;

	/**
	 * Location of Velocity template file for Embeddable class generation. If
	 * omitted, default template is used.
	 */
	@Parameter
	private String embeddableTemplate;

	/**
	 * If set to <code>true</code> (default), a directory tree will be generated
	 * in "destDir" corresponding to the class package structure, if set to
	 * <code>false</code>, classes will be generated in &quot;destDir&quot;
	 * ignoring their package.
	 */
	@Parameter
	private Boolean usePkgPath;

    /**
     * If set to <code>true</code>, will generate String Property names.
     * Default is <code>false</code>.
     */
    @Parameter
    private Boolean createPropertyNames;

	/**
	 * If set to <code>true</code>, will skip file modification time validation and regenerate all.
	 * Default is <code>false</code>.
	 *
	 * @since 4.1
	 */
	@Parameter(defaultValue = "false", property = "force")
	private boolean force;

	@Parameter
	private String queryTemplate;

	@Parameter
	private String querySuperTemplate;

    /**
     * If set to <code>true</code>, will generate PK attributes as Properties.
     * Default is <code>false</code>.
     * @since 4.1
     */
    @Parameter
    private Boolean createPKProperties;

    /**
     * Optional path (classpath or filesystem) to external velocity tool configuration file
     * @since 4.2 
     */
    @Parameter
    private String externalToolConfig;
    
    private transient Injector injector;

    private static final Logger logger = LoggerFactory.getLogger(CayenneGeneratorMojo.class);

    private boolean useConfigFromDataMap;

	public void execute() throws MojoExecutionException, MojoFailureException {
		// Create the destination directory if necessary.
		// TODO: (KJM 11/2/06) The destDir really should be added as a
		// compilation resource for maven.

		injector = new ToolsInjectorBuilder()
				.addModule(new ToolsModule(LoggerFactory.getLogger(CayenneGeneratorMojo.class)))
				.create();

		Logger logger = new MavenLogger(this);
		CayenneGeneratorMapLoaderAction loaderAction = new CayenneGeneratorMapLoaderAction(injector);
		loaderAction.setMainDataMapFile(map);

		try {
			loaderAction.setAdditionalDataMapFiles(convertAdditionalDataMaps());

			DataMap dataMap = loaderAction.getMainDataMap();
			ClassGenerationAction generator = createGenerator(dataMap);
			CayenneGeneratorEntityFilterAction filterEntityAction = new CayenneGeneratorEntityFilterAction();
			filterEntityAction.setNameFilter(NamePatternMatcher.build(logger, includeEntities, excludeEntities));

			CayenneGeneratorEmbeddableFilterAction filterEmbeddableAction = new CayenneGeneratorEmbeddableFilterAction();
			filterEmbeddableAction.setNameFilter(NamePatternMatcher.build(logger, null, excludeEmbeddables));
			filterEntityAction.setClient(generator.getCgenConfiguration().isClient());
			generator.setLogger(logger);

			if(force) {
				// will (re-)generate all files
				generator.getCgenConfiguration().setForce(true);
			}
			generator.getCgenConfiguration().setTimestamp(map.lastModified());
			if(!hasConfig() && useConfigFromDataMap) {
				generator.prepareArtifacts();
			} else {
				generator.addEntities(filterEntityAction.getFilteredEntities(dataMap));
				generator.addEmbeddables(filterEmbeddableAction.getFilteredEmbeddables(dataMap));
				generator.addQueries(dataMap.getQueryDescriptors());
			}
			generator.execute();
		} catch (Exception e) {
			throw new MojoExecutionException("Error generating classes: ", e);
		}
	}

	/**
	 * Loads and returns DataMap based on <code>cgenConfiguration</code> attribute.
	 */
	protected File[] convertAdditionalDataMaps() throws Exception {

		if (additionalMaps == null) {
			return NO_FILES;
		}

		if (!additionalMaps.isDirectory()) {
			throw new MojoFailureException("'additionalMaps' must be a directory.");
		}

        return additionalMaps.listFiles(
        		(dir, name) -> name != null && name.toLowerCase().endsWith(".map.xml")
		);
	}

	private boolean hasConfig() {
		return destDir != null || encoding != null || client != null || excludeEntities != null || excludeEmbeddables != null || includeEntities != null ||
				makePairs != null || mode != null || outputPattern != null || overwrite != null || superPkg != null ||
				superTemplate != null || template != null || embeddableTemplate != null || embeddableSuperTemplate != null ||
				usePkgPath != null || createPropertyNames != null || force || queryTemplate != null ||
				querySuperTemplate != null || createPKProperties != null || externalToolConfig != null;
	}

	/**
	 * Factory method to create internal class generator. Called from
	 * constructor.
	 */
	private ClassGenerationAction createGenerator(DataMap dataMap) {
		CgenConfiguration cgenConfiguration = buildConfiguration(dataMap);
		return injector.getInstance(ClassGenerationActionFactory.class).createAction(cgenConfiguration);
	}

	private CgenConfiguration buildConfiguration(DataMap dataMap) {
		CgenConfiguration cgenConfiguration = injector.getInstance(DataChannelMetaData.class).get(dataMap, CgenConfiguration.class);
		if(hasConfig()) {
			logger.info("Using cgen config from pom.xml");
			return cgenConfigFromPom(dataMap);
		} else if(cgenConfiguration != null) {
			logger.info("Using cgen config from " + cgenConfiguration.getDataMap().getName());
			useConfigFromDataMap = true;
			return cgenConfiguration;
		} else {
			logger.info("Using default cgen config.");
			cgenConfiguration = new CgenConfiguration(false);
			cgenConfiguration.setDataMap(dataMap);
			cgenConfiguration.setRelPath(defaultDir.getPath());
			return cgenConfiguration;
		}
	}

	private CgenConfiguration cgenConfigFromPom(DataMap dataMap){
		CgenConfiguration cgenConfiguration = new CgenConfiguration(client != null ? client : false);
		cgenConfiguration.setDataMap(dataMap);
		cgenConfiguration.setRelPath(destDir != null ? destDir.getPath() : defaultDir.getPath());
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
		cgenConfiguration.setExternalToolConfig(externalToolConfig != null ? externalToolConfig : cgenConfiguration.getExternalToolConfig());
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
}
