/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.map;

import org.objectstyle.cayenne.util.XMLEncoder;

/**
 * DbKeyGenerator is an abstraction of a primary key generator
 * It configures the primary key generation per DbEntity in a RDBMS independent
 * manner. DbAdapter generates actual key values based on the configuration.
 * For more details see data-map.dtd
 *
 * @author Andriy Shapochka
 */

public class DbKeyGenerator extends MapObject {
    public static final String ORACLE_TYPE = "ORACLE";
    public static final String NAMED_SEQUENCE_TABLE_TYPE = "NAMED_SEQUENCE_TABLE";

    private String generatorType;
    private Integer keyCacheSize;
    private String generatorName;

    public DbKeyGenerator() {
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {

        String type = getGeneratorType();
        if (type == null) {
            return;
        }

        encoder.println("<db-key-generator>");
        encoder.indent(1);

        encoder.print("<db-generator-type>");
        encoder.print(type);
        encoder.println("</db-generator-type>");

        String name = getGeneratorName();
        if (name != null) {
            encoder.print("<db-generator-name>");
            encoder.print(name);
            encoder.println("</db-generator-name>");
        }

        Integer cacheSize = getKeyCacheSize();
        if (cacheSize != null) {
            encoder.print("<db-key-cache-size>");
            encoder.print(String.valueOf(cacheSize));
            encoder.println("</db-key-cache-size>");
        }

        encoder.indent(-1);
        encoder.println("</db-key-generator>");
    }

    public DbEntity getDbEntity() {
        return (DbEntity) getParent();
    }

    public void setDbEntity(DbEntity entity) {
        setParent(entity);
    }

    public void setGeneratorType(String generatorType) {
        this.generatorType = generatorType;
        if (this.generatorType != null) {
            this.generatorType = this.generatorType.trim().toUpperCase();
            if (!(ORACLE_TYPE.equals(this.generatorType)
                || NAMED_SEQUENCE_TABLE_TYPE.equals(this.generatorType)))
                this.generatorType = null;
        }
    }

    public String getGeneratorType() {
        return generatorType;
    }

    public void setKeyCacheSize(Integer keyCacheSize) {
        this.keyCacheSize = keyCacheSize;
        if (this.keyCacheSize != null && this.keyCacheSize.intValue() < 1) {
            this.keyCacheSize = null;
        }
    }

    public Integer getKeyCacheSize() {
        return keyCacheSize;
    }

    public void setGeneratorName(String generatorName) {
        this.generatorName = generatorName;
        if (this.generatorName != null) {
            this.generatorName = this.generatorName.trim();
            if (this.generatorName.length() == 0)
                this.generatorName = null;
        }
    }

    public String getGeneratorName() {
        return generatorName;
    }

    public String toString() {
        return "{Type="
            + generatorType
            + ", Name="
            + generatorName
            + ", Cache="
            + keyCacheSize
            + "}";
    }
}