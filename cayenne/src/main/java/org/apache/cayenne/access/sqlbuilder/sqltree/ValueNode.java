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

import java.util.Objects;
import java.util.function.Supplier;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.sqlbuilder.SQLGenerationContext;
import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.access.sqlbuilder.QuotingAppendable;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.map.DbAttribute;

/**
 * @since 4.2
 */
public class ValueNode extends Node {

    private final Object value;
    private final boolean isArray;
    // Used as hint for type of this value
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
    public QuotingAppendable append(QuotingAppendable buffer) {
        appendValue(value, buffer);
        return buffer;
    }

    protected void appendNullValue(QuotingAppendable buffer) {
        buffer.append(" NULL");
    }

    private void appendValue(Object val, QuotingAppendable buffer) {
        if(val == null) {
            appendNullValue(buffer);
            return;
        }

        if(isArray && val.getClass().isArray()) {
            if(val instanceof short[]) {
                appendValue((short[])val, buffer);
            } else if(val instanceof char[]) {
                appendValue((char[])val, buffer);
            } else if(val instanceof int[]) {
                appendValue((int[])val, buffer);
            } else if(val instanceof long[]) {
                appendValue((long[])val, buffer);
            } else if(val instanceof float[]) {
                appendValue((float[])val, buffer);
            } else if(val instanceof double[]) {
                appendValue((double[])val, buffer);
            } else if(val instanceof boolean[]) {
                appendValue((boolean[])val, buffer);
            } else if(val instanceof Object[]) {
                appendValue((Object[]) val, buffer);
            } else if(val instanceof byte[]) {
                // append byte[] array as single object
                appendValue((byte[])val, buffer);
            } else {
                throw new CayenneRuntimeException("Unsupported array type %s", val.getClass().getName());
            }
        } else {
            if(val instanceof Persistent) {
                appendValue((Persistent) val, buffer);
            } else if(val instanceof ObjectId) {
                appendValue((ObjectId) val, buffer);
            } else if(val instanceof Supplier) {
                appendValue(((Supplier<?>) val).get(), buffer);
            } else if(val instanceof CharSequence) {
                appendStringValue(buffer, (CharSequence)val);
            } else {
                appendObjectValue(buffer, val);
            }
        }
    }

    protected void appendObjectValue(QuotingAppendable buffer, Object value) {
        if(value == null) {
            return;
        }
        if(buffer.getContext() == null || !needBinding) {
            buffer.append(' ').append(value.toString());
        } else {
            buffer.append(" ?");
            addValueBinding(buffer, value);
        }
    }

    protected void appendStringValue(QuotingAppendable buffer, CharSequence value) {
        if(buffer.getContext() == null || !needBinding) {
            buffer.append(" '").append(value).append("'");
        } else {
            // value can't be null here
            buffer.append(" ?");
            addValueBinding(buffer, value);
        }
    }

    protected void addValueBinding(QuotingAppendable buffer, Object value) {
        // value can't be null here
        SQLGenerationContext context = buffer.getContext();
        // allow translation in out-of-context scope, to be able to use as a standalone SQL generator
        ExtendedType<?> extendedType = context.getAdapter().getExtendedTypes().getRegisteredType(value.getClass());
        DbAttributeBinding binding = new DbAttributeBinding(attribute);
        binding.setStatementPosition(context.getBindings().size() + 1);
        binding.setExtendedType(extendedType);
        binding.setValue(value);
        context.getBindings().add(binding);
    }

    private void appendValue(Persistent value, QuotingAppendable buffer) {
        appendValue(value.getObjectId(), buffer);
    }

    private void appendValue(ObjectId value, QuotingAppendable buffer) {
        for(Object idVal: value.getIdSnapshot().values()) {
            appendValue(idVal, buffer);
        }
    }

    private void appendValue(short[] val, QuotingAppendable buffer) {
        boolean first = true;
        for(short i : val) {
            if(first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(char[] val, QuotingAppendable buffer) {
        boolean first = true;
        for(char i : val) {
            if(first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(int[] val, QuotingAppendable buffer) {
        boolean first = true;
        for(int i : val) {
            if(first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(long[] val, QuotingAppendable buffer) {
        boolean first = true;
        for(long i : val) {
            if(first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(float[] val, QuotingAppendable buffer) {
        boolean first = true;
        for(float i : val) {
            if(first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(double[] val, QuotingAppendable buffer) {
        boolean first = true;
        for(double i : val) {
            if(first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(boolean[] val, QuotingAppendable buffer) {
        boolean first = true;
        for(boolean i : val) {
            if(first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(byte[] val, QuotingAppendable buffer) {
        boolean first = true;
        for(byte i : val) {
            if(first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(Object[] val, QuotingAppendable buffer) {
        boolean first = true;
        for(Object i : val) {
            if(first) {
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
