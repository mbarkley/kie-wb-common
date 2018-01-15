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
package org.kie.workbench.common.screens.library.client.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.ext.uberfire.social.activities.model.ExtendedTypes;
import org.ext.uberfire.social.activities.model.SocialFileSelectedEvent;
import org.guvnor.common.services.project.client.context.WorkspaceProjectContext;
import org.guvnor.common.services.project.client.preferences.ProjectScopedResolutionStrategySupplier;
import org.guvnor.common.services.project.context.WorkspaceProjectContextChangeEvent;
import org.guvnor.common.services.project.context.WorkspaceProjectContextChangeHandler;
import org.guvnor.common.services.project.events.RenameModuleEvent;
import org.guvnor.common.services.project.model.WorkspaceProject;
import org.guvnor.common.services.project.service.WorkspaceProjectService;
import org.guvnor.common.services.project.social.ModuleEventType;
import org.guvnor.structure.organizationalunit.OrganizationalUnit;
import org.guvnor.structure.repositories.RepositoryRemovedEvent;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jboss.errai.ioc.client.api.ManagedInstance;
import org.jboss.errai.ui.client.local.spi.TranslationService;
import org.kie.soup.commons.validation.PortablePreconditions;
import org.kie.workbench.common.screens.examples.client.wizard.ExamplesWizard;
import org.kie.workbench.common.screens.library.api.LibraryService;
import org.kie.workbench.common.screens.library.client.events.AssetDetailEvent;
import org.kie.workbench.common.screens.library.client.events.WorkbenchProjectMetricsEvent;
import org.kie.workbench.common.screens.library.client.perspective.LibraryPerspective;
import org.kie.workbench.common.screens.library.client.resources.i18n.LibraryConstants;
import org.kie.workbench.common.screens.library.client.widgets.library.LibraryToolbarPresenter;
import org.kie.workbench.common.services.shared.project.KieModuleService;
import org.kie.workbench.common.widgets.client.handlers.NewResourceSuccessEvent;
import org.kie.workbench.common.workbench.client.docks.AuthoringWorkbenchDocks;
import org.uberfire.backend.vfs.ObservablePath;
import org.uberfire.backend.vfs.Path;
import org.uberfire.backend.vfs.VFSService;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.mvp.PlaceStatus;
import org.uberfire.client.workbench.events.PlaceGainFocusEvent;
import org.uberfire.ext.editor.commons.client.event.ConcurrentRenameAcceptedEvent;
import org.uberfire.ext.preferences.client.central.screen.PreferencesRootScreen;
import org.uberfire.ext.preferences.client.event.PreferencesCentralInitializationEvent;
import org.uberfire.ext.preferences.client.event.PreferencesCentralSaveEvent;
import org.uberfire.ext.preferences.client.event.PreferencesCentralUndoChangesEvent;
import org.uberfire.ext.widgets.common.client.breadcrumbs.UberfireBreadcrumbs;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.mvp.impl.PathPlaceRequest;
import org.uberfire.preferences.shared.impl.PreferenceScopeResolutionStrategyInfo;
import org.uberfire.workbench.events.NotificationEvent;
import org.uberfire.workbench.model.impl.PartDefinitionImpl;

@ApplicationScoped
public class LibraryPlaces implements WorkspaceProjectContextChangeHandler {

    public static final String LIBRARY_PERSPECTIVE = "LibraryPerspective";
    public static final String LIBRARY_SCREEN = "LibraryScreen";
    public static final String PROJECT_SCREEN = "ProjectScreen";
    public static final String IMPORT_PROJECTS_SCREEN = "TrySamplesScreen";
    public static final String PROJECT_DETAIL_SCREEN = "ProjectsDetailScreen";
    public static final String ORG_UNITS_METRICS_SCREEN = "OrgUnitsMetricsScreen";
    public static final String PROJECT_METRICS_SCREEN = "ProjectMetricsScreen";
    public static final String ORGANIZATIONAL_UNITS_SCREEN = "LibraryOrganizationalUnitsScreen";
    public static final String PROJECT_SETTINGS = "projectScreen";
    public static final String PROJECT_EXPLORER = "org.kie.guvnor.explorer";
    public static final String MESSAGES = "org.kie.workbench.common.screens.messageconsole.MessageConsole";
    public static final String REPOSITORY_STRUCTURE_SCREEN = "repositoryStructureScreen";

