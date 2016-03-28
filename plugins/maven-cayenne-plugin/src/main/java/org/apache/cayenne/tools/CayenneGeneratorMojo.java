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

import org.apache.cayenne.access.loader.NamePatternMatcher;
import org.apache.cayenne.map.template.ClassGenerationDescriptor;
import org.apache.cayenne.map.template.ClassTemplate;
import org.apache.cayenne.map.template.TemplateType;
import static org.apache.cayenne.map.template.TemplateType.*;

import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClientClassGenerationAction;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.apache.commons.logging.Log;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.FilenameFilter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Maven mojo to perform class generation from data map. This class is an Maven
 * adapter to DefaultClassGenerator class.
 *
 * @phase generate-sources
 * @goal cgen
 * @since 3.0
 */
public class CayenneGeneratorMojo extends AbstractMojo {

    public static final File[] NO_FILES = new File[0];
    /**
     * Path to additional DataMap XML files to use for class generation.
     *
     * @parameter additionalMaps="additionalMaps"
     */
    private File additionalMaps;

    /**
     * Whether we are generating classes for the client tier in a Remote Object
     * Persistence application. Default is <code>false</code>.
     *
     * @parameter client="client" default-value="false"
     */
    private boolean client;

    /**
     * Destination directory for Java classes (ignoring their package names).
     *
     * @parameter destDir="destDir" default-value="${project.build.sourceDirectory}"
     */
    private File destDir;

    /**
     * Specify generated file encoding if different from the default on current
     * platform. Target encoding must be supported by the JVM running Maven
     * build. Standard encodings supported by Java on all platforms are
     * US-ASCII, ISO-8859-1, UTF-8, UTF-16BE, UTF-16LE, UTF-16. See Sun Java
     * Docs for java.nio.charset.Charset for more information.
     *
     * @parameter encoding="encoding"
     */
    private String encoding;

    /**
     * Entities (expressed as a perl5 regex) to exclude from template
     * generation. (Default is to include all entities in the DataMap).
     *
     * @parameter excludeEntities="excludeEntities"
     */
    private String excludeEntities;

    public void setExcludeEntities(String excludeEntities) {
        isClassGenerationDefined = true;
        this.excludeEntities = excludeEntities;
    }

    /**
     * Entities (expressed as a perl5 regex) to include in template generation.
     * (Default is to include all entities in the DataMap).
     *
     * @parameter includeEntities="includeEntities"
     */
    private String includeEntities;

    public void setIncludeEntities(String includeEntities) {
        isClassGenerationDefined = true;
        this.includeEntities = includeEntities;
    }

    /**
     * If set to <code>true</code>, will generate subclass/superclass pairs,
     * with all generated code included in superclass (default is
     * <code>true</code>).
     *
     * @parameter makePairs="makePairs" default-value="true"
     */
    private boolean makePairs;

    /**
     * DataMap XML file to use as a base for class generation.
     *
     * @parameter map="map"
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
     * @parameter mode="mode" default-value="entity"
     */
    private String mode;

    /**
     * Name of file for generated output. (Default is &quot;*.java&quot;)
     *
     * @parameter outputPattern="outputPattern" default-value="*.java"
     */
    private String outputPattern;

    /**
     * If set to <code>true</code>, will overwrite older versions of generated
     * classes. Ignored unless makepairs is set to <code>false</code>.
     *
     * @parameter overwrite="overwrite" default-value="false"
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
     * @parameter superPkg="superPkg"
     */
    private String superPkg;

    /**
     * Location of Velocity template file for Entity superclass generation.
     * Ignored unless <code>makepairs</code> set to <code>true</code>. If
     * omitted, default template is used.
     *
     * @parameter superTemplate="superTemplate"
     */
    private String superTemplate;

    public void setSuperTemplate(String superTemplate) {
        isClassGenerationDefined = true;
        this.superTemplate = superTemplate;
    }

    /**
     * Location of Velocity template file for Entity class generation. If
     * omitted, default template is used.
     *
     * @parameter template="template"
     */
    private String template;

    public void setTemplate(String template) {
        isClassGenerationDefined = true;
        this.template = template;
    }

    /**
     * Location of Velocity template file for Embeddable superclass generation.
     * Ignored unless <code>makepairs</code> set to <code>true</code>. If
     * omitted, default template is used.
     *
     * @parameter embeddableSuperTemplate="embeddableSuperTemplate"
     */
    private String embeddableSuperTemplate;

    public void setEmbeddableSuperTemplate(String embeddableSuperTemplate) {
        isClassGenerationDefined = true;
        this.embeddableSuperTemplate = embeddableSuperTemplate;
    }

    /**
     * Location of Velocity template file for Embeddable class generation. If
     * omitted, default template is used.
     *
     * @parameter embeddableTemplate="embeddableTemplate"
     */
    private String embeddableTemplate;

    public void setEmbeddableTemplate(String embeddableTemplate) {
        isClassGenerationDefined = true;
        this.embeddableTemplate = embeddableTemplate;
    }

