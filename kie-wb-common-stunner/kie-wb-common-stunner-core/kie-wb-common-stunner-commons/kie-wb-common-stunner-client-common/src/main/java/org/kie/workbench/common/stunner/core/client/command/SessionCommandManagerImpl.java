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

package org.kie.workbench.common.stunner.core.client.command;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.errai.ioc.client.api.LoadAsync;
import org.kie.workbench.common.stunner.core.client.api.AbstractClientSessionManager;
import org.kie.workbench.common.stunner.core.client.canvas.AbstractCanvasHandler;
import org.kie.workbench.common.stunner.core.client.session.Session;
import org.kie.workbench.common.stunner.core.command.Command;
import org.kie.workbench.common.stunner.core.command.CommandListener;
import org.kie.workbench.common.stunner.core.command.CommandResult;
import org.kie.workbench.common.stunner.core.command.impl.CommandRegistryListener;
import org.kie.workbench.common.stunner.core.registry.command.CommandRegistry;
import org.uberfire.async.UberfireActivityFragment;

/**
 * The default session command manager implementation.
 * Commands that must be keep into the session for further undoing/redoing operations must be executed
 * using this implementation, as it keep the successful executed commands in the session's registry.
 */
@ApplicationScoped
@Session
@LoadAsync(UberfireActivityFragment.class)
public class SessionCommandManagerImpl
        extends AbstractSessionCommandManager {

    private final AbstractClientSessionManager clientSessionManager;

    protected SessionCommandManagerImpl() {
        this(null);
    }

    @Inject
    public SessionCommandManagerImpl(final AbstractClientSessionManager clientSessionManager) {
        this.clientSessionManager = clientSessionManager;
    }

    @Override
    protected AbstractClientSessionManager getClientSessionManager() {
        return clientSessionManager;
    }

    @Override
    protected CommandListener<AbstractCanvasHandler, CanvasViolation> getRegistryListener() {
        return registryListener;
    }

    private CommandRegistryListener<AbstractCanvasHandler, CanvasViolation> registryListener =
            new CommandRegistryListener<AbstractCanvasHandler, CanvasViolation>() {

                @Override
                public void onAllow(final AbstractCanvasHandler context,
                                    final Command<AbstractCanvasHandler, CanvasViolation> command,
                                    final CommandResult<CanvasViolation> result) {
                    // Nothing to do with the command registry for the allow operation.
                    // Notify listener, if any.
                    SessionCommandManagerImpl.this.postAllow(context,
                                                             command,
                                                             result);
                }

                @Override
                public void onExecute(final AbstractCanvasHandler context,
                                      final Command<AbstractCanvasHandler, CanvasViolation> command,
                                      final CommandResult<CanvasViolation> result) {
                    super.onExecute(context,
                                    command,
                                    result);
                    // Notify listener, if any.
                    SessionCommandManagerImpl.this.postExecute(context,
                                                               command,
                                                               result);
                }

                @Override
                public void onUndo(final AbstractCanvasHandler context,
                                   final Command<AbstractCanvasHandler, CanvasViolation> command,
                                   final CommandResult<CanvasViolation> result) {
                    super.onUndo(context,
                                 command,
                                 result);
                    // Notify listener, if any.
                    SessionCommandManagerImpl.this.postUndo(context,
                                                            command,
                                                            result);
                }

                @Override
                protected CommandRegistry<Command<AbstractCanvasHandler, CanvasViolation>> getRegistry() {
                    return SessionCommandManagerImpl.this.getRegistry();
                }
            };
}
