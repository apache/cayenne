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


package org.apache.cayenne.regression;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.lang.SystemUtils;

/**
 * Preferences reads test configuration files (Apache ExtendedProperties format).
 * For more information see the sample configuration files.
 *
 * @author Andriy Shapochka
 */

public class Preferences {
    private File cayenneProject;
    private File workDirectory = new File(SystemUtils.JAVA_IO_TMPDIR);
    protected String schema;
    private String schemaDirPrefix = "schema";
    private int tableCount = 20;
    private long seed = System.currentTimeMillis();
    private int newObjectPerTableCount = 10;
    private int deleteObjectPerTableCount = 5;
    private int maxReferencesPerTable = 3;
    private int maxForeignKeysPerTable = 3;
    private int loopCount = 10;
    private int maxLoopsPerTable = 3;
    private File configFile;
    private boolean recordAll = true;
    private File outFile;
    private int schemaCount = 1;
    private int commitsPerSchema = 1;

    public Preferences(String[] args) throws Exception {
        if (args.length > 0 && read(args[0]))
            return;
        else if (read("./schema-test.config"))
            return;
        else
            throw new FileNotFoundException("preferences file not found.");
    }

    /**
     * Initializes Preferences from a properties object.
     */
    public Preferences(Properties props) throws Exception {
        if(!init(ExtendedProperties.convertProperties(props))) {
        	throw new Exception("Initialization failed.");
        }
    }

    protected boolean read(String confPath) {
        configFile = new File(confPath).getAbsoluteFile();

        try {
            return init(new ExtendedProperties(confPath));
        } catch (IOException ex) {
            return false;
        }
    }

    protected boolean initProjectFile(ExtendedProperties conf) {
        String s = conf.getString("cayenne.project", "./cayenne.xml");
        File f = new File(s);
        if (!f.canRead())
            return false;
        cayenneProject = f;

        return true;
    }

    protected boolean init(ExtendedProperties conf) {
        if (!initProjectFile(conf)) {
            return false;
        }

        schema = conf.getString("cayenne.schema");
        String s = conf.getString("test.workdir");
        if (s != null) {
            File f = new File(s);
            if (f.isDirectory())
                workDirectory = f;
        }
        schemaDirPrefix =
            conf.getString("test.schemadir_prefix", schemaDirPrefix);
        try {
            tableCount = conf.getInt("test.table_count", tableCount);
        } catch (Exception e) {
        }
        try {
            seed = conf.getLong("test.seed", seed);
        } catch (Exception e) {
        }
        try {
            newObjectPerTableCount =
                conf.getInt(
                    "test.new_objects_per_table",
                    newObjectPerTableCount);
        } catch (Exception e) {
        }
        try {
            deleteObjectPerTableCount =
                conf.getInt(
                    "test.delete_objects_per_table",
                    deleteObjectPerTableCount);
        } catch (Exception e) {
        }
        try {
            maxReferencesPerTable =
                conf.getInt(
                    "test.max_references_per_table",
                    maxReferencesPerTable);
        } catch (Exception e) {
        }
        try {
            maxForeignKeysPerTable =
                conf.getInt(
                    "test.max_foreign_keys_per_table",
                    maxForeignKeysPerTable);
        } catch (Exception e) {
        }
        try {
            loopCount = conf.getInt("test.loop_count", loopCount);
        } catch (Exception e) {
        }
        try {
            maxLoopsPerTable =
                conf.getInt("test.max_loops_per_table", maxLoopsPerTable);
        } catch (Exception e) {
        }
        try {
            schemaCount = conf.getInt("test.schema_count", schemaCount);
        } catch (Exception e) {
        }
        try {
            commitsPerSchema =
                conf.getInt("test.commits_per_schema", commitsPerSchema);
        } catch (Exception e) {
        }
        try {
            recordAll = conf.getBoolean("test.record_all", recordAll);
        } catch (Exception e) {
        }

        try {
            recordAll = conf.getBoolean("test.record_all", recordAll);
        } catch (Exception e) {
        }

        s = conf.getString("test.out");
        if (s != null)
            outFile = new File(workDirectory, s);

        return true;
    }

    public File getCayenneProject() {
        return cayenneProject;
    }

    public File getWorkDirectory() {
        return workDirectory;
    }

    public String getSchema() {
        return schema;
    }

    public String getSchemaDirPrefix() {
        return schemaDirPrefix;
    }

    public int getTableCount() {
        return tableCount;
    }

    public long getSeed() {
        return seed;
    }

    public int getNewObjectPerTableCount() {
        return newObjectPerTableCount;
    }

    public int getDeleteObjectPerTableCount() {
        return deleteObjectPerTableCount;
    }

    public int getMaxReferencesPerTable() {
        return maxReferencesPerTable;
    }

    public int getMaxForeignKeysPerTable() {
        return maxForeignKeysPerTable;
    }

    public int getLoopCount() {
        return loopCount;
    }

    public int getMaxLoopsPerTable() {
        return maxLoopsPerTable;
    }
    public java.io.File getConfigFile() {
        return configFile;
    }
    public boolean isRecordAll() {
        return recordAll;
    }
    public java.io.File getOutFile() {
        return outFile;
    }
    public int getSchemaCount() {
        return schemaCount;
    }
    public int getCommitsPerSchema() {
        return commitsPerSchema;
    }

    public void print(PrintWriter out) {
        out.println("config file: " + configFile);
        String path =
            (cayenneProject != null)
                ? cayenneProject.getAbsolutePath()
                : "<null>";
        out.println("cayenne project: " + path);
        out.println("database schema: " + schema);
        out.println("work dir: " + workDirectory);
        out.println("schema dir prefix: " + schemaDirPrefix);
        out.println("table count: " + tableCount);
        out.println("random seed: " + seed);
        out.println("new objects per table: " + newObjectPerTableCount);
        out.println("delete objects per table: " + deleteObjectPerTableCount);
        out.println("max references per table: " + maxReferencesPerTable);
        out.println("max foreign keys per table: " + maxForeignKeysPerTable);
        out.println("loop count: " + loopCount);
        out.println("max loops per table: " + maxLoopsPerTable);
        out.println("record all: " + recordAll);
        out.println("out file: " + outFile);
        out.println("count of schemas to generate: " + schemaCount);
        out.println(
            "count of commits to create per schema: " + commitsPerSchema);
    }
    
}
