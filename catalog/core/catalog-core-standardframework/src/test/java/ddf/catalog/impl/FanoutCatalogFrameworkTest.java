/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceInfoRequest;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.operation.impl.SourceInfoRequestEnterprise;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.util.impl.SourcePoller;
import ddf.catalog.util.impl.SourcePollerRunner;

public class FanoutCatalogFrameworkTest {
    private static final String OLD_SOURCE_ID = "oldSourceId";

    private static final String NEW_SOURCE_ID = "newSourceId";

    private static final Double RELEVANCE_SCORE = 2.0;

    private static final Double DISTANCE_SCORE = 3.0;

    private CatalogFrameworkImpl framework;

    @Before
    public void initFramework() {

        // Mock register the provider in the container
        SourcePollerRunner runner = new SourcePollerRunner();
        SourcePoller poller = new SourcePoller(runner);
        ArrayList<PostIngestPlugin> postIngestPlugins = new ArrayList<PostIngestPlugin>();
        FrameworkProperties frameworkProperties = new FrameworkProperties();
        frameworkProperties.setSourcePoller(poller);
        frameworkProperties.setFederationStrategy(new MockFederationStrategy());
        frameworkProperties.setPostIngest(postIngestPlugins);

        framework = createCatalogFramework(frameworkProperties);
    }

    private CatalogFrameworkImpl createCatalogFramework(FrameworkProperties frameworkProperties) {
        OperationsSecuritySupport opsSecurity = new OperationsSecuritySupport();
        SourceOperations sourceOperations = new SourceOperations(frameworkProperties);
        TransformOperations transformOperations = new TransformOperations(frameworkProperties);
        QueryOperations queryOperations = new QueryOperations(frameworkProperties,
                sourceOperations,
                opsSecurity,
                null);
        OperationsCrudSupport opsCrud = new OperationsCrudSupport(frameworkProperties,
                queryOperations,
                sourceOperations);
        ResourceOperations resourceOperations = new ResourceOperations(frameworkProperties,
                queryOperations,
                opsSecurity);
        CreateOperations createOperations = new CreateOperations(frameworkProperties,
                queryOperations,
                sourceOperations,
                opsSecurity,
                null,
                opsCrud);
        UpdateOperations updateOperations = new UpdateOperations(frameworkProperties,
                queryOperations,
                sourceOperations,
                opsSecurity,
                null,
                opsCrud);
        DeleteOperations deleteOperations = new DeleteOperations(frameworkProperties,
                queryOperations,
                sourceOperations,
                opsSecurity,
                null,
                opsCrud);

        framework = new CatalogFrameworkImpl(frameworkProperties,
                opsCrud,
                createOperations,
                updateOperations,
                deleteOperations,
                queryOperations,
                resourceOperations,
                sourceOperations,
                transformOperations);
        framework.setId(NEW_SOURCE_ID);
        framework.setFanoutEnabled(true);

        return framework;
    }

    @Test
    public void testReplaceSourceId() {
        QueryRequest request = new QueryRequestImpl(null);

        List<Result> results = new ArrayList<Result>();

        MetacardImpl newCard1 = new MetacardImpl();
        newCard1.setSourceId(OLD_SOURCE_ID);
        ResultImpl result1 = new ResultImpl(newCard1);

        MetacardImpl newCard2 = new MetacardImpl();
        newCard2.setSourceId(OLD_SOURCE_ID);

        ResultImpl result2 = new ResultImpl(newCard2);

        results.add(result1);
        results.add(result2);

        QueryResponse response = new QueryResponseImpl(request, results, 2);

        QueryResponse newResponse = framework.getQueryOperations()
                .replaceSourceId(response);
        assertNotNull(newResponse);

        List<Result> newResults = newResponse.getResults();
        assertNotNull(newResults);

        assertEquals(2, newResults.size());
        Metacard card = new MetacardImpl();
        // Make sure the sourceId was replaced

        for (Result newResult : newResults) {
            card = newResult.getMetacard();
            assertNotNull(card);
            assertEquals(NEW_SOURCE_ID, card.getSourceId());
        }
    }

