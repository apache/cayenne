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
package org.apache.cayenne.dbimport;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * @since 4.0.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class PatternParam {

    @XmlAttribute(name = "pattern")
    private String pattern;

    public PatternParam() {
    }

    public PatternParam(String pattern) {
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    /**
     * used my maven
     *
     * @param pattern
     */
    public void set(String pattern) {
        setPattern(pattern);
    }


    /**
     * used my ant
     *
     * @param pattern
     */
    public void addText(String pattern) {
        if (pattern.trim().isEmpty()) {
            return;
        }

        setPattern(pattern);
    }

    public void addConfiguredPattern(AntNestedElement pattern) {
        set(pattern.getName());
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
