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

package org.kie.workbench.common.stunner.core.client.event.keyboard;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.ioc.client.api.LoadAsync;
import org.kie.workbench.common.stunner.core.client.api.SessionManager;
import org.kie.workbench.common.stunner.core.client.session.ClientSession;
import org.uberfire.async.UberfireActivityFragment;

/**
 * A helper class for component that listen to keyboard events
 * but it only delegates to handlers if the current session is same
 * session bind to this component.
 */
@Dependent
@LoadAsync(UberfireActivityFragment.class)
public class SessionKeyShortcutsHandler {

    private final SessionManager clientSessionManager;
    private final ClientKeyShortcutsHandler keyShortcutsHandler;
    private ClientSession session;

    @Inject
    public SessionKeyShortcutsHandler(final SessionManager clientSessionManager,
                                      final ClientKeyShortcutsHandler keyShortcutsHandler) {
        this.clientSessionManager = clientSessionManager;
        this.keyShortcutsHandler = keyShortcutsHandler;
    }

    public SessionKeyShortcutsHandler setKeyShortcutCallback(final ClientKeyShortcutsHandler.KeyShortcutCallback shortcutCallback) {
        this.keyShortcutsHandler.setKeyShortcutCallback(new SessionKeyShortcutCallback(shortcutCallback));
        return this;
    }

    public SessionKeyShortcutsHandler bind(final ClientSession session) {
        this.session = session;
        return this;
    }

    public SessionKeyShortcutsHandler unbind() {
        this.session = null;
        return this;
    }

    class SessionKeyShortcutCallback implements ClientKeyShortcutsHandler.KeyShortcutCallback {

        private final ClientKeyShortcutsHandler.KeyShortcutCallback delegate;

        private SessionKeyShortcutCallback(final ClientKeyShortcutsHandler.KeyShortcutCallback delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onKeyShortcut(final KeyboardEvent.Key... keys) {
            if (isSameSession(session)) {
                delegate.onKeyShortcut(keys);
            }
        }

        public ClientKeyShortcutsHandler.KeyShortcutCallback getDelegate() {
            return delegate;
        }
    }

    private boolean isSameSession(final ClientSession session) {
        return null != session && session.equals(clientSessionManager.getCurrentSession());
    }
}
