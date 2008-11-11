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

import javax.persistence.FetchType;

import org.apache.cayenne.util.TreeNodeChild;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * An attribute container.
 * 
 */
public class JpaAttributes implements XMLSerializable {

    protected Collection<JpaId> ids;
    protected JpaEmbeddedId embeddedId;
    protected Collection<JpaBasic> basicAttributes;
    protected Collection<JpaVersion> versionAttributes;
    protected Collection<JpaManyToOne> manyToOneRelationships;
    protected Collection<JpaOneToMany> oneToManyRelationships;
    protected Collection<JpaOneToOne> oneToOneRelationships;
    protected Collection<JpaManyToMany> manyToManyRelationships;
    protected Collection<JpaEmbedded> embeddedAttributes;
    protected Collection<JpaTransient> transientAttributes;

    public void encodeAsXML(XMLEncoder encoder) {

        if (size() == 0) {
            return;
        }

        encoder.println("<attributes>");
        encoder.indent(1);

        if (ids != null) {
            encoder.print(ids);
        }

        if (embeddedId != null) {
            embeddedId.encodeAsXML(encoder);
        }

        if (basicAttributes != null) {
            encoder.print(basicAttributes);
        }

        if (versionAttributes != null) {
            encoder.print(versionAttributes);
        }

        if (manyToOneRelationships != null) {
            encoder.print(manyToOneRelationships);
        }

        if (oneToManyRelationships != null) {
            encoder.print(oneToManyRelationships);
        }

        if (oneToOneRelationships != null) {
            encoder.print(oneToOneRelationships);
        }

        if (manyToManyRelationships != null) {
            encoder.print(manyToManyRelationships);
        }

        if (embeddedAttributes != null) {
            encoder.print(embeddedAttributes);
        }

        if (transientAttributes != null) {
            encoder.print(transientAttributes);
        }

        encoder.indent(-1);
        encoder.println("</attributes>");
    }

    /**
     * Returns the names of attributes that are fetched lazily.
     */
    public Collection<String> getLazyAttributeNames() {
        Collection<String> lazyAttributes = new ArrayList<String>();

        if (basicAttributes != null) {
            for (JpaBasic attribute : basicAttributes) {
                if (attribute.getFetch() == FetchType.LAZY) {
                    lazyAttributes.add(attribute.getName());
                }
            }
        }

        // TODO: andrus 12/22/2007 - since Cayenne fetches all relationships lazily unless
        // query specifies a prefetch, for now we'll treat all relationships as LAZY (even
        // though JPA defines all one-to-one relationships as EAGER). To be JPA compliant
        // we need to change that at some point...

        if (oneToOneRelationships != null) {
            for (JpaOneToOne attribute : oneToOneRelationships) {
                lazyAttributes.add(attribute.getName());
            }
        }

        if (oneToManyRelationships != null) {
            for (JpaOneToMany attribute : oneToManyRelationships) {
                lazyAttributes.add(attribute.getName());
            }
        }

        if (manyToOneRelationships != null) {
            for (JpaManyToOne attribute : manyToOneRelationships) {
                lazyAttributes.add(attribute.getName());
            }
        }

        if (manyToManyRelationships != null) {
            for (JpaManyToMany attribute : manyToManyRelationships) {
                lazyAttributes.add(attribute.getName());
            }
        }

        return lazyAttributes;
    }

    public JpaAttribute getAttribute(String name) {
        if (name == null) {
            return null;
        }

        if (embeddedId != null && name.equals(embeddedId.getName())) {
            return embeddedId;
        }

        JpaAttribute attribute;

        attribute = getId(name);
        if (attribute != null) {
            return attribute;
        }

        attribute = getBasicAttribute(name);
        if (attribute != null) {
            return attribute;
        }

        attribute = getVersionAttribute(name);
        if (attribute != null) {
            return attribute;
        }

        attribute = getManyToOneRelationship(name);
        if (attribute != null) {
            return attribute;
        }

        attribute = getOneToManyRelationship(name);
        if (attribute != null) {
            return attribute;
        }

        attribute = getOneToOneRelationship(name);
        if (attribute != null) {
            return attribute;
        }

        attribute = getManyToManyRelationship(name);
        if (attribute != null) {
            return attribute;
        }

        attribute = getTransientAttribute(name);
        if (attribute != null) {
            return attribute;
        }

        attribute = getEmbeddedAttribute(name);
        if (attribute != null) {
            return attribute;
        }

        return null;
    }

