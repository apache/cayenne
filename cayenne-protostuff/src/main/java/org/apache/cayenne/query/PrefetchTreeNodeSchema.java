/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.query;

import io.protostuff.Input;
import io.protostuff.Output;
import io.protostuff.Schema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * As {@link PrefetchTreeNode} has {@link PrefetchTreeNode#readResolve readResolve} method, which isn't supported
 * by Protostuff, we have to provide custom schema for this class.
 *
 * @see java.io.Serializable
 */
public class PrefetchTreeNodeSchema implements Schema<PrefetchTreeNode> {

    private static final HashMap<String, Integer> fieldMap = new HashMap<>();

    static {
        fieldMap.put("name", 1);
        fieldMap.put("phantom", 2);
        fieldMap.put("semantics", 3);
        fieldMap.put("ejbqlPathEntityId", 4);
        fieldMap.put("entityName", 5);
        fieldMap.put("children", 6);
    }

    @Override
    public String getFieldName(int number) {
        switch (number) {
            case 1:
                return "name";
            case 2:
                return "phantom";
            case 3:
                return "semantics";
            case 4:
                return "ejbqlPathEntityId";
            case 5:
                return "entityName";
            case 6:
                return "children";
            default:
                return null;
        }
    }

    @Override
    public int getFieldNumber(String name) {
        return fieldMap.getOrDefault(name, 0);
    }

    @Override
    public boolean isInitialized(PrefetchTreeNode message) {
        return true;
    }

    @Override
    public PrefetchTreeNode newMessage() {
        return new PrefetchTreeNode();
    }

    @Override
    public String messageName() {
        return PrefetchTreeNode.class.getSimpleName();
    }

    @Override
    public String messageFullName() {
        return PrefetchTreeNode.class.getName();
    }

    @Override
    public Class<PrefetchTreeNode> typeClass() {
        return PrefetchTreeNode.class;
    }

    @Override
    public void mergeFrom(Input input, PrefetchTreeNode message) throws IOException {
        for (int number = input.readFieldNumber(this);; number = input.readFieldNumber(this)) {
            switch (number) {
                case 0:
                    message.readResolve();
                    return;
                case 1:
                    message.name = input.readString();
                    break;
                case 2:
                    message.setPhantom(input.readBool());
                    break;
                case 3:
                    message.setSemantics(input.readInt32());
                    break;
                case 4:
                    message.setEjbqlPathEntityId(input.readString());
                    break;
                case 5:
                    message.setEntityName(input.readString());
                    break;
                case 6:
                    if (message.children == null) {
                        message.children = new ArrayList<>(4);
                    }
                    message.children.add(input.mergeObject(null, this));
                    break;
                default:
                    input.handleUnknownField(number, this);
            }
        }
    }

    @Override
    public void writeTo(Output output, PrefetchTreeNode message) throws IOException {
        if (message.getName() != null) {
            output.writeString(1, message.getName(), false);
        }

        output.writeBool(2, message.isPhantom(), false);
        output.writeInt32(3, message.getSemantics(), false);

        if (message.getEjbqlPathEntityId() != null) {
            output.writeString(4, message.getEjbqlPathEntityId(), false);
        }

        if (message.getEntityName() != null) {
            output.writeString(5, message.getEntityName(), false);
        }

        if (message.hasChildren()) {
            for (PrefetchTreeNode node : message.getChildren()) {
                output.writeObject(6, node, this, true);
            }
        }
    }

}
