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
package org.apache.cayenne.enhancer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * An enhancer that adds interceptor code to the getters and setters.
 * 
 * @since 3.0
 */
public abstract class AccessorVisitor extends ClassAdapter {

    // duplicated from JpaClassDescriptor.
    private static final Pattern GETTER_PATTERN = Pattern
            .compile("^(is|get)([A-Z])(.*)$");

    private static final Pattern SETTER_PATTERN = Pattern.compile("^set([A-Z])(.*)$");

    private EnhancementHelper helper;

    public static String propertyNameForGetter(String getterName) {
        Matcher getMatch = GETTER_PATTERN.matcher(getterName);
        if (getMatch.matches()) {
            return getMatch.group(2).toLowerCase() + getMatch.group(3);
        }

        return null;
    }

    public static String propertyNameForSetter(String setterName) {
        Matcher setMatch = SETTER_PATTERN.matcher(setterName);

        if (setMatch.matches()) {
            return setMatch.group(1).toLowerCase() + setMatch.group(2);
        }

        return null;
    }

    public AccessorVisitor(ClassVisitor cw) {
        super(cw);
        this.helper = new EnhancementHelper(this);
    }

    protected abstract boolean isEnhancedProperty(String property);

    protected abstract boolean isLazyFaulted(String property);

    @Override
    public void visit(
            int version,
            int access,
            String name,
            String signature,
            String superName,
            String[] interfaces) {

        helper.reset(name);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    protected MethodVisitor visitGetter(
            MethodVisitor mv,
            String property,
            Type propertyType) {

        if (isEnhancedProperty(property)) {
            if (isLazyFaulted(property)) {
                return new GetterVisitor(mv, helper, property, true);
            }
            else {
                return new GetterVisitor(mv, helper, property, false);
            }
        }

        return mv;
    }

    protected MethodVisitor visitSetter(
            MethodVisitor mv,
            String property,
            Type propertyType) {

        if (isEnhancedProperty(property)) {
            return new SetterVisitor(mv, helper, property, propertyType);
        }

        return mv;
    }

    @Override
    public MethodVisitor visitMethod(
            int access,
            String name,
            String desc,
            String signature,
            String[] exceptions) {

        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        // TODO: andrus, 10/8/2006 - what other signature checks do we need to do?

        Type returnType = Type.getReturnType(desc);
        Type[] args = Type.getArgumentTypes(desc);

        // possible setter
        if ("V".equals(returnType.getDescriptor())) {
            if (args.length == 1) {
                String setProperty = AccessorVisitor.propertyNameForSetter(name);
                if (setProperty != null) {
                    return visitSetter(mv, setProperty, args[0]);
                }
            }
        }
        // possible getter
        else if (args.length == 0) {
            String getProperty = AccessorVisitor.propertyNameForGetter(name);
            if (getProperty != null) {
                return visitGetter(mv, getProperty, returnType);
            }
        }

        return mv;
    }
}
