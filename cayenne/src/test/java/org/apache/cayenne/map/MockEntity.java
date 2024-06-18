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

package org.apache.cayenne.map;

import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.XMLEncoder;

/**
 */
public class MockEntity extends Entity<MockEntity, MockAttribute, MockRelationship> {

    public MockEntity() {
        super();
    }

    public MockEntity(String name) {
        super(name);
    }

    @Override
    public Expression translateToRelatedEntity(
            Expression expression,
            CayennePath relationshipPath) {
        return null;
    }

    @Override
    public Iterator<CayenneMapEntry> resolvePathComponents(Expression pathExp) throws ExpressionException {
        return null;
    }
    
    @Override
    public Iterable<PathComponent<MockAttribute, MockRelationship>> resolvePath(Expression pathExp, Map<String, String> joinAliases) {
        return null;
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
    }

}
