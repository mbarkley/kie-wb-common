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
package org.kie.workbench.common.stunner.core.client.session.impl;

import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.errai.ioc.client.api.LoadAsync;
import org.jboss.errai.ioc.client.api.ManagedInstance;
import org.kie.workbench.common.stunner.core.client.session.ClientFullSession;
import org.kie.workbench.common.stunner.core.client.session.ClientSessionFactory;
import org.uberfire.async.UberfireActivityFragment;

/**
 * Stunner's default session factory for sessions of type <code>ClientFullSession</code>.
 */
@ApplicationScoped
@LoadAsync(UberfireActivityFragment.class)
public class ClientFullSessionFactory implements ClientSessionFactory<ClientFullSession> {

    private static Logger LOGGER = Logger.getLogger(ClientFullSessionFactory.class.getName());

    private final ManagedInstance<ClientFullSessionImpl> fullSessionInstances;

    protected ClientFullSessionFactory() {
        this(null);
    }

    @Inject
    public ClientFullSessionFactory(final ManagedInstance<ClientFullSessionImpl> fullSessionInstances) {
        this.fullSessionInstances = fullSessionInstances;
    }

    @Override
    public ClientFullSession newSession() {
        return this.fullSessionInstances.get();
    }

    @Override
    public Class<ClientFullSession> getSessionType() {
        return ClientFullSession.class;
    }
}
