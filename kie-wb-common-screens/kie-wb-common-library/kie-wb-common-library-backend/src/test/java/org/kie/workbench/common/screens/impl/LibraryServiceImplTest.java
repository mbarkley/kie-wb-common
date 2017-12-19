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
package org.kie.workbench.common.screens.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ext.uberfire.social.activities.model.SocialUser;
import org.ext.uberfire.social.activities.service.SocialUserRepositoryAPI;
import org.guvnor.common.services.project.context.ProjectContextChangeEvent;
import org.guvnor.common.services.project.model.GAV;
import org.guvnor.common.services.project.model.Module;
import org.guvnor.common.services.project.model.POM;
import org.guvnor.common.services.project.model.Package;
import org.guvnor.common.services.project.model.WorkspaceProject;
import org.guvnor.common.services.project.project.ProjectMigrationService;
import org.guvnor.common.services.project.service.DeploymentMode;
import org.guvnor.common.services.project.service.ProjectService;
import org.guvnor.structure.organizationalunit.OrganizationalUnit;
import org.guvnor.structure.organizationalunit.OrganizationalUnitService;
import org.guvnor.structure.repositories.Branch;
import org.guvnor.structure.repositories.Repository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.screens.examples.model.ExampleOrganizationalUnit;
import org.kie.workbench.common.screens.examples.model.ExampleProject;
import org.kie.workbench.common.screens.examples.model.ExampleRepository;
import org.kie.workbench.common.screens.examples.service.ExamplesService;
import org.kie.workbench.common.screens.explorer.backend.server.ExplorerServiceHelper;
import org.kie.workbench.common.screens.library.api.AssetInfo;
import org.kie.workbench.common.screens.library.api.LibraryInfo;
import org.kie.workbench.common.screens.library.api.OrganizationalUnitRepositoryInfo;
import org.kie.workbench.common.screens.library.api.ProjectAssetsQuery;
import org.kie.workbench.common.screens.library.api.preferences.LibraryInternalPreferences;
import org.kie.workbench.common.screens.library.api.preferences.LibraryOrganizationalUnitPreferences;
import org.kie.workbench.common.screens.library.api.preferences.LibraryPreferences;
import org.kie.workbench.common.screens.library.api.preferences.LibraryProjectPreferences;
import org.kie.workbench.common.services.refactoring.model.index.terms.valueterms.ValueIndexTerm;
import org.kie.workbench.common.services.refactoring.model.query.RefactoringPageRequest;
import org.kie.workbench.common.services.refactoring.model.query.RefactoringPageRow;
import org.kie.workbench.common.services.refactoring.service.RefactoringQueryService;
import org.kie.workbench.common.services.shared.project.KieModuleService;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.uberfire.backend.vfs.Path;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.file.NoSuchFileException;
import org.uberfire.paging.PageResponse;
import org.uberfire.rpc.SessionInfo;
import org.uberfire.security.authz.AuthorizationManager;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LibraryServiceImplTest {

    @Mock
    private OrganizationalUnitService ouService;

    @Mock
    private ProjectService projectService;

    @Mock
    private KieModuleService moduleService;

    @Mock
    private LibraryPreferences preferences;

    @Mock
    private LibraryInternalPreferences internalPreferences;

    @Mock
    private ExplorerServiceHelper explorerServiceHelper;

    @Mock
    private ExamplesService examplesService;

    @Mock
    private RefactoringQueryService refactoringQueryService;

    @Mock
    private IOService ioService;

    @Mock
    private SocialUserRepositoryAPI socialUserRepositoryAPI;

    @Mock
    private OrganizationalUnit ou1;

    @Mock
    private OrganizationalUnit ou2;

    @Mock
    private Repository repo1;

    @Mock
    private Repository repo2Default;

    @Captor
    private ArgumentCaptor<RefactoringPageRequest> pageRequestArgumentCaptor;

    @Captor
    private ArgumentCaptor<POM> pomArgumentCaptor;

    private LibraryServiceImpl libraryService;
    private List<OrganizationalUnit> ous;
    private Set<Module> modulesMock;

    private Branch makeBranch(final String branchName) {
        return new Branch(branchName,
                          mock(Path.class));
    }

    @Before
    public void setup() {
        ous = Arrays.asList(ou1,
                            ou2);
        when(ouService.getOrganizationalUnits()).thenReturn(ous);
        when(ou1.getIdentifier()).thenReturn("ou1");
        when(ou2.getIdentifier()).thenReturn("ou2");
        when(repo1.getAlias()).thenReturn("repo_created_by_user");
        when(repo1.getBranches()).thenReturn(Arrays.asList(makeBranch("repo1-branch1"),
                                                           makeBranch("repo1-branch2")));
        when(repo2Default.getAlias()).thenReturn("ou2-repo-alias");
        when(repo2Default.getBranches()).thenReturn(Collections.singletonList(makeBranch("repo2-branch1")));
        when(ou2.getRepositories()).thenReturn(Arrays.asList(repo1,
                                                             repo2Default));

        modulesMock = new HashSet<>();
        modulesMock.add(mock(Module.class));
        modulesMock.add(mock(Module.class));
        modulesMock.add(mock(Module.class));

        when(preferences.getOrganizationalUnitPreferences()).thenReturn(spy(new LibraryOrganizationalUnitPreferences()));
        when(preferences.getProjectPreferences()).thenReturn(spy(new LibraryProjectPreferences()));

        libraryService = spy(new LibraryServiceImpl(ouService,
                                                    refactoringQueryService,
                                                    preferences,
                                                    mock(AuthorizationManager.class),
                                                    mock(SessionInfo.class),
                                                    explorerServiceHelper,
                                                    projectService,
                                                    moduleService,
                                                    examplesService,
                                                    mock(ProjectMigrationService.class),
                                                    ioService,
                                                    internalPreferences,
                                                    socialUserRepositoryAPI
        ));
    }

    @Test
    public void getDefaultOrganizationalUnitRepositoryInfoTest() {
        final OrganizationalUnitRepositoryInfo info = mock(OrganizationalUnitRepositoryInfo.class);
        doReturn(info).when(libraryService).getOrganizationalUnitRepositoryInfo(any(OrganizationalUnit.class));

        assertEquals(info,
                     libraryService.getDefaultOrganizationalUnitRepositoryInfo());
    }

    @Test
    public void getOrganizationalUnitRepositoryInfoForNullOrganizationalUnitTest() {
        assertNull(libraryService.getOrganizationalUnitRepositoryInfo(null));
    }

    @Test
    public void getLibraryInfoTest() {
        final Path path = mockPath("file://the_project");
        final Project project = mock(Project.class);
        when(project.getRootPath()).thenReturn(path);
        doReturn(true).when(ioService).exists(any());

        final Repository repository = mock(Repository.class);
        final Set<Project> projects = new HashSet<>();
        projects.add(project);
        doReturn(projects).when(kieProjectService).getProjects(eq(repository),
                                                               anyString());

        final LibraryInfo libraryInfo = libraryService.getLibraryInfo(ou1);

        assertEquals(new HashSet<>(projects),
                     libraryInfo.getProjects());
    }

    @Test
    public void newModuleTest() {
        when(preferences.getOrganizationalUnitPreferences().getName()).thenReturn("ou2");
        when(preferences.getOrganizationalUnitPreferences().getAliasInSingular()).thenReturn("team");
        when(preferences.getProjectPreferences().getBranch()).thenReturn("master");
        when(preferences.getProjectPreferences().getVersion()).thenReturn("1.0");

        final OrganizationalUnit organizationalUnit = mock(OrganizationalUnit.class);
        when(organizationalUnit.getDefaultGroupId()).thenReturn("ouGroupID");

        libraryService.createProject("Module Name!",
                                     organizationalUnit,
                                     "description",
                                     DeploymentMode.VALIDATED);

        verify(projectService).newProject(eq(organizationalUnit),
                                          pomArgumentCaptor.capture(),
                                          any());

        final POM pom = pomArgumentCaptor.getValue();
        assertEquals("Module Name!",
                     pom.getName());
        assertEquals("ouGroupID",
                     pom.getGav().getGroupId());
        assertEquals("ModuleName",
                     pom.getGav().getArtifactId());
        assertEquals("description",
                     pom.getDescription());
    }

    @Test
    public void thereIsNotAModuleInTheWorkbenchTest() {

        final Boolean thereIsAModuleInTheWorkbench = libraryService.thereIsAProjectInTheWorkbench();

        assertFalse(thereIsAModuleInTheWorkbench);

        verify(projectService,
               times(1)).getAllProjects();
    }

    @Test
    public void thereIsAModuleInTheWorkbenchTest() {
        Set<WorkspaceProject> projects = new HashSet<>();
        projects.add(new WorkspaceProject(ou1,
                                          repo1,
                                          new Branch("master",
                                                     mock(Path.class)),
                                          mock(Module.class)));
        doReturn(projects).when(projectService).getAllProjects();

        final Boolean thereIsAModuleInTheWorkbench = libraryService.thereIsAProjectInTheWorkbench();

        assertTrue(thereIsAModuleInTheWorkbench);

        verify(projectService,
               times(1)).getAllProjects();
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNullModuleAssetsTest() {
        libraryService.getProjectAssets(null);
    }

    @Test
    public void emptyFirstPage() throws Exception {
        final WorkspaceProject project = mock(WorkspaceProject.class);
        final Branch branch = mock(Branch.class);
        final Path path = mock(Path.class);

        when(project.getBranch()).thenReturn(branch);
        when(branch.getPath()).thenReturn(path);
        when(path.toURI()).thenReturn("file://a/b/c");

        doReturn(true).when(ioService).exists(any());

        final ProjectAssetsQuery query = new ProjectAssetsQuery(project,
                                                                "",
                                                                0,
                                                                10);

        final PageResponse<RefactoringPageRow> pageRowPageResponse = new PageResponse<>();
        pageRowPageResponse.setPageRowList(new ArrayList<>());
        when(refactoringQueryService.query(any(RefactoringPageRequest.class))).thenReturn(pageRowPageResponse);

        libraryService.getProjectAssets(query);

        verify(refactoringQueryService).query(pageRequestArgumentCaptor.capture());

        final RefactoringPageRequest pageRequest = pageRequestArgumentCaptor.getValue();

        assertEquals(FindAllLibraryAssetsQuery.NAME,
                     pageRequest.getQueryName());
        assertEquals(1,
                     pageRequest.getQueryTerms().size());

        assertEquals("file://a/b/c",
                     pageRequest.getQueryTerms().iterator().next().getValue());

        assertEquals(0,
                     pageRequest.getStartRowIndex());
        assertEquals(10,
                     (int) pageRequest.getPageSize());
    }

    @Test
    public void queryWithAFilter() throws Exception {

        final WorkspaceProject project = mock(WorkspaceProject.class);
        final Branch branch = mock(Branch.class);
        final Path path = mockPath("file://the_project");

        when(project.getBranch()).thenReturn(branch);
        when(branch.getPath()).thenReturn(path);

        doReturn(true).when(ioService).exists(any());

        final ProjectAssetsQuery query = new ProjectAssetsQuery(project,
                                                                "helloo",
                                                                10,
                                                                20);

        final PageResponse<RefactoringPageRow> pageRowPageResponse = new PageResponse<>();
        pageRowPageResponse.setPageRowList(new ArrayList<>());
        when(refactoringQueryService.query(any(RefactoringPageRequest.class))).thenReturn(pageRowPageResponse);

        libraryService.getProjectAssets(query);

        verify(refactoringQueryService).query(pageRequestArgumentCaptor.capture());

        final RefactoringPageRequest pageRequest = pageRequestArgumentCaptor.getValue();

        assertEquals(FindAllLibraryAssetsQuery.NAME,
                     pageRequest.getQueryName());
        assertEquals(2,
                     pageRequest.getQueryTerms().size());

        assertQueryTermsContains(pageRequest.getQueryTerms(),
                                 "file://the_project");
        assertQueryTermsContains(pageRequest.getQueryTerms(),
                                 "*helloo*");

        assertEquals(10,
                     pageRequest.getStartRowIndex());
        assertEquals(20,
                     (int) pageRequest.getPageSize());
    }

    @Test
    public void queryAnItemThatIsInLuceneIndexButAlreadyDeletedFromGitRepository() throws Exception {

        final Path path = mockPath("file://the_project");

        final WorkspaceProject project = mock(WorkspaceProject.class);
        final Branch branch = mock(Branch.class);

        when(project.getBranch()).thenReturn(branch);
        when(branch.getPath()).thenReturn(path);

        doReturn(true).when(ioService).exists(any());

        final ProjectAssetsQuery query = new ProjectAssetsQuery(project,
                                                                "",
                                                                10,
                                                                20);

        final PageResponse<RefactoringPageRow> pageRowPageResponse = new PageResponse<>();
        final ArrayList<RefactoringPageRow> assetPageRowList = new ArrayList<>();
        final RefactoringPageRow pageRow = mock(RefactoringPageRow.class);
        final Path filePath = mockPath("file://the_project/delete.me");
        when(filePath.getFileName()).thenReturn("delete.me");
        when(pageRow.getValue()).thenReturn(filePath);
        assetPageRowList.add(pageRow);

        pageRowPageResponse.setPageRowList(assetPageRowList);
        when(refactoringQueryService.query(any(RefactoringPageRequest.class))).thenReturn(pageRowPageResponse);

        when(ioService.readAttributes(any())).thenThrow(new NoSuchFileException());

        final List<AssetInfo> projectAssets = libraryService.getProjectAssets(query);

        assertTrue(projectAssets.isEmpty());
    }

    private Path mockPath(final String uri) {
        final Path path = mock(Path.class);
        when(path.toURI()).thenReturn(uri);
        return path;
    }

    private void assertQueryTermsContains(final Set<ValueIndexTerm> terms,
                                          final String value) {
        assertTrue(terms.stream().filter((t) -> t.getValue().equals(value)).findFirst().isPresent());
    }

    @Test
    public void assertLoadPreferences() {
        libraryService.getPreferences();

        verify(preferences).load();
    }

    @Test
    public void hasProjectsTest() {
        final Path path = mockPath("file://the_project");
        final Project project = mock(Project.class);
        when(project.getRootPath()).thenReturn(path);
        doReturn(true).when(ioService).exists(any());

        final Repository emptyRepository = mock(Repository.class);
        final Repository repository = mock(Repository.class);
        final Set<Project> projects = new HashSet<>();
        projects.add(project);
        doReturn(projects).when(kieProjectService).getProjects(eq(repository),
                                                               anyString());

        assertTrue(libraryService.hasProjects(ou1));
    }

    @Test
    public void hasAssetsTest() {
        doReturn(true).when(ioService).exists(any());

        final Package package1 = mock(Package.class);
        final Module project1 = mock(Module.class);
        doReturn(package1).when(moduleService).resolveDefaultPackage(project1);
        doReturn(true).when(explorerServiceHelper).hasAssets(package1);

        final Package package2 = mock(Package.class);
        final Module project2 = mock(Module.class);
        doReturn(package2).when(moduleService).resolveDefaultPackage(project2);
        doReturn(false).when(explorerServiceHelper).hasAssets(package2);

        assertTrue(libraryService.hasAssets(new WorkspaceProject(mock(OrganizationalUnit.class),
                                                                 mock(Repository.class),
                                                                 mock(Branch.class),
                                                                 project1)));
        assertFalse(libraryService.hasAssets(new WorkspaceProject(mock(OrganizationalUnit.class),
                                                                  mock(Repository.class),
                                                                  mock(Branch.class),
                                                                  project2)));
    }

    @Test
    public void unexistentProjectDosNotHaveAssetsTest() {
        final Path path = mockPath("file://the_project");
        final Package package1 = mock(Package.class);
        final Project project1 = mock(Project.class);

        when(project1.getRootPath()).thenReturn(path);
        doReturn(false).when(ioService).exists(any());
        doReturn(package1).when(projectService).resolveDefaultPackage(project1);
        doReturn(true).when(explorerServiceHelper).hasAssets(package1);

        assertFalse(libraryService.hasAssets(project1));
    }

    @Test
    public void getCustomExampleProjectsTest() {
        System.setProperty("org.kie.project.examples.repository.url",
                           "importProjectsUrl");

        final Set<ExampleProject> exampleProjects = new HashSet<>();
        exampleProjects.add(mock(ExampleProject.class));
        doReturn(exampleProjects).when(examplesService).getProjects(new ExampleRepository("importProjectsUrl"));

        final Set<ExampleProject> loadedExampleProjects = libraryService.getExampleProjects();

        assertEquals(exampleProjects,
                     loadedExampleProjects);
    }

    @Test
    public void getDefaultExampleProjectsTest() {
        System.setProperty("org.kie.project.examples.repository.url",
                           "");

        final ExampleRepository playgroundRepository = new ExampleRepository("playgroundRepositoryUrl");
        doReturn(playgroundRepository).when(examplesService).getPlaygroundRepository();

        final Set<ExampleProject> exampleProjects = new HashSet<>();
        exampleProjects.add(mock(ExampleProject.class));
        doReturn(exampleProjects).when(examplesService).getProjects(playgroundRepository);

        final Set<ExampleProject> loadedExampleProjects = libraryService.getExampleProjects();

        assertEquals(exampleProjects,
                     loadedExampleProjects);
    }

    @Test
    public void importProjectTest() {
        final OrganizationalUnit organizationalUnit = mock(OrganizationalUnit.class);
        final ExampleProject exampleProject = mock(ExampleProject.class);

        final WorkspaceProject project = mock(WorkspaceProject.class);
        final Module module = mock(Module.class);
        doReturn(module).when(project).getMainModule();

        final ProjectContextChangeEvent projectContextChangeEvent = mock(ProjectContextChangeEvent.class);
        doReturn(project).when(projectContextChangeEvent).getWorkspaceProject();
        doReturn(projectContextChangeEvent).when(examplesService).setupExamples(any(ExampleOrganizationalUnit.class),
                                                                                anyList());

        final WorkspaceProject importedProject = libraryService.importProject(organizationalUnit,
                                                                              exampleProject);

        assertEquals(module,
                     importedProject.getMainModule());
    }

    @Test
    public void importDefaultProjectTest() {
        final Repository repository = mock(Repository.class);
        when(repository.getAlias()).thenReturn("example");
        final OrganizationalUnit organizationalUnit = mock(OrganizationalUnit.class);
        when(organizationalUnit.getName()).thenReturn("ou");
        when(organizationalUnit.getIdentifier()).thenReturn("ou");
        when(organizationalUnit.getRepositories()).thenReturn(singletonList(repository));
        when(ouService.getOrganizationalUnits()).thenReturn(singletonList(organizationalUnit));

        final ExampleProject exampleProject = mock(ExampleProject.class);
        doReturn("example").when(exampleProject).getName();

        final WorkspaceProject project = mock(WorkspaceProject.class);
        final Module module = mock(Module.class);
        doReturn(module).when(project).getMainModule();
        final ProjectContextChangeEvent projectContextChangeEvent = mock(ProjectContextChangeEvent.class);
        doReturn(project).when(projectContextChangeEvent).getWorkspaceProject();
        doReturn(projectContextChangeEvent).when(examplesService).setupExamples(any(ExampleOrganizationalUnit.class),
                                                                                anyList());

        final WorkspaceProject importedProject = libraryService.importProject(exampleProject);

        assertEquals(module,
                     importedProject.getMainModule());
        verify(examplesService).setupExamples(new ExampleOrganizationalUnit(organizationalUnit.getName()),
                                              singletonList(exampleProject));
    }

    @Test
    public void createPOM() {
        final OrganizationalUnit organizationalUnit = mock(OrganizationalUnit.class);
        when(organizationalUnit.getDefaultGroupId()).thenReturn("ouGroupID");

        when(preferences.getProjectPreferences().getVersion()).thenReturn("1.0");
        when(preferences.getProjectPreferences().getDescription()).thenReturn("desc");

        GAV gav = libraryService.createGAV("proj",
                                           organizationalUnit);
        POM proj = libraryService.createPOM("proj",
                                            "description",
                                            gav);

        assertEquals("proj",
                     proj.getName());
        assertEquals("description",
                     proj.getDescription());
        assertEquals(gav,
                     proj.getGav());
    }

    @Test
    public void createGAV() {
        final OrganizationalUnit organizationalUnit = mock(OrganizationalUnit.class);
        when(organizationalUnit.getDefaultGroupId()).thenReturn("ouGroupID");

        when(preferences.getProjectPreferences().getVersion()).thenReturn("1.0");

        GAV gav = libraryService.createGAV("proj",
                                           organizationalUnit);

        assertEquals(organizationalUnit.getDefaultGroupId(),
                     gav.getGroupId());
        assertEquals("proj",
                     gav.getArtifactId());
        assertEquals(preferences.getProjectPreferences().getVersion(),
                     gav.getVersion());
    }

    @Test
    public void getSecondaryDefaultRepositoryNameTest() {
        assertEquals("myalias-myrepo",
                     libraryService.getSecondaryDefaultRepositoryName(getOrganizationalUnit("myalias")));
        assertEquals("my-alias-myrepo",
                     libraryService.getSecondaryDefaultRepositoryName(getOrganizationalUnit("my alias")));
    }

    @Test
    public void getAllUsersTest() {
        List<SocialUser> allUsers = new ArrayList<>();
        allUsers.add(new SocialUser("system"));
        allUsers.add(new SocialUser("admin"));
        allUsers.add(new SocialUser("user"));
        doReturn(allUsers).when(socialUserRepositoryAPI).findAllUsers();

        final List<SocialUser> users = libraryService.getAllUsers();

        assertEquals(2,
                     users.size());
        assertEquals("admin",
                     users.get(0).getUserName());
        assertEquals("user",
                     users.get(1).getUserName());
    }

    private void organizationalUnitWithSecondaryRepositoryExistent(final OrganizationalUnit organizationalUnit,
                                                                   final String repositoryIdentifier) {
        final OrganizationalUnitRepositoryInfo info5 = libraryService.getOrganizationalUnitRepositoryInfo(organizationalUnit);
        assertOrganizationalUnitRepositoryInfo(info5,
                                               4,
                                               organizationalUnit.getIdentifier(),
                                               1,
                                               repositoryIdentifier);
    }

    private void organizationalUnitWithNoRepositoriesCreatesTheSecondaryRepositorySincePrimaryAlreadyExists(final OrganizationalUnit organizationalUnit,
                                                                                                            final String repositoryIdentifier,
                                                                                                            final Repository alreadyExistentPrimaryRepository) {
        doReturn(alreadyExistentPrimaryRepository).when(repositoryService).getRepository("repository1");
        final OrganizationalUnitRepositoryInfo info = libraryService.getOrganizationalUnitRepositoryInfo(organizationalUnit);
        assertOrganizationalUnitRepositoryInfo(info,
                                               4,
                                               organizationalUnit.getIdentifier(),
                                               0,
                                               repositoryIdentifier);
    }

    private void organizationalUnitWithNoRepositoriesCreatesATertiaryRepositorySincePrimaryAndSecondaryAlreadyExists(final OrganizationalUnit organizationalUnit,
                                                                                                                     final String repositoryIdentifier,
                                                                                                                     final Repository alreadyExistentPrimaryRepository,
                                                                                                                     final Repository alreadyExistentSecondaryRepository) {
        doReturn(alreadyExistentPrimaryRepository).when(repositoryService).getRepository("repository1");
        doReturn(alreadyExistentSecondaryRepository).when(repositoryService).getRepository("organizationalUnit3-repository1");

        final OrganizationalUnitRepositoryInfo info = libraryService.getOrganizationalUnitRepositoryInfo(organizationalUnit);
        assertOrganizationalUnitRepositoryInfo(info,
                                               4,
                                               organizationalUnit.getIdentifier(),
                                               0,
                                               repositoryIdentifier);
    }

    private void organizationalUnitWithNoRepositoriesCreatesThePrimaryRepository(final OrganizationalUnit organizationalUnit,
                                                                                 final String repositoryIdentifier) {
        doReturn(null).when(repositoryService).getRepository("repository1");
        final OrganizationalUnitRepositoryInfo info = libraryService.getOrganizationalUnitRepositoryInfo(organizationalUnit);
        assertOrganizationalUnitRepositoryInfo(info,
                                               4,
                                               organizationalUnit.getIdentifier(),
                                               0,
                                               repositoryIdentifier);
    }

    private void organizationalUnitWithTwoRepositoriesSelectsTheFirst(final OrganizationalUnit organizationalUnit,
                                                                      final String repositoryIdentifier) {
        final OrganizationalUnitRepositoryInfo info = libraryService.getOrganizationalUnitRepositoryInfo(organizationalUnit);
        assertOrganizationalUnitRepositoryInfo(info,
                                               4,
                                               organizationalUnit.getIdentifier(),
                                               2,
                                               repositoryIdentifier);
    }

    private void organizationalUnitWithPrimaryRepositoryExistent(final OrganizationalUnit organizationalUnit,
                                                                 final String repositoryIdentifier) {
        final OrganizationalUnitRepositoryInfo info = libraryService.getOrganizationalUnitRepositoryInfo(organizationalUnit);
        assertOrganizationalUnitRepositoryInfo(info,
                                               4,
                                               organizationalUnit.getIdentifier(),
                                               1,
                                               repositoryIdentifier);
    }

    private void assertOrganizationalUnitRepositoryInfo(final OrganizationalUnitRepositoryInfo info,
                                                        final int totalOfOrganizationalUnits,
                                                        final String organizationalUnitIdentifier,
                                                        final int totalOfRepositories,
                                                        final String repositoryAlias) {
        assertEquals(totalOfOrganizationalUnits,
                     info.getOrganizationalUnits().size());
        assertEquals(organizationalUnitIdentifier,
                     info.getSelectedOrganizationalUnit().getIdentifier());
        assertEquals(totalOfRepositories,
                     info.getRepositories().size());
        assertEquals(repositoryAlias,
                     info.getSelectedRepository().getAlias());
    }

    private Repository getRepository(final String alias) {
        final Repository repository1 = mock(Repository.class);
        doReturn(alias).when(repository1).getAlias();

        return repository1;
    }

    private OrganizationalUnit getOrganizationalUnit(final String identifier,
                                                     final Repository... repositories) {
        final OrganizationalUnit organizationalUnit = mock(OrganizationalUnit.class);
        doReturn(identifier).when(organizationalUnit).getIdentifier();
        doReturn(Arrays.asList(repositories)).when(organizationalUnit).getRepositories();

        return organizationalUnit;
    }
}