    public static final List<String> LIBRARY_PLACES = Collections.unmodifiableList(new ArrayList<String>(7) {{
        add(LIBRARY_SCREEN);
        add(ORG_UNITS_METRICS_SCREEN);
        add(PROJECT_SCREEN);
        add(PROJECT_METRICS_SCREEN);
        add(PROJECT_DETAIL_SCREEN);
        add(ORGANIZATIONAL_UNITS_SCREEN);
        add(PROJECT_SETTINGS);
        add(PreferencesRootScreen.IDENTIFIER);
    }});

    private UberfireBreadcrumbs breadcrumbs;

    private TranslationService ts;

    private Event<WorkbenchProjectMetricsEvent> projectMetricsEvent;

    private Event<AssetDetailEvent> assetDetailEvent;

    private ResourceUtils resourceUtils;

    private Caller<LibraryService> libraryService;

    private Caller<WorkspaceProjectService> projectService;

    private Caller<KieModuleService> moduleService;

    private PlaceManager placeManager;

    private LibraryPerspective libraryPerspective;

    private WorkspaceProjectContext projectContext;

    private LibraryToolbarPresenter libraryToolbar;

    private AuthoringWorkbenchDocks docks;

    private Event<WorkspaceProjectContextChangeEvent> projectContextChangeEvent;

    private Event<NotificationEvent> notificationEvent;

    private ManagedInstance<ExamplesWizard> examplesWizards;

    private TranslationUtils translationUtils;

    private Caller<VFSService> vfsService;

    private ProjectScopedResolutionStrategySupplier projectScopedResolutionStrategySupplier;

    private Event<PreferencesCentralInitializationEvent> preferencesCentralInitializationEvent;

    private boolean docksReady = false;

    private boolean docksHidden = true;

    private WorkspaceProject lastViewedProject = null;

    private boolean closingLibraryPlaces = false;

    public LibraryPlaces() {
    }

    @Inject
    public LibraryPlaces(final UberfireBreadcrumbs breadcrumbs,
                         final TranslationService ts,
                         final Event<WorkbenchProjectMetricsEvent> projectMetricsEvent,
                         final Event<AssetDetailEvent> assetDetailEvent,
                         final ResourceUtils resourceUtils,
                         final Caller<LibraryService> libraryService,
                         final Caller<WorkspaceProjectService> projectService,
                         final Caller<KieModuleService> moduleService,
                         final PlaceManager placeManager,
                         final WorkspaceProjectContext projectContext,
                         final LibraryToolbarPresenter libraryToolbar,
                         final AuthoringWorkbenchDocks docks,
                         final Event<WorkspaceProjectContextChangeEvent> projectContextChangeEvent,
                         final Event<NotificationEvent> notificationEvent,
                         final ManagedInstance<ExamplesWizard> examplesWizards,
                         final TranslationUtils translationUtils,
                         final Caller<VFSService> vfsService,
                         final ProjectScopedResolutionStrategySupplier projectScopedResolutionStrategySupplier,
                         final Event<PreferencesCentralInitializationEvent> preferencesCentralInitializationEvent) {
        this.breadcrumbs = breadcrumbs;
        this.ts = ts;
        this.projectMetricsEvent = projectMetricsEvent;
        this.assetDetailEvent = assetDetailEvent;
        this.resourceUtils = resourceUtils;
        this.libraryService = libraryService;
        this.projectService = projectService;
        this.moduleService = moduleService;
        this.placeManager = placeManager;
        this.projectContext = projectContext;
        this.libraryToolbar = libraryToolbar;
        this.docks = docks;
        this.projectContextChangeEvent = projectContextChangeEvent;
        this.notificationEvent = notificationEvent;
        this.examplesWizards = examplesWizards;
        this.translationUtils = translationUtils;
        this.vfsService = vfsService;
        this.projectScopedResolutionStrategySupplier = projectScopedResolutionStrategySupplier;
        this.preferencesCentralInitializationEvent = preferencesCentralInitializationEvent;

        projectContext.addChangeHandler(this);

        breadcrumbs.addToolbar(LibraryPlaces.LIBRARY_PERSPECTIVE,
                               libraryToolbar.getView().getElement());
    }