    /**
     * Returns combined count of all attributes and relationships.
     */
    public int size() {
        int size = 0;

        if (embeddedId != null) {
            size++;
        }

        if (ids != null) {
            size += ids.size();
        }
        if (basicAttributes != null) {
            size += basicAttributes.size();
        }
        if (versionAttributes != null) {
            size += versionAttributes.size();
        }
        if (manyToOneRelationships != null) {
            size += manyToOneRelationships.size();
        }
        if (oneToManyRelationships != null) {
            size += oneToManyRelationships.size();
        }
        if (oneToOneRelationships != null) {
            size += oneToOneRelationships.size();
        }
        if (manyToManyRelationships != null) {
            size += manyToManyRelationships.size();
        }
        if (embeddedAttributes != null) {
            size += embeddedAttributes.size();
        }
        if (transientAttributes != null) {
            size += transientAttributes.size();
        }
        return size;
    }

    public JpaId getId(String idName) {
        if (idName == null) {
            throw new IllegalArgumentException("Null id name");
        }

        if (ids != null) {
            for (JpaId id : ids) {
                if (idName.equals(id.getName())) {
                    return id;
                }
            }
        }

        return null;
    }

    public JpaId getIdForColumnName(String idColumnName) {
        if (idColumnName == null) {
            throw new IllegalArgumentException("Null id column name");
        }

        if (ids != null) {
            for (JpaId id : ids) {

                if (id.getColumn() != null
                        && idColumnName.equals(id.getColumn().getName())) {
                    return id;
                }
            }
        }

        return null;
    }

    /**
     * Returns a JpaAttribute for a given property name
     */
    public JpaBasic getBasicAttribute(String attributeName) {
        if (attributeName == null) {
            throw new IllegalArgumentException("Null attribute name");
        }

        if (basicAttributes != null) {
            for (JpaBasic attribute : basicAttributes) {
                if (attributeName.equals(attribute.getName())) {
                    return attribute;
                }
            }
        }

        return null;
    }

    public JpaManyToOne getManyToOneRelationship(String attributeName) {
        if (attributeName == null) {
            throw new IllegalArgumentException("Null attribute name");
        }

        if (manyToOneRelationships != null) {
            for (JpaManyToOne attribute : manyToOneRelationships) {
                if (attributeName.equals(attribute.getName())) {
                    return attribute;
                }
            }
        }

        return null;
    }

    public JpaOneToMany getOneToManyRelationship(String attributeName) {
        if (attributeName == null) {
            throw new IllegalArgumentException("Null attribute name");
        }

        if (oneToManyRelationships != null) {
            for (JpaOneToMany attribute : oneToManyRelationships) {
                if (attributeName.equals(attribute.getName())) {
                    return attribute;
                }
            }
        }

        return null;
    }

    @TreeNodeChild(type = JpaId.class)
    public Collection<JpaId> getIds() {
        if (ids == null) {
            ids = new ArrayList<JpaId>();
        }

        return ids;
    }

    @TreeNodeChild
    public JpaEmbeddedId getEmbeddedId() {
        return embeddedId;
    }

    public void setEmbeddedId(JpaEmbeddedId embeddedId) {
        this.embeddedId = embeddedId;
    }

    @TreeNodeChild(type = JpaBasic.class)
    public Collection<JpaBasic> getBasicAttributes() {
        if (basicAttributes == null) {
            basicAttributes = new ArrayList<JpaBasic>();
        }
        return basicAttributes;
    }

