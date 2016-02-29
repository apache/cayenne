/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/
package org.apache.cayenne.dbimport;


import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.bind.annotation.*;
import java.io.*;
import java.util.*;

/**
 * @since 4.0.
 */
@XmlRootElement(name = "reverseEngineering")
@XmlAccessorType(XmlAccessType.FIELD)
public class ReverseEngineering extends FilterContainer implements ConfigurationNode, Serializable {

    private static final Log LOG = LogFactory.getLog(ReverseEngineering.class);

    public ReverseEngineering(String name) {
        this.name = name;
    }

    @XmlTransient
    private String name;

    private Boolean skipRelationshipsLoading;

    private Boolean skipPrimaryKeyLoading;

    /*
     * Typical types are "TABLE",
     * "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY",
     * "LOCAL TEMPORARY", "ALIAS", "SYNONYM"., etc.
     */
    @XmlElement(name = "tableType")
    private Collection<String> tableTypes = new LinkedList<String>();

    @XmlElement(name = "catalog")
    private Collection<Catalog> catalogs = new LinkedList<Catalog>();

    @XmlElement(name = "schema")
    private Collection<Schema> schemas = new LinkedList<Schema>();

    /**
     * @since 4.0
     */
    @XmlTransient
    protected Resource configurationSource;

    public ReverseEngineering() {
    }

    public Boolean getSkipRelationshipsLoading() {
        return skipRelationshipsLoading;
    }

    public void setSkipRelationshipsLoading(Boolean skipRelationshipsLoading) {
        this.skipRelationshipsLoading = skipRelationshipsLoading;
    }

    public Boolean getSkipPrimaryKeyLoading() {
        return skipPrimaryKeyLoading;
    }

    public void setSkipPrimaryKeyLoading(Boolean skipPrimaryKeyLoading) {
        this.skipPrimaryKeyLoading = skipPrimaryKeyLoading;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<Catalog> getCatalogs() {
        return catalogs;
    }

    public void setCatalogs(Collection<Catalog> catalogs) {
        this.catalogs = catalogs;
    }

    public Collection<Schema> getSchemas() {
        return schemas;
    }

    public void setSchemas(Collection<Schema> schemas) {
        this.schemas = schemas;
    }

    public String[] getTableTypes() {
        return tableTypes.toArray(new String[tableTypes.size()]);
    }

    /*
     * Typical types are "TABLE",
     * "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY",
     * "LOCAL TEMPORARY", "ALIAS", "SYNONYM"., etc.
     */
    public void setTableTypes(Collection<String> tableTypes) {
        this.tableTypes = tableTypes;
    }

    /*
     * Typical types are "TABLE",
     * "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY",
     * "LOCAL TEMPORARY", "ALIAS", "SYNONYM"., etc.
     */
    public void addTableType(String type) {
        this.tableTypes.add(type);
    }

    public void addSchema(Schema schema) {
        this.schemas.add(schema);
    }

    public void addCatalog(Catalog catalog) {
        this.catalogs.add(catalog);
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append("ReverseEngineering: ").append("\n");

        if (!isBlank(catalogs)) {
            for (Catalog catalog : catalogs) {
                catalog.toString(res, "  ");
            }
        }

        if (!isBlank(schemas)) {
            for (Schema schema : schemas) {
                schema.toString(res, "  ");
            }
        }

        if (skipRelationshipsLoading != null && skipRelationshipsLoading) {
            res.append("\n").append("        Skip Relationships Loading");
        }
        if (skipPrimaryKeyLoading != null && skipPrimaryKeyLoading) {
            res.append("\n").append("        Skip PrimaryKey Loading");
        }

        return super.toString(res, "  ").toString();
    }

    @Override
    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return visitor.visitReverseEngineering(this);
    }

    public void encodeAsXML(XMLEncoder encoder) {
        DefaultReverseEngineeringWriter defaultReverseEngineeringWriter = new DefaultReverseEngineeringWriter();
        defaultReverseEngineeringWriter.write(this, encoder.getPrintWriter());
    }

    /**
     * @since 4.0
     */
    public Resource getConfigurationSource() {
        return configurationSource;
    }

    /**
     * @since 4.0
     */
    public void setConfigurationSource(Resource configurationSource) {
        this.configurationSource = configurationSource;
    }
}