    public void onSelectPlaceEvent(@Observes final PlaceGainFocusEvent placeGainFocusEvent) {
        if (isLibraryPerspectiveOpen() && !closingLibraryPlaces) {
            final PlaceRequest place = placeGainFocusEvent.getPlace();

            if (place instanceof PathPlaceRequest) {
                final PathPlaceRequest pathPlaceRequest = (PathPlaceRequest) place;
                setupLibraryBreadCrumbsForAsset(pathPlaceRequest.getPath());
                showDocks();
            } else if (!place.getIdentifier().equals(MESSAGES) && isLibraryPlace(place)) {
                hideDocks();
                if (place.getIdentifier().equals(PROJECT_SETTINGS)) {
                    setupLibraryBreadCrumbsForAsset(null);
                } else if (projectContext.getActiveWorkspaceProject() != null
                        && place.getIdentifier().equals(LibraryPlaces.PROJECT_SCREEN)) {
                    setupLibraryBreadCrumbs();
                } else if (place.getIdentifier().equals(LibraryPlaces.LIBRARY_SCREEN)) {
                    setupLibraryBreadCrumbs();
                }
            }
        }
    }

    public void hideDocks() {
        if (!docksHidden) {
            docks.hide();
            docksHidden = true;
        }
    }

    public void showDocks() {
        if (docksHidden) {
            if (!docksReady) {
                docks.setup(LibraryPlaces.LIBRARY_PERSPECTIVE,
                            new DefaultPlaceRequest(PROJECT_EXPLORER));
                docksReady = true;
            }
            docks.show();
            docksHidden = false;
        }
    }

    private boolean isLibraryPlace(final PlaceRequest place) {
        return LIBRARY_PLACES.contains(place.getIdentifier());
    }

    public void onNewResourceCreated(@Observes final NewResourceSuccessEvent newResourceSuccessEvent) {
        if (isLibraryPerspectiveOpen()) {
            assetDetailEvent.fire(new AssetDetailEvent(projectContext.getActiveWorkspaceProject(),
                                                       newResourceSuccessEvent.getPath()));
        }
    }

    public void onAssetRenamedAccepted(@Observes final ConcurrentRenameAcceptedEvent concurrentRenameAcceptedEvent) {
        if (isLibraryPerspectiveOpen()) {
            final ObservablePath path = concurrentRenameAcceptedEvent.getPath();
            goToAsset(path);
            setupLibraryBreadCrumbsForAsset(path);
        }
    }

    public void onProjectDeleted(@Observes final RepositoryRemovedEvent repositoryRemovedEvent) {
        if (isLibraryPerspectiveOpen()) {
            if (repositoryRemovedEvent.getRepository().equals(lastViewedProject.getRepository())) {
                closeAllPlaces();
                goToLibrary();
                notificationEvent.fire(new NotificationEvent(ts.getTranslation(LibraryConstants.ProjectDeleted),
                                                             NotificationEvent.NotificationType.DEFAULT));
            }
        }
    }

    public void onProjectRenamed(@Observes final RenameModuleEvent renameModuleEvent) {
        if (isLibraryPerspectiveOpen()) {
            if (renameModuleEvent.getOldModule().equals(projectContext.getActiveWorkspaceProject().getMainModule())) {
                refresh(null);
            }
        }
    }

