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

import org.apache.cayenne.gen.AntClassGenerator;
import org.apache.cayenne.gen.ClassGenerator;
import org.apache.cayenne.gen.DefaultClassGenerator;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;

import foundrylogic.vpp.VPPConfig;

/**
 * Ant task to perform class generation from data map. This class is an Ant adapter to
 * DefaultClassGenerator class.
 * 
 * @author Andrus Adamchik, Kevin Menard
 */
public class CayenneGenerator extends Task {

    protected String includeEntitiesPattern;
    protected String excludeEntitiesPattern;
    protected VPPConfig vppConfig;

    protected File map;
    protected File additionalMaps[];
    protected DefaultClassGenerator generator;

    protected CayenneGeneratorUtil generatorUtil;
    
    public CayenneGenerator() {
        generator = createGenerator();
        generatorUtil = new CayenneGeneratorUtil();
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
        if (false == ClassGenerator.VERSION_1_1.equals(generator.getVersionString())) {
            initializeVppConfig();
            generator.setVppConfig(vppConfig);
        }
        
        // Initialize the util generator state.
        generatorUtil.setAdditionalMaps(additionalMaps);
        generatorUtil.setExcludeEntitiesPattern(excludeEntitiesPattern);
        generatorUtil.setGenerator(generator);
        generatorUtil.setIncludeEntitiesPattern(includeEntitiesPattern);
        generatorUtil.setLogger(new AntTaskLogger(this));
        generatorUtil.setMap(map);

        try {
            generatorUtil.execute();
        }
        catch (Exception e) {
            throw new BuildException(e);
        }
    }

    /**
     * Validates atttributes that are not related to internal DefaultClassGenerator.
     * Throws BuildException if attributes are invalid.
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
     * 
     * @since 1.2
     */
    public void setClient(boolean client) {
        generator.setClient(client);
    }

    /**
     * Sets <code>version</code> property.
     * 
     * @since 1.2
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
     * 
     * @since 1.2
     */
    public void setEncoding(String encoding) {
        generator.setEncoding(encoding);
    }

    /**
     * Sets <code>excludeEntitiesPattern</code> property.
     * 
     * @since 1.2
     */
    public void setExcludeEntities(String excludeEntitiesPattern) {
        this.excludeEntitiesPattern = excludeEntitiesPattern;
    }

    /**
     * Sets <code>includeEntitiesPattern</code> property.
     * 
     * @since 1.2
     */
    public void setIncludeEntities(String includeEntitiesPattern) {
        this.includeEntitiesPattern = includeEntitiesPattern;
    }

    /**
     * Sets <code>outputPattern</code> property.
     * 
     * @since 1.2
     */
    public void setOutputPattern(String outputPattern) {
        generator.setOutputPattern(outputPattern);
    }

    /**
     * Sets <code>mode</code> property.
     * 
     * @since 1.2
     */
    public void setMode(String mode) {
        generator.setMode(mode);
    }

    /**
     * Provides a <code>VPPConfig</code> object to configure. (Written with
     * createConfig() instead of addConfig() to avoid run-time dependency on VPP).
     * 
     * @since 1.2
     */
    public Object createConfig() {
        this.vppConfig = new VPPConfig();
        return this.vppConfig;
    }
    
    /**
     * If no VppConfig element specified, use the default one.
     * 
     * @since 1.2
     */
    private void initializeVppConfig() {
        if (vppConfig == null) {
            vppConfig = VPPConfig.getDefaultConfig(getProject());
        }
    }
}
