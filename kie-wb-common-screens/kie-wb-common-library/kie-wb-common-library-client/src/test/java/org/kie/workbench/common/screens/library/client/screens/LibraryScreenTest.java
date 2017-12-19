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
package org.kie.workbench.common.screens.library.client.screens;

import java.util.ArrayList;
import java.util.List;

import org.guvnor.common.services.project.client.security.ProjectController;
import org.guvnor.common.services.project.context.ProjectContext;
import org.guvnor.structure.client.security.OrganizationalUnitController;
import org.guvnor.structure.events.AfterEditOrganizationalUnitEvent;
import org.guvnor.structure.organizationalunit.OrganizationalUnit;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.dom.HTMLElement;
import org.jboss.errai.ioc.client.api.ManagedInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.screens.library.api.LibraryService;
import org.kie.workbench.common.screens.library.client.screens.importrepository.ImportRepositoryPopUpPresenter;
import org.kie.workbench.common.screens.library.client.screens.organizationalunit.contributors.edit.EditContributorsPopUpPresenter;
import org.kie.workbench.common.screens.library.client.screens.organizationalunit.contributors.tab.ContributorsListPresenter;
import org.kie.workbench.common.screens.library.client.screens.organizationalunit.delete.DeleteOrganizationalUnitPopUpPresenter;
import org.kie.workbench.common.screens.library.client.util.LibraryPlaces;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.uberfire.mocks.CallerMock;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LibraryScreenTest {

    @Mock
    private LibraryScreen.View view;

    @Mock
    private ManagedInstance<EditContributorsPopUpPresenter> editContributorsPopUpPresenters;

    @Mock
    private ManagedInstance<DeleteOrganizationalUnitPopUpPresenter> deleteOrganizationalUnitPopUpPresenters;

    @Mock
    private ManagedInstance<ImportRepositoryPopUpPresenter> importRepositoryPopUpPresenters;

    @Mock
    private ProjectContext projectContext;

    @Mock
    private OrganizationalUnitController organizationalUnitController;

    @Mock
    private ProjectController projectController;

    @Mock
    private EmptyLibraryScreen emptyLibraryScreen;

    @Mock
    private PopulatedLibraryScreen populatedLibraryScreen;

    @Mock
    private OrgUnitsMetricsScreen orgUnitsMetricsScreen;

    @Mock
    private ContributorsListPresenter contributorsListPresenter;

    @Mock
    private LibraryService libraryService;

    @Mock
    private LibraryPlaces libraryPlaces;

    @Mock
    private EditContributorsPopUpPresenter editContributorsPopUpPresenter;

    @Mock
    private DeleteOrganizationalUnitPopUpPresenter deleteOrganizationalUnitPopUpPresenter;

    @Mock
    private ImportRepositoryPopUpPresenter importRepositoryPopUpPresenter;

    @Mock
    private ProjectContext projectContext;

    private LibraryScreen libraryScreen;

    @Before
    public void setup() {

        doReturn(importRepositoryPopUpPresenter).when(importRepositoryPopUpPresenters).get();
        doReturn(editContributorsPopUpPresenter).when(editContributorsPopUpPresenters).get();
        doReturn(deleteOrganizationalUnitPopUpPresenter).when(deleteOrganizationalUnitPopUpPresenters).get();

        doReturn(true).when(projectController).canCreateProjects();
        doReturn(true).when(organizationalUnitController).canUpdateOrgUnit(any());
        doReturn(true).when(organizationalUnitController).canDeleteOrgUnit(any());

        doReturn(mock(PopulatedLibraryScreen.View.class)).when(populatedLibraryScreen).getView();
        doReturn(mock(EmptyLibraryScreen.View.class)).when(emptyLibraryScreen).getView();

        libraryScreen = new LibraryScreen(view,
                                          deleteOrganizationalUnitPopUpPresenters,
                                          editContributorsPopUpPresenters,
                                          importRepositoryPopUpPresenters,
                                          projectContext,
                                          organizationalUnitController,
                                          projectController,
                                          emptyLibraryScreen,
                                          populatedLibraryScreen,
                                          orgUnitsMetricsScreen,
                                          contributorsListPresenter,
                                          libraryServiceCaller,
                                          libraryPlaces);
    }

    @Test
    public void setupTest() {
        final OrganizationalUnit organizationalUnit = mock(OrganizationalUnit.class);
        doReturn("name").when(organizationalUnit).getName();
        doReturn(organizationalUnit).when(libraryPlaces).getSelectedOrganizationalUnit();
        doReturn(12).when(contributorsListPresenter).getContributorsCount();

        libraryScreen.init();

        verify(view).init(libraryScreen);
        verify(view).setTitle("name");
        verify(view).setContributorsCount(12);
    }

    private void refresh() {
        final PlaceRequest placeRequest = mock(PlaceRequest.class);
        doReturn(LibraryPlaces.LIBRARY_SCREEN).when(placeRequest).getIdentifier();
        libraryScreen.refreshOnFocus(new PlaceGainFocusEvent(placeRequest));
    }

    @Test
    public void trySamplesWithPermissionTest() {
        libraryScreen.trySamples();

        verify(libraryPlaces).goToTrySamples();
    }

    @Test
    public void trySamplesWithoutPermissionTest() {
        doReturn(false).when(projectController).canCreateProjects();

        libraryScreen.trySamples();

        verify(libraryPlaces,
               never()).goToTrySamples();
    }

    @Test
    public void importProjectWithPermissionTest() {
        libraryScreen.importProject();

        verify(importRepositoryPopUpPresenter).show();
    }

    @Test
    public void importProjectWithoutPermissionTest() {
        doReturn(false).when(projectController).canCreateProjects();

        libraryScreen.importProject();

        verify(importRepositoryPopUpPresenter,
               never()).show();
    }

    @Test
    public void editContributorsWithPermissionTest() {
        libraryScreen.editContributors();

        verify(editContributorsPopUpPresenter).show(any());
    }

    @Test
    public void editContributorsWithoutPermissionTest() {
        doReturn(false).when(organizationalUnitController).canUpdateOrgUnit(any());

        libraryScreen.editContributors();

        verify(editContributorsPopUpPresenter,
               never()).show(any());
    }

    @Test
    public void deleteWithPermissionTest() {
        libraryScreen.delete();

        verify(deleteOrganizationalUnitPopUpPresenter).show(any());
    }

    @Test
    public void deleteWithoutPermissionTest() {
        doReturn(false).when(organizationalUnitController).canDeleteOrgUnit(any());

        libraryScreen.delete();

        verify(deleteOrganizationalUnitPopUpPresenter,
               never()).show(any());
    }

    @Test
    public void showProjectsTest() {
        doReturn(true).when(libraryService).hasProjects(any(),
                                                        any());
        final HTMLElement populatedLibraryScreenElement = mock(HTMLElement.class);
        when(populatedLibraryScreen.getView().getElement()).thenReturn(populatedLibraryScreenElement);
        doReturn(3).when(populatedLibraryScreen).getProjectsCount();

        libraryScreen.showProjects();

        verify(view).updateContent(populatedLibraryScreenElement);
        verify(view).setProjectsCount(3);
    }

    @Test
    public void showNoProjectsTest() {
        doReturn(false).when(libraryService).hasProjects(any(),
                                                         any());
        final HTMLElement emptyLibraryScreenElement = mock(HTMLElement.class);
        when(emptyLibraryScreen.getView().getElement()).thenReturn(emptyLibraryScreenElement);

        libraryScreen.showProjects();

        verify(view).updateContent(emptyLibraryScreenElement);
        verify(view).setProjectsCount(0);
    }

    @Test
    public void organizationalUnitEditedTest() {
        final OrganizationalUnit organizationalUnit = mock(OrganizationalUnit.class);
        final List<String> contributors = new ArrayList<>();
        contributors.add("admin");
        doReturn(contributors).when(organizationalUnit).getContributors();

        libraryScreen.organizationalUnitEdited(new AfterEditOrganizationalUnitEvent(mock(OrganizationalUnit.class),
                                                                                    organizationalUnit));

        verify(view).setContributorsCount(contributors.size());
    }
}