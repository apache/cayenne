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

public class ResultIteratorConverterDecorator implements ResultIterator {
    private final ResultIterator iterator;
    private final DataDomainQueryAction.ObjectConversionStrategy converter;

    public ResultIteratorConverterDecorator(ResultIterator iterator, DataDomainQueryAction.ObjectConversionStrategy converter) {
        this.iterator = iterator;
        this.converter = converter;
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
        if (converter != null) {
            return converter.convert(iterator.nextRow());
        }
        return iterator.nextRow();
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
        return new IteratorConverterDecorator(iterator,converter);
    }

}

class IteratorConverterDecorator implements Iterator{
    private final ResultIterator iterator;
    private final DataDomainQueryAction.ObjectConversionStrategy converter;

    public IteratorConverterDecorator(ResultIterator iterator, DataDomainQueryAction.ObjectConversionStrategy converter) {
        this.iterator = iterator;
        this.converter = converter;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNextRow();
    }

    @Override
    public Object next() {
        if (converter != null) {
            return converter.convert(iterator.nextRow());
        }
        return iterator.nextRow();
    }


}
