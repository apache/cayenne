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

import java.io.Writer;
import java.util.Properties;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.NullLogSystem;

import foundrylogic.vpp.VPPConfig;

/**
 * Class generation engine for ObjEntities based on <a
 * href="http://jakarta.apache.org/velocity/" target="_blank">Velocity templates </a>.
 * Instance of ClassGenerationInfo is available inside Velocity template under the key
 * "classGen".
 * 
 * @deprecated since 3.0 template logic is merged into the code generation action.
 */
public class ClassGenerator {

    public static final String VERSION_1_1 = "1.1";
    public static final String VERSION_1_2 = "1.2";

    protected String versionString;
    protected Template classTemplate;
    protected Context velCtxt;
    protected ClassGenerationInfo classGenerationInfo; // only used for VERSION_1_1

    /**
     * Creates a new ClassGenerationInfo that uses a specified Velocity template.
     * 
     * @since 1.2
     * @param template to use
     * @param versionString of cgen
     * @throws Exception
     */
    public ClassGenerator(String template, String versionString) throws Exception {
        this.versionString = versionString;

        if (!VERSION_1_1.equals(versionString)) {
            throw new IllegalStateException(
                    "Illegal Version in generateClass(Writer,ObjEntity): "
                            + versionString);
        }

        velCtxt = new VelocityContext();
        classGenerationInfo = new ClassGenerationInfo();
        velCtxt.put("classGen", classGenerationInfo);

        initializeClassTemplate(template);
    }

    /**
     * Creates a new ClassGenerationInfo that uses a specified Velocity template.
     * 
     * @since 1.2
     * @param template to use
     * @param versionString of cgen
     * @param vppConfig for configuring VelocityEngine and VelocityContext
     * @throws Exception
     */
    public ClassGenerator(String template, String versionString, VPPConfig vppConfig)
            throws Exception {

        this.versionString = versionString;

        if (false == VERSION_1_2.equals(versionString)) {
            throw new IllegalStateException(
                    "Illegal Version in generateClass(Writer,ObjEntity): "
                            + versionString);
        }

        if (vppConfig != null) {
            velCtxt = vppConfig.getVelocityContext();
        }
        else {
            velCtxt = new VelocityContext();
        }

        initializeClassTemplate(template);
    }

    /**
     * Sets up VelocityEngine properties, creates a VelocityEngine instance, and fetches a
     * template using the VelocityEngine instance.
     * 
     * @since 1.2
     */
    private void initializeClassTemplate(String template) throws CayenneRuntimeException {
        VelocityEngine velocityEngine = new VelocityEngine();
        try {

            // use ClasspathResourceLoader for velocity templates lookup
            // if Cayenne URL is not null, load resource from this URL
            Properties props = new Properties();

            // null logger that will prevent velocity.log from being generated
            props.put(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, NullLogSystem.class
                    .getName());

            props.put("resource.loader", "cayenne");

            props.put("cayenne.resource.loader.class", ClassGeneratorResourceLoader.class
                    .getName());
            velocityEngine.init(props);
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Can't initialize Velocity", ex);
        }

        try {
            classTemplate = velocityEngine.getTemplate(template);
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Can't create template: " + template, ex);
        }
    }

    /**
     * Generates Java code for the ObjEntity. Output is written to the provided Writer.
     */
    public void generateClass(Writer out, ObjEntity entity) throws Exception {

        if (!VERSION_1_1.equals(versionString)) {
            throw new IllegalStateException(
                    "Illegal Version in generateClass(Writer,ObjEntity): "
                            + versionString);
        }

        classGenerationInfo.setObjEntity(entity);
        classTemplate.merge(velCtxt, out);
    }

    /**
     * Generates Java code for the ObjEntity. Output is written to the provided Writer.
     */
    public void generateClass(
            Writer out,
            DataMap dataMap,
            ObjEntity entity,
            String fqnBaseClass,
            String fqnSuperClass,
            String fqnSubClass) throws Exception {

        if (!VERSION_1_2.equals(versionString)) {
            throw new IllegalStateException(
                    "Illegal Version in generateClass(Writer,ObjEntity,String,String,String): "
                            + versionString);
        }

        if (null == dataMap) {
            throw new IllegalStateException(
                    "DataMap MapClassGenerator constructor required for v1.2 templating.");
        }

        velCtxt.put("objEntity", entity);
        velCtxt.put("stringUtils", StringUtils.getInstance());
        velCtxt.put("entityUtils", new EntityUtils(
                dataMap,
                entity,
                fqnBaseClass,
                fqnSuperClass,
                fqnSubClass));
        velCtxt.put("importUtils", new ImportUtils());

        classTemplate.merge(velCtxt, out);
    }

    // deprecated, delegated methods previously used internally in cayenne
    /**
     * @return Returns the classGenerationInfo in template.
     */
    public ClassGenerationInfo getClassGenerationInfo() {
        return classGenerationInfo;
    }
}
