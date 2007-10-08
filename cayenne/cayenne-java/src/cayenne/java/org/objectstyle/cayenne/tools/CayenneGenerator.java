/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;
import org.objectstyle.cayenne.gen.AntClassGenerator;
import org.objectstyle.cayenne.gen.ClassGenerator;
import org.objectstyle.cayenne.gen.DefaultClassGenerator;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.MapLoader;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.util.Util;
import org.xml.sax.InputSource;

import foundrylogic.vpp.VPPConfig;

/**
 * Ant task to perform class generation from data map. This class is an Ant adapter to
 * DefaultClassGenerator class.
 * 
 * @author Andrus Adamchik
 */
public class CayenneGenerator extends CayenneTask {

    protected String includeEntitiesPattern;
    protected String excludeEntitiesPattern;
    protected VPPConfig vppConfig;

    protected File map;
    protected File additionalMaps[];
    protected DefaultClassGenerator generator;

    public CayenneGenerator() {
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
        configureLogging();
        validateAttributes();

        try {
            processMap();
        }
        catch (Throwable th) {
            th = Util.unwindException(th);

            String thMessage = th.getLocalizedMessage();
            String message = "Error generating classes: ";
            message += (!Util.isEmptyString(thMessage)) ? thMessage : th
                    .getClass()
                    .getName();

            super.log(message);
            throw new BuildException(message, th);
        }
    }

    protected void processMap() throws Exception {

        DataMap dataMap = loadDataMap();
        DataMap additionalDataMaps[] = loadAdditionalDataMaps();

        // Create MappingNamespace for maps.
        EntityResolver entityResolver = new EntityResolver(Collections.singleton(dataMap));
        dataMap.setNamespace(entityResolver);
        for (int i = 0; i < additionalDataMaps.length; i++) {
            entityResolver.addDataMap(additionalDataMaps[i]);
            additionalDataMaps[i].setNamespace(entityResolver);
        }

        Collection allEntities = dataMap.getObjEntities();
        List filteredEntities = new ArrayList(allEntities.size());

        // filter client entities
        if (generator.isClient()) {
            if (dataMap.isClientSupported()) {
                Iterator it = allEntities.iterator();
                while (it.hasNext()) {
                    ObjEntity entity = (ObjEntity) it.next();
                    if (entity.isClientAllowed()) {
                        filteredEntities.add(entity);
                    }
                }
            }
        }
        else {
            filteredEntities.addAll(allEntities);
        }

        // filter names according to the specified pattern
        NamePatternMatcher namePatternMatcher = new NamePatternMatcher(
                this,
                includeEntitiesPattern,
                excludeEntitiesPattern);
        namePatternMatcher.filter(filteredEntities);

        if (false == ClassGenerator.VERSION_1_1.equals(generator.getVersionString())) {
            initializeVppConfig();
            generator.setVppConfig(vppConfig);
        }

        generator.setTimestamp(map.lastModified());
        generator.setDataMap(dataMap);
        generator.setObjEntities(filteredEntities);
        generator.validateAttributes();
        generator.execute();
    }

    /** Loads and returns a DataMap by File. */
    protected DataMap loadDataMap(File mapName) throws Exception {
        InputSource in = new InputSource(mapName.getCanonicalPath());
        return new MapLoader().loadDataMap(in);
    }

    /** Loads and returns DataMap based on <code>map</code> attribute. */
    protected DataMap loadDataMap() throws Exception {
        return loadDataMap(map);
    }

    /** Loads and returns DataMap based on <code>map</code> attribute. */
    protected DataMap[] loadAdditionalDataMaps() throws Exception {
        if (null == additionalMaps) {
            return new DataMap[0];
        }

        DataMap dataMaps[] = new DataMap[additionalMaps.length];
        for (int i = 0; i < additionalMaps.length; i++) {
            dataMaps[i] = loadDataMap(additionalMaps[i]);
        }
        return dataMaps;
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
     * Sets <code>outputPattern</code> property.
     * 
     * @since 1.2
     */
    public void setMode(String mode) {
        generator.setMode(mode);
    }

    /**
     * Provides a <code>VPPConfig</code> objec to configure. (Written with
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