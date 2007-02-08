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

package org.objectstyle.cayenne.gen;

import java.util.Iterator;

import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.project.validator.MappingNamesHelper;
import org.objectstyle.cayenne.util.NameConverter;

/**
 * Class generation engine for ObjEntities based on <a
 * href="http://jakarta.apache.org/velocity/" target="_blank">Velocity templates
 * </a>. Instance of ClassGenerationInfo is available inside Velocity template under
 * the key "classGen".
 * 
 * @author Andrei Adamchik
 * @since 1.2
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
     * Returns <code>superPackageName</code> property that defines a
     * superclass's package name.
     */
    public String getSuperPackageName() {
        return superPackageName;
    }

    /**
     * Sets <code>superPackageName</code> property that defines a superclass's
     * package name.
     */
    protected void setSuperPackageName(String superPackageName) {
        this.superPackageName = superPackageName;
    }

    /**
     * Returns class name (without a package) of the class associated with this
     * generator.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets class name of the class associated with this
     * generator. Class name must not include a package.
     */
    protected void setClassName(String className) {
        this.className = className;
    }

    protected void setSuperPrefix(String superPrefix) {
        this.superPrefix = superPrefix;
    }

    public String formatJavaType(String type) {
        if (type != null) {
            if (type.startsWith("java.lang.") && type.indexOf(10, '.') < 0) {
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

    public String formatVariableName(String variableName) {
        if (MappingNamesHelper.getInstance().isReservedJavaKeyword(variableName)) {
            return "_" + variableName;
        } else {
            return variableName;
        }
    }

    /**
     * Returns prefix used to distinguish between superclass and subclass when
     * generating classes in pairs.
     */
    public String getSuperPrefix() {
        return superPrefix;
    }

    /**
     * Sets current class property name. This method is called during template
     * parsing for each of the class properties.
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
     * @return a current property name converted to a format used by java static
     *         final variables - all capitalized with underscores.
     * 
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
        
        Iterator it = entity.getDeclaredRelationships().iterator();
        while(it.hasNext()) {
            Relationship r = (Relationship) it.next();
            if(r.isToMany()) {
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
        
        Iterator it = entity.getRelationships().iterator();
        while(it.hasNext()) {
            Relationship r = (Relationship) it.next();
            if(r.isToMany()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns <code>true</code> if a class associated with this generator is
     * located in a package.
     */
    public boolean isUsingPackage() {
        return packageName != null;
    }

    /**
     * Returns <code>true</code> if a superclass class associated with this
     * generator is located in a package.
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
     * Returns the fully qualified super class of the data object class
     * associated with this generator
     */
    public String getSuperClassName() {
        return superClassName;
    }

    /**
     * Sets the fully qualified super class of the data object class associated
     * with this generator
     */
    protected void setSuperClassName(String value) {
        this.superClassName = value;
    }
}