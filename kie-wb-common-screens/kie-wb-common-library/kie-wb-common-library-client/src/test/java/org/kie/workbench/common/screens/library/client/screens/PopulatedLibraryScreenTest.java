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

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.event.Event;

import org.guvnor.common.services.project.client.security.ProjectController;
import org.guvnor.common.services.project.context.WorkspaceProjectContext;
import org.guvnor.common.services.project.model.Module;
import org.guvnor.common.services.project.model.WorkspaceProject;
import org.guvnor.structure.organizationalunit.OrganizationalUnit;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.ioc.client.api.ManagedInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.screens.library.api.LibraryInfo;
import org.kie.workbench.common.screens.library.api.LibraryService;
import org.kie.workbench.common.screens.library.client.events.ProjectDetailEvent;
import org.kie.workbench.common.screens.library.client.screens.project.AddProjectPopUpPresenter;
import org.kie.workbench.common.screens.library.client.util.LibraryPlaces;
import org.kie.workbench.common.screens.library.client.widgets.common.TileWidget;
import org.kie.workbench.common.screens.library.client.widgets.library.AddProjectButtonPresenter;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.uberfire.mocks.CallerMock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PopulatedLibraryScreenTest {

    @Mock
    private PopulatedLibraryScreen.View view;

    @Mock
    private LibraryPlaces libraryPlaces;

    @Mock
    private Event<ProjectDetailEvent> projectDetailEvent;

    @Mock
    private LibraryService libraryService;
    private Caller<LibraryService> libraryServiceCaller;

    @Mock
    private ProjectController projectController;

    @Mock
    private ManagedInstance<TileWidget> tileWidgets;

    @Mock
    private AddProjectButtonPresenter addProjectButtonPresenter;

    @Mock
    private TileWidget tileWidget;

    @Mock
    private AddProjectPopUpPresenter addProjectPopUpPresenter;

    @Mock
    private OrganizationalUnit organizationalUnit;

    @Mock
    private WorkspaceProjectContext projectContext;

    private PopulatedLibraryScreen libraryScreen;

    private WorkspaceProject project1;
    private WorkspaceProject project2;
    private WorkspaceProject project3;

    @Before
    public void setup() {
        libraryServiceCaller = new CallerMock<>(libraryService);

        libraryScreen = spy(new PopulatedLibraryScreen(view,
                                                       libraryPlaces,
                                                       projectDetailEvent,
                                                       libraryServiceCaller,
                                                       projectController,
                                                       projectContext,
                                                       tileWidgets,
                                                       addProjectButtonPresenter));

        doReturn(true).when(projectController).canCreateProjects();

        project1 = mockProject("project1Name");
        project2 = mockProject("project2Name");
        project3 = mockProject("project3Name");

        final List<WorkspaceProject> projects = new ArrayList<>();
        projects.add(project1);
        projects.add(project2);
        projects.add(project3);

        doReturn(organizationalUnit).when(projectContext).getActiveOrganizationalUnit();

        final LibraryInfo libraryInfo = new LibraryInfo(projects);
        doReturn(libraryInfo).when(libraryService).getLibraryInfo(organizationalUnit);

        doReturn(mock(TileWidget.View.class)).when(tileWidget).getView();
        doReturn(tileWidget).when(tileWidgets).get();
        doReturn(mock(AddProjectButtonPresenter.View.class)).when(addProjectButtonPresenter).getView();

        libraryScreen.setup();
    }

    private WorkspaceProject mockProject(final String projectName) {
        final WorkspaceProject result = mock(WorkspaceProject.class);
        doReturn(projectName).when(result).getName();
        doReturn(mock(Module.class)).when(result).getMainModule();
        return result;
    }

    @Test
    public void setupTest() {
        verify(addProjectButtonPresenter).getView();
        verify(view).addAction(any());

        verify(view).clearFilterText();
        verify(view).clearProjects();

        verify(tileWidget,
               times(3)).init(any(),
                              any(),
                              any(),
                              any(),
                              any());
        verify(tileWidget).init(eq("project1Name"),
                                any(),
                                any(),
                                any(),
                                any());
        verify(tileWidget).init(eq("project2Name"),
                                any(),
                                any(),
                                any(),
                                any());
        verify(tileWidget).init(eq("project3Name"),
                                any(),
                                any(),
                                any(),
                                any());
        verify(view,
               times(3)).addProject(any());
    }

    @Test
    public void selectCommandTest() {
        libraryScreen.selectCommand(project1).execute();

        verify(libraryPlaces).goToProject(any(WorkspaceProject.class));
    }

    @Test
    public void filterProjectsTest() {
        assertEquals(3,
                     libraryScreen.projects.size());
        assertEquals(1,
                     libraryScreen.filterProjects("project1").size());
        assertEquals(1,
                     libraryScreen.filterProjects("roject1").size());
        assertEquals(0,
                     libraryScreen.filterProjects("unexistent").size());
    }
}