    /**
     * If set to <code>true</code> (default), a directory tree will be generated
     * in "destDir" corresponding to the class package structure, if set to
     * <code>false</code>, classes will be generated in &quot;destDir&quot;
     * ignoring their package.
     *
     * @parameter usePkgPath="usePkgPath" default-value="true"
     */
    private boolean usePkgPath;

    /**
     * If set to <code>true</code>, will generate String Property names.
     * Default is <code>false</code>.
     *
     * @parameter createPropertyNames="createPropertyNames" default-value="false"
     */
    private boolean createPropertyNames;

    /**
     * Flag which defines from where to take the configuration of cgen.
     * If we define the config of cgen in pom.xml
     * we should set it to true or it will be setted to true automatically
     * if we will define some configuration parameters in pom.xml
     * Else it remains default(false) and for cgen
     * we use the configuration defined in signed dataMap
     *
     * @parameter isClassGenerationDefined="isClassGenerationDefined" default-value="false"
     */
    private boolean isClassGenerationDefined;

    public void setIsClassGenerationDefined(boolean isClassGenerationDefined) {
        this.isClassGenerationDefined = isClassGenerationDefined;
    }

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
        filterAction.setNameFilter(NamePatternMatcher.build(logger, includeEntities, excludeEntities));

        try {
            loaderAction.setAdditionalDataMapFiles(convertAdditionalDataMaps());

            DataMap dataMap = loaderAction.getMainDataMap();

            ClassGenerationAction generator = createGenerator(dataMap);
            generator.setLogger(logger);
            generator.setTimestamp(map.lastModified());
            generator.setDataMap(dataMap);
            generator.addEntities(filterAction.getFilteredEntities(dataMap));
            // ksenia khailenko 15.10.2010
            // TODO add the "includeEmbeddables" and "excludeEmbeddables"
            // attributes
            generator.addEmbeddables(dataMap.getEmbeddables());
            // TODO add the "includeQueries" and "excludeQueries" attributes
            generator.addQueries(dataMap.getQueryDescriptors());
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
            throw new MojoFailureException(
                    "'additionalMaps' must be a directory.");
        }

