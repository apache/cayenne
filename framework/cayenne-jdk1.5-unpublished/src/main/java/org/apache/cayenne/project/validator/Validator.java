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


package org.apache.cayenne.project.validator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.conf.ConfigStatus;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectPath;

/** 
 * Validator is used to validate Cayenne projects.
 * 
 */
public class Validator {
    protected Project project;
    protected List<ValidationInfo> validationResults = new ArrayList<ValidationInfo>();
    protected int maxSeverity;

    /**
     * Creates a new validator initialized with the project.
     * 
     * @param project
     */
    public Validator(Project project) {
        this.project = project;
    }

    /**
     * Initializes validator with the project loading config status.
     * 
     * @param project
     * @param status
     */
    public Validator(Project project, ConfigStatus status) {
        this(project);

        if (status.hasFailures()) {
            ProjectPath path = new ProjectPath(project);

            for (final String message : status.getOtherFailures()) {
                registerError(message, path);
            }

            for (final String message : status.getFailedMaps().keySet()) {
                registerError("Map failed to load: " + message, path);
            }

            for (final String message : status.getFailedAdapters().keySet()) {
                registerError("Adapter failed to load: " + message, path);
            }

            for (final String message : status.getFailedDataSources().keySet()) {
                registerError("DataSource failed to load: " + message, path);
            }

            for (final String message : status.getFailedMapRefs()) {
                registerError("Map reference failed to load: " + message, path);
            }
        }
    }

    /**
     * Returns the project.
     * @return Project
     */
    public Project getProject() {
        return project;
    }

    /** 
     * Resets internal state. 
     * Called internally before starting validation.
     */
    protected void reset() {
        if (validationResults != null) {
            validationResults = new ArrayList<ValidationInfo>();
        }
        maxSeverity = ValidationInfo.VALID;
    }

    /** 
     * Returns maximum severity level encountered during 
     * the last validation run. 
     */
    public int getMaxSeverity() {
        return maxSeverity;
    }

    /**
     * Registers validation result. 
     * Increases internally stored max severity if 
     * <code>result</code> parameter has a higher severity then the current value. 
     * Leaves current value unchanged otherwise.
     */
    public void registerValidated(
        int severity,
        String message,
        ProjectPath treeNodePath) {
        ValidationInfo result = new ValidationInfo(severity, message, treeNodePath);
        validationResults.add(result);
        if (maxSeverity < severity) {
            maxSeverity = severity;
        }
    }

    public void registerError(String message, ProjectPath treeNodePath) {
        registerValidated(ValidationInfo.ERROR, message, treeNodePath);
    }

    public void registerWarning(String message, ProjectPath treeNodePath) {
        registerValidated(ValidationInfo.WARNING, message, treeNodePath);
    }

    /** Return collection of ValidationInfo objects from last validation. */
    public List<ValidationInfo> validationResults() {
        return validationResults;
    }

    /** 
     * Validates all project elements.
     * 
     * @return ValidationInfo.VALID if no errors were found, 
     * or an error code of the error with the highest severity 
     * if there were errors.
     */
    public synchronized int validate() {
        return validate(project.treeNodes());
    }

	/** 
	 * Validates a set of tree nodes passed as an iterator.
	 * 
	 * @return ValidationInfo.VALID if no errors were found, 
	 * or an error code of the error with the highest severity 
	 * if there were errors.
	 */
    public synchronized int validate(Iterator treeNodes) {
        reset();

        while (treeNodes.hasNext()) {
            TreeNodeValidator.validate((ProjectPath) treeNodes.next(), this);
        }

        return getMaxSeverity();
    }
}
