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

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.NullLogSystem;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;

import foundrylogic.vpp.VPPConfig;

/**
 * Class generation engine for ObjEntities based on <a
 * href="http://jakarta.apache.org/velocity/" target="_blank">Velocity templates </a>.
 * Instance of ClassGenerationInfo is available inside Velocity template under the key
 * "classGen".
 * 
 * @author Andrei Adamchik
 */
public class ClassGenerator {

    public static final String VERSION_1_1 = "1.1";
    public static final String VERSION_1_2 = "1.2";

    protected String versionString;
    protected Template classTemplate;
    protected Context velCtxt;
    protected ClassGenerationInfo classGenerationInfo; // only used for VERSION_1_1

    /**
     * @deprecated since 1.2, as now Cayenne uses thread ClassLoader.
     */
    public synchronized static final void bootstrapVelocity(Class cl) {
        // noop
    }

    /**
     * Creates a new ClassGenerationInfo that uses a specified Velocity template.
     * 
     * @deprecated Since 1.2 use ClassGenerator(String template, String versionString)
     *             instead.
     */
    public ClassGenerator(String template) throws Exception {
        this(template, "1.1");
    }

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

        if (false == VERSION_1_1.equals(versionString)) {
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
        if (false == VERSION_1_1.equals(versionString)) {
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
        if (false == VERSION_1_2.equals(versionString)) {
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

    /**
     * Returns Java package name of the class associated with this generator.
     * 
     * @deprecated Since 1.2 use getClassGenerationInfo().getPackageName()
     */
    public String getPackageName() {
        return classGenerationInfo.getPackageName();
    }

    /**
     * Sets Java package name of the class associated with this generator.
     * 
     * @deprecated Since 1.2 use getClassGenerationInfo().setPackageName()
     */
    public void setPackageName(String packageName) {
        classGenerationInfo.setPackageName(packageName);
    }

    /**
     * Sets <code>superPackageName</code> property that defines a superclass's package
     * name.
     * 
     * @deprecated Since 1.2 use getClassGenerationInfo().setSuperPackageName()
     */
    public void setSuperPackageName(String superPackageName) {
        classGenerationInfo.setSuperPackageName(superPackageName);
    }

    /**
     * Returns class name (without a package) of the class associated with this generator.
     * 
     * @deprecated Since 1.2 use getClassGenerationInfo().getClassName()
     */
    public String getClassName() {
        return classGenerationInfo.getClassName();
    }

    /**
     * Sets class name of the class associated with this generator. Class name must not
     * include a package.
     * 
     * @deprecated Since 1.2 use getClassGenerationInfo().setClassName()
     */
    public void setClassName(String className) {
        classGenerationInfo.setClassName(className);
    }

    /**
     * Sets the fully qualified super class of the data object class associated with this
     * generator
     * 
     * @deprecated Since 1.2 use getClassGenerationInfo().setSuperClassName()
     */
    public void setSuperClassName(String value) {
        classGenerationInfo.setSuperClassName(value);
    }

    /**
     * Returns prefix used to distinguish between superclass and subclass when generating
     * classes in pairs.
     * 
     * @deprecated Since 1.2 use getClassGenerationInfo().setSuperPrefix()
     */
    public void setSuperPrefix(String superPrefix) {
        classGenerationInfo.setSuperPrefix(superPrefix);
    }

    // deprecated, delegated methods not used internally in cayenne

    /**
     * Returns <code>superPackageName</code> property that defines a superclass's
     * package name.
     * 
     * @deprecated Since 1.2 use getClassGenerationInfo().getSuperPackageName()
     */
    public String getSuperPackageName() {
        return classGenerationInfo.getSuperPackageName();
    }

    /**
     * @deprecated use getClassGenerationInfo().formatJavaType(String)
     */
    public String formatJavaType(String type) {
        return classGenerationInfo.formatJavaType(type);
    }

    /**
     * @deprecated Since 1.2 use getClassGenerationInfo().formatVariableName(String)
     */
    public String formatVariableName(String variableName) {
        return classGenerationInfo.formatVariableName(variableName);
    }

    /**
     * Returns prefix used to distinguish between superclass and subclass when generating
     * classes in pairs.
     * 
     * @deprecated Since 1.2 use getClassGenerationInfo().getSuperPrefix()
     */
    public String getSuperPrefix() {
        return classGenerationInfo.getSuperPrefix();
    }

    /**
     * Sets current class property name. This method is called during template parsing for
     * each of the class properties.
     * 
     * @deprecated Since 1.2 use getClassGenerationInfo().setProp(String)
     */
    public void setProp(String prop) {
        classGenerationInfo.setProp(prop);
    }

    /**
     * @deprecated Since 1.2 use getClassGenerationInfo().getProp()
     */
    public String getProp() {
        return classGenerationInfo.getProp();
    }

    /**
     * Capitalizes the first letter of the property name.
     * 
     * @since 1.1
     * @deprecated Since 1.2 use getClassGenerationInfo().capitalized(String)
     */
    public String capitalized(String name) {
        return classGenerationInfo.capitalized(name);
    }

    /**
     * Converts property name to Java constants naming convention.
     * 
     * @since 1.1
     * @deprecated Since 1.2 use getClassGenerationInfo().capitalizedAsConstant(String)
     */
    public String capitalizedAsConstant(String name) {
        return classGenerationInfo.capitalizedAsConstant(name);
    }

    /**
     * Returns current property name with capitalized first letter
     * 
     * @deprecated Since 1.2 use getClassGenerationInfo().getCappedProp()
     */
    public String getCappedProp() {
        return classGenerationInfo.getCappedProp();
    }

    /**
     * @return a current property name converted to a format used by java static final
     *         variables - all capitalized with underscores.
     * @since 1.0.3
     * @deprecated Since 1.2 use getClassGenerationInfo().getPropAsConstantName()
     */
    public String getPropAsConstantName() {
        return classGenerationInfo.getPropAsConstantName();
    }

    /**
     * Returns true if current entity contains at least one List property.
     * 
     * @since 1.1
     * @deprecated Since 1.2 use getClassGenerationInfo().isContainingListProperties()
     */
    public boolean isContainingListProperties() {
        return classGenerationInfo.isContainingListProperties();
    }

    /**
     * Returns <code>true</code> if a class associated with this generator is located in
     * a package.
     * 
     * @deprecated Since 1.2 use getClassGenerationInfo().isUsingPackage()
     */
    public boolean isUsingPackage() {
        return classGenerationInfo.isUsingPackage();
    }

    /**
     * Returns <code>true</code> if a superclass class associated with this generator is
     * located in a package.
     * 
     * @deprecated Since 1.2 use getClassGenerationInfo().isUsingSuperPackage()
     */
    public boolean isUsingSuperPackage() {
        return classGenerationInfo.isUsingSuperPackage();
    }

    /**
     * Returns entity for the class associated with this generator.
     * 
     * @deprecated Since 1.2 use getClassGenerationInfo().getEntity()
     */
    public ObjEntity getEntity() {
        return classGenerationInfo.getEntity();
    }

    /**
     * Returns the fully qualified super class of the data object class associated with
     * this generator
     * 
     * @deprecated Since 1.2 use getClassGenerationInfo().getSuperClassName()
     */
    public String getSuperClassName() {
        return classGenerationInfo.getSuperClassName();
    }
}
