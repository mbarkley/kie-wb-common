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
package org.kie.workbench.common.screens.library.client.screens;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.gwt.user.client.Event;
import org.jboss.errai.common.client.dom.Anchor;
import org.jboss.errai.common.client.dom.Div;
import org.jboss.errai.common.client.dom.Heading;
import org.jboss.errai.ioc.client.api.LoadAsync;
import org.jboss.errai.ioc.client.api.ManagedInstance;
import org.jboss.errai.ui.client.local.api.IsElement;
import org.jboss.errai.ui.client.local.spi.TranslationService;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.EventHandler;
import org.jboss.errai.ui.shared.api.annotations.SinkNative;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.kie.workbench.common.screens.examples.model.ExampleProject;
import org.kie.workbench.common.screens.library.client.resources.i18n.LibraryConstants;
import org.kie.workbench.common.screens.library.client.widgets.library.ImportExampleButtonWidget;
import org.kie.workbench.common.screens.library.client.widgets.library.ImportProjectButtonWidget;
import org.kie.workbench.common.screens.library.client.widgets.library.NewProjectButtonWidget;
import org.uberfire.async.UberfireActivityFragment;

@Templated
@LoadAsync(UberfireActivityFragment.class)
public class EmptyLibraryView implements EmptyLibraryScreen.View,
                                         IsElement {

    private EmptyLibraryScreen presenter;

    @Inject
    private TranslationService ts;

    @Inject
    private ManagedInstance<ImportExampleButtonWidget> importExampleButtonWidgets;

    @Inject
    private NewProjectButtonWidget newProjectButtonWidget;

    @Inject
    private ImportProjectButtonWidget importProjectButtonWidget;

    @Inject
    @Named("h1")
    @DataField
    private Heading welcome;

    @Inject
    @DataField("import-examples-container")
    private Div importExamplesContainer;

    @Inject
    @DataField("import-examples-buttons-container")
    private Div importExamplesButtonsContainer;

    @Inject
    @DataField("new-project-link")
    Anchor newProjectLink;

    @Inject
    @DataField("create-project-container")
    Div createProjectContainer;

    @Inject
    @DataField("projects-container")
    private Div projectsContainer;

    @Override
    public void init(EmptyLibraryScreen presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setup(String username) {
        welcome.setInnerHTML(ts.getTranslation(LibraryConstants.EmptyLibraryView_Welcome) + " " + username + ".");
        if (presenter.userCanCreateProjects()) {
            createProjectContainer.appendChild(newProjectButtonWidget.getView().getElement());
            createProjectContainer.appendChild(importProjectButtonWidget.getView().getElement());
        } else {
            projectsContainer.setTextContent("");
        }
    }

    @Override
    public void addProjectToImport(final ExampleProject exampleProject) {
        final ImportExampleButtonWidget importExampleButton = importExampleButtonWidgets.get();
        importExampleButton.init(exampleProject.getName(),
                                 exampleProject.getDescription(),
                                 () -> presenter.importProject(exampleProject));

        importExamplesButtonsContainer.appendChild(importExampleButton.getElement());
    }

    @Override
    public void clearImportExamplesButtonsContainer() {
        importExamplesButtonsContainer.setInnerHTML("");
    }

    @Override
    public void clearImportExamplesContainer() {
        importExamplesContainer.setInnerHTML("");
    }

    @SinkNative(Event.ONCLICK)
    @EventHandler("new-project-link")
    public void newProjectLink(Event e) {
        presenter.newProject();
    }
}