    public void onAssetSelected(@Observes final AssetDetailEvent assetDetails) {
        goToAsset(assetDetails.getPath());
    }

    public void setUpBranches() {
        libraryToolbar.setUpBranches();
    }

    private boolean isLibraryPerspectiveOpen() {
        return placeManager.getStatus(LIBRARY_PERSPECTIVE).equals(PlaceStatus.OPEN)
                || placeManager.getStatus(getLibraryPlaceRequestWithoutRefresh()).equals(PlaceStatus.OPEN);
    }

    public void onPreferencesSave(@Observes PreferencesCentralSaveEvent event) {
        if (isLibraryPerspectiveOpen()) {
            goToProject();
        }
    }

    public void onPreferencesCancel(@Observes PreferencesCentralUndoChangesEvent event) {
        if (isLibraryPerspectiveOpen()) {
            goToProject();
        }
    }

    public void onSocialFileSelected(@Observes final SocialFileSelectedEvent event) {
        vfsService.call(new RemoteCallback<Path>() {
            @Override
            public void callback(Path path) {

                projectService.call(new RemoteCallback<WorkspaceProject>() {
                    @Override
                    public void callback(final WorkspaceProject project) {
                        openBestSuitedScreen(event.getEventType(),
                                             path,
                                             project);
                    }
                }).resolveProject(path);
            }
        }).get(event.getUri());
    }

    private void openBestSuitedScreen(final String eventType,
                                      final Path path,
                                      final WorkspaceProject project) {

        if (!projectContext.getActiveWorkspaceProject().equals(project)) {
            projectContextChangeEvent.fire(new WorkspaceProjectContextChangeEvent(project));
        }

        final PlaceRequest libraryPerspectivePlace = getLibraryPlaceRequestWithoutRefresh();

        if (isRepositoryEvent(eventType)) {
            placeManager.goTo(REPOSITORY_STRUCTURE_SCREEN);
        } else if (isModuleEvent(eventType)) {
            placeManager.goTo(libraryPerspectivePlace);
            goToProject();
        } else if (path != null) {
            placeManager.goTo(libraryPerspectivePlace);
            goToProject(() -> goToAsset(path));
        }
    }

    PlaceRequest getLibraryPlaceRequestWithoutRefresh() {
        final Map<String, String> params = new HashMap<>();
        params.put("refresh",
                   "false");
        return new DefaultPlaceRequest(LIBRARY_PERSPECTIVE,
                                       params);
    }

    private boolean isRepositoryEvent(String eventType) {
        if (eventType == null || eventType.isEmpty()) {
            return false;
        }

        if (ExtendedTypes.NEW_REPOSITORY_EVENT.name().equals(eventType)) {
            return true;
        }

        return false;
    }

    private boolean isModuleEvent(final String eventType) {
        return ModuleEventType.NEW_MODULE.name().equals(eventType);
    }

    public void setupLibraryBreadCrumbs() {
        breadcrumbs.clearBreadcrumbs(LibraryPlaces.LIBRARY_PERSPECTIVE);
        breadcrumbs.addBreadCrumb(LibraryPlaces.LIBRARY_PERSPECTIVE,
                                  translationUtils.getOrganizationalUnitAliasInPlural(),
                                  () -> goToOrganizationalUnits());
        if (projectContext.getActiveOrganizationalUnit() != null) {

            breadcrumbs.addBreadCrumb(LibraryPlaces.LIBRARY_PERSPECTIVE,
                                      projectContext.getActiveOrganizationalUnit().getName(),
                                      () -> goToLibrary());
        }
        if (projectContext.getActiveWorkspaceProject() != null) {

            breadcrumbs.addBreadCrumb(LibraryPlaces.LIBRARY_PERSPECTIVE,
                                      projectContext.getActiveWorkspaceProject().getName(),
                                      () -> goToProject());
        }
        libraryToolbar.setUpBranches();
    }

