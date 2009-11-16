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

import org.apache.cayenne.gen.ArtifactsGenerationMode;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClientClassGenerationAction;
import org.apache.cayenne.map.DataMap;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;
import org.apache.velocity.VelocityContext;

import foundrylogic.vpp.VPPConfig;

/**
 * An Ant task to perform class generation based on CayenneDataMap.
 * 
 * @since 3.0
 */
public class CayenneGeneratorTask extends CayenneTask {

    protected String includeEntitiesPattern;
    protected String excludeEntitiesPattern;
    protected VPPConfig vppConfig;

    protected File map;
    protected File additionalMaps[];
    protected boolean client;
    protected File destDir;
    protected String encoding;
    protected boolean makepairs;
    protected String mode;
    protected String outputPattern;
    protected boolean overwrite;
    protected String superpkg;
    protected String supertemplate;
    protected String template;
    protected String embeddabletemplate;
    protected String embeddablesupertemplate;
    protected boolean usepkgpath;

    public CayenneGeneratorTask() {
        this.makepairs = true;
        this.mode = ArtifactsGenerationMode.ENTITY.getLabel();
        this.outputPattern = "*.java";
        this.usepkgpath = true;
    }

    protected VelocityContext getVppContext() {
        initializeVppConfig();
        return vppConfig.getVelocityContext();
    }

    protected ClassGenerationAction createGeneratorAction() {
        ClassGenerationAction action;
        if (client) {
            action = new ClientClassGenerationAction();
            action.setContext(getVppContext());
        }
        else {
            action = new ClassGenerationAction();
            action.setContext(getVppContext());
        }

        action.setDestDir(destDir);
        action.setEncoding(encoding);
        action.setMakePairs(makepairs);
        action.setArtifactsGenerationMode(mode);
        action.setOutputPattern(outputPattern);
        action.setOverwrite(overwrite);
        action.setSuperPkg(superpkg);
        action.setSuperTemplate(supertemplate);
        action.setTemplate(template);
        action.setEmbeddableSuperTemplate(embeddablesupertemplate);
        action.setEmbeddableTemplate(embeddabletemplate);
        action.setUsePkgPath(usepkgpath);

        return action;
    }

    /**
     * Executes the task. It will be called by ant framework.
     */
    @Override
    public void execute() throws BuildException {
        validateAttributes();

        AntLogger logger = new AntLogger(this);
        CayenneGeneratorMapLoaderAction loadAction = new CayenneGeneratorMapLoaderAction();

        loadAction.setMainDataMapFile(map);
        loadAction.setAdditionalDataMapFiles(additionalMaps);

        CayenneGeneratorEntityFilterAction filterAction = new CayenneGeneratorEntityFilterAction();
        filterAction.setClient(client);
        filterAction.setNameFilter(new NamePatternMatcher(
                logger,
                includeEntitiesPattern,
                excludeEntitiesPattern));

        try {

            DataMap dataMap = loadAction.getMainDataMap();

            ClassGenerationAction generatorAction = createGeneratorAction();
            generatorAction.setLogger(logger);
            generatorAction.setTimestamp(map.lastModified());
            generatorAction.setDataMap(dataMap);
            generatorAction.addEntities(filterAction.getFilteredEntities(dataMap));
            generatorAction.addEmbeddables(filterAction.getFilteredEmbeddables(dataMap));
            generatorAction.addQueries(dataMap.getQueries());
            generatorAction.execute();
        }
        catch (Exception e) {
            throw new BuildException(e);
        }
    }

    /**
     * Validates attributes that are not related to internal DefaultClassGenerator. Throws
     * BuildException if attributes are invalid.
     */
    protected void validateAttributes() throws BuildException {
        if (map == null && this.getProject() == null) {
            throw new BuildException("either 'map' or 'project' is required.");
        }
    }

    /**
     * Sets the map.
     * 
     * @param map The map to set
     */
    public void setMap(File map) {
        this.map = map;
    }

    /**
     * Sets the additional DataMaps.
     * 
     * @param additionalMapsPath The additional DataMaps to set
     */
    public void setAdditionalMaps(Path additionalMapsPath) {
        String additionalMapFilenames[] = additionalMapsPath.list();
        this.additionalMaps = new File[additionalMapFilenames.length];

        for (int i = 0; i < additionalMapFilenames.length; i++) {
            additionalMaps[i] = new File(additionalMapFilenames[i]);
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
    public void setMakepairs(boolean makepairs) {
        this.makepairs = makepairs;
    }

    /**
     * Sets <code>template</code> property.
     */
    public void setTemplate(String template) {
        this.template = template;
    }

    /**
     * Sets <code>supertemplate</code> property.
     */
    public void setSupertemplate(String supertemplate) {
        this.supertemplate = supertemplate;
    }

    /**
     * Sets <code>usepkgpath</code> property.
     */
    public void setUsepkgpath(boolean usepkgpath) {
        this.usepkgpath = usepkgpath;
    }

    /**
     * Sets <code>superpkg</code> property.
     */
    public void setSuperpkg(String superpkg) {
        this.superpkg = superpkg;
    }

    /**
     * Sets <code>client</code> property.
     */
    public void setClient(boolean client) {
        this.client = client;
    }

    /**
     * Sets <code>encoding</code> property that allows to generate files using non-default
     * encoding.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Sets <code>excludeEntitiesPattern</code> property.
     */
    public void setExcludeEntities(String excludeEntitiesPattern) {
        this.excludeEntitiesPattern = excludeEntitiesPattern;
    }

    /**
     * Sets <code>includeEntitiesPattern</code> property.
     */
    public void setIncludeEntities(String includeEntitiesPattern) {
        this.includeEntitiesPattern = includeEntitiesPattern;
    }

    /**
     * Sets <code>outputPattern</code> property.
     */
    public void setOutputPattern(String outputPattern) {
        this.outputPattern = outputPattern;
    }

    /**
     * Sets <code>mode</code> property.
     */
    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setEmbeddabletemplate(String embeddabletemplate) {
        this.embeddabletemplate = embeddabletemplate;
    }

    public void setEmbeddablesupertemplate(String embeddablesupertemplate) {
        this.embeddablesupertemplate = embeddablesupertemplate;
    }

    /**
     * Provides a <code>VPPConfig</code> object to configure. (Written with createConfig()
     * instead of addConfig() to avoid run-time dependency on VPP).
     */
    public Object createConfig() {
        this.vppConfig = new VPPConfig();
        return this.vppConfig;
    }

    /**
     * If no VppConfig element specified, use the default one.
     */
    private void initializeVppConfig() {
        if (vppConfig == null) {
            vppConfig = VPPConfig.getDefaultConfig(getProject());
        }
    }
}
