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
package org.kie.workbench.common.services.refactoring.backend.server.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.jboss.errai.bus.server.annotations.Service;
import org.kie.soup.commons.validation.PortablePreconditions;
import org.kie.workbench.common.services.refactoring.backend.server.query.response.ResponseBuilder;
import org.kie.workbench.common.services.refactoring.backend.server.query.standard.FindAllChangeImpactQuery;
import org.kie.workbench.common.services.refactoring.model.index.terms.valueterms.ValueBranchNameIndexTerm;
import org.kie.workbench.common.services.refactoring.model.index.terms.valueterms.ValueIndexTerm;
import org.kie.workbench.common.services.refactoring.model.index.terms.valueterms.ValueModuleNameIndexTerm;
import org.kie.workbench.common.services.refactoring.model.index.terms.valueterms.ValueModuleRootPathIndexTerm;
import org.kie.workbench.common.services.refactoring.model.query.RefactoringPageRequest;
import org.kie.workbench.common.services.refactoring.model.query.RefactoringPageRow;
import org.kie.workbench.common.services.refactoring.service.RefactoringQueryService;
import org.kie.workbench.common.services.refactoring.service.impact.QueryOperationRequest;
import org.uberfire.ext.metadata.MetadataConfig;
import org.uberfire.ext.metadata.model.KObject;
import org.uberfire.ext.metadata.search.ClusterSegment;
import org.uberfire.paging.PageResponse;

@Service
@ApplicationScoped
public class RefactoringQueryServiceImpl implements RefactoringQueryService {

    private MetadataConfig config;
    private NamedQueries namedQueries;
    private PageResponse<RefactoringPageRow> emptyResponse;

    public RefactoringQueryServiceImpl() {
        //Make proxyable
    }

    @Inject
    public RefactoringQueryServiceImpl(@Named("luceneConfig") final MetadataConfig config,
                                       final NamedQueries namedQueries) {
        this.config = PortablePreconditions.checkNotNull("config",
                                                         config);
        this.namedQueries = PortablePreconditions.checkNotNull("namedQueries",
                                                               namedQueries);
    }

    @PostConstruct
    public void init() {
        emptyResponse = new PageResponse<RefactoringPageRow>();
        emptyResponse.setPageRowList(Collections.<RefactoringPageRow>emptyList());
        emptyResponse.setStartRowIndex(0);
        emptyResponse.setTotalRowSize(0);
        emptyResponse.setLastPage(true);
        emptyResponse.setTotalRowSizeExact(true);
    }

    public Set<String> getQueries() {
        return namedQueries.getQueries();
    }

    @Override
    public int queryHitCount(final RefactoringPageRequest request) {
        PortablePreconditions.checkNotNull("request",
                                           request);
        final String queryName = PortablePreconditions.checkNotNull("queryName",
                                                                    request.getQueryName());
        final NamedQuery namedQuery = namedQueries.findNamedQuery(queryName);

        //Validate provided terms against those required for the named query
        namedQuery.validateTerms(request.getQueryTerms());

        final Query query = namedQuery.toQuery(request.getQueryTerms());
        final Sort sort = namedQuery.getSortOrder();

        try {
            List<KObject> found = config.getIndexProvider().findByQuery(Collections.EMPTY_LIST,
                                                                        query,
                                                                        sort,
                                                                        0);
            return found.size();
        } catch (final Exception ex) {
            throw new RuntimeException("Error during Query!",
                                       ex);
        }
    }

    @Override
    public PageResponse<RefactoringPageRow> query(final RefactoringPageRequest request) {
        PortablePreconditions.checkNotNull("request",
                                           request);
        final String queryName = PortablePreconditions.checkNotNull("queryName",
                                                                    request.getQueryName());
        final NamedQuery namedQuery = namedQueries.findNamedQuery(queryName);

        //Validate provided terms against those required for the named query
        namedQuery.validateTerms(request.getQueryTerms());

        final Query query = namedQuery.toQuery(request.getQueryTerms());
        final Sort sort = namedQuery.getSortOrder();

        final int pageSize = request.getPageSize();
        final int startIndex = request.getStartRowIndex();

        final List<KObject> kObjects
                = search(query,
                         sort,
                         () -> (startIndex),
                         // start index of docs to get
                         (numHits) -> (numHits - startIndex > pageSize ? pageSize : numHits - startIndex)
                         // num docs to add to response
        );

        if (!kObjects.isEmpty()) {
            final ResponseBuilder responseBuilder = namedQuery.getResponseBuilder();
            return responseBuilder.buildResponse(pageSize,
                                                 startIndex,
                                                 kObjects);
        } else {
            return emptyResponse;
        }
    }

