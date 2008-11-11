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

import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.project.validator.MappingNamesHelper;
import org.apache.cayenne.util.NameConverter;

/**
 * Class generation engine for ObjEntities based on <a
 * href="http://jakarta.apache.org/velocity/" target="_blank">Velocity templates </a>.
 * Instance of ClassGenerationInfo is available inside Velocity template under the key
 * "classGen".
 * 
 * @since 1.2
 * @deprecated since 3.0, as class generator version 1.1 is deprecated.
 */
public class ClassGenerationInfo {

    protected ObjEntity entity;

    // template substitution values
    protected String packageName;
    protected String className;
    protected String superPrefix;
    protected String prop;
    protected String superPackageName;
    protected String superClassName;

    /**
     * Returns Java package name of the class associated with this generator.
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Sets Java package name of the class associated with this generator.
     */
    protected void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * Returns <code>superPackageName</code> property that defines a superclass's
     * package name.
     */
    public String getSuperPackageName() {
        return superPackageName;
    }

    /**
     * Sets <code>superPackageName</code> property that defines a superclass's package
     * name.
     */
    protected void setSuperPackageName(String superPackageName) {
        this.superPackageName = superPackageName;
    }

    /**
     * Returns class name (without a package) of the class associated with this generator.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets class name of the class associated with this generator. Class name must not
     * include a package.
     */
    protected void setClassName(String className) {
        this.className = className;
    }

    protected void setSuperPrefix(String superPrefix) {
        this.superPrefix = superPrefix;
    }

    public String formatJavaType(String type) {
        if (type != null) {
            if (type.startsWith("java.lang.") && type.indexOf('.', 10) < 0) {
                return type.substring("java.lang.".length());
            }

            if (packageName != null
                    && type.startsWith(packageName + '.')
                    && type.indexOf(packageName.length() + 1, '.') < 0) {
                return type.substring(packageName.length() + 1);
            }
        }

        return type;
    }

    /**
     * @since 3.0
     */
    public String formatJavaTypeAsNonBooleanPrimitive(String type) {
        String value = ImportUtils.classesForPrimitives.get(type);
        return formatJavaType(value != null ? value : type);
    }

    public boolean isNonBooleanPrimitive(String type) {
        return ImportUtils.classesForPrimitives.containsKey(type) && !isBoolean(type);
    }

    /**
     * @since 3.0
     */
    public boolean isBoolean(String type) {
        return "boolean".equals(type);
    }

    public String formatVariableName(String variableName) {
        if (MappingNamesHelper.getInstance().isReservedJavaKeyword(variableName)) {
            return "_" + variableName;
        }
        else {
            return variableName;
        }
    }

    /**
     * Returns prefix used to distinguish between superclass and subclass when generating
     * classes in pairs.
     */
    public String getSuperPrefix() {
        return superPrefix;
    }

    /**
     * Sets current class property name. This method is called during template parsing for
     * each of the class properties.
     */
    public void setProp(String prop) {
        this.prop = prop;
    }

    public String getProp() {
        return prop;
    }

    /**
     * Capitalizes the first letter of the property name.
     * 
     * @since 1.1
     */
    public String capitalized(String name) {
        if (name == null || name.length() == 0)
            return name;

        char c = Character.toUpperCase(name.charAt(0));
        return (name.length() == 1) ? Character.toString(c) : c + name.substring(1);
    }

    /**
     * Converts property name to Java constants naming convention.
     * 
     * @since 1.1
     */
    public String capitalizedAsConstant(String name) {
        if (name == null || name.length() == 0)
            return name;

        return NameConverter.javaToUnderscored(name);
    }

    /** Returns current property name with capitalized first letter */
    public String getCappedProp() {
        return capitalized(prop);
    }

    /**
     * @return a current property name converted to a format used by java static final
     *         variables - all capitalized with underscores.
     * @since 1.0.3
     */
    public String getPropAsConstantName() {
        return capitalizedAsConstant(prop);
    }

    /**
     * Returns true if current entity contains at least one Declared List property.
     * 
     * @since 1.2
     */
    public boolean isContainingDeclaredListProperties() {
        if (entity == null) {
            return false;
        }

        for (Relationship r : entity.getDeclaredRelationships()) {
            if (r.isToMany()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if current entity contains at least one List property.
     * 
     * @since 1.1
     */
    public boolean isContainingListProperties() {
        if (entity == null) {
            return false;
        }

        for (Relationship r : entity.getRelationships()) {
            if (r.isToMany()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns <code>true</code> if a class associated with this generator is located in
     * a package.
     */
    public boolean isUsingPackage() {
        return packageName != null;
    }

    /**
     * Returns <code>true</code> if a superclass class associated with this generator is
     * located in a package.
     */
    public boolean isUsingSuperPackage() {
        return superPackageName != null;
    }

    /** Returns entity for the class associated with this generator. */
    public ObjEntity getEntity() {
        return entity;
    }

    /**
     * @param entity The entity to set.
     */
    protected void setObjEntity(ObjEntity entity) {
        this.entity = entity;
    }

    /**
     * Returns the fully qualified super class of the data object class associated with
     * this generator
     */
    public String getSuperClassName() {
        return superClassName;
    }

    /**
     * Sets the fully qualified super class of the data object class associated with this
     * generator
     */
    protected void setSuperClassName(String value) {
        this.superClassName = value;
    }
}
