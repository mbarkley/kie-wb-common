/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.screens.library.client.screens.importrepository;

import java.util.Set;
import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.common.client.api.Caller;
import org.kie.workbench.common.screens.examples.model.ExampleProject;
import org.kie.workbench.common.screens.library.api.LibraryService;
import org.kie.workbench.common.screens.library.client.screens.samples.ImportProjectsSetupEvent;
import org.kie.workbench.common.screens.library.client.util.LibraryPlaces;
import org.uberfire.client.mvp.UberElement;
import org.uberfire.ext.widgets.common.client.callbacks.DefaultErrorCallback;
import org.uberfire.ext.widgets.common.client.common.HasBusyIndicator;

public class ImportRepositoryPopUpPresenter {

    public interface View extends UberElement<ImportRepositoryPopUpPresenter>,
                                  HasBusyIndicator {

        String getRepositoryURL();

        String getUserName();

        String getPassword();

        void show();

        void hide();

        void showError(final String errorMessage);

        String getLoadingMessage();

        String getNoProjectsToImportMessage();

        String getEmptyRepositoryURLValidationMessage();
    }

    private View view;

    private LibraryPlaces libraryPlaces;

    private Caller<LibraryService> libraryService;

    private Event<ImportProjectsSetupEvent> importProjectsSetupEvent;

    @Inject
    public ImportRepositoryPopUpPresenter(final View view,
                                          final LibraryPlaces libraryPlaces,
                                          final Caller<LibraryService> libraryService,
                                          final Event<ImportProjectsSetupEvent> importProjectsSetupEvent) {
        this.view = view;
        this.libraryPlaces = libraryPlaces;
        this.libraryService = libraryService;
        this.importProjectsSetupEvent = importProjectsSetupEvent;
    }

    @PostConstruct
    public void setup() {
        view.init(this);
    }

    public void show() {
        view.show();
    }

    public void importRepository() {
        final String repositoryUrl = view.getRepositoryURL();
        if (isEmpty(repositoryUrl)) {
            view.showError(view.getEmptyRepositoryURLValidationMessage());
            return;
        }

        view.showBusyIndicator(view.getLoadingMessage());
        libraryService.call((Set<ExampleProject> projects) -> {
                                view.hideBusyIndicator();
                                view.hide();
                                libraryPlaces.goToImportProjects(null);
                                importProjectsSetupEvent.fire(new ImportProjectsSetupEvent(projects));
                            },
                            new DefaultErrorCallback() {
                                @Override
                                public boolean error(final Message message,
                                                     final Throwable throwable) {
                                    view.hideBusyIndicator();
                                    view.showError(view.getNoProjectsToImportMessage());
                                    return false;
                                }
                            }).getProjects(repositoryUrl,
                                           view.getUserName(),
                                           view.getPassword());
    }

    public void cancel() {
        view.hide();
    }

    public View getView() {
        return view;
    }

    private boolean isEmpty(final String text) {
        return text == null || text.trim().isEmpty();
    }
}