    @Override
    public List<RefactoringPageRow> query(final String queryName,
                                          final Set<ValueIndexTerm> queryTerms) {
        PortablePreconditions.checkNotNull("queryName",
                                           queryName);
        PortablePreconditions.checkNotNull("queryTerms",
                                           queryTerms);

        final NamedQuery namedQuery = namedQueries.findNamedQuery(queryName);

        //Validate provided terms against those required for the named query
        namedQuery.validateTerms(queryTerms);

        final Query query = namedQuery.toQuery(queryTerms);
        final Sort sort = namedQuery.getSortOrder();

        final List<KObject> kObjects
                = search(query,
                         sort,
                         () -> (0),
                         // start index of docs to get
                         (numHits) -> (numHits)
                         // num docs to add to response
        );

        if (!kObjects.isEmpty()) {
            final ResponseBuilder responseBuilder = namedQuery.getResponseBuilder();
            return responseBuilder.buildResponse(kObjects);
        } else {
            return Collections.emptyList();
        }
    }

    private List<KObject> search(final Query query,
                                 final Sort sort,
                                 final Supplier<Integer> startIndexSupplier,
                                 final IntFunction<Integer> numOfHitsToReturnSupplier,
                                 final ClusterSegment... clusterSegments) {

        final List<KObject> result = new ArrayList<KObject>();
        try {
            List<String> indices = Arrays.stream(clusterSegments)
                    .map(clusterSegment -> clusterSegment.getClusterId())
                    .collect(Collectors.toList());

            List<KObject> found = config.getIndexProvider().findByQuery(indices,
                                                                        query,
                                                                        sort,
                                                                        0);
            final int startIndex = startIndexSupplier.get();
            final int numOfHitsToReturn = numOfHitsToReturnSupplier.apply(found.size());

            return found.subList(startIndex,
                                 startIndex + numOfHitsToReturn);
        } catch (
                final Exception ex)

        {
            throw new RuntimeException("Error during Query!",
                                       ex);
        }
    }

    /* (non-Javadoc)
     * @see org.kie.workbench.common.services.refactoring.service.RefactoringQueryService#queryToPageResponse(org.kie.workbench.common.services.refactoring.service.impact.QueryOperationRequest)
     */
    @Override
    public PageResponse<RefactoringPageRow> queryToPageResponse(QueryOperationRequest queryOpRequest) {
        final RefactoringPageRequest request = convertToRefactoringPageRequest(queryOpRequest);

        final PageResponse<RefactoringPageRow> response = query(request);

        return response;
    }

    @Override
    public List<RefactoringPageRow> queryToList(final QueryOperationRequest queryOpRequest) {
        final RefactoringPageRequest request = convertToRefactoringPageRequest(queryOpRequest);

        final List<RefactoringPageRow> response = query(request.getQueryName(),
                                                        request.getQueryTerms());

        return response;
    }

    private RefactoringPageRequest convertToRefactoringPageRequest(QueryOperationRequest refOpRequest) {
        RefactoringPageRequest request = new RefactoringPageRequest(
                FindAllChangeImpactQuery.NAME,
                new HashSet<>(),
                refOpRequest.getStartRowIndex(),
                refOpRequest.getPageSize());

        request.getQueryTerms().addAll(refOpRequest.getQueryTerms());

        // add project info
        String projectName = refOpRequest.getModuleName();
        if (projectName != null && projectName != QueryOperationRequest.ALL) {
            ValueModuleNameIndexTerm valueIndexTerm = new ValueModuleNameIndexTerm(projectName);
            Set<ValueIndexTerm> queryTerms = new HashSet<ValueIndexTerm>(1);
            queryTerms.add(valueIndexTerm);
            request.getQueryTerms().addAll(queryTerms);
        }

        String projectRootPathURI = refOpRequest.getModuleRootPathURI();
        if (projectRootPathURI != null && projectRootPathURI != QueryOperationRequest.ALL) {
            ValueModuleRootPathIndexTerm valueIndexTerm = new ValueModuleRootPathIndexTerm(projectRootPathURI);
            Set<ValueIndexTerm> queryTerms = new HashSet<ValueIndexTerm>(1);
            queryTerms.add(valueIndexTerm);
            request.getQueryTerms().addAll(queryTerms);
        }

        String branchName = refOpRequest.getBranchName();
        if (branchName != null && branchName != QueryOperationRequest.ALL) {
            ValueBranchNameIndexTerm valueIndexTerm = new ValueBranchNameIndexTerm(branchName);
            Set<ValueIndexTerm> queryTerms = new HashSet<ValueIndexTerm>(1);
            queryTerms.add(valueIndexTerm);
            request.getQueryTerms().addAll(queryTerms);
        }

        return request;
    }
}
