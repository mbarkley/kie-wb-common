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

package org.kie.workbench.common.stunner.cm.project.client.editor;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

import org.jboss.errai.ioc.client.api.LoadAsync;
import org.kie.workbench.common.stunner.bpmn.factory.BPMNGraphFactory;
import org.kie.workbench.common.stunner.client.widgets.presenters.session.SessionPresenterFactory;
import org.kie.workbench.common.stunner.cm.project.client.type.CaseManagementDiagramResourceType;
import org.kie.workbench.common.stunner.core.client.api.SessionManager;
import org.kie.workbench.common.stunner.core.client.session.command.impl.SessionCommandFactory;
import org.kie.workbench.common.stunner.core.client.session.impl.AbstractClientFullSession;
import org.kie.workbench.common.stunner.core.client.session.impl.AbstractClientReadOnlySession;
import org.kie.workbench.common.stunner.core.diagram.Diagram;
import org.kie.workbench.common.stunner.project.client.editor.AbstractProjectDiagramEditor;
import org.kie.workbench.common.stunner.project.client.editor.ProjectDiagramEditorMenuItemsBuilder;
import org.kie.workbench.common.stunner.project.client.editor.event.OnDiagramFocusEvent;
import org.kie.workbench.common.stunner.project.client.editor.event.OnDiagramLoseFocusEvent;
import org.kie.workbench.common.stunner.project.client.service.ClientProjectDiagramService;
import org.kie.workbench.common.stunner.project.diagram.ProjectDiagram;
import org.uberfire.async.UberfireActivityFragment;
import org.uberfire.backend.vfs.ObservablePath;
import org.uberfire.client.annotations.WorkbenchEditor;
import org.uberfire.client.annotations.WorkbenchMenu;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartTitleDecoration;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.workbench.events.ChangeTitleWidgetEvent;
import org.uberfire.client.workbench.widgets.common.ErrorPopupPresenter;
import org.uberfire.ext.editor.commons.client.file.popups.SavePopUpPresenter;
import org.uberfire.lifecycle.OnClose;
import org.uberfire.lifecycle.OnFocus;
import org.uberfire.lifecycle.OnLostFocus;
import org.uberfire.lifecycle.OnMayClose;
import org.uberfire.lifecycle.OnOpen;
import org.uberfire.lifecycle.OnStartup;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.workbench.model.menu.Menus;

@Dependent
@WorkbenchEditor(identifier = CaseManagementDiagramEditor.EDITOR_ID, supportedTypes = {CaseManagementDiagramResourceType.class})
@LoadAsync(UberfireActivityFragment.class)
public class CaseManagementDiagramEditor extends AbstractProjectDiagramEditor<CaseManagementDiagramResourceType> {

    public static final String EDITOR_ID = "CaseManagementDiagramEditor";

    @Inject
    public CaseManagementDiagramEditor(final AbstractProjectDiagramEditor.View view,
                                       final PlaceManager placeManager,
                                       final ErrorPopupPresenter errorPopupPresenter,
                                       final Event<ChangeTitleWidgetEvent> changeTitleNotificationEvent,
                                       final SavePopUpPresenter savePopUpPresenter,
                                       final CaseManagementDiagramResourceType resourceType,
                                       final ClientProjectDiagramService projectDiagramServices,
                                       final SessionManager sessionManager,
                                       final SessionPresenterFactory<Diagram, AbstractClientReadOnlySession, AbstractClientFullSession> sessionPresenterFactory,
                                       final SessionCommandFactory sessionCommandFactory,
                                       final ProjectDiagramEditorMenuItemsBuilder menuItemsBuilder,
                                       final Event<OnDiagramFocusEvent> onDiagramFocusEvent,
                                       final Event<OnDiagramLoseFocusEvent> onDiagramLostFocusEvent) {
        super(view,
              placeManager,
              errorPopupPresenter,
              changeTitleNotificationEvent,
              savePopUpPresenter,
              resourceType,
              projectDiagramServices,
              sessionManager,
              sessionPresenterFactory,
              sessionCommandFactory,
              menuItemsBuilder,
              onDiagramFocusEvent,
              onDiagramLostFocusEvent);
    }

    @OnStartup
    public void onStartup(final ObservablePath path,
                          final PlaceRequest place) {
        super.doStartUp(path,
                        place);
    }

    @Override
    protected int getCanvasWidth() {
        return (int) BPMNGraphFactory.GRAPH_DEFAULT_WIDTH;
    }

    @Override
    protected int getCanvasHeight() {
        return (int) BPMNGraphFactory.GRAPH_DEFAULT_HEIGHT;
    }

    @Override
    protected String getEditorIdentifier() {
        return CaseManagementDiagramEditor.EDITOR_ID;
    }

    @OnOpen
    public void onOpen() {
        super.doOpen();
    }

    @Override
    protected void open(final ProjectDiagram diagram) {
        super.open(diagram);
        presenter.displayNotifications(type -> false);
    }

    @OnClose
    public void onClose() {
        super.doClose();
    }

    @OnFocus
    public void onFocus() {
        super.doFocus();
    }

    @OnLostFocus
    public void onLostFocus() {
        super.doLostFocus();
    }

    @Override
    @WorkbenchPartTitleDecoration
    public IsWidget getTitle() {
        return super.getTitle();
    }

    @Override
    @WorkbenchPartTitle
    public String getTitleText() {
        return super.getTitleText();
    }

    @Override
    @WorkbenchMenu
    public Menus getMenus() {
        return super.getMenus();
    }

    @Override
    @WorkbenchPartView
    public Widget getWidget() {
        return getView().asWidget();
    }

    @OnMayClose
    public boolean onMayClose() {
        return super.mayClose(getCurrentDiagramHash());
    }
}
