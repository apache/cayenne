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

package org.apache.cayenne.access;

import org.apache.cayenne.ResultIterator;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

 class ResultIteratorConverterDecorator implements ResultIterator {
    private final ResultIterator iterator;
    private final DataDomainQueryAction.ObjectConversionStrategy converter;

     ResultIteratorConverterDecorator(ResultIterator iterator, DataDomainQueryAction.ObjectConversionStrategy converter) {
        this.iterator = Objects.requireNonNull(iterator);
        this.converter = Objects.requireNonNull(converter);
    }

    @Override
    public List allRows() {
        return iterator.allRows();
    }

    @Override
    public boolean hasNextRow() {
        return iterator.hasNextRow();
    }

    @Override
    public Object nextRow() {
        return converter.convert(iterator.nextRow());
    }

    @Override
    public void skipRow() {
        iterator.skipRow();
    }

    @Override
    public void close() {
        iterator.close();
    }

    @Override
    public Iterator iterator() {
        return new Iterator() {
            @Override
            public boolean hasNext() {
                return iterator.hasNextRow();
            }

            @Override
            public Object next() {
                return converter.convert(iterator.nextRow());
            }
        };
    }
 }
