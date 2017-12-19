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
package org.kie.workbench.common.screens.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.guvnor.common.services.project.model.Package;
import org.kie.workbench.common.screens.library.api.index.LibraryFileNameIndexTerm;
import org.kie.workbench.common.screens.library.api.index.LibraryModuleRootPathIndexTerm;
import org.kie.workbench.common.services.refactoring.KPropertyImpl;
import org.kie.workbench.common.services.refactoring.backend.server.indexing.AbstractFileIndexer;
import org.kie.workbench.common.services.refactoring.backend.server.indexing.DefaultIndexBuilder;
import org.kie.workbench.common.services.refactoring.backend.server.util.KObjectUtil;
import org.kie.workbench.common.services.refactoring.model.index.terms.ModuleNameIndexTerm;
import org.kie.workbench.common.services.refactoring.model.index.terms.PackageNameIndexTerm;
import org.kie.workbench.common.services.shared.project.KieModule;
import org.kie.workbench.common.services.shared.project.KieModuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.ext.metadata.backend.lucene.fields.FieldFactory;
import org.uberfire.ext.metadata.model.KObject;
import org.uberfire.ext.metadata.model.KObjectKey;
import org.uberfire.ext.metadata.model.KProperty;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.file.Path;

@ApplicationScoped
public class LibraryIndexer extends AbstractFileIndexer {

    private static final Logger logger = LoggerFactory.getLogger(LibraryIndexer.class);

    private static final String LIBRARY_CLASSIFIER = "library";

    private LibraryAssetTypeDefinition filter;

    @Inject
    public LibraryIndexer(final LibraryAssetTypeDefinition filter) {
        this.filter = filter;
    }

    void setIOService(final IOService ioService) {
        this.ioService = ioService;
    }

    void setModuleService(final KieModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @Override
    public boolean supportsPath(final Path path) {
        return filter.accept(Paths.convert(path));
    }

    @Override
    protected DefaultIndexBuilder fillIndexBuilder(final Path path) throws Exception {
        final KieModule module = getModule(path);
        if (module == null) {
            logger.debug("Unable to index " + path.toUri().toString() + ": module could not be resolved.");
            return null;
        }

        final Package pkg = getPackage(path);
        if (pkg == null) {
            logger.debug("Unable to index " + path.toUri().toString() + ": package could not be resolved.");
            return null;
        }

        // responsible for basic index info: module name, branch, etc
        final DefaultIndexBuilder builder = new DefaultIndexBuilder(Paths.convert(path).getFileName(),
                                                                    module,
                                                                    pkg) {
            @Override
            public Set<KProperty<?>> build() {
                final Set<KProperty<?>> indexElements = new HashSet<>();

                indexElements.add(new KPropertyImpl<>(LibraryFileNameIndexTerm.TERM,
                                                      fileName));
                indexElements.add(new KPropertyImpl<>(FieldFactory.FILE_NAME_FIELD_SORTED,
                                                      fileName.toLowerCase(),
                                                      false,
                                                      true));

                if (module.getRootPath() != null) {
                    final String moduleRootUri = module.getRootPath().toURI();
                    indexElements.add(new KPropertyImpl<>(LibraryModuleRootPathIndexTerm.TERM,
                                                          moduleRootUri));
                }
                if (module.getModuleName() != null) {
                    final String moduleName = module.getModuleName();
                    indexElements.add(new KPropertyImpl<>(ModuleNameIndexTerm.TERM,
                                                          moduleName));
                }

                if (pkgName == null) {
                    pkgName = pkg.getPackageName();
                }
                if (pkgName != null) {
                    indexElements.add(new KPropertyImpl<>(PackageNameIndexTerm.TERM,
                                                          pkgName));
                }
                return indexElements;
            }
        };
        return builder;
    }

    @Override
    public KObject toKObject(final Path path) {
        KObject index = null;

        try {
            // create a builder with the default information
            DefaultIndexBuilder builder = fillIndexBuilder(path);

            Set<KProperty<?>> indexElements = null;
            if (builder != null) {
                // build index document
                indexElements = builder.build();
            } else {
                indexElements = Collections.emptySet();
            }

            index = KObjectUtil.toKObject(path,
                                          LIBRARY_CLASSIFIER,
                                          indexElements);
        } catch (Exception e) {
            // Unexpected parsing or processing error
            logger.error("Unable to index '" + path.toUri().toString() + "'.",
                         e.getMessage(),
                         e);
        }

        return index;
    }

    @Override
    public KObjectKey toKObjectKey(final Path path) {
        return KObjectUtil.toKObjectKey(path,
                                        LIBRARY_CLASSIFIER);
    }

    protected KieModule getModule(final Path path) {
        return moduleService.resolveModule(Paths.convert(path));
    }

    protected Package getPackage(final Path path) {
        return moduleService.resolvePackage(Paths.convert(path));
    }
}

