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
package org.apache.cayenne.project.validation;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.validation.SimpleValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A base superclass of various node validators.
 * 
 * @since 3.1
 */
public abstract class ConfigurationNodeValidator<T extends ConfigurationNode> {

    protected final Supplier<ValidationConfig> configSupplier;

    /**
     * @param configSupplier the config defining the behavior of this validator.
     * @since 5.0
     */
    public ConfigurationNodeValidator(Supplier<ValidationConfig> configSupplier) {
        this.configSupplier = configSupplier;
    }

    /**
     * @param node the node that needs to be validated.
     * @param validationResult the appendable validation result.
     * @since 5.0
     */
    public abstract void validate(T node, ValidationResult validationResult);

    public void addFailure(ValidationResult validationResult, T source, String messageFormat,
                           Object... messageParameters) {
        String message = String.format(messageFormat, messageParameters);
        validationResult.addFailure(new SimpleValidationFailure(source, message));
    }
    
    public void addFailure(ValidationResult validationResult, SimpleValidationFailure failure) {
    	validationResult.addFailure(failure);
    }

    protected Performer<T> on(T node, ValidationResult validationResult) {
        return new Performer<>(node, validationResult);
    }

    protected class Performer<N> {

        private final N node;
        private final ValidationResult validationResult;

        protected Performer(N node, ValidationResult validationResult) {
            this.node = node;
            this.validationResult = validationResult;
        }

        protected Performer<N> performIfEnabled(Inspection inspection, ValidationAction<N> validationAction) {
            if (configSupplier.get().isEnabled(inspection)) {
                validationAction.perform(node, validationResult);
            }
            return this;
        }

        protected Performer<N> performIfEnabled(Inspection inspection, Runnable validationAction) {
            if (configSupplier.get().isEnabled(inspection)) {
                validationAction.run();
            }
            return this;
        }

        protected Performer<N> performIf(Predicate<N> predicate, ValidationAction<N> validationAction) {
            if (predicate.test(node)) {
                validationAction.perform(node, validationResult);
            }
            return this;
        }

        protected Performer<N> performIf(Predicate<N> predicate, Runnable validationAction) {
            if (predicate.test(node)) {
                validationAction.run();
            }
            return this;
        }
    }

    protected interface ValidationAction<N> {

        void perform(N node, ValidationResult validationResult);
    }
}
