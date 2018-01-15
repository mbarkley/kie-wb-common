/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.screens.examples.backend.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.enterprise.event.Event;

import org.guvnor.common.services.project.context.WorkspaceProjectContextChangeEvent;
import org.guvnor.common.services.project.events.NewProjectEvent;
import org.guvnor.common.services.project.model.Module;
import org.guvnor.common.services.project.model.POM;
import org.guvnor.common.services.project.model.WorkspaceProject;
import org.guvnor.common.services.project.service.WorkspaceProjectService;
import org.guvnor.common.services.shared.metadata.MetadataService;
import org.guvnor.structure.organizationalunit.OrganizationalUnit;
import org.guvnor.structure.organizationalunit.OrganizationalUnitService;
import org.guvnor.structure.organizationalunit.impl.OrganizationalUnitImpl;
import org.guvnor.structure.repositories.Branch;
import org.guvnor.structure.repositories.Repository;
import org.guvnor.structure.repositories.RepositoryCopier;
import org.guvnor.structure.repositories.RepositoryService;
import org.guvnor.structure.repositories.impl.git.GitRepository;
import org.guvnor.structure.server.config.ConfigGroup;
import org.guvnor.structure.server.config.ConfigType;
import org.guvnor.structure.server.config.ConfigurationFactory;
import org.guvnor.structure.server.repositories.RepositoryFactory;
import org.jboss.errai.security.shared.api.identity.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.screens.examples.model.ExampleOrganizationalUnit;
import org.kie.workbench.common.screens.examples.model.ExampleProject;
import org.kie.workbench.common.screens.examples.model.ExampleRepository;
import org.kie.workbench.common.screens.examples.model.ExamplesMetaData;
import org.kie.workbench.common.services.shared.project.KieModule;
import org.kie.workbench.common.services.shared.project.KieModuleService;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.uberfire.backend.vfs.Path;
import org.uberfire.backend.vfs.PathFactory;
import org.uberfire.io.IOService;
import org.uberfire.mocks.EventSourceMock;
import org.uberfire.rpc.SessionInfo;
import org.uberfire.spaces.Space;
import org.uberfire.spaces.SpacesAPI;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ExamplesServiceImplTest {

    @Mock
    private IOService ioService;

    @Mock
    private ConfigurationFactory configurationFactory;

    @Mock
    private RepositoryFactory repositoryFactory;

    @Mock
    private KieModuleService moduleService;

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private RepositoryCopier repositoryCopier;

    @Mock
    private OrganizationalUnitService ouService;

    @Mock
    private MetadataService metadataService;

    @Spy
    private Event<NewProjectEvent> newProjectEvent = new EventSourceMock<NewProjectEvent>() {
        @Override
        public void fire(final NewProjectEvent event) {
            //Do nothing. Default implementation throws an exception.
        }
    };

    @Mock
    private SessionInfo sessionInfo;

    @Mock
    private User user;

    @Mock
    private WorkspaceProjectService projectService;

    @Mock
    private SpacesAPI spaces;

    private ExamplesServiceImpl service;

    @Before
    public void setup() {
        service = spy(new ExamplesServiceImpl(ioService,
                                              configurationFactory,
                                              repositoryFactory,
                                              moduleService,
                                              repositoryService,
                                              repositoryCopier,
                                              ouService,
                                              projectService,
                                              metadataService,
                                              spaces,
                                              newProjectEvent));
        when(ouService.getOrganizationalUnits()).thenReturn(new HashSet<OrganizationalUnit>() {{
            add(new OrganizationalUnitImpl("ou1Name",
                                           "ou1Owner",
                                           "ou1GroupId"));
            add(new OrganizationalUnitImpl("ou2Name",
                                           "ou2Owner",
                                           "ou2GroupId"));
        }});
        when(moduleService.resolveModule(any(Path.class))).thenAnswer(new Answer<KieModule>() {
            @Override
            public KieModule answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final Path path = (Path) invocationOnMock.getArguments()[0];
                final KieModule module = new KieModule(path,
                                                       path,
                                                       path,
                                                       path,
                                                       path,
                                                       path,
                                                       mock(POM.class));
                return module;
            }
        });
        when(sessionInfo.getId()).thenReturn("sessionId");
        when(sessionInfo.getIdentity()).thenReturn(user);
        when(user.getIdentifier()).thenReturn("user");
        when(configurationFactory.newConfigGroup(any(ConfigType.class),
                                                 anyString(),
                                                 anyString())).thenReturn(mock(ConfigGroup.class));
    }

    @Test
    public void initPlaygroundRepository() {
        //Emulate @PostConstruct mechanism
        service.initPlaygroundRepository();

        final ExampleRepository exampleRepository = service.getPlaygroundRepository();

        assertNotNull(exampleRepository);
    }

    @Test
    public void testGetMetaData() {
        //Emulate @PostConstruct mechanism
        service.initPlaygroundRepository();

        final ExamplesMetaData metaData = service.getMetaData();

        assertNotNull(metaData);
        assertNotNull(metaData.getRepository());

        assertNotNull(metaData.getRepository().getUrl());
    }

    @Test
    public void testGetProjects_NullRepository() {
        final Set<ExampleProject> modules = service.getProjects(null);
        assertNotNull(modules);
        assertEquals(0,
                     modules.size());
    }

    @Test
    public void testGetProjects_NullRepositoryUrl() {
        final Set<ExampleProject> modules = service.getProjects(new ExampleRepository(null));
        assertNotNull(modules);
        assertEquals(0,
                     modules.size());
    }

    @Test
    public void testGetProjects_EmptyRepositoryUrl() {
        final Set<ExampleProject> modules = service.getProjects(new ExampleRepository(""));
        assertNotNull(modules);
        assertEquals(0,
                     modules.size());
    }

    @Test
    public void testGetProjects_WhiteSpaceRepositoryUrl() {
        final Set<ExampleProject> modules = service.getProjects(new ExampleRepository("   "));
        assertNotNull(modules);
        assertEquals(0,
                     modules.size());
    }

    @Test
    public void testGetProjects_DefaultDescription() {
        final Path moduleRoot = mock(Path.class);
        final KieModule module = mock(KieModule.class);
        when(module.getRootPath()).thenReturn(moduleRoot);
        when(module.getModuleName()).thenReturn("module1");
        when(moduleRoot.toURI()).thenReturn("default:///module1");
        when(metadataService.getTags(any(Path.class))).thenReturn(Arrays.asList("tag1",
                                                                                "tag2"));

        final GitRepository repository = makeGitRepository();
        when(repositoryFactory.newRepository(any(ConfigGroup.class))).thenReturn(repository);
        when(moduleService.getAllModules(any(Branch.class))).thenReturn(new HashSet<Module>() {{
            add(module);
        }});

        service.setPlaygroundRepository(mock(ExampleRepository.class));

        final Set<ExampleProject> modules = service.getProjects(new ExampleRepository("https://github.com/guvnorngtestuser1/guvnorng-playground.git"));
        assertNotNull(modules);
        assertEquals(1,
                     modules.size());
        assertTrue(modules.contains(new ExampleProject(moduleRoot,
                                                       "module1",
                                                       "Example 'module1' module",
                                                       Arrays.asList("tag1",
                                                                     "tag2"))));
    }

    @Test
    public void testGetProjects_CustomDescription() {
        final Path moduleRoot = mock(Path.class);
        final KieModule module = mock(KieModule.class);
        when(module.getRootPath()).thenReturn(moduleRoot);
        when(module.getModuleName()).thenReturn("module1");
        when(moduleRoot.toURI()).thenReturn("default:///module1");
        when(ioService.exists(any(org.uberfire.java.nio.file.Path.class))).thenReturn(true);
        when(ioService.readAllString(any(org.uberfire.java.nio.file.Path.class))).thenReturn("This is custom description.\n\n This is a new line.");
        when(metadataService.getTags(any(Path.class))).thenReturn(Arrays.asList("tag1",
                                                                                "tag2"));

        final GitRepository repository = makeGitRepository();
        when(repositoryFactory.newRepository(any(ConfigGroup.class))).thenReturn(repository);
        when(moduleService.getAllModules(any(Branch.class))).thenReturn(new HashSet<Module>() {{
            add(module);
        }});

        final Set<ExampleProject> modules = service.getProjects(new ExampleRepository("https://github.com/guvnorngtestuser1/guvnorng-playground.git"));
        assertNotNull(modules);
        assertEquals(1,
                     modules.size());
        assertTrue(modules.contains(new ExampleProject(moduleRoot,
                                                       "module1",
                                                       "This is custom description. This is a new line.",
                                                       Arrays.asList("tag1",
                                                                     "tag2"))));
    }

    @Test
    public void testGetProjects_PomDescription() {
        final Path moduleRoot = mock(Path.class);
        final POM pom = mock(POM.class);
        final KieModule module = mock(KieModule.class);
        when(pom.getDescription()).thenReturn("pom description");
        when(module.getRootPath()).thenReturn(moduleRoot);
        when(module.getModuleName()).thenReturn("module1");
        when(module.getPom()).thenReturn(pom);
        when(moduleRoot.toURI()).thenReturn("default:///module1");
        when(metadataService.getTags(any(Path.class))).thenReturn(Arrays.asList("tag1",
                                                                                "tag2"));

        final GitRepository repository = makeGitRepository();
        when(repositoryFactory.newRepository(any(ConfigGroup.class))).thenReturn(repository);
        when(moduleService.getAllModules(any(Branch.class))).thenReturn(new HashSet<Module>() {{
            add(module);
        }});

        final Set<ExampleProject> modules = service.getProjects(new ExampleRepository("https://github.com/guvnorngtestuser1/guvnorng-playground.git"));
        assertNotNull(modules);
        assertEquals(1,
                     modules.size());
        assertTrue(modules.contains(new ExampleProject(moduleRoot,
                                                       "module1",
                                                       "pom description",
                                                       Arrays.asList("tag1",
                                                                     "tag2"))));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetupExamples_NullOrganizationalUnit() {
        service.setupExamples(null,
                              mock(List.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetupExamples_NullModule() {
        service.setupExamples(mock(ExampleOrganizationalUnit.class),
                              null);
    }

    @Test(expected = IllegalStateException.class)
    public void testSetupExamples_ZeroModules() {
        service.setupExamples(mock(ExampleOrganizationalUnit.class),
                              Collections.<ExampleProject>emptyList());
    }

    @Test
    public void testSetupExamples_NewOrganizationalUnitNewRepository() {
        final ExampleOrganizationalUnit exOU = mock(ExampleOrganizationalUnit.class);
        final ExampleProject exModule = mock(ExampleProject.class);
        final List<ExampleProject> exModules = new ArrayList<ExampleProject>() {{
            add(exModule);
        }};
        final OrganizationalUnit ou = mock(OrganizationalUnit.class);
        final GitRepository repository = mock(GitRepository.class);
        final Path repositoryRoot = mock(Path.class);
        final Path moduleRoot = mock(Path.class);

        when(exOU.getName()).thenReturn("ou");
        when(exModule.getName()).thenReturn("module");
        when(exModule.getRoot()).thenReturn(moduleRoot);

        when(repository.getDefaultBranch()).thenReturn(Optional.of(new Branch("master",
                                                                              repositoryRoot)));
        when(repositoryRoot.toURI()).thenReturn("default:///");
        when(moduleRoot.toURI()).thenReturn("default:///module");

        when(ouService.getOrganizationalUnit(eq("ou"))).thenReturn(null);
        when(ouService.createOrganizationalUnit(eq("ou"),
                                                eq(""),
                                                eq(""))).thenReturn(ou);
        when(repositoryCopier.copy(eq(ou),
                                   anyString(),
                                   eq(moduleRoot))).thenReturn(repository);
        final WorkspaceProject project = new WorkspaceProject();
        doReturn(project).when(projectService).resolveProject(repository);

        final WorkspaceProjectContextChangeEvent event = service.setupExamples(exOU,
                                                                               exModules);

        assertNull(event.getOrganizationalUnit());
        assertEquals(project,
                     event.getWorkspaceProject());

        verify(ouService,
               times(1)).createOrganizationalUnit(eq("ou"),
                                                  eq(""),
                                                  eq(""));
        verify(repositoryCopier,
               times(1)).copy(eq(ou),
                              anyString(),
                              eq(moduleRoot));
        verify(newProjectEvent,
               times(1)).fire(any(NewProjectEvent.class));
    }

    @Test
    public void testSetupExamples_ProjectCopy() {
        final ExampleOrganizationalUnit exOU = mock(ExampleOrganizationalUnit.class);
        final ExampleProject exProject1 = mock(ExampleProject.class);
        final ExampleProject exProject2 = mock(ExampleProject.class);
        final List<ExampleProject> exProjects = new ArrayList<ExampleProject>() {{
            add(exProject1);
            add(exProject2);
        }};
        final OrganizationalUnit ou = mock(OrganizationalUnit.class);
        final GitRepository repository1 = mock(GitRepository.class);
        final Path repositoryRoot = mock(Path.class);
        final Path module1Root = mock(Path.class);
        final Path module2Root = mock(Path.class);

        when(exOU.getName()).thenReturn("ou");
        when(exProject1.getName()).thenReturn("module1");
        when(exProject1.getRoot()).thenReturn(module1Root);
        when(exProject2.getName()).thenReturn("module2");
        when(exProject2.getRoot()).thenReturn(module2Root);

        when(repository1.getBranch("dev_branch")).thenReturn(Optional.of(new Branch("dev_branch",
                                                                                    repositoryRoot)));
        final Optional<Branch> master = Optional.of(new Branch("master",
                                                               PathFactory.newPath("testFile",
                                                                                   "file:///")));
        when(repository1.getDefaultBranch()).thenReturn(master);

        when(repositoryRoot.toURI()).thenReturn("default:///");
        when(module1Root.toURI()).thenReturn("default:///module1");
        when(module2Root.toURI()).thenReturn("default:///module2");

        when(ouService.getOrganizationalUnit(eq("ou"))).thenReturn(ou);

        doReturn(repository1).when(repositoryCopier).copy(eq(ou),
                                                          anyString(),
                                                          eq(module1Root));

        doReturn(repository1).when(repositoryCopier).copy(eq(ou),
                                                          anyString(),
                                                          eq(module2Root));
        final WorkspaceProject project = new WorkspaceProject();
        doReturn(project).when(projectService).resolveProject(repository1);

        final WorkspaceProjectContextChangeEvent event = service.setupExamples(exOU,
                                                                               exProjects);

        assertNull(event.getOrganizationalUnit());
        assertEquals(project,
                     event.getWorkspaceProject());

        verify(ouService,
               never()).createOrganizationalUnit(eq("ou"),
                                                 eq(""),
                                                 eq(""));
        verify(repositoryCopier,
               times(2)).copy(eq(ou),
                              anyString(),
                              any(Path.class));

        verify(newProjectEvent,
               times(2)).fire(any(NewProjectEvent.class));
    }

    @Test
    public void resolveRepositoryUrlOnWindows() {
        doReturn("\\").when(service).getFileSeparator();

        final String playgroundDirectoryPath = "C:\\folder\\.kie-wb-playground";

        final String repositoryUrl = service.resolveRepositoryUrl(playgroundDirectoryPath);

        assertEquals("file:///C:/folder/.kie-wb-playground",
                     repositoryUrl);
    }

    @Test
    public void resolveRepositoryUrlOnUnix() {
        doReturn("/").when(service).getFileSeparator();

        final String playgroundDirectoryPath = "/home/user/folder/.kie-wb-playground";

        final String repositoryUrl = service.resolveRepositoryUrl(playgroundDirectoryPath);

        assertEquals("file:///home/user/folder/.kie-wb-playground",
                     repositoryUrl);
    }

    @Test
    public void resolveGitRepositoryNotClonedBefore() {
        ExampleRepository playgroundRepository = new ExampleRepository("file:///home/user/folder/.kie-wb-playground");
        service.setPlaygroundRepository(playgroundRepository);

        ConfigGroup configGroup = mock(ConfigGroup.class);
        when(configurationFactory.newConfigGroup(any(ConfigType.class),
                                                 anyString(),
                                                 anyString())).thenReturn(configGroup);

        Repository repository = mock(Repository.class);
        when(repositoryFactory.newRepository(configGroup)).thenReturn(repository);

        Repository result = service.resolveGitRepository(playgroundRepository);

        assertEquals(repository,
                     result);

        verify(repositoryFactory,
               times(1)).newRepository(configGroup);
    }

    @Test
    public void resolveGitRepositoryClonedBefore() {
        ExampleRepository playgroundRepository = new ExampleRepository("file:///home/user/folder/.kie-wb-playground");
        service.setPlaygroundRepository(playgroundRepository);

        GitRepository repository = mock(GitRepository.class);
        Map<String, Object> repositoryEnvironment = new HashMap<>();
        repositoryEnvironment.put("origin",
                                  playgroundRepository.getUrl());
        when(repository.getEnvironment()).thenReturn(repositoryEnvironment);

        service.getClonedRepositories().add(repository);

        Repository result = service.resolveGitRepository(playgroundRepository);

        assertEquals(repository,
                     result);

        verify(repositoryFactory,
               never()).newRepository(any(ConfigGroup.class));
    }

    private GitRepository makeGitRepository() {
        final GitRepository repository = new GitRepository("guvnorng-playground",
                                                           new Space("space"));

        final HashMap<String, Branch> branches = new HashMap<>();
        branches.put("master",
                     new Branch("master",
                                mock(Path.class)));
        repository.setBranches(branches);

        return repository;
    }
}
