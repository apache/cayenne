/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.tools.dbimport;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.cayenne.access.DbLoader;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.DbAdapterFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.merge.AbstractToModelToken;
import org.apache.cayenne.merge.AddRelationshipToDb;
import org.apache.cayenne.merge.DbMerger;
import org.apache.cayenne.merge.ExecutingMergerContext;
import org.apache.cayenne.merge.MergerContext;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.merge.MergerToken;
import org.apache.cayenne.merge.ModelMergeDelegate;
import org.apache.cayenne.merge.ProxyModelMergeDelegate;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.validation.SimpleValidationFailure;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;

/**
 * A thin wrapper around {@link DbLoader} that encapsulates DB import logic for
 * the benefit of Ant and Maven db importers.
 *
 * @since 4.0
 */
public interface DbImportAction {
    void execute(DbImportConfiguration config) throws Exception;
}
