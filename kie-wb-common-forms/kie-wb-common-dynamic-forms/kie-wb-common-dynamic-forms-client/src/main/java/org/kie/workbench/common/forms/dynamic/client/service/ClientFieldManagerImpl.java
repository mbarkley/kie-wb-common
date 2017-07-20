/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.workbench.common.forms.dynamic.client.service;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.jboss.errai.ioc.client.api.LoadAsync;
import org.jboss.errai.ioc.client.api.ManagedInstance;
import org.kie.workbench.common.forms.fields.shared.AbstractFieldManager;
import org.kie.workbench.common.forms.fields.shared.FieldProvider;
import org.uberfire.async.UberfireActivityFragment;

@ApplicationScoped
@LoadAsync(UberfireActivityFragment.class)
public class ClientFieldManagerImpl extends AbstractFieldManager {

    @Inject
    @Any
    private ManagedInstance<FieldProvider<?>> providers;

    @PostConstruct
    protected void init() {
        for (FieldProvider<?> provider : providers) {
            registerFieldProvider(provider);
        }
    }
}