    public void setupLibraryBreadCrumbsForNewProject() {
        setupLibraryBreadCrumbs();
        breadcrumbs.addBreadCrumb(LibraryPlaces.LIBRARY_PERSPECTIVE,
                                  ts.getTranslation(LibraryConstants.TrySamples),
                                  () -> goToTrySamples());
    }

    public void setupLibraryBreadCrumbsForImportProjects(final String repositoryUrl) {
        breadcrumbs.clearBreadcrumbs(LibraryPlaces.LIBRARY_PERSPECTIVE);
        breadcrumbs.addBreadCrumb(LibraryPlaces.LIBRARY_PERSPECTIVE,
                                  translationUtils.getOrganizationalUnitAliasInPlural(),
                                  () -> goToOrganizationalUnits());
        breadcrumbs.addBreadCrumb(LibraryPlaces.LIBRARY_PERSPECTIVE,
                                  projectContext.getActiveOrganizationalUnit().getName(),
                                  () -> goToLibrary());
        breadcrumbs.addBreadCrumb(LibraryPlaces.LIBRARY_PERSPECTIVE,
                                  ts.getTranslation(LibraryConstants.ImportProjects),
                                  () -> goToImportProjects(repositoryUrl));
    }

    public void setupLibraryBreadCrumbsForProjectMetrics() {
        setupLibraryBreadCrumbs();
        breadcrumbs.addBreadCrumb(LibraryPlaces.LIBRARY_PERSPECTIVE,
                                  translationUtils.getProjectMetrics(),
                                  () -> goToProjectMetrics());
    }

    public void setupLibraryBreadCrumbsForOrgUnitsMetrics() {
        setupLibraryBreadCrumbs();
        breadcrumbs.addBreadCrumb(LibraryPlaces.LIBRARY_PERSPECTIVE,
                                  translationUtils.getOrgUnitsMetrics(),
                                  () -> goToOrgUnitsMetrics());
    }

    public void setupLibraryBreadCrumbsForAsset(final Path path) {
        setupLibraryBreadCrumbs();
        breadcrumbs.addBreadCrumb(LibraryPlaces.LIBRARY_PERSPECTIVE,
                                  getAssetName(path),
                                  () -> goToAsset(path));
    }

    private String getAssetName(final Path path) {
        if (path != null) {
            return resourceUtils.getBaseFileName(path);
        } else {
            return ts.format(LibraryConstants.Settings);
        }
    }

    public void setupLibraryBreadCrumbsForPreferences() {
        setupLibraryBreadCrumbs();
        breadcrumbs.addBreadCrumb(LibraryPlaces.LIBRARY_PERSPECTIVE,
                                  ts.getTranslation(LibraryConstants.Preferences),
                                  () -> goToPreferences());
    }

    public void refresh(final Command callback) {
        breadcrumbs.clearBreadcrumbs(LibraryPlaces.LIBRARY_PERSPECTIVE);
        translationUtils.refresh(() -> {
            libraryToolbar.init(() -> {
                if (callback != null) {
                    callback.execute();
                }
            });
        });
    }

    public void goToOrganizationalUnits() {
        if (closeAllPlacesOrNothing()) {
            PortablePreconditions.checkNotNull("libraryPerspective.closeAllPlacesOrNothing",
                                               libraryPerspective);

            projectContextChangeEvent.fire(new WorkspaceProjectContextChangeEvent());

            final DefaultPlaceRequest placeRequest = new DefaultPlaceRequest(LibraryPlaces.ORGANIZATIONAL_UNITS_SCREEN);
            final PartDefinitionImpl part = new PartDefinitionImpl(placeRequest);
            part.setSelectable(false);
            placeManager.goTo(part,
                              libraryPerspective.getRootPanel());
            setupLibraryBreadCrumbs();
        }
    }

