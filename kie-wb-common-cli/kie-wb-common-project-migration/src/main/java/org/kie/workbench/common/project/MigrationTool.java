/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.workbench.common.project;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.cli.ParseException;
import org.guvnor.common.services.project.model.WorkspaceProject;
import org.guvnor.common.services.project.project.WorkspaceProjectMigrationService;
import org.guvnor.common.services.project.service.WorkspaceProjectService;
import org.guvnor.structure.repositories.EnvironmentParameters;
import org.guvnor.structure.repositories.Repository;
import org.guvnor.structure.repositories.RepositoryService;
import org.guvnor.structure.server.config.ConfigGroup;
import org.guvnor.structure.server.config.ConfigItem;
import org.guvnor.structure.server.config.ConfigType;
import org.guvnor.structure.server.config.ConfigurationService;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.kie.workbench.common.project.cli.ToolConfig;
import org.uberfire.java.nio.fs.jgit.JGitFileSystemProviderConfiguration;

@ApplicationScoped
public class MigrationTool {

    private static final String MIGRATION_TOOL_NAME = "migration-tool";

    private static String[] systemRepos = {
                                           "system.git",
                                           "datasets.git",
                                           "datasources.git",
                                           "plugins.git",
                                           "preferences.git",
                                           "security.git"
    };

    public static void main(String[] args) {
        // TODO find appropriate constants
        ToolConfig config = parseToolConfigOrExit(args);
        Path niogitDir = config.getTarget();

        validateTarget(niogitDir);
        maybePromptForBackupAndExit(config, niogitDir);

        Path systemSpace = niogitDir.resolve("system");
        ensureSystemSpaceOrExit(systemSpace);
        moveSystemRepos(niogitDir, systemSpace);

        configureProperties(niogitDir);
        migrateAndExit(niogitDir);
    }

    private static void maybePromptForBackupAndExit(ToolConfig config, Path niogitDir) {
        if (!config.isBatch() && !promptForBackup(niogitDir)) {
            System.exit(0);
        }
    }

    private static boolean promptForBackup(Path niogitDir) {
        Console console = System.console();
        console.format("WARNING: Please ensure that you have made backups of the directory [%s] before proceeding.\n", niogitDir);
        Collection<String> validResponses = Arrays.asList("yes", "no");
        String response;
        do {
            response = console.readLine("Do you wish to continue? [yes/no]: ").toLowerCase();
        } while (!validResponses.contains(response));

        return "yes".equals(response);
    }

    private static void validateTarget(Path niogitDir) {
        Optional<String> errorMessage = Optional.empty();
        try {
            File dirFile = niogitDir.toFile();
            if (!dirFile.exists()) {
                errorMessage = Optional.of(String.format("The target path does not exist. Given: %s", niogitDir));
            }
            else if (!dirFile.isDirectory()) {
                errorMessage = Optional.of(String.format("The target path is not a directory. Given: %s", niogitDir));
            }
        } catch (UnsupportedOperationException e) {
            errorMessage = Optional.of(String.format("The target path must be a file. Given: %s", niogitDir));
        }

        errorMessage.ifPresent(msg -> {
            System.err.println(msg);
            System.exit(1);
        });
    }

    private static ToolConfig parseToolConfigOrExit(String[] args) {
        ToolConfig config = null;
        try {
            config = ToolConfig.parse(args);
        } catch (ParseException e) {
            System.err.printf("Could not parse arguments: %s\n", e.getMessage());
            ToolConfig.printHelp(System.err, MIGRATION_TOOL_NAME);
            System.exit(1);
        }
        return config;
    }

    private static void migrateAndExit(Path niogitDir) {
        int exitStatus = 0;
        try (WeldContainer container = startContainer()) {
            MigrationTool tool = loadToolInstance(container);
            tool.migrateAllProjects(niogitDir);
        } catch (Throwable t) {
            exitStatus = 1;
            t.printStackTrace(System.err);
        }

        System.exit(exitStatus);
    }

    @Inject
    private WorkspaceProjectService projectService;

    @Inject
    private ConfigurationService configService;

    @Inject
    private WorkspaceProjectMigrationService projectMigrationService;

    @Inject
    private RepositoryService repoService;

    public void migrateAllProjects(Path niogitDir) {
        List<ConfigGroup> orgUnitConfigs = configService.getConfiguration(ConfigType.ORGANIZATIONAL_UNIT);
        List<ConfigGroup> repoConfigs = configService.getConfiguration(ConfigType.REPOSITORY);
        Map<String, String> orgUnitByRepo = getOrgUnitsByRepo(orgUnitConfigs);

        addSpacesToRepoConfigs(configService, orgUnitByRepo, repoConfigs);
        migrateReposToSpaces(niogitDir, orgUnitConfigs, orgUnitByRepo);
        migrateProjectsToIndividualRepos();

        System.out.println("Done");
    }

