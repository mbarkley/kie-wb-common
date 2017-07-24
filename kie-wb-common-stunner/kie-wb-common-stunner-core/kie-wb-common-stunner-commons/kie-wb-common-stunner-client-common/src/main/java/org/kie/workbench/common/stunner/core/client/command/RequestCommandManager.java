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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.errai.ioc.client.api.LoadAsync;
import org.kie.workbench.common.stunner.core.client.api.AbstractClientSessionManager;
import org.kie.workbench.common.stunner.core.client.canvas.AbstractCanvasHandler;
import org.kie.workbench.common.stunner.core.client.canvas.event.mouse.CanvasMouseDownEvent;
import org.kie.workbench.common.stunner.core.client.canvas.event.mouse.CanvasMouseUpEvent;
import org.kie.workbench.common.stunner.core.client.session.event.SessionDestroyedEvent;
import org.kie.workbench.common.stunner.core.client.session.event.SessionOpenedEvent;
import org.kie.workbench.common.stunner.core.command.Command;
import org.kie.workbench.common.stunner.core.command.CommandListener;
import org.kie.workbench.common.stunner.core.command.CommandResult;
import org.kie.workbench.common.stunner.core.command.impl.CommandRegistryListener;
import org.kie.workbench.common.stunner.core.command.impl.CompositeCommandImpl;
import org.kie.workbench.common.stunner.core.command.util.CommandUtils;
import org.kie.workbench.common.stunner.core.registry.command.CommandRegistry;
import org.uberfire.async.UberfireActivityFragment;

import static org.uberfire.commons.validation.PortablePreconditions.checkNotNull;

/**
 * This is a concrete implementation for a SessionCommandManager, but instead
 * of adding each executed command in the session's registry, it adds only a single composite command, which is
 * composed by the commands executed on each client request.
 * This implementation considers a client request the time frame between mouse down and mouse up events are fired
 * on the canvas.
 * Using this implementation is useful for components that use commands that must be executed in an atomic operation, so
 * undo/redo will be done as a single execution as well.
 */
@ApplicationScoped
@Request
@LoadAsync(UberfireActivityFragment.class)
public class RequestCommandManager extends AbstractSessionCommandManager {

    private static Logger LOGGER = Logger.getLogger(RequestCommandManager.class.getName());

    private final AbstractClientSessionManager clientSessionManager;

    protected RequestCommandManager() {
        this(null);
    }

    @Inject
    public RequestCommandManager(final AbstractClientSessionManager clientSessionManager) {
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

    /**
     * The current command builder instance for each client request. It aggregates the commands executed during the request.
     */
    private CompositeCommandImpl.CompositeCommandBuilder<AbstractCanvasHandler, CanvasViolation> currentCommandBuilder;

    /**
     * The custom command registry listener implementation - instead of adding commands into session's registry
     * once successful executions, it aggregates the commands into the command builder instance to perform a single
     * execution once client request is completed.
     */
    private CommandListener<AbstractCanvasHandler, CanvasViolation> registryListener =
            new CommandRegistryListener<AbstractCanvasHandler, CanvasViolation>() {

                @Override
                public void onAllow(final AbstractCanvasHandler context,
                                    final Command<AbstractCanvasHandler, CanvasViolation> command,
                                    final CommandResult<CanvasViolation> result) {
                    // Nothing to do with the command registry for the allow operation.
                    // Notify listener, if any.
                    RequestCommandManager.this.postAllow(context,
                                                         command,
                                                         result);
                }

                @Override
                public void onExecute(final AbstractCanvasHandler context,
                                      final Command<AbstractCanvasHandler, CanvasViolation> command,
                                      final CommandResult<CanvasViolation> result) {
                    if (!CommandUtils.isError(result)) {
                        LOGGER.log(Level.FINEST,
                                   "Adding command [" + command + "] into current request command builder.");
                        currentCommandBuilder.addCommand(command);
                    }
                    // Notify listener, if any.
                    RequestCommandManager.this.postExecute(context,
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
                    RequestCommandManager.this.postUndo(context,
                                                        command,
                                                        result);
                }

                @Override
                protected CommandRegistry<Command<AbstractCanvasHandler, CanvasViolation>> getRegistry() {
                    return RequestCommandManager.this.getRegistry();
                }
            };

    /**
     * Listens to canvas mouse down event. It produces a new client request to start.
     */
    void onCanvasMouseDownEvent(final @Observes CanvasMouseDownEvent mouseDownEvent) {
        checkNotNull("mouseDownEvent",
                     mouseDownEvent);
        start();
    }

    /**
     * Listens to canvas up down event. It produces the current client request to complete.
     */
    void onCanvasMouseUpEvent(final @Observes CanvasMouseUpEvent mouseUpEvent) {
        checkNotNull("mouseUpEvent",
                     mouseUpEvent);
        complete();
    }

    /**
     * Checks that once opening a new client session, no pending requests are present.
     */
    void onCanvasSessionOpened(final @Observes SessionOpenedEvent sessionOpenedEvent) {
        checkNotNull("sessionOpenedEvent",
                     sessionOpenedEvent);
        if (isRequestStarted()) {
            LOGGER.log(Level.WARNING,
                       "New session opened but the request has not been completed. Unexpected behaviors can occur.");
            clear();
        }
    }

    /**
     * Checks that once disposing a client session, no pending requests are present.
     */
    void onCanvasSessionDestroyed(final @Observes SessionDestroyedEvent sessionDestroyedEvent) {
        checkNotNull("sessionDestroyedEvent",
                     sessionDestroyedEvent);
        if (isRequestStarted()) {
            LOGGER.log(Level.WARNING,
                       "Current client request has not been completed yet.");
        }
    }

    /**
     * Starts a new client request.
     */
    private void start() {
        if (isRequestStarted()) {

            LOGGER.log(Level.WARNING,
                       "Current client request has not been completed yet." +
                               "A new client request cannot be started!");
            clear();
        }
        LOGGER.log(Level.FINEST,
                   "New client request started.");
        currentCommandBuilder = new CompositeCommandImpl
                .CompositeCommandBuilder<AbstractCanvasHandler, CanvasViolation>()
                .forward();
    }

    /**
     * Completes the current client request. It registers the composite command into the
     * session's registry.
     */
    private void complete() {
        LOGGER.log(Level.FINEST,
                   "Checking if current client request has been completed...");
        if (isRequestStarted()) {
            // If any commands have been aggregated, let's execute those.
            if (currentCommandBuilder.size() > 0) {
                LOGGER.log(Level.FINEST,
                           "Adding commands for current request into registry [size=" + currentCommandBuilder.size() + "]");
                getRegistry().register(currentCommandBuilder.build());
            }
            clear();
            LOGGER.log(Level.FINEST,
                       "Current client request completed.");
        } else {
            LOGGER.log(Level.WARNING,
                       "Current client request has not been started.");
        }
    }

    private boolean isRequestStarted() {
        return null != currentCommandBuilder;
    }

    private void clear() {
        currentCommandBuilder = null;
    }
}
