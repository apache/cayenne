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

import org.apache.cayenne.ObjectContext;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * @since 3.0
 */
public class SetterVisitor extends MethodAdapter {

    private EnhancementHelper helper;
    private String propertyName;
    private Type propertyType;

    public SetterVisitor(MethodVisitor mv, EnhancementHelper helper, String propertyName,
            Type propertyType) {
        super(mv);
        this.helper = helper;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
    }

    @Override
    public void visitCode() {
        super.visitCode();

        String field = helper.getPropertyField("objectContext");
        Type objectContextType = Type.getType(ObjectContext.class);
        String propertyDescriptor = propertyType.getDescriptor();

        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(
                Opcodes.GETFIELD,
                helper.getCurrentClass().getInternalName(),
                field,
                objectContextType.getDescriptor());
        Label l1 = new Label();
        mv.visitJumpInsn(Opcodes.IFNULL, l1);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(
                Opcodes.GETFIELD,
                helper.getCurrentClass().getInternalName(),
                field,
                objectContextType.getDescriptor());
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn(propertyName);
        mv.visitVarInsn(Opcodes.ALOAD, 0);

        mv.visitFieldInsn(
                Opcodes.GETFIELD,
                helper.getCurrentClass().getInternalName(),
                propertyName,
                propertyDescriptor);

        if (!visitPrimitiveConversion(propertyDescriptor)) {
            mv.visitVarInsn(Opcodes.ALOAD, 1);
        }

        mv
                .visitMethodInsn(
                        Opcodes.INVOKEINTERFACE,
                        objectContextType.getInternalName(),
                        "propertyChanged",
                        "(Lorg/apache/cayenne/Persistent;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V");
        mv.visitLabel(l1);
    }

    protected boolean visitPrimitiveConversion(String propertyDescriptor) {
        if (propertyDescriptor.length() != 1) {
            return false;
        }

        switch (propertyDescriptor.charAt(0)) {
            case 'I':
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Integer",
                        "valueOf",
                        "(I)Ljava/lang/Integer;");
                mv.visitVarInsn(Opcodes.ILOAD, 1);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Integer",
                        "valueOf",
                        "(I)Ljava/lang/Integer;");
                return true;
            case 'D':
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Double",
                        "valueOf",
                        "(D)Ljava/lang/Double;");
                mv.visitVarInsn(Opcodes.DLOAD, 1);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Double",
                        "valueOf",
                        "(D)Ljava/lang/Double;");
                return true;
            case 'F':
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Float",
                        "valueOf",
                        "(F)Ljava/lang/Float;");
                mv.visitVarInsn(Opcodes.FLOAD, 1);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Float",
                        "valueOf",
                        "(F)Ljava/lang/Float;");
                return true;
            case 'Z':
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Boolean",
                        "valueOf",
                        "(Z)Ljava/lang/Boolean;");
                mv.visitVarInsn(Opcodes.ILOAD, 1);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Boolean",
                        "valueOf",
                        "(Z)Ljava/lang/Boolean;");
                return true;
            case 'J':
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Long",
                        "valueOf",
                        "(J)Ljava/lang/Long;");
                mv.visitVarInsn(Opcodes.LLOAD, 1);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Long",
                        "valueOf",
                        "(J)Ljava/lang/Long;");
                return true;
            case 'B':
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Byte",
                        "valueOf",
                        "(B)Ljava/lang/Byte;");
                mv.visitVarInsn(Opcodes.ILOAD, 1);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Byte",
                        "valueOf",
                        "(B)Ljava/lang/Byte;");
                return true;
            case 'C':
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Character",
                        "valueOf",
                        "(C)Ljava/lang/Character;");
                mv.visitVarInsn(Opcodes.ILOAD, 1);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Character",
                        "valueOf",
                        "(C)Ljava/lang/Character;");
                return true;
            case 'S':
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Short",
                        "valueOf",
                        "(S)Ljava/lang/Short;");
                mv.visitVarInsn(Opcodes.ILOAD, 1);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Short",
                        "valueOf",
                        "(S)Ljava/lang/Short;");
                return true;
            default:
                return false;
        }
    }
}
