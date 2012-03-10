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

import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClientClassGenerationAction;
import org.apache.cayenne.map.DataMap;
import org.apache.commons.logging.Log;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Maven mojo to perform class generation from data map. This class is an Maven
 * adapter to DefaultClassGenerator class.
 * 
 * @since 3.0
 * 
 * @phase generate-sources
 * @goal cgen
 */
public class CayenneGeneratorMojo extends AbstractMojo {

	/**
	 * Path to additional DataMap XML files to use for class generation.
	 * 
	 * @parameter expression="${cgen.additionalMaps}"
	 */
	private File additionalMaps;

	/**
	 * Whether we are generating classes for the client tier in a Remote Object
	 * Persistence application. Default is <code>false</code>.
	 * 
	 * @parameter expression="${cgen.client}" default-value="false"
	 */
	private boolean client;

	/**
	 * Destination directory for Java classes (ignoring their package names).
	 * 
	 * @parameter expression="${cgen.destDir}" default-value=
	 *            "${project.build.sourceDirectory}/java/generated-sources/cayenne"
	 */
	private File destDir;

	/**
	 * Specify generated file encoding if different from the default on current
	 * platform. Target encoding must be supported by the JVM running Maven
	 * build. Standard encodings supported by Java on all platforms are
	 * US-ASCII, ISO-8859-1, UTF-8, UTF-16BE, UTF-16LE, UTF-16. See Sun Java
	 * Docs for java.nio.charset.Charset for more information.
	 * 
	 * @parameter expression="${cgen.encoding}"
	 */
	private String encoding;

	/**
	 * Entities (expressed as a perl5 regex) to exclude from template
	 * generation. (Default is to include all entities in the DataMap).
	 * 
	 * @parameter expression="${cgen.excludeEntities}"
	 */
	private String excludeEntities;

	/**
	 * Entities (expressed as a perl5 regex) to include in template generation.
	 * (Default is to include all entities in the DataMap).
	 * 
	 * @parameter expression="${cgen.includeEntities}"
	 */
	private String includeEntities;

	/**
	 * If set to <code>true</code>, will generate subclass/superclass pairs,
	 * with all generated code included in superclass (default is
	 * <code>true</code>).
	 * 
	 * @parameter expression="${cgen.makePairs}" default-value="true"
	 */
	private boolean makePairs;

	/**
	 * DataMap XML file to use as a base for class generation.
	 * 
	 * @parameter expression="${cgen.map}"
	 * @required
	 */
	private File map;

	/**
	 * Specifies generator iteration target. &quot;entity&quot; performs one
	 * iteration for each selected entity. &quot;datamap&quot; performs one
	 * iteration per datamap (This is always one iteration since cgen currently
	 * supports specifying one-and-only-one datamap). (Default is
	 * &quot;entity&quot;)
	 * 
	 * @parameter expression="${cgen.mode}" default-value="entity"
	 */
	private String mode;

	/**
	 * Name of file for generated output. (Default is &quot;*.java&quot;)
	 * 
	 * @parameter expression="${cgen.outputPattern}" default-value="*.java"
	 */
	private String outputPattern;

	/**
	 * If set to <code>true</code>, will overwrite older versions of generated
	 * classes. Ignored unless makepairs is set to <code>false</code>.
	 * 
	 * @parameter expression="${cgen.overwrite}" default-value="false"
	 */
	private boolean overwrite;

	/**
	 * Java package name of generated superclasses. Ignored unless
	 * <code>makepairs</code> set to <code>true</code>. If omitted, each
	 * superclass will be assigned the same package as subclass. Note that
	 * having superclass in a different package would only make sense when
	 * <code>usepkgpath</code> is set to <code>true</code>. Otherwise classes
	 * from different packages will end up in the same directory.
	 * 
	 * @parameter expression="${cgen.superPkg}"
	 */
	private String superPkg;

