/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.stunner.client.widgets.toolbar.command;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.gwtbootstrap3.client.ui.constants.IconType;
import org.jboss.errai.ioc.client.api.LoadAsync;
import org.kie.workbench.common.stunner.core.client.session.ClientFullSession;
import org.kie.workbench.common.stunner.core.client.session.command.impl.ClearSessionCommand;
import org.kie.workbench.common.stunner.core.client.session.command.impl.SessionCommandFactory;
import org.uberfire.async.UberfireActivityFragment;

// TODO: I18n.
@Dependent
@LoadAsync(UberfireActivityFragment.class)
public class ClearToolbarCommand extends AbstractToolbarCommand<ClientFullSession, ClearSessionCommand> {

    @Inject
    public ClearToolbarCommand(final SessionCommandFactory sessionCommandFactory) {
        super(sessionCommandFactory.newClearCommand());
    }

    @Override
    public IconType getIcon() {
        return IconType.ERASER;
    }

    @Override
    public String getCaption() {
        return null;
    }

    @Override
    public String getTooltip() {
        return "Clear diagram";
    }

    @Override
    protected boolean requiresConfirm() {
        return true;
    }

    /**
     * Added alert message - the operation cannot be reverted.
     * See <a>org.kie.workbench.common.stunner.core.client.session.command.impl.ClearSessionCommand</a>
     */
    @Override
    protected String getConfirmMessage() {
        return super.getConfirmMessage() + " This operation cannot be reverted.";
    }
}
