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


package org.apache.cayenne.jpa.map;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.NamedQuery;

import org.apache.cayenne.util.TreeNodeChild;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

public class JpaNamedQuery implements XMLSerializable {

    protected String name;
    protected String query;
    protected Collection<JpaQueryHint> hints;

    public JpaNamedQuery() {

    }

    public JpaNamedQuery(NamedQuery annotation) {
        name = annotation.name();
        query = annotation.query();

        getHints();
        for (int i = 0; i < annotation.hints().length; i++) {
            hints.add(new JpaQueryHint(annotation.hints()[i]));
        }
    }
    
    public void encodeAsXML(XMLEncoder encoder) {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @TreeNodeChild(type=JpaQueryHint.class)
    public Collection<JpaQueryHint> getHints() {
        if (hints == null) {
            hints = new ArrayList<JpaQueryHint>();
        }

        return hints;
    }

    /**
     * Returns a hint matching hint name.
     */
    public JpaQueryHint getHint(String name) {
        if (hints == null) {
            return null;
        }

        for (JpaQueryHint hint : hints) {
            if (name.equals(hint.getName())) {
                return hint;
            }
        }
        return null;
    }
}
