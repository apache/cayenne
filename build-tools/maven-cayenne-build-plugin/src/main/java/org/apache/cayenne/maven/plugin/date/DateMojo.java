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
package org.apache.cayenne.maven.plugin.date;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Exports the "project.build.date" and "project.build.time" properties to the
 * environment.
 * 
 * @goal date
 * @phase initialize
 * @requiresProject
 */
public class DateMojo extends AbstractMojo {

    static final String BUILD_DATE_PROPERTY = "project.build.date";
    static final String BUILD_TIME_PROPERTY = "project.build.time";
    static final String BUILD_YEAR_PROPERTY = "project.build.year";

    static final String BUILD_DATE_FORMAT = "MMM dd yyyy";
    static final String BUILD_TIME_FORMAT = "HH:mm:ss";

    /**
     * POM
     * 
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        Format dateFormat = new SimpleDateFormat(BUILD_DATE_FORMAT);
        Format timeFormat = new SimpleDateFormat(BUILD_TIME_FORMAT);

        // convert to GMT
        Calendar calendar = Calendar.getInstance();
        int offset = calendar.get(Calendar.ZONE_OFFSET);
        calendar.add(Calendar.MILLISECOND, -offset);
        Date gmtTime = calendar.getTime();

        project.getProperties().put(BUILD_DATE_PROPERTY, dateFormat.format(gmtTime));
        project.getProperties().put(BUILD_TIME_PROPERTY, timeFormat.format(gmtTime));
        project.getProperties().put(
                BUILD_YEAR_PROPERTY,
                String.valueOf(calendar.get(Calendar.YEAR)));
    }
}
