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

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * @since 4.0.
 */
public class Catalog extends SchemaContainer implements XMLSerializable {

    public Catalog() {
    }

    public Catalog(String name) {
        setName(name);
    }

    public Catalog(Catalog original) {
        super(original);
    }

    @Override
    public StringBuilder toString(StringBuilder res, String prefix) {
        res.append(prefix).append("Catalog: ").append(getName()).append("\n");
        return super.toString(res, prefix + "  ");
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
        encoder.start("catalog")
            .nested(this.getIncludeTables(), delegate)
            .nested(this.getExcludeTables(), delegate)
            .nested(this.getIncludeColumns(), delegate)
            .nested(this.getExcludeColumns(), delegate)
            .nested(this.getIncludeProcedures(), delegate)
            .nested(this.getExcludeProcedures(), delegate)
            .simpleTag("name", this.getName())
            .nested(this.getSchemas(), delegate)
        .end();
    }
}