        FilenameFilter mapFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name != null &&
                       name.toLowerCase().endsWith(".map.xml");
            }
        };
        return additionalMaps.listFiles(mapFilter);
    }

    /**
     * Factory method to create internal class generator. Called from
     * constructor.
     */
    protected ClassGenerationAction createGenerator(DataMap dataMap)
            throws UnsupportedEncodingException, MalformedURLException {

        ClassGenerationAction action;
        if (client) {
            action = new ClientClassGenerationAction();
        } else {
            action = new ClassGenerationAction();
        }

        if (isClassGenerationDefined) {
            createGenerator(action);
        } else {
            if (dataMap.getClassGenerationDescriptor() != null) {
                createGeneratorFromMap(action, dataMap.getClassGenerationDescriptor());
            }
        }

        action.setDestDir(destDir);
        action.setEncoding(encoding);
        action.setMakePairs(makePairs);
        action.setOutputPattern(outputPattern);
        action.setOverwrite(overwrite);
        action.setSuperPkg(superPkg);
        action.setUsePkgPath(usePkgPath);
        action.setCreatePropertyNames(createPropertyNames);

        return action;
    }

    protected void createGenerator(ClassGenerationAction action) {
        action.setArtifactsGenerationMode(mode);
        action.setSuperTemplate(superTemplate);
        action.setTemplate(template);
        action.setEmbeddableSuperTemplate(embeddableSuperTemplate);
        action.setEmbeddableTemplate(embeddableTemplate);
    }

    abstract static class GeneratorByTemplate {

        public static Map<TemplateType, GeneratorByTemplate> GENERATORS = new HashMap<>();

        public static void setTemplate_(ClassGenerationAction action, ClassTemplate template, File map)
                throws UnsupportedEncodingException, MalformedURLException {

            GeneratorByTemplate generator = GENERATORS.get(template.getType());
            if (generator == null) {
                throw new IllegalArgumentException("Invalid template type: " + template.getType());
            }
            Injector injector = DIBootstrap.createInjector(new ServerModule());
            ConfigurationNameMapper nameMapper = injector.getInstance(ConfigurationNameMapper.class);
            String templateLocation = nameMapper.configurationLocation(ClassTemplate.class, template.getName());
            Resource templateResource = new URLResource(map.toURI().toURL()).getRelativeResource(templateLocation);
            template.setConfigurationSource(templateResource);
            generator.setTemplate(action, template);

        }

        static {
            GENERATORS.put(ENTITY_SINGLE_CLASS, new GeneratorByTemplate() {
                @Override
                void setTemplate(ClassGenerationAction action, ClassTemplate template) throws UnsupportedEncodingException {
                    if (!isTemplateDefined) {
                        String path = URLDecoder.decode(template.getConfigurationSource().getURL().getPath(), "UTF-8");
                        action.setTemplate(path);
                        isTemplateDefined = true;
                    } else {
                        throw new IllegalStateException("Entity template defined more than one time in datamap. " +
                                "Delete redundant ones or use mvn plugin");
                    }
                }
            });

            GENERATORS.put(ENTITY_SUPERCLASS, new GeneratorByTemplate() {
                @Override
                void setTemplate(ClassGenerationAction action, ClassTemplate template) throws UnsupportedEncodingException {
                    if (!isTemplateDefined) {
                        String path = URLDecoder.decode(template.getConfigurationSource().getURL().getPath(), "UTF-8");
                        action.setSuperTemplate(path);
                        isTemplateDefined = true;
                    } else {
                        throw new IllegalStateException("Entity super template defined more than one time in datamap. " +
                                "Delete redundant ones or use mvn plugin");
                    }
                }
            });

            GENERATORS.put(ENTITY_SUBCLASS, new GeneratorByTemplate() {
                @Override
                void setTemplate(ClassGenerationAction action, ClassTemplate template) {
                    if (!isTemplateDefined) {
                        isTemplateDefined = true;
                    } else {
                        throw new IllegalStateException("Entity sub template defined more than one time in datamap. " +
                                "Delete redundant ones or use mvn plugin");
                    }
                }
            });

            GENERATORS.put(EMBEDDABLE_SINGLE_CLASS, new GeneratorByTemplate() {
                @Override
                void setTemplate(ClassGenerationAction action, ClassTemplate template) throws UnsupportedEncodingException {
                    if (!isTemplateDefined) {
                        String path = URLDecoder.decode(template.getConfigurationSource().getURL().getPath(), "UTF-8");
                        action.setEmbeddableTemplate(path);
                        isTemplateDefined = true;
                    } else {
                        throw new IllegalStateException("Embeddable template defined more than one time in datamap. " +
                                "Delete redundant ones or use mvn plugin");
                    }
                }
            });

            GENERATORS.put(EMBEDDABLE_SUPERCLASS, new GeneratorByTemplate() {
                @Override
                void setTemplate(ClassGenerationAction action, ClassTemplate template) throws UnsupportedEncodingException {
                    if (!isTemplateDefined) {
                        String path = URLDecoder.decode(template.getConfigurationSource().getURL().getPath(), "UTF-8");
                        action.setEmbeddableSuperTemplate(path);
                        isTemplateDefined = true;
                    } else {
                        throw new IllegalStateException("Embeddable super template defined more than one time in datamap. " +
                                "Delete redundant ones or use mvn plugin");
                    }
                }
            });

            GENERATORS.put(EMBEDDABLE_SUBCLASS, new GeneratorByTemplate() {
                @Override
                void setTemplate(ClassGenerationAction action, ClassTemplate template) {
                    if (!isTemplateDefined) {
                        isTemplateDefined = true;
                    } else {
                        throw new IllegalStateException("Embeddable sub template defined more than one time in datamap. " +
                                "Delete redundant ones or use mvn plugin");
                    }
                }
            });

            GENERATORS.put(DATAMAP_SINGLE_CLASS, new GeneratorByTemplate() {
                @Override
                void setTemplate(ClassGenerationAction action, ClassTemplate template) throws UnsupportedEncodingException {
                    if (!isTemplateDefined) {
                        String path = URLDecoder.decode(template.getConfigurationSource().getURL().getPath(), "UTF-8");
                        action.setQueryTemplate(path);
                        isTemplateDefined = true;
                    } else {
                        throw new IllegalStateException("Query template defined more than one time in datamap. " +
                                "Delete redundant ones or use mvn plugin");
                    }
                }
            });

            GENERATORS.put(DATAMAP_SUPERCLASS, new GeneratorByTemplate() {
                @Override
                void setTemplate(ClassGenerationAction action, ClassTemplate template) throws UnsupportedEncodingException {
                    if (!isTemplateDefined) {
                        String path = URLDecoder.decode(template.getConfigurationSource().getURL().getPath(), "UTF-8");
                        action.setQuerySuperTemplate(path);
                        isTemplateDefined = true;
                    } else {
                        throw new IllegalStateException("Query super template defined more than one time in datamap. " +
                                "Delete redundant ones or use mvn plugin");
                    }
                }
            });

            GENERATORS.put(DATAMAP_SUBCLASS, new GeneratorByTemplate() {
                @Override
                void setTemplate(ClassGenerationAction action, ClassTemplate template) {
                    if (!isTemplateDefined) {
                        isTemplateDefined = true;
                    } else {
                        throw new IllegalStateException("Query sub template defined more than one time in datamap. " +
                                "Delete redundant ones or use mvn plugin");
                    }
                }
            });
        }

        boolean isTemplateDefined = false;

        abstract void setTemplate(ClassGenerationAction action, ClassTemplate template)
                throws UnsupportedEncodingException;

    }

    protected void createGeneratorFromMap(ClassGenerationAction action,
                                          ClassGenerationDescriptor descriptor) throws UnsupportedEncodingException, MalformedURLException {
        if (descriptor.getArtifactsGenerationMode() != null) {
            action.setArtifactsGenerationMode(descriptor.getArtifactsGenerationMode().getLabel());
        } else {
            action.setArtifactsGenerationMode(mode);
        }
        for (ClassTemplate template : descriptor.getTemplates().values()) {
            GeneratorByTemplate.setTemplate_(action, template, map);
        }
    }
}
