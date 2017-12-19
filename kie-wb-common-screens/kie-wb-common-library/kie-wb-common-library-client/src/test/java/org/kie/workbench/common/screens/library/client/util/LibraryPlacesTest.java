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

package org.kie.workbench.common.screens.library.client.util;

import javax.enterprise.event.Event;

import org.ext.uberfire.social.activities.model.ExtendedTypes;
import org.ext.uberfire.social.activities.model.SocialFileSelectedEvent;
import org.guvnor.common.services.project.client.preferences.ProjectScopedResolutionStrategySupplier;
import org.guvnor.common.services.project.context.ProjectContext;
import org.guvnor.common.services.project.context.ProjectContextChangeEvent;
import org.guvnor.common.services.project.events.RenameModuleEvent;
import org.guvnor.common.services.project.model.Module;
import org.guvnor.common.services.project.model.Package;
import org.guvnor.common.services.project.model.WorkspaceProject;
import org.guvnor.common.services.project.service.ProjectService;
import org.guvnor.common.services.project.social.ModuleEventType;
import org.guvnor.structure.organizationalunit.OrganizationalUnit;
import org.guvnor.structure.repositories.Branch;
import org.guvnor.structure.repositories.Repository;
import org.guvnor.structure.repositories.RepositoryRemovedEvent;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.ioc.client.api.ManagedInstance;
import org.jboss.errai.ui.client.local.spi.TranslationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.screens.examples.client.wizard.ExamplesWizard;
import org.kie.workbench.common.screens.explorer.model.URIStructureExplorerModel;
import org.kie.workbench.common.screens.library.api.LibraryService;
import org.kie.workbench.common.screens.library.client.events.AssetDetailEvent;
import org.kie.workbench.common.screens.library.client.events.ProjectMetricsEvent;
import org.kie.workbench.common.screens.library.client.perspective.LibraryPerspective;
import org.kie.workbench.common.screens.library.client.widgets.library.LibraryToolbarPresenter;
import org.kie.workbench.common.services.shared.project.KieModuleService;
import org.kie.workbench.common.workbench.client.docks.AuthoringWorkbenchDocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.uberfire.backend.vfs.ObservablePath;
import org.uberfire.backend.vfs.Path;
import org.uberfire.backend.vfs.VFSService;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.mvp.PlaceStatus;
import org.uberfire.client.mvp.UberElement;
import org.uberfire.client.workbench.events.PlaceGainFocusEvent;
import org.uberfire.ext.preferences.client.central.screen.PreferencesRootScreen;
import org.uberfire.ext.preferences.client.event.PreferencesCentralInitializationEvent;
import org.uberfire.ext.preferences.client.event.PreferencesCentralSaveEvent;
import org.uberfire.ext.preferences.client.event.PreferencesCentralUndoChangesEvent;
import org.uberfire.ext.widgets.common.client.breadcrumbs.UberfireBreadcrumbs;
import org.uberfire.mocks.CallerMock;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.ConditionalPlaceRequest;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.mvp.impl.PathPlaceRequest;
import org.uberfire.preferences.shared.impl.PreferenceScopeResolutionStrategyInfo;
import org.uberfire.workbench.events.NotificationEvent;
import org.uberfire.workbench.model.PanelDefinition;
import org.uberfire.workbench.model.impl.PartDefinitionImpl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LibraryPlacesTest {

    @Mock
    private UberfireBreadcrumbs breadcrumbs;

    @Mock
    private TranslationService ts;

    @Mock
    private Event<ProjectMetricsEvent> projectMetricsEvent;

    @Mock
    private Event<AssetDetailEvent> assetDetailEvent;

    @Mock
    private ResourceUtils resourceUtils;

    @Mock
    private LibraryService libraryService;
    private Caller<LibraryService> libraryServiceCaller;

    @Mock
    private PlaceManager placeManager;

    @Mock
    private ProjectContext projectContext;

    @Mock
    private LibraryToolbarPresenter libraryToolbar;

    @Mock
    private AuthoringWorkbenchDocks docks;

    @Mock
    private Event<ProjectContextChangeEvent> projectContextChangeEvent;

    @Mock
    private Event<NotificationEvent> notificationEvent;

    @Mock
    private ManagedInstance<ExamplesWizard> examplesWizards;

    @Mock
    private TranslationUtils translationUtils;

    @Mock
    private VFSService vfsService;
    private Caller<VFSService> vfsServiceCaller;

    @Mock
    private ProjectScopedResolutionStrategySupplier projectScopedResolutionStrategySupplier;

    @Mock
    private Event<PreferencesCentralInitializationEvent> preferencesCentralInitializationEvent;

    @Mock
    private ProjectService projectService;

    @Mock
    private KieModuleService moduleService;

    @Captor
    private ArgumentCaptor<ProjectContextChangeEvent> projectContextChangeEventArgumentCaptor;

    private LibraryPlaces libraryPlaces;

    private OrganizationalUnit activeOrganizationalUnit;
    private Repository activeRepository;
    private Branch activeBranch;
    private Module activeModule;
    private WorkspaceProject activeProject;

    @Before
    public void setup() {
        libraryServiceCaller = new CallerMock<>(libraryService);
        vfsServiceCaller = new CallerMock<>(vfsService);

        final UberElement libraryToolBarView = mock(UberElement.class);
        doReturn(libraryToolBarView).when(libraryToolbar).getView();

        libraryPlaces = spy(new LibraryPlaces(breadcrumbs,
                                              ts,
                                              projectMetricsEvent,
                                              assetDetailEvent,
                                              resourceUtils,
                                              libraryServiceCaller,
                                              new CallerMock<>(projectService),
                                              new CallerMock<>(moduleService),
                                              placeManager,
                                              projectContext,
                                              libraryToolbar,
                                              docks,
                                              projectContextChangeEvent,
                                              notificationEvent,
                                              examplesWizards,
                                              translationUtils,
                                              vfsServiceCaller,
                                              projectScopedResolutionStrategySupplier,
                                              preferencesCentralInitializationEvent));

        verify(libraryToolBarView).getElement();

        libraryPlaces.init(mock(LibraryPerspective.class));

        activeOrganizationalUnit = mock(OrganizationalUnit.class);
        activeRepository = mock(Repository.class);
        activeBranch = new Branch("master",
                                  mock(Path.class));
        activeModule = mock(Module.class);

        doReturn(activeOrganizationalUnit).when(projectContext).getActiveOrganizationalUnit();
        activeProject = new WorkspaceProject(activeOrganizationalUnit,
                                             activeRepository,
                                             activeBranch,
                                             activeModule);
        doReturn(activeProject).when(projectContext).getActiveWorkspaceProject();
        doReturn(activeModule).when(projectContext).getActiveModule();

        final URIStructureExplorerModel model = mock(URIStructureExplorerModel.class);
        doReturn(mock(Repository.class)).when(model).getRepository();
        doReturn(mock(Module.class)).when(model).getModule();

        doReturn(mock(Path.class)).when(vfsService).get(any());

        doNothing().when(libraryPlaces).setupLibraryBreadCrumbs();
        doNothing().when(libraryPlaces).setupLibraryBreadCrumbsForTrySamples();
        doNothing().when(libraryPlaces).setupLibraryBreadCrumbsForNewProject();
        doNothing().when(libraryPlaces).setupLibraryBreadCrumbsForAsset(any(Path.class));
        final PathPlaceRequest pathPlaceRequest = mock(PathPlaceRequest.class);
        doReturn(mock(ObservablePath.class)).when(pathPlaceRequest).getPath();
        doReturn(pathPlaceRequest).when(libraryPlaces).createPathPlaceRequest(any());

        doReturn(true).when(placeManager).closeAllPlacesOrNothing();
    }

    @Test
    public void projectContextListenerIsSetup() {
        verify(projectContext).addChangeHandler(any(LibraryPlaces.class));
    }

    @Test
    public void onChange() {

        libraryPlaces.onChange();

        verify(libraryPlaces).goToProject();
    }

    @Test
    public void onChangeNoActiveProject() {

        doReturn(null).when(projectContext).getActiveWorkspaceProject();

        libraryPlaces.onChange();

        verify(libraryPlaces, never()).goToProject();
    }

    @Test
    public void onNoChangeWhenThereIsActivePackage() {

        doReturn(new Package()).when(projectContext).getActivePackage();

        libraryPlaces.onChange();

        verify(libraryPlaces, never()).goToProject();
    }

    @Test
    public void onSelectPlaceOutsideLibraryTest() {
        doReturn(PlaceStatus.CLOSE).when(placeManager).getStatus(LibraryPlaces.LIBRARY_PERSPECTIVE);
        doReturn(PlaceStatus.CLOSE).when(placeManager).getStatus(any(PlaceRequest.class));

        final PlaceGainFocusEvent placeGainFocusEvent = mock(PlaceGainFocusEvent.class);
        libraryPlaces.onSelectPlaceEvent(placeGainFocusEvent);

        verify(placeGainFocusEvent,
               never()).getPlace();
    }

    @Test
    public void onSelectAssetTest() {
        doReturn(PlaceStatus.OPEN).when(placeManager).getStatus(LibraryPlaces.LIBRARY_PERSPECTIVE);

        final ObservablePath path = mock(ObservablePath.class);
        final PathPlaceRequest pathPlaceRequest = mock(PathPlaceRequest.class);
        doReturn(path).when(pathPlaceRequest).getPath();
        final PlaceGainFocusEvent placeGainFocusEvent = mock(PlaceGainFocusEvent.class);
        doReturn(pathPlaceRequest).when(placeGainFocusEvent).getPlace();

        libraryPlaces.onSelectPlaceEvent(placeGainFocusEvent);

        verify(libraryPlaces).setupLibraryBreadCrumbsForAsset(path);
        verify(libraryPlaces).showDocks();
    }

    @Test
    public void onSelectProjectSettingsTest() {
        doReturn(PlaceStatus.OPEN).when(placeManager).getStatus(LibraryPlaces.LIBRARY_PERSPECTIVE);

        final DefaultPlaceRequest projectSettingsPlaceRequest = new DefaultPlaceRequest(LibraryPlaces.PROJECT_SETTINGS);
        final PlaceGainFocusEvent placeGainFocusEvent = mock(PlaceGainFocusEvent.class);
        doReturn(projectSettingsPlaceRequest).when(placeGainFocusEvent).getPlace();

        libraryPlaces.onSelectPlaceEvent(placeGainFocusEvent);

        verify(libraryPlaces).hideDocks();
        verify(libraryPlaces).setupLibraryBreadCrumbsForAsset(null);
    }

    @Test
    public void onSelectProjectTest() {
        doReturn(PlaceStatus.OPEN).when(placeManager).getStatus(LibraryPlaces.LIBRARY_PERSPECTIVE);

        final DefaultPlaceRequest projectSettingsPlaceRequest = new DefaultPlaceRequest(LibraryPlaces.PROJECT_SCREEN);
        final PlaceGainFocusEvent placeGainFocusEvent = mock(PlaceGainFocusEvent.class);
        doReturn(projectSettingsPlaceRequest).when(placeGainFocusEvent).getPlace();

        libraryPlaces.onSelectPlaceEvent(placeGainFocusEvent);

        verify(libraryPlaces).hideDocks();
        verify(libraryPlaces).setupLibraryBreadCrumbs();
    }

    @Test
    public void onSelectLibraryTest() {
        doReturn(PlaceStatus.OPEN).when(placeManager).getStatus(LibraryPlaces.LIBRARY_PERSPECTIVE);

        final DefaultPlaceRequest projectSettingsPlaceRequest = new DefaultPlaceRequest(LibraryPlaces.LIBRARY_SCREEN);
        final PlaceGainFocusEvent placeGainFocusEvent = mock(PlaceGainFocusEvent.class);
        doReturn(projectSettingsPlaceRequest).when(placeGainFocusEvent).getPlace();

        libraryPlaces.onSelectPlaceEvent(placeGainFocusEvent);

        verify(libraryPlaces).hideDocks();
        verify(libraryPlaces).setupLibraryBreadCrumbs();
    }

    @Test
    public void hideDocksTest() {
        libraryPlaces.showDocks();

        reset(docks);

        libraryPlaces.hideDocks();
        libraryPlaces.hideDocks();

        verify(docks,
               times(1)).hide();
        verify(docks,
               never()).setup(anyString(),
                              any(PlaceRequest.class));
        verify(docks,
               never()).show();
        verify(docks,
               never()).expandProjectExplorer();
    }

    @Test
    public void showDocksTest() {
        libraryPlaces.showDocks();
        libraryPlaces.showDocks();

        verify(docks,
               times(1)).setup(LibraryPlaces.LIBRARY_PERSPECTIVE,
                               new DefaultPlaceRequest(LibraryPlaces.PROJECT_EXPLORER));
        verify(docks,
               times(1)).show();
        verify(docks,
               never()).hide();
    }

    @Test
    public void onPreferencesSaveTest() {
        doReturn(PlaceStatus.OPEN).when(placeManager).getStatus(LibraryPlaces.LIBRARY_PERSPECTIVE);
        doNothing().when(libraryPlaces).goToProject();

        libraryPlaces.onPreferencesSave(mock(PreferencesCentralSaveEvent.class));

        verify(libraryPlaces).goToProject();
    }

    @Test
    public void onPreferencesSaveOutsideLibraryTest() {
        doReturn(PlaceStatus.CLOSE).when(placeManager).getStatus(LibraryPlaces.LIBRARY_PERSPECTIVE);
        doReturn(PlaceStatus.CLOSE).when(placeManager).getStatus(any(PlaceRequest.class));

        libraryPlaces.onPreferencesSave(mock(PreferencesCentralSaveEvent.class));

        verify(libraryPlaces,
               never()).goToProject();
    }

    @Test
    public void onPreferencesCancelTest() {
        doReturn(PlaceStatus.OPEN).when(placeManager).getStatus(LibraryPlaces.LIBRARY_PERSPECTIVE);
        doNothing().when(libraryPlaces).goToProject();

        libraryPlaces.onPreferencesCancel(mock(PreferencesCentralUndoChangesEvent.class));

        verify(libraryPlaces).goToProject();
    }

    @Test
    public void onPreferencesCancelOutsideLibraryTest() {
        doReturn(PlaceStatus.CLOSE).when(placeManager).getStatus(LibraryPlaces.LIBRARY_PERSPECTIVE);
        doReturn(PlaceStatus.CLOSE).when(placeManager).getStatus(any(PlaceRequest.class));

        libraryPlaces.onPreferencesCancel(mock(PreferencesCentralUndoChangesEvent.class));

        verify(libraryPlaces,
               never()).goToProject();
    }

    @Test
    public void goToOrganizationalUnitsTest() {
        final PlaceRequest placeRequest = new DefaultPlaceRequest(LibraryPlaces.ORGANIZATIONAL_UNITS_SCREEN);
        final PartDefinitionImpl part = new PartDefinitionImpl(placeRequest);
        part.setSelectable(false);

        libraryPlaces.goToOrganizationalUnits();

        verify(projectContextChangeEvent).fire(projectContextChangeEventArgumentCaptor.capture());
        assertNull(projectContextChangeEventArgumentCaptor.getValue().getOrganizationalUnit());

        final ArgumentCaptor<ProjectContextChangeEvent> eventArgumentCaptor = ArgumentCaptor.forClass(ProjectContextChangeEvent.class);
        verify(projectContextChangeEvent).fire(eventArgumentCaptor.capture());
        final ProjectContextChangeEvent event = eventArgumentCaptor.getValue();
        assertNull(event.getOrganizationalUnit());
        assertNull(event.getWorkspaceProject());
        verify(placeManager).closeAllPlacesOrNothing();
        verify(placeManager).goTo(eq(part),
                                  any(PanelDefinition.class));
        verify(libraryPlaces).setupLibraryBreadCrumbs();
    }

    @Test
    public void goToAssetTest() {
        final ObservablePath path = mock(ObservablePath.class);
        final PathPlaceRequest pathPlaceRequest = mock(PathPlaceRequest.class);
        doReturn(path).when(pathPlaceRequest).getPath();
        doReturn(pathPlaceRequest).when(libraryPlaces).createPathPlaceRequest(any(Path.class));

        libraryPlaces.goToAsset(path);

        verify(placeManager).goTo(pathPlaceRequest);
        final ArgumentCaptor<ProjectContextChangeEvent> eventArgumentCaptor = ArgumentCaptor.forClass(ProjectContextChangeEvent.class);
        verify(projectContextChangeEvent).fire(eventArgumentCaptor.capture());

        final ProjectContextChangeEvent value = eventArgumentCaptor.getValue();
        assertEquals(activeProject, value.getWorkspaceProject());
        assertEquals(activeModule, value.getModule());
        assertNull(value.getPackage());
    }

    @Test
    public void goToAssetTestWithPackage() {

        final ObservablePath path = mock(ObservablePath.class);
        final PathPlaceRequest pathPlaceRequest = mock(PathPlaceRequest.class);
        doReturn(path).when(pathPlaceRequest).getPath();
        doReturn(pathPlaceRequest).when(libraryPlaces).createPathPlaceRequest(any(Path.class));

        final Package pkg = mock(Package.class);
        doReturn(pkg).when(moduleService).resolvePackage(path);

        libraryPlaces.goToAsset(path);

        verify(projectContextChangeEvent).fire(projectContextChangeEventArgumentCaptor.capture());
        final ProjectContextChangeEvent contextChangeEvent = projectContextChangeEventArgumentCaptor.getValue();
        assertEquals(activeProject, contextChangeEvent.getWorkspaceProject());
        assertEquals(activeModule, contextChangeEvent.getModule());
        assertEquals(pkg, contextChangeEvent.getPackage());

        verify(placeManager).goTo(pathPlaceRequest);
        final ArgumentCaptor<ProjectContextChangeEvent> eventArgumentCaptor = ArgumentCaptor.forClass(ProjectContextChangeEvent.class);
        verify(projectContextChangeEvent).fire(eventArgumentCaptor.capture());

        final ProjectContextChangeEvent value = eventArgumentCaptor.getValue();
        assertEquals(activeProject, value.getWorkspaceProject());
        assertEquals(activeModule, value.getModule());
        assertEquals(pkg, value.getPackage());
    }

    @Test
    public void goToProjectSettingsTest() {
        final DefaultPlaceRequest placeRequest = new DefaultPlaceRequest(LibraryPlaces.PROJECT_SETTINGS);

        libraryPlaces.goToAsset(null);

        verify(placeManager).goTo(placeRequest);
    }

    @Test
    public void goToLibraryTest() {
        final PlaceRequest placeRequest = new DefaultPlaceRequest(LibraryPlaces.LIBRARY_SCREEN);
        final PartDefinitionImpl part = new PartDefinitionImpl(placeRequest);
        part.setSelectable(false);

        libraryPlaces.goToLibrary();

        verify(libraryPlaces).closeLibraryPlaces();
        verify(placeManager).goTo(eq(part),
                                  any(PanelDefinition.class));
        verify(libraryPlaces).setupLibraryBreadCrumbs();
        verify(projectContextChangeEvent,
               never()).fire(any(ProjectContextChangeEvent.class));
    }

    @Test
    public void goToProjectTest() {
        final PlaceRequest projectScreen = new DefaultPlaceRequest(LibraryPlaces.PROJECT_SCREEN);
        final PartDefinitionImpl part = new PartDefinitionImpl(projectScreen);
        part.setSelectable(false);

        libraryPlaces.goToProject();

        verify(placeManager).goTo(eq(part),
                                  any(PanelDefinition.class));
        verify(projectContextChangeEvent,
               never()).fire(any(ProjectContextChangeEvent.class));
        verify(libraryPlaces).setupLibraryBreadCrumbs();
    }

    @Test
    public void goToOrgUnitsMetricsTest() {
        final PlaceRequest metricsScreen = new DefaultPlaceRequest(LibraryPlaces.ORG_UNITS_METRICS_SCREEN);
        final PartDefinitionImpl part = new PartDefinitionImpl(metricsScreen);
        part.setSelectable(false);

        libraryPlaces.goToOrgUnitsMetrics();

        verify(placeManager).goTo(eq(part),
                                  any(PanelDefinition.class));
        verify(libraryPlaces).setupLibraryBreadCrumbsForOrgUnitsMetrics();
    }

    @Test
    public void goToProjectMetricsTest() {
        final PlaceRequest projectScreen = new DefaultPlaceRequest(LibraryPlaces.PROJECT_METRICS_SCREEN);
        final PartDefinitionImpl part = new PartDefinitionImpl(projectScreen);
        part.setSelectable(false);

        libraryPlaces.goToProjectMetrics();

        verify(placeManager).goTo(eq(part),
                                  any(PanelDefinition.class));
        verify(projectMetricsEvent).fire(any(ProjectMetricsEvent.class));
        verify(libraryPlaces).setupLibraryBreadCrumbsForProjectMetrics();
    }

    @Test
    public void closeLibraryPlacesTest() {
        libraryPlaces.closeLibraryPlaces();
        verify(placeManager).closePlace(LibraryPlaces.LIBRARY_SCREEN);
        verify(placeManager).closePlace(LibraryPlaces.PROJECT_SCREEN);
        verify(placeManager).closePlace(LibraryPlaces.PROJECT_METRICS_SCREEN);
        verify(placeManager).closePlace(LibraryPlaces.PROJECT_DETAIL_SCREEN);
        verify(placeManager).closePlace(LibraryPlaces.ORGANIZATIONAL_UNITS_SCREEN);
        verify(placeManager).closePlace(LibraryPlaces.PROJECT_SETTINGS);
        verify(placeManager).closePlace(PreferencesRootScreen.IDENTIFIER);
    }

    @Test
    public void goToSameProjectTest() {
        final ProjectInfo projectInfo = new ProjectInfo(activeOrganizationalUnit,
                                                        activeRepository,
                                                        activeBranch,
                                                        activeProject);
        libraryPlaces.goToProject(projectInfo);
        libraryPlaces.goToProject(projectInfo);

        verify(placeManager,
               times(1)).closeAllPlacesOrNothing();
    }

    @Test
    public void goToImportProjectWizardTest() {
        final ExamplesWizard examplesWizard = mock(ExamplesWizard.class);
        doReturn(examplesWizard).when(examplesWizards).get();

        libraryPlaces.goToImportProjectWizard();

        verify(examplesWizard).start();
        verify(examplesWizard).setDefaultTargetOrganizationalUnit(anyString());
    }

    @Test
    public void goToMessages() {
        libraryPlaces.goToMessages();

        verify(placeManager).goTo(LibraryPlaces.MESSAGES);
    }

    @Test
    public void goToPreferencesTest() {
        final PreferenceScopeResolutionStrategyInfo scopeResolutionStrategyInfo = mock(PreferenceScopeResolutionStrategyInfo.class);
        doReturn(scopeResolutionStrategyInfo).when(projectScopedResolutionStrategySupplier).get();

        final DefaultPlaceRequest placeRequest = new DefaultPlaceRequest(PreferencesRootScreen.IDENTIFIER);
        final PartDefinitionImpl part = new PartDefinitionImpl(placeRequest);
        part.setSelectable(false);

        libraryPlaces.goToPreferences();

        verify(placeManager).goTo(eq(part),
                                  any(PanelDefinition.class));
        verify(preferencesCentralInitializationEvent).fire(new PreferencesCentralInitializationEvent("ProjectPreferences",
                                                                                                     scopeResolutionStrategyInfo,
                                                                                                     null));
        verify(libraryPlaces).setupLibraryBreadCrumbsForPreferences();
    }

    @Test
    public void projectDeletedRedirectsToLibraryWhenItIsOpenedTest() {
//        final Repository activeRepository = mock(Repository.class);
        final RepositoryRemovedEvent repositoryRemovedEvent = mock(RepositoryRemovedEvent.class);

        doReturn(PlaceStatus.OPEN).when(placeManager).getStatus(LibraryPlaces.LIBRARY_PERSPECTIVE);
        doReturn(activeRepository).when(repositoryRemovedEvent).getRepository();

        libraryPlaces.goToProject();
        libraryPlaces.onProjectDeleted(repositoryRemovedEvent);

        verify(libraryPlaces).closeAllPlaces();
        verify(libraryPlaces).goToLibrary();
        verify(notificationEvent).fire(any());
    }

    @Test
    public void projectDeletedDoesNotRedirectToLibraryWhenItIsNotOpenedTest() {

        libraryPlaces.goToProject();

        final Repository deletedRepository = mock(Repository.class);
        final RepositoryRemovedEvent repositoryRemovedEvent = mock(RepositoryRemovedEvent.class);

        doReturn(PlaceStatus.OPEN).when(placeManager).getStatus(LibraryPlaces.LIBRARY_PERSPECTIVE);
        doReturn(deletedRepository).when(repositoryRemovedEvent).getRepository();

        libraryPlaces.onProjectDeleted(repositoryRemovedEvent);

        verify(libraryPlaces,
               never()).closeAllPlaces();
        verify(libraryPlaces,
               never()).goToLibrary();
        verify(notificationEvent,
               never()).fire(any());
    }

    @Test
    public void placesAreUpdatedWhenActiveModuleIsRenamedTest() {
        final Module renamedModule = mock(Module.class);
        final RenameModuleEvent renameModuleEvent = mock(RenameModuleEvent.class);

        doReturn(PlaceStatus.OPEN).when(placeManager).getStatus(LibraryPlaces.LIBRARY_PERSPECTIVE);

        doReturn(activeModule).when(renameModuleEvent).getOldModule();
        doReturn(renamedModule).when(renameModuleEvent).getNewModule();

        libraryPlaces.onProjectRenamed(renameModuleEvent);

        verify(breadcrumbs).clearBreadcrumbs(LibraryPlaces.LIBRARY_PERSPECTIVE);
    }

    @Test
    public void breadcrumbIsNotUpdatedWhenInactiveModuleIsRenamedTest() {
        final Module activeModule = mock(Module.class);
        final Module renamedModule = mock(Module.class);
        final Module otherModule = mock(Module.class);
        final RenameModuleEvent renameModuleEvent = mock(RenameModuleEvent.class);

        doReturn(PlaceStatus.OPEN).when(placeManager).getStatus(LibraryPlaces.LIBRARY_PERSPECTIVE);

        doReturn(activeModule).when(projectContext).getActiveModule();
        doReturn(otherModule).when(renameModuleEvent).getOldModule();
        doReturn(renamedModule).when(renameModuleEvent).getNewModule();

        libraryPlaces.onProjectRenamed(renameModuleEvent);

        verify(libraryPlaces,
               never()).setupLibraryBreadCrumbsForAsset(any());
    }

    @Test
    public void testOnSocialFileSelected_Repository() {

        doReturn(mock(WorkspaceProject.class)).when(projectService).resolveProject(any(Path.class));

        doReturn(PlaceStatus.OPEN).when(placeManager).getStatus(LibraryPlaces.LIBRARY_PERSPECTIVE);

        final SocialFileSelectedEvent event = new SocialFileSelectedEvent(ExtendedTypes.NEW_REPOSITORY_EVENT.name(),
                                                                          null);

        libraryPlaces.onSocialFileSelected(event);

        verify(placeManager).goTo(LibraryPlaces.REPOSITORY_STRUCTURE_SCREEN);
    }

    @Test
    public void testOnSocialFileSelected_Module() {

        doReturn(mock(WorkspaceProject.class)).when(projectService).resolveProject(any(Path.class));

        doReturn(PlaceStatus.OPEN).when(placeManager).getStatus(LibraryPlaces.LIBRARY_PERSPECTIVE);

        final PlaceRequest libraryPerspective = libraryPlaces.getLibraryPlaceRequestWithoutRefresh();
        final SocialFileSelectedEvent event = new SocialFileSelectedEvent(ModuleEventType.NEW_MODULE.name(),
                                                                          null);

        libraryPlaces.onSocialFileSelected(event);

        verify(placeManager).goTo(libraryPerspective);
        verify(libraryPlaces).goToProject();
    }

    @Test
    public void testOnSocialFileSelected_Asset() {

        doReturn(mock(WorkspaceProject.class)).when(projectService).resolveProject(any(Path.class));

        doReturn(PlaceStatus.OPEN).when(placeManager).getStatus(LibraryPlaces.LIBRARY_PERSPECTIVE);

        final PlaceRequest libraryPerspective = libraryPlaces.getLibraryPlaceRequestWithoutRefresh();
        final SocialFileSelectedEvent event = new SocialFileSelectedEvent("any",
                                                                          "uri");

        libraryPlaces.onSocialFileSelected(event);

        verify(placeManager).goTo(libraryPerspective);
        verify(libraryPlaces).goToAsset(any(Path.class));
    }
}