	/**
	 * Location of Velocity template file for Entity superclass generation.
	 * Ignored unless <code>makepairs</code> set to <code>true</code>. If
	 * omitted, default template is used.
	 * 
	 * @parameter expression="${cgen.superTemplate}"
	 */
	private String superTemplate;

	/**
	 * Location of Velocity template file for Entity class generation. If
	 * omitted, default template is used.
	 * 
	 * @parameter expression="${cgen.template}"
	 */
	private String template;

	/**
	 * Location of Velocity template file for Embeddable superclass generation.
	 * Ignored unless <code>makepairs</code> set to <code>true</code>. If
	 * omitted, default template is used.
	 * 
	 * @parameter expression="${cgen.embeddableSuperTemplate}"
	 */
	private String embeddableSuperTemplate;

	/**
	 * Location of Velocity template file for Embeddable class generation. If
	 * omitted, default template is used.
	 * 
	 * @parameter expression="${cgen.embeddableTemplate}"
	 */
	private String embeddableTemplate;

	/**
	 * If set to <code>true</code> (default), a directory tree will be generated
	 * in "destDir" corresponding to the class package structure, if set to
	 * <code>false</code>, classes will be generated in &quot;destDir&quot;
	 * ignoring their package.
	 * 
	 * @parameter expression="${cgen.usePkgPath}" default-value="true"
	 */
	private boolean usePkgPath;

	public void execute() throws MojoExecutionException, MojoFailureException {
		// Create the destination directory if necessary.
		// TODO: (KJM 11/2/06) The destDir really should be added as a
		// compilation resource for maven.
		if (!destDir.exists()) {
			destDir.mkdirs();
		}

		Log logger = new MavenLogger(this);
		CayenneGeneratorMapLoaderAction loaderAction = new CayenneGeneratorMapLoaderAction();
		loaderAction.setMainDataMapFile(map);

		CayenneGeneratorEntityFilterAction filterAction = new CayenneGeneratorEntityFilterAction();
		filterAction.setClient(client);
		filterAction.setNameFilter(new NamePatternMatcher(logger,
				includeEntities, excludeEntities));

		try {
			loaderAction.setAdditionalDataMapFiles(convertAdditionalDataMaps());

			DataMap dataMap = loaderAction.getMainDataMap();

			ClassGenerationAction generator = createGenerator();
			generator.setLogger(logger);
			generator.setTimestamp(map.lastModified());
			generator.setDataMap(dataMap);
			generator.addEntities(filterAction.getFilteredEntities(dataMap));
			// ksenia khailenko 15.10.2010
			// TODO add the "includeEmbeddables" and "excludeEmbeddables"
			// attributes
			generator.addEmbeddables(dataMap.getEmbeddables());
			// TODO add the "includeQueries" and "excludeQueries" attributes
			generator.addQueries(dataMap.getQueries());
			generator.execute();
		} catch (Exception e) {
			throw new MojoExecutionException("Error generating classes: ", e);
		}
	}

	/**
	 * Loads and returns DataMap based on <code>map</code> attribute.
	 */
	protected File[] convertAdditionalDataMaps() throws Exception {

		if (null == additionalMaps) {
			return new File[0];
		}

		if (!additionalMaps.isDirectory()) {
			throw new MojoFailureException(
					"'additionalMaps' must be a directory containing only datamap files.");
		}

		String[] maps = additionalMaps.list();
		File[] dataMaps = new File[maps.length];
		for (int i = 0; i < maps.length; i++) {
			dataMaps[i] = new File(maps[i]);
		}
		return dataMaps;
	}

	/**
	 * Factory method to create internal class generator. Called from
	 * constructor.
	 */
	protected ClassGenerationAction createGenerator() {

		ClassGenerationAction action;
		if (client) {
			action = new ClientClassGenerationAction();
		} else {
			action = new ClassGenerationAction();
		}

		action.setDestDir(destDir);
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

		return action;
	}
}
