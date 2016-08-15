/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.forms.model;

public class DefaultFieldTypeInfo implements FieldTypeInfo {

    private String type;

    private boolean isList = false;

    private boolean isEnum = false;

    public DefaultFieldTypeInfo( String type ) {
        this.type = type;
    }

    public DefaultFieldTypeInfo( String type, boolean isList, boolean isEnum ) {
        this.type = type;
        this.isList = isList;
        this.isEnum = isEnum;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType( String type ) {
        this.type = type;
    }

    @Override
    public boolean isList() {
        return isList;
    }

    public void setList( boolean list ) {
        isList = list;
    }

    @Override
    public boolean isEnum() {
        return isEnum;
    }

    public void setEnum( boolean anEnum ) {
        isEnum = anEnum;
    }
}