    public void goToLibrary() {
        if (projectContext.getActiveOrganizationalUnit() == null) {
            libraryService.call(
                new RemoteCallback<OrganizationalUnit>() {
                    @Override
                    public void callback(OrganizationalUnit organizationalUnit) {
                        projectContextChangeEvent.fire(new WorkspaceProjectContextChangeEvent(organizationalUnit));
                        setupLibraryPerspective();
                    }
                }
            ).getDefaultOrganizationalUnit();
        } else {
            setupLibraryPerspective();
        }
    }

    private Boolean setupLibraryPerspective() {
        return libraryService.call(hasProjects -> {
            PortablePreconditions.checkNotNull("libraryPerspective",
                                               libraryPerspective);

            final PlaceRequest placeRequest = new DefaultPlaceRequest(LibraryPlaces.LIBRARY_SCREEN);
            final PartDefinitionImpl part = new PartDefinitionImpl(placeRequest);
            part.setSelectable(false);

            if (projectContext.getActiveWorkspaceProject() == null) {
                projectContextChangeEvent.fire(new WorkspaceProjectContextChangeEvent(projectContext.getActiveOrganizationalUnit()));
            }

            closeLibraryPlaces();
            placeManager.goTo(part,
                              libraryPerspective.getRootPanel());

            setupLibraryBreadCrumbs();

            hideDocks();
        }).hasProjects(projectContext.getActiveOrganizationalUnit());
    }

    public void goToProject(final WorkspaceProject project) {
        if (!project.equals(projectContext.getActiveWorkspaceProject())) {
            if (closeAllPlacesOrNothing()) {
                projectContextChangeEvent.fire(new WorkspaceProjectContextChangeEvent(project,
                                                                                      project.getMainModule()));
                goToProject();
            }
        } else {
            goToProject();
        }
    }

    void goToProject() {
        goToProject(() -> {
            // do nothing.
        });
    }

    private void goToProject(final Command callback) {
        lastViewedProject = projectContext.getActiveWorkspaceProject();
        setupLibraryBreadCrumbs();

        final PartDefinitionImpl part = new PartDefinitionImpl(new DefaultPlaceRequest(LibraryPlaces.PROJECT_SCREEN));
        part.setSelectable(false);

        placeManager.goTo(part,
                          libraryPerspective.getRootPanel());

        if (callback != null) {
            callback.execute();
        }
    }

    public void goToOrgUnitsMetrics() {
        final PlaceRequest metricsScreen = new DefaultPlaceRequest(LibraryPlaces.ORG_UNITS_METRICS_SCREEN);
        final PartDefinitionImpl part = new PartDefinitionImpl(metricsScreen);
        part.setSelectable(false);
        placeManager.goTo(part,
                          libraryPerspective.getRootPanel());
        setupLibraryBreadCrumbsForOrgUnitsMetrics();
    }

    public void goToProjectMetrics() {
        final PlaceRequest metricsScreen = new DefaultPlaceRequest(LibraryPlaces.PROJECT_METRICS_SCREEN);
        final PartDefinitionImpl part = new PartDefinitionImpl(metricsScreen);
        part.setSelectable(false);
        placeManager.goTo(part,
                          libraryPerspective.getRootPanel());
        setupLibraryBreadCrumbsForProjectMetrics();
        projectMetricsEvent.fire(new WorkbenchProjectMetricsEvent(projectContext.getActiveWorkspaceProject()));
    }

    public void goToAsset(final Path path) {

        moduleService.call(new RemoteCallback<org.guvnor.common.services.project.model.Package>() {
            @Override
            public void callback(final org.guvnor.common.services.project.model.Package response) {

                projectContextChangeEvent.fire(new WorkspaceProjectContextChangeEvent(projectContext.getActiveWorkspaceProject(),
                                                                                      projectContext.getActiveModule(),
                                                                                      response));

                final PlaceRequest placeRequest = generatePlaceRequest(path);
                placeManager.goTo(placeRequest);

                if (path != null) {
                    final ObservablePath observablePath = ((PathPlaceRequest) placeRequest).getPath();
                    observablePath.onRename(() -> setupLibraryBreadCrumbsForAsset(observablePath));
                }
            }
        }).resolvePackage(path);
    }