    @TreeNodeChild(type = JpaEmbedded.class)
    public Collection<JpaEmbedded> getEmbeddedAttributes() {
        if (embeddedAttributes == null) {
            embeddedAttributes = new ArrayList<JpaEmbedded>();
        }
        return embeddedAttributes;
    }

    public JpaEmbedded getEmbeddedAttribute(String attributeName) {
        if (attributeName == null) {
            throw new IllegalArgumentException("Null attribute name");
        }

        if (embeddedAttributes != null) {
            for (JpaEmbedded attribute : embeddedAttributes) {
                if (attributeName.equals(attribute.getName())) {
                    return attribute;
                }
            }
        }

        return null;
    }

    @TreeNodeChild(type = JpaManyToMany.class)
    public Collection<JpaManyToMany> getManyToManyRelationships() {
        if (manyToManyRelationships == null) {
            manyToManyRelationships = new ArrayList<JpaManyToMany>();
        }
        return manyToManyRelationships;
    }

    public JpaManyToMany getManyToManyRelationship(String attributeName) {
        if (attributeName == null) {
            throw new IllegalArgumentException("Null attribute name");
        }

        if (manyToManyRelationships != null) {
            for (JpaManyToMany attribute : manyToManyRelationships) {
                if (attributeName.equals(attribute.getName())) {
                    return attribute;
                }
            }
        }

        return null;
    }

    @TreeNodeChild(type = JpaManyToOne.class)
    public Collection<JpaManyToOne> getManyToOneRelationships() {
        if (manyToOneRelationships == null) {
            manyToOneRelationships = new ArrayList<JpaManyToOne>();
        }
        return manyToOneRelationships;
    }

    @TreeNodeChild(type = JpaOneToMany.class)
    public Collection<JpaOneToMany> getOneToManyRelationships() {
        if (oneToManyRelationships == null) {
            oneToManyRelationships = new ArrayList<JpaOneToMany>();
        }
        return oneToManyRelationships;
    }

    @TreeNodeChild(type = JpaOneToOne.class)
    public Collection<JpaOneToOne> getOneToOneRelationships() {
        if (oneToOneRelationships == null) {
            oneToOneRelationships = new ArrayList<JpaOneToOne>();
        }
        return oneToOneRelationships;
    }

    public JpaOneToOne getOneToOneRelationship(String attributeName) {
        if (attributeName == null) {
            throw new IllegalArgumentException("Null attribute name");
        }

        if (oneToOneRelationships != null) {
            for (JpaOneToOne attribute : oneToOneRelationships) {
                if (attributeName.equals(attribute.getName())) {
                    return attribute;
                }
            }
        }

        return null;
    }

    @TreeNodeChild(type = JpaTransient.class)
    public Collection<JpaTransient> getTransientAttributes() {
        if (transientAttributes == null) {
            transientAttributes = new ArrayList<JpaTransient>();
        }
        return transientAttributes;
    }

    /**
     * Returns a JpaTransient for a given property name
     */
    public JpaTransient getTransientAttribute(String attributeName) {
        if (attributeName == null) {
            throw new IllegalArgumentException("Null attribute name");
        }

        if (transientAttributes != null) {
            for (JpaTransient attribute : transientAttributes) {
                if (attributeName.equals(attribute.getName())) {
                    return attribute;
                }
            }
        }

        return null;
    }

    @TreeNodeChild(type = JpaVersion.class)
    public Collection<JpaVersion> getVersionAttributes() {
        if (versionAttributes == null) {
            versionAttributes = new ArrayList<JpaVersion>();
        }
        return versionAttributes;
    }

    /**
     * Returns a JpaTransient for a given property name
     */
    public JpaVersion getVersionAttribute(String attributeName) {
        if (attributeName == null) {
            throw new IllegalArgumentException("Null attribute name");
        }

        if (versionAttributes != null) {
            for (JpaVersion attribute : versionAttributes) {
                if (attributeName.equals(attribute.getName())) {
                    return attribute;
                }
            }
        }

        return null;
    }
}
