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

import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.dbsync.filter.NamePatternMatcher;
import org.apache.cayenne.dbsync.reverse.configuration.ToolsModule;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.gen.CgenModule;
import org.apache.cayenne.gen.ClassGenerationAction;
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

import java.io.File;

/**
 * Maven mojo to perform class generation from data map. This class is an Maven
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
	@Parameter(defaultValue = "false")
	private boolean client;

	/**
	 * Destination directory for Java classes (ignoring their package names).
	 */
	@Parameter(defaultValue = "${project.build.sourceDirectory}")
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
    @Parameter(defaultValue = "false")
    private Boolean createPKProperties;

    private transient Injector injector;

    private static final Logger logger = LoggerFactory.getLogger(CayenneGeneratorMojo.class);

	public void execute() throws MojoExecutionException, MojoFailureException {
		// Create the destination directory if necessary.
		// TODO: (KJM 11/2/06) The destDir really should be added as a
		// compilation resource for maven.

		if (!destDir.exists()) {
			destDir.mkdirs();
		}

		injector = DIBootstrap.createInjector(new CgenModule(), new ToolsModule(LoggerFactory.getLogger(CayenneGeneratorMojo.class)));

		Logger logger = new MavenLogger(this);
		CayenneGeneratorMapLoaderAction loaderAction = new CayenneGeneratorMapLoaderAction(injector);
		loaderAction.setMainDataMapFile(map);

		CayenneGeneratorEntityFilterAction filterAction = new CayenneGeneratorEntityFilterAction();
		filterAction.setClient(client);
		filterAction.setNameFilter(NamePatternMatcher.build(logger, includeEntities, excludeEntities));

		try {
			loaderAction.setAdditionalDataMapFiles(convertAdditionalDataMaps());

			DataMap dataMap = loaderAction.getMainDataMap();

			ClassGenerationAction generator = createGenerator(dataMap);
			generator.setLogger(logger);

			if(force) {
				// will (re-)generate all files
				generator.setForce(true);
			}
			generator.setTimestamp(map.lastModified());
			generator.setDataMap(dataMap);
			if(!generator.getEntities().isEmpty() || !generator.getEmbeddables().isEmpty()){
				generator.prepareArtifacts();
			} else {
				generator.addEntities(filterAction.getFilteredEntities(dataMap));
				generator.addEmbeddables(dataMap.getEmbeddables());
				generator.addQueries(dataMap.getQueryDescriptors());
			}
			generator.execute();
		} catch (Exception e) {
			throw new MojoExecutionException("Error generating classes: ", e);
		}
	}

	/**
	 * Loads and returns DataMap based on <code>map</code> attribute.
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

	/**
	 * Factory method to create internal class generator. Called from
	 * constructor.
	 */
	protected ClassGenerationAction createGenerator(DataMap dataMap) {

	    ClassGenerationAction action = injector.getInstance(DataChannelMetaData.class).get(dataMap, ClassGenerationAction.class);

		if (client) {
			action = new ClientClassGenerationAction();
		} else {
            if(action == null) {
                action = new ClassGenerationAction();
            }
		}

		injector.injectMembers(action);

//		action.setDestDir(destDir.toPath());
		action.setEncoding(encoding != null ? encoding : action.getEncoding());
		action.setMakePairs(makePairs != null ? makePairs : action.isMakePairs());
		action.setArtifactsGenerationMode(mode != null ? mode : action.getArtifactsGenerationMode());
		action.setOutputPattern(outputPattern != null ? outputPattern : action.getOutputPattern());
		action.setOverwrite(overwrite != null ? overwrite : action.isOverwrite());
		action.setSuperPkg(superPkg != null ? superPkg : action.getSuperPkg());
		action.setSuperTemplate(superTemplate != null ? superTemplate : action.getSuperclassTemplate());
		action.setTemplate(template != null ? template : action.getTemplate());
		action.setEmbeddableSuperTemplate(embeddableSuperTemplate != null ? embeddableSuperTemplate : action.getEmbeddableSuperTemplate());
		action.setEmbeddableTemplate(embeddableTemplate != null ? embeddableTemplate : action.getEmbeddableTemplate());
		action.setUsePkgPath(usePkgPath != null ? usePkgPath : action.isUsePkgPath());
		action.setCreatePropertyNames(createPropertyNames != null ? createPropertyNames : action.isCreatePropertyNames());
		action.setQueryTemplate(queryTemplate != null ? queryTemplate : action.getQueryTemplate());
		action.setQuerySuperTemplate(querySuperTemplate != null ? querySuperTemplate : action.getQuerySuperTemplate());
		action.setCreatePKProperties(createPKProperties != null ? createPKProperties : action.isCreatePropertyNames());
		return action;
	}
}