    private void migrateProjectsToIndividualRepos() {
        Collection<WorkspaceProject> allProjects = projectService.getAllWorkspaceProjects();
        System.out.printf("Found %s projects\n", allProjects.size());
        Set<Repository> cleanup = new LinkedHashSet<>();
        allProjects
            .forEach(proj -> {
                System.out.printf("Migrating %s...\n", proj.getName());
                cleanup.add(proj.getRepository());
                projectMigrationService.migrate(proj);
            });
        cleanup.forEach(repo -> {
            System.out.printf("Removing migrated repository, %s...\n", repo.getAlias());
            repoService.removeRepository(repo.getAlias());
        });
    }

    private static void migrateReposToSpaces(Path niogitDir, List<ConfigGroup> orgUnitConfigs, Map<String, String> orgUnitByRepo) {
        createSpaceDirs(niogitDir, orgUnitConfigs);
        moveRepos(niogitDir, orgUnitByRepo);
    }

    private static void moveRepos(Path niogitDir, Map<String, String> orgUnitByRepo) {
        orgUnitByRepo
            .forEach((repo, ou) -> {
                String repoFolderName = repo + ".git";
                Path oldRepo = niogitDir.resolve(repoFolderName);
                Path newRepo = niogitDir.resolve(ou).resolve(repoFolderName);
                try {
                    Files.move(oldRepo, newRepo);
                } catch (IOException e) {
                    System.err.printf("Unable to move %s.\n", oldRepo);
                    e.printStackTrace(System.err);
                }
            });
    }

    private static void createSpaceDirs(Path niogitDir, List<ConfigGroup> orgUnitConfigs) {
        orgUnitConfigs
            .stream()
            .map(group -> group.getName())
            .forEach(ou -> {
                Path ouSpace = niogitDir.resolve(ou);
                ouSpace.toFile().mkdir();
            });
    }

    private static void addSpacesToRepoConfigs(ConfigurationService configService, Map<String, String> orgUnitByRepo, List<ConfigGroup> repoConfigs) {
        configService.startBatch();
        repoConfigs.forEach(group -> {
            String space = orgUnitByRepo.get(group.getName());
            if (space != null) {
                ConfigItem<Object> item = new ConfigItem<>();
                item.setName(EnvironmentParameters.SPACE);
                item.setValue(space);
                group.setConfigItem(item);
                configService.updateConfiguration(group);
            }
        });
        configService.endBatch();
    }

    private static Map<String, String> getOrgUnitsByRepo(List<ConfigGroup> orgUnitConfigs) {
        Map<String, String> orgUnitByRepo = new LinkedHashMap<>();
        orgUnitConfigs
                      .stream()
                      .forEach(group -> {
                          @SuppressWarnings("unchecked")
                          ConfigItem<List<String>> repos = group.getConfigItem("repositories");
                          Optional.ofNullable(repos)
                                  .map(r -> r.getValue())
                                  .ifPresent(r -> r.forEach(repo -> orgUnitByRepo.put(repo, group.getName())));
                      });
        return orgUnitByRepo;
    }

    private static void moveSystemRepos(Path niogitDir, Path systemSpace) {
        Arrays
        .stream(systemRepos)
        .forEach(oldRepoRelPath -> {
            Path oldRepoAbsPath = niogitDir.resolve(oldRepoRelPath);
            if (oldRepoAbsPath.toFile().exists()) {
                tryMovingRepo(systemSpace, oldRepoRelPath, oldRepoAbsPath);
            }
        });
    }

    private static void tryMovingRepo(Path systemSpace, String oldRepoRelPath, Path oldRepoAbsPath) {
        try {
            Files.move(oldRepoAbsPath, systemSpace.resolve(oldRepoRelPath));
        } catch (IOException e) {
            System.err.println("Unable to move " + oldRepoAbsPath);
            e.printStackTrace(System.err);
        }
    }

    private static void ensureSystemSpaceOrExit(Path systemSpace) {
        if (!systemSpace.toFile().exists()) {
            try {
                Files.createDirectory(systemSpace);
            } catch (IOException e) {
                throw new RuntimeException("Could not create system space at " + systemSpace, e);
            }
        }
        else if (!systemSpace.toFile().isDirectory()) {
            throw new RuntimeException("Cannot create system space because of file: " + systemSpace);
        }
    }

    private static void configureProperties(Path niogitDir) {
        System.setProperty(JGitFileSystemProviderConfiguration.GIT_NIO_DIR, niogitDir.getParent().toString());
        System.setProperty(JGitFileSystemProviderConfiguration.GIT_NIO_DIR_NAME, niogitDir.getFileName().toString());
        System.setProperty(JGitFileSystemProviderConfiguration.GIT_DAEMON_ENABLED, "false");
        System.setProperty(JGitFileSystemProviderConfiguration.GIT_SSH_ENABLED, "false");
    }

    private static WeldContainer startContainer() {
        Weld weld = new Weld();
        return weld.initialize();
    }

    private static MigrationTool loadToolInstance(WeldContainer container) {
        return container.instance().select(MigrationTool.class).get();
    }

}
