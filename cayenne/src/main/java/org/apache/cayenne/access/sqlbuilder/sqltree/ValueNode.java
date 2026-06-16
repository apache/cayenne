/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access.sqlbuilder.sqltree;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.sqlbuilder.SQLAppendable;
import org.apache.cayenne.access.sqlbuilder.SQLGenerationContext;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * @since 4.2
 */
public class ValueNode extends Node {

    private final Object value;
    private final boolean isArray;
    private final DbAttribute attribute;
    private final boolean needBinding;

    public ValueNode(Object value, boolean isArray, DbAttribute attribute, boolean needBinding) {
        super(NodeType.VALUE);
        this.value = value;
        this.isArray = isArray;
        this.attribute = attribute;
        this.needBinding = needBinding;
    }

    public Object getValue() {
        return value;
    }

    public DbAttribute getAttribute() {
        return attribute;
    }

    public boolean isArray() {
        return isArray;
    }

    @Override
    public SQLAppendable append(SQLAppendable buffer) {
        appendValue(value, buffer);
        return buffer;
    }

    protected void appendNullValue(SQLAppendable buffer) {
        buffer.append(" NULL");
    }

    private void appendValue(Object val, SQLAppendable buffer) {
        if (val == null) {
            appendNullValue(buffer);
            return;
        }

        if (isArray && val.getClass().isArray()) {
            switch (val) {
                case short[] shorts -> appendValue(shorts, buffer);
                case char[] chars -> appendValue(chars, buffer);
                case int[] ints -> appendValue(ints, buffer);
                case long[] longs -> appendValue(longs, buffer);
                case float[] floats -> appendValue(floats, buffer);
                case double[] doubles -> appendValue(doubles, buffer);
                case boolean[] booleans -> appendValue(booleans, buffer);
                case Object[] objects -> appendValue(objects, buffer);
                // append byte[] array as a single object
                case byte[] bytes -> appendValue(bytes, buffer);
                default -> throw new CayenneRuntimeException("Unsupported array type %s", val.getClass().getName());
            }
        } else {
            switch (val) {
                case Persistent persistent -> appendValue(persistent, buffer);
                case ObjectId objectId -> appendValue(objectId, buffer);
                case Supplier<?> supplier -> appendValue(supplier.get(), buffer);
                case CharSequence charSequence -> appendStringValue(buffer, charSequence);
                default -> appendObjectValue(buffer, val);
            }
        }
    }

    protected void appendObjectValue(SQLAppendable buffer, Object value) {
        if (value == null) {
            return;
        }
        if (buffer.getContext() == null || !needBinding) {
            buffer.append(' ').append(value.toString());
        } else {
            buffer.append(" ?");
            addValueBinding(buffer, value);
        }
    }

    protected void appendStringValue(SQLAppendable buffer, CharSequence value) {
        if (buffer.getContext() == null || !needBinding) {
            buffer.append(" '").append(value.toString()).append("'");
        } else {
            // value can't be null here
            buffer.append(" ?");
            addValueBinding(buffer, value);
        }
    }

    protected void addValueBinding(SQLAppendable buffer, Object value) {
        // value can't be null here
        SQLGenerationContext context = buffer.getContext();
        // allow translation in out-of-context scope, to be able to use as a standalone SQL generator
        ExtendedType<?> extendedType = context.getAdapter().getExtendedTypes().getRegisteredType(value.getClass());

        // 'attribute' is only a type hint and may be absent (e.g. function arguments and other
        // literals not bound to a column); fall back to deriving the JDBC type from the value
        ParameterBinding binding = (attribute != null)
                ? new ParameterBinding(context.getAdapter().preferredBindingType(attribute.getType()), attribute.getScale(), attribute)
                : new ParameterBinding(context.getAdapter().preferredBindingType(TypesMapping.getSqlTypeByJava(value.getClass())), -1);

        binding.reset(context.getBindings().size() + 1, value, extendedType);
        context.getBindings().add(binding);
    }

    private void appendValue(Persistent value, SQLAppendable buffer) {
        appendValue(value.getObjectId(), buffer);
    }

    private void appendValue(ObjectId value, SQLAppendable buffer) {
        for (Object idVal : value.getIdSnapshot().values()) {
            appendValue(idVal, buffer);
        }
    }

    private void appendValue(short[] val, SQLAppendable buffer) {
        boolean first = true;
        for (short i : val) {
            if (first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(char[] val, SQLAppendable buffer) {
        boolean first = true;
        for (char i : val) {
            if (first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(int[] val, SQLAppendable buffer) {
        boolean first = true;
        for (int i : val) {
            if (first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(long[] val, SQLAppendable buffer) {
        boolean first = true;
        for (long i : val) {
            if (first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(float[] val, SQLAppendable buffer) {
        boolean first = true;
        for (float i : val) {
            if (first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(double[] val, SQLAppendable buffer) {
        boolean first = true;
        for (double i : val) {
            if (first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(boolean[] val, SQLAppendable buffer) {
        boolean first = true;
        for (boolean i : val) {
            if (first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(byte[] val, SQLAppendable buffer) {
        boolean first = true;
        for (byte i : val) {
            if (first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(Object[] val, SQLAppendable buffer) {
        boolean first = true;
        for (Object i : val) {
            if (first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    @Override
    public Node copy() {
        return new ValueNode(value, isArray, attribute, needBinding);
    }

    public boolean isNeedBinding() {
        return needBinding;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ValueNode valueNode = (ValueNode) o;
        return isArray == valueNode.isArray
                && needBinding == valueNode.needBinding
                && Objects.equals(value, valueNode.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value, isArray, needBinding);
    }
}
