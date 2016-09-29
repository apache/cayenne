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
package org.apache.cayenne.tools.dbimport.config;

import org.apache.cayenne.dbsync.reverse.mapper.DbType;
import org.apache.cayenne.dbsync.reverse.mapper.DefaultJdbc2JavaTypeMapper;
import org.apache.cayenne.dbsync.reverse.mapper.Jdbc2JavaTypeMapper;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.logging.Log;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * @since 4.0.
 */
public class DefaultTypeMapperBuilder {

    private final DefaultJdbc2JavaTypeMapper mapper;
    private final Log logger;

    public DefaultTypeMapperBuilder(Log logger, TypeMapper typeMapper) {
        this.logger = logger;
        this.mapper = createMapper(typeMapper.getMapperClassName());

        for (Type type : typeMapper.getTypes()) {
            this.mapper.add(buildType(type), type.getJava());
        }
    }

    private DbType buildType(Type type) {
        return new DbType(
                type.getJdbc(),
                type.getLength(),
                type.getPrecision(),
                type.getScale(),
                type.getNotNull()
        );
    }

    private DefaultJdbc2JavaTypeMapper createMapper(String className) {
        if (!isBlank(className)) {
            try {
                return (DefaultJdbc2JavaTypeMapper) ClassUtils.getClass(Thread.currentThread()
                        .getContextClassLoader(), className).newInstance();
            } catch (ClassNotFoundException e) {
                logger.error("Can't load class '" + className + "': ", e);
            } catch (InstantiationException e) {
                logger.error("Can't instantiate '" + className + "' make sure it has default constructor.", e);
            } catch (IllegalAccessException e) {
                logger.error("Can't instantiate '" + className + "' make sure it has default constructor.", e);
            }
        }

        return new DefaultJdbc2JavaTypeMapper();
    }

    public DefaultTypeMapperBuilder setUsePrimitives(Boolean usePrimitives) {
        mapper.setUsePrimitives(usePrimitives);

        return this;
    }

    public Jdbc2JavaTypeMapper build() {
        return mapper;
    }
}