    @Test
    public void testReplaceRelevance() {
        QueryRequest request = new QueryRequestImpl(null);

        List<Result> results = new ArrayList<Result>();

        MetacardImpl newCard1 = new MetacardImpl();

        ResultImpl result1 = new ResultImpl(newCard1);
        result1.setRelevanceScore(RELEVANCE_SCORE);

        MetacardImpl newCard2 = new MetacardImpl();

        ResultImpl result2 = new ResultImpl(newCard2);
        result2.setRelevanceScore(RELEVANCE_SCORE);

        results.add(result1);
        results.add(result2);

        QueryResponse response = new QueryResponseImpl(request, results, 2);

        QueryResponse newResponse = framework.getQueryOperations()
                .replaceSourceId(response);
        assertNotNull(newResponse);

        List<Result> newResults = newResponse.getResults();
        assertNotNull(newResponse);

        assertEquals(2, newResults.size());
        Metacard card = new MetacardImpl();
        // Make sure the relevance score was copied over
        for (Result newResult : newResults) {
            card = newResult.getMetacard();
            assertNotNull(card);
            assertEquals(RELEVANCE_SCORE, newResult.getRelevanceScore());
        }
    }

    @Test
    public void testReplaceDistance() {
        QueryRequest request = new QueryRequestImpl(null);

        List<Result> results = new ArrayList<Result>();

        MetacardImpl newCard1 = new MetacardImpl();

        ResultImpl result1 = new ResultImpl(newCard1);
        result1.setRelevanceScore(RELEVANCE_SCORE);
        result1.setDistanceInMeters(DISTANCE_SCORE);

        MetacardImpl newCard2 = new MetacardImpl();

        ResultImpl result2 = new ResultImpl(newCard2);
        result2.setRelevanceScore(RELEVANCE_SCORE);
        result2.setDistanceInMeters(DISTANCE_SCORE);

        results.add(result1);
        results.add(result2);

        QueryResponse response = new QueryResponseImpl(request, results, 2);

        QueryResponse newResponse = framework.getQueryOperations()
                .replaceSourceId(response);
        assertNotNull(newResponse);
        List<Result> newResults = newResponse.getResults();
        assertNotNull(newResults);
        assertEquals(2, newResults.size());
        Metacard card = new MetacardImpl();
        // Make sure the relevance and distance score was copied over
        for (Result newResult : newResults) {
            card = newResult.getMetacard();
            assertNotNull(card);
            assertEquals(RELEVANCE_SCORE, newResult.getRelevanceScore());
            assertEquals(DISTANCE_SCORE, newResult.getDistanceInMeters());
        }
    }

    @Test
    public void testGetSourceIds() {
        Set<String> sourceIds = framework.getSourceIds();
        assertNotNull(sourceIds);
        assertEquals(1, sourceIds.size());
        assertEquals(NEW_SOURCE_ID,
                sourceIds.iterator()
                        .next());
    }

    /**
     * This test is to verify that an NPE will not be thrown if {@code source.getContentTypes}
     * returns null.
     *
     * @throws SourceUnavailableException
     */
    @Test
    public void testNullContentTypesInGetSourceInfo() throws SourceUnavailableException {
        SourcePollerRunner runner = new SourcePollerRunner();
        SourcePoller poller = new SourcePoller(runner);
        ArrayList<PostIngestPlugin> postIngestPlugins = new ArrayList<PostIngestPlugin>();

        SourceInfoRequest request = new SourceInfoRequestEnterprise(true);
        List<FederatedSource> fedSources = new ArrayList<FederatedSource>();

        FederatedSource mockFederatedSource = Mockito.mock(FederatedSource.class);
        Mockito.when(mockFederatedSource.isAvailable())
                .thenReturn(true);

        // Mockito would not accept Collections.emptySet() as the parameter for
        // thenReturn for mockFederatedSource.getContentTypes()
        Mockito.when(mockFederatedSource.getContentTypes())
                .thenReturn(null);

        fedSources.add(mockFederatedSource);

        FrameworkProperties frameworkProperties = new FrameworkProperties();
        frameworkProperties.setSourcePoller(poller);
        frameworkProperties.setFederationStrategy(new MockFederationStrategy());
        frameworkProperties.setPostIngest(postIngestPlugins);
        Map<String, FederatedSource> sourceMap = new HashMap<>();
        for (FederatedSource federatedSource : fedSources) {
            sourceMap.put(federatedSource.getId(), federatedSource);
        }
        frameworkProperties.setFederatedSources(sourceMap);

        CatalogFrameworkImpl framework = createCatalogFramework(frameworkProperties);

        // Assert not null simply to prove that we returned an object.
        assertNotNull(framework.getSourceInfo(request));

    }

}
