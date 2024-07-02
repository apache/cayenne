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
package org.apache.cayenne.swing.components.tree;

import java.util.Objects;

/**
 * @since 5.0
 */
public class CheckBoxNodeData {

    protected final Object value;
    protected final State state;

    public CheckBoxNodeData(Object value, boolean isSelected) {
        this(value, isSelected ? State.SELECTED : State.DESELECTED);
    }

    public CheckBoxNodeData(CheckBoxNodeData data) {
        this(data.value, data.state);
    }

    public CheckBoxNodeData(Object value, State state) {
        this.value = value;
        this.state = Objects.requireNonNull(state);
    }

    protected CheckBoxNodeData(CheckBoxNodeData data, State state) {
        this(data.value, state);
    }

    public CheckBoxNodeData toggleState() {
        switch (state) {
            case DESELECTED:
            case INDETERMINATE:
                return withState(State.SELECTED);
            case SELECTED:
                return withState(State.DESELECTED);
            default:
                throw new IllegalStateException();
        }
    }

    public CheckBoxNodeData withState(State state) {
        return new CheckBoxNodeData(this, state);
    }

    public CheckBoxNodeData withState(boolean isSelected) {
        return isSelected ? withState(State.SELECTED) : withState(State.DESELECTED);
    }

    public boolean isSelected() {
        return getState() == State.SELECTED;
    }

    public State getState() {
        return state;
    }

    public Object getValue() {
        return value;
    }

    public String getLabel() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        CheckBoxNodeData data = (CheckBoxNodeData) object;
        return Objects.equals(value, data.value) && state == data.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, state);
    }

    @Override
    public String toString() {
        return "CheckBoxNodeData{" +
                "value=" + value +
                ", state=" + state +
                '}';
    }

    public enum State {
        SELECTED,
        DESELECTED,
        INDETERMINATE
    }
}
