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
package org.apache.cayenne.gen;

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.commons.logging.Log;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

/**
 * A backwards compatible class generation action that delegates to the deprecated
 * {@link DefaultClassGenerator}.
 * 
 * @since 3.0
 * @deprecated since 3.0
 */
public class ClassGenerationAction1_1 extends ClassGenerationAction {

    public static final String SINGLE_CLASS_TEMPLATE = MapClassGenerator.SINGLE_CLASS_TEMPLATE_1_1;
    public static final String SUBCLASS_TEMPLATE = MapClassGenerator.SUBCLASS_TEMPLATE_1_1;
    public static final String SUPERCLASS_TEMPLATE = MapClassGenerator.SUPERCLASS_TEMPLATE_1_1;

    protected DefaultClassGenerator generator;
    protected List<ObjEntity> entities;
    protected Log logger;

    public ClassGenerationAction1_1() {
        this.generator = new LoggingGenerator();
        this.generator.setVersionString(ClassGenerator.VERSION_1_1);
        this.entities = new ArrayList<ObjEntity>();
    }

    @Override
    public void addEmbeddables(Collection<Embeddable> embeddables) {
        // noop - no embeddables support
    }

    @Override
    public void addEntities(Collection<ObjEntity> entities) {
        if (entities != null) {
            this.entities.addAll(entities);
            generator.setObjEntities(this.entities);
        }
    }

    @Override
    protected String customTemplateName(TemplateType type) {
        throw new UnsupportedOperationException("Not supported in 1.1 mode");
    }

    @Override
    protected String defaultTemplateName(TemplateType type) {
        throw new UnsupportedOperationException("Not supported in 1.1 mode");
    }

    @Override
    public void execute() throws Exception {
        generator.execute();
    }

    @Override
    protected void execute(Artifact artifact) throws Exception {
        throw new UnsupportedOperationException("Not supported in 1.1 mode");
    }

    @Override
    protected File fileForClass() throws Exception {
        throw new UnsupportedOperationException("Not supported in 1.1 mode");
    }

    @Override
    protected File fileForSuperclass() throws Exception {
        throw new UnsupportedOperationException("Not supported in 1.1 mode");
    }

    @Override
    protected String getSuperclassPrefix() {
        return super.getSuperclassPrefix();
    }

    @Override
    protected Template getTemplate(TemplateType type) throws Exception {
        throw new UnsupportedOperationException("Not supported in 1.1 mode");
    }

    @Override
    protected void resetContextForArtifact(Artifact artifact) {
        throw new UnsupportedOperationException("Not supported in 1.1 mode");
    }

    @Override
    protected boolean isOld(File file) {
        throw new UnsupportedOperationException("Not supported in 1.1 mode");
    }

    @Override
    protected File mkpath(File dest, String pkgName) throws Exception {
        throw new UnsupportedOperationException("Not supported in 1.1 mode");
    }

    @Override
    protected Writer openWriter(TemplateType templateType) throws Exception {
        throw new UnsupportedOperationException("Not supported in 1.1 mode");
    }

    @Override
    public void setArtifactsGenerationMode(String mode) {
        generator.setMode(mode);
    }

    @Override
    public void setContext(VelocityContext context) {
        // noop...
    }

    @Override
    public void setDataMap(DataMap dataMap) {
        generator.setDataMap(dataMap);
    }

    @Override
    public void setDestDir(File destDir) {
        generator.setDestDir(destDir);
    }

    @Override
    public void setEmbeddableSuperTemplate(String embeddableSuperTemplate) {
        // noop
    }

    @Override
    public void setEmbeddableTemplate(String embeddableTemplate) {
        // noop
    }

    @Override
    public void setEncoding(String encoding) {
        generator.setEncoding(encoding);
    }

    @Override
    public void setLogger(Log logger) {
        this.logger = logger;
    }

    @Override
    public void setMakePairs(boolean makePairs) {
        generator.setMakePairs(makePairs);
    }

    @Override
    public void setOutputPattern(String outputPattern) {
        generator.setOutputPattern(outputPattern);
    }

    @Override
    public void setOverwrite(boolean overwrite) {
        generator.setOverwrite(overwrite);
    }

    @Override
    public void setSuperPkg(String superPkg) {
        generator.setSuperPkg(superPkg);
    }

    @Override
    public void setSuperTemplate(String superTemplate) {
        generator.setSuperTemplate(superTemplate);
    }

    @Override
    public void setTemplate(String template) {
        generator.setTemplate(template);
    }

    @Override
    public void setTimestamp(long timestamp) {
        generator.setTimestamp(timestamp);
    }

    @Override
    public void setUsePkgPath(boolean usePkgPath) {
        generator.setUsePkgPath(usePkgPath);
    }

    @Override
    protected void validateAttributes() {
        try {
            generator.validateAttributes();
        }
        catch (Exception e) {
            throw new CayenneRuntimeException(e);
        }
    }

    final class LoggingGenerator extends DefaultClassGenerator {

        @Override
        protected File fileForClass(String pkgName, String className) throws Exception {

            File outFile = super.fileForClass(pkgName, className);
            if (outFile != null && logger != null) {
                logger.info("Generating class file: " + outFile.getCanonicalPath());
            }

            return outFile;
        }

        @Override
        protected File fileForSuperclass(String pkgName, String className)
                throws Exception {
            File outFile = super.fileForSuperclass(pkgName, className);
            if (outFile != null && logger != null) {
                logger.info("Generating superclass file: " + outFile.getCanonicalPath());
            }

            return outFile;
        }
    }
}
