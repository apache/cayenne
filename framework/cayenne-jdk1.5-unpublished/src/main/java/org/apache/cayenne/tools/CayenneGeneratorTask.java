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
import java.util.List;

import org.apache.cayenne.gen.ClassGenerator;
import org.apache.cayenne.gen.DefaultClassGenerator;
import org.apache.cayenne.map.ObjEntity;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;

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
    protected DefaultClassGenerator generator;

    public CayenneGeneratorTask() {
        generator = createGenerator();
    }

    /**
     * Factory method to create internal class generator. Called from constructor.
     */
    protected DefaultClassGenerator createGenerator() {
        AntClassGenerator gen = new AntClassGenerator();
        gen.setParentTask(this);
        return gen;
    }

    /**
     * Executes the task. It will be called by ant framework.
     */
    public void execute() throws BuildException {
        validateAttributes();

        // Take care of setting up VPP for the generator.
        if (!ClassGenerator.VERSION_1_1.equals(generator.getVersionString())) {
            initializeVppConfig();
            generator.setVppConfig(vppConfig);
        }

        ILog logger = new AntTaskLogger(this);
        CayenneGeneratorMapLoaderAction loadAction = new CayenneGeneratorMapLoaderAction();

        loadAction.setMainDataMapFile(map);
        loadAction.setAdditionalDataMapFiles(additionalMaps);

        CayenneGeneratorEntityFilterAction filterAction = new CayenneGeneratorEntityFilterAction();
        filterAction.setClient(generator.isClient());
        filterAction.setNameFilter(new NamePatternMatcher(
                logger,
                includeEntitiesPattern,
                excludeEntitiesPattern));

        try {
            generator.setTimestamp(map.lastModified());
            generator.setDataMap(loadAction.getMainDataMap());
            generator.setObjEntities((List<ObjEntity>) filterAction
                    .getFilteredEntities(loadAction.getMainDataMap()));
            generator.validateAttributes();
            generator.execute();
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
        generator.setDestDir(destDir);
    }

    /**
     * Sets <code>overwrite</code> property.
     */
    public void setOverwrite(boolean overwrite) {
        generator.setOverwrite(overwrite);
    }

    /**
     * Sets <code>makepairs</code> property.
     */
    public void setMakepairs(boolean makepairs) {
        generator.setMakePairs(makepairs);
    }

    /**
     * Sets <code>template</code> property.
     */
    public void setTemplate(String template) {
        generator.setTemplate(template);
    }

    /**
     * Sets <code>supertemplate</code> property.
     */
    public void setSupertemplate(String supertemplate) {
        generator.setSuperTemplate(supertemplate);
    }

    /**
     * Sets <code>usepkgpath</code> property.
     */
    public void setUsepkgpath(boolean usepkgpath) {
        generator.setUsePkgPath(usepkgpath);
    }

    /**
     * Sets <code>superpkg</code> property.
     */
    public void setSuperpkg(String superpkg) {
        generator.setSuperPkg(superpkg);
    }

    /**
     * Sets <code>client</code> property.
     */
    public void setClient(boolean client) {
        generator.setClient(client);
    }

    /**
     * Sets <code>version</code> property.
     */
    public void setVersion(String versionString) {
        try {
            generator.setVersionString(versionString);
        }
        catch (IllegalStateException e) {
            throw new BuildException(e.getMessage(), e);
        }
    }

    /**
     * Sets <code>encoding</code> property that allows to generate files using
     * non-default encoding.
     */
    public void setEncoding(String encoding) {
        generator.setEncoding(encoding);
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
        generator.setOutputPattern(outputPattern);
    }

    /**
     * Sets <code>mode</code> property.
     */
    public void setMode(String mode) {
        generator.setMode(mode);
    }

    /**
     * Provides a <code>VPPConfig</code> object to configure. (Written with
     * createConfig() instead of addConfig() to avoid run-time dependency on VPP).
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
