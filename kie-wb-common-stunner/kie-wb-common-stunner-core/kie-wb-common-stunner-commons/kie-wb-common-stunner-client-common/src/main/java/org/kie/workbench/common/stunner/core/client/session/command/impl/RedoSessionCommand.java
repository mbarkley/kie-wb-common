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

package org.kie.workbench.common.stunner.core.client.session.command.impl;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.errai.ioc.client.api.LoadAsync;
import org.kie.workbench.common.stunner.core.client.canvas.AbstractCanvasHandler;
import org.kie.workbench.common.stunner.core.client.canvas.event.command.CanvasCommandExecutedEvent;
import org.kie.workbench.common.stunner.core.client.canvas.event.command.CanvasUndoCommandExecutedEvent;
import org.kie.workbench.common.stunner.core.client.command.CanvasViolation;
import org.kie.workbench.common.stunner.core.client.command.SessionCommandManager;
import org.kie.workbench.common.stunner.core.client.event.keyboard.ClientKeyShortcutsHandler;
import org.kie.workbench.common.stunner.core.client.event.keyboard.KeyboardEvent;
import org.kie.workbench.common.stunner.core.client.event.keyboard.SessionKeyShortcutsHandler;
import org.kie.workbench.common.stunner.core.client.session.ClientFullSession;
import org.kie.workbench.common.stunner.core.client.session.Session;
import org.kie.workbench.common.stunner.core.client.session.command.AbstractClientSessionCommand;
import org.kie.workbench.common.stunner.core.command.Command;
import org.kie.workbench.common.stunner.core.command.CommandResult;
import org.kie.workbench.common.stunner.core.command.util.RedoCommandHandler;
import org.uberfire.async.UberfireActivityFragment;

import static org.uberfire.commons.validation.PortablePreconditions.checkNotNull;

@Dependent
@LoadAsync(UberfireActivityFragment.class)
public class RedoSessionCommand extends AbstractClientSessionCommand<ClientFullSession> {

    private final SessionCommandManager<AbstractCanvasHandler> sessionCommandManager;
    private final RedoCommandHandler<Command<AbstractCanvasHandler, CanvasViolation>> redoCommandHandler;
    private final SessionKeyShortcutsHandler keyboardListener;

    protected RedoSessionCommand() {
        this(null,
             null,
             null);
    }

    @Inject
    public RedoSessionCommand(final @Session SessionCommandManager<AbstractCanvasHandler> sessionCommandManager,
                              final RedoCommandHandler<Command<AbstractCanvasHandler, CanvasViolation>> redoCommandHandler,
                              final SessionKeyShortcutsHandler keyboardListener) {
        super(false);
        this.redoCommandHandler = redoCommandHandler;
        this.sessionCommandManager = sessionCommandManager;
        this.keyboardListener = keyboardListener;
    }

    @PostConstruct
    public void init() {
        this.keyboardListener.setKeyShortcutCallback(keys -> {
            if (isRedoShortcut(keys)) {
                RedoSessionCommand.this.execute();
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> void execute(final Callback<V> callback) {
        checkNotNull("callback",
                     callback);
        CommandResult<?> result = null;
        if (redoCommandHandler.isEnabled()) {
            result = redoCommandHandler.execute(getSession().getCanvasHandler(),
                                                sessionCommandManager);
            checkState();
        }
        callback.onSuccess();
    }

    @Override
    public AbstractClientSessionCommand<ClientFullSession> bind(final ClientFullSession session) {
        super.bind(session);
        keyboardListener.bind(session);
        return this;
    }

    @Override
    public void unbind() {
        super.unbind();
        redoCommandHandler.clear();
        keyboardListener.unbind();
    }

    @SuppressWarnings("unchecked")
    void onCommandExecuted(final @Observes CanvasCommandExecutedEvent commandExecutedEvent) {
        checkNotNull("commandExecutedEvent",
                     commandExecutedEvent);
        if (null != commandExecutedEvent.getCommand()) {
            redoCommandHandler.onCommandExecuted(commandExecutedEvent.getCommand());
        }
        checkState();
    }

    @SuppressWarnings("unchecked")
    void onCommandUndoExecuted(final @Observes CanvasUndoCommandExecutedEvent commandUndoExecutedEvent) {
        checkNotNull("commandUndoExecutedEvent",
                     commandUndoExecutedEvent);
        if (null != commandUndoExecutedEvent.getCommand()) {
            redoCommandHandler.onUndoCommandExecuted(commandUndoExecutedEvent.getCommand());
        }
        checkState();
    }

    private void checkState() {
        setEnabled(null != getSession() && redoCommandHandler.isEnabled());
        fire();
    }

    private boolean isRedoShortcut(final KeyboardEvent.Key... keys) {
        return ClientKeyShortcutsHandler.isSameShortcut(keys,
                                                        KeyboardEvent.Key.CONTROL,
                                                        KeyboardEvent.Key.SHIFT,
                                                        KeyboardEvent.Key.Z);
    }
}
