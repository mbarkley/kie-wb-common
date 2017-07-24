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
package org.kie.workbench.common.stunner.client.widgets.menu.dev.impl;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.ioc.client.api.LoadAsync;
import org.kie.workbench.common.stunner.client.widgets.menu.dev.AbstractMenuDevCommand;
import org.kie.workbench.common.stunner.core.client.api.AbstractClientSessionManager;
import org.kie.workbench.common.stunner.core.client.session.ClientFullSession;
import org.kie.workbench.common.stunner.core.client.util.StunnerClientLogger;
import org.uberfire.async.UberfireActivityFragment;

@Dependent
@LoadAsync(UberfireActivityFragment.class)
public class LogCommandHistoryDevCommand extends AbstractMenuDevCommand {

    private static Logger LOGGER = Logger.getLogger(LogCommandHistoryDevCommand.class.getName());

    protected LogCommandHistoryDevCommand() {
        this(null);
    }

    @Inject
    public LogCommandHistoryDevCommand(final AbstractClientSessionManager sessionManager) {
        super(sessionManager);
    }

    @Override
    public String getText() {
        return "Log Command History";
    }

    @Override
    public void execute() {
        try {
            StunnerClientLogger.logCommandHistory((ClientFullSession) getSession());
        } catch (ClassCastException e) {
            LOGGER.log(Level.WARNING,
                       "Session is not an instance of ClientFullSession");
        }
    }
}
