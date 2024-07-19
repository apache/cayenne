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

package org.apache.cayenne.dbsync.reverse.dbimport;

import java.util.Objects;

import static org.apache.cayenne.util.Util.isBlank;

/**
 * @since 4.0.
 */
public class PatternParam {

    private String pattern;

    public PatternParam() {
    }

    public PatternParam(String pattern) {
        this.pattern = pattern;
    }

    public PatternParam(PatternParam original) {
        this.setPattern(original.getPattern());
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void setName(String name) {
        setPattern(name);
    }

    /**
     * Used by Maven plugin
     */
    public void set(String pattern) {
        setPattern(pattern);
    }


    /**
     * Used by Ant task
     */
    public void addText(String pattern) {
        if (isBlank(pattern)) {
            return;
        }

        setPattern(pattern);
    }

    /**
     * used by Ant?
     */
    public void addConfiguredPattern(AntNestedElement pattern) {
        set(pattern.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        }
        PatternParam patternParam = (PatternParam) obj;
        return patternParam.getPattern().equals(pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder(), "").toString();
    }

    public StringBuilder toString(StringBuilder res, String s) {
        res.append(s).append(getClass().getSimpleName()).append(": ").append(pattern).append("\n");
        return res;
    }
}