    public void goToTrySamples() {
        Map<String, String> params = new HashMap<>();
        params.put("trySamples",
                   "true");
        final DefaultPlaceRequest placeRequest = new DefaultPlaceRequest(LibraryPlaces.IMPORT_PROJECTS_SCREEN,
                                                                         params);
        final PartDefinitionImpl part = new PartDefinitionImpl(placeRequest);
        part.setSelectable(false);

        closeLibraryPlaces();
        placeManager.goTo(part,
                          libraryPerspective.getRootPanel());
    }

    public void goToImportProjects(final String repositoryUrl) {
        Map<String, String> params = new HashMap<>();
        params.put("title",
                   ts.getTranslation(LibraryConstants.ImportProjects));
        if (repositoryUrl != null) {
            params.put("repositoryUrl",
                       repositoryUrl);
        }
        final DefaultPlaceRequest placeRequest = new DefaultPlaceRequest(LibraryPlaces.IMPORT_PROJECTS_SCREEN,
                                                                         params);
        final PartDefinitionImpl part = new PartDefinitionImpl(placeRequest);
        part.setSelectable(false);

        closeLibraryPlaces();
        placeManager.goTo(part,
                          libraryPerspective.getRootPanel());
        setupLibraryBreadCrumbsForImportProjects(repositoryUrl);
    }

    public void goToSettings() {
        assetDetailEvent.fire(new AssetDetailEvent(projectContext.getActiveWorkspaceProject(),
                                                   null));
    }

    public void goToImportProjectWizard() {
        final String organizationalUnitName = projectContext.getActiveOrganizationalUnit().getName();

        final ExamplesWizard examplesWizard = examplesWizards.get();
        examplesWizard.start();
        examplesWizard.setDefaultTargetOrganizationalUnit(organizationalUnitName);
    }

    public void goToMessages() {
        placeManager.goTo(MESSAGES);
    }

    public void goToPreferences() {

        final PreferenceScopeResolutionStrategyInfo customScopeResolutionStrategy = projectScopedResolutionStrategySupplier.get();

        final PreferencesCentralInitializationEvent initEvent = new PreferencesCentralInitializationEvent("ProjectPreferences",
                                                                                                          customScopeResolutionStrategy,
                                                                                                          null);

        final DefaultPlaceRequest placeRequest = new DefaultPlaceRequest(PreferencesRootScreen.IDENTIFIER);
        final PartDefinitionImpl part = new PartDefinitionImpl(placeRequest);
        part.setSelectable(false);

        placeManager.goTo(part,
                          libraryPerspective.getRootPanel());

        preferencesCentralInitializationEvent.fire(initEvent);
        setupLibraryBreadCrumbsForPreferences();
    }

    PlaceRequest generatePlaceRequest(final Path path) {
        if (path == null) {
            return new DefaultPlaceRequest(PROJECT_SETTINGS);
        }

        return createPathPlaceRequest(path);
    }

    PathPlaceRequest createPathPlaceRequest(final Path path) {
        return new PathPlaceRequest(path);
    }

    void closeLibraryPlaces() {
        closingLibraryPlaces = true;
        LIBRARY_PLACES.forEach(place -> placeManager.closePlace(place));
        closingLibraryPlaces = false;
    }

    boolean closeAllPlacesOrNothing() {
        closingLibraryPlaces = true;
        final boolean placesClosed = placeManager.closeAllPlacesOrNothing();
        closingLibraryPlaces = false;

        return placesClosed;
    }

    void closeAllPlaces() {
        closingLibraryPlaces = true;
        placeManager.closeAllPlaces();
        closingLibraryPlaces = false;
    }

    public void init(final LibraryPerspective libraryPerspective) {
        this.libraryPerspective = libraryPerspective;
    }

    @Override
    public void onChange() {
        if (projectContext.getActiveWorkspaceProject() != null && projectContext.getActivePackage() == null) {
            goToProject();
        }
    }
}
