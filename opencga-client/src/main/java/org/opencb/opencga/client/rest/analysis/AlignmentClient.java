package org.opencb.opencga.client.rest.analysis;

import org.ga4gh.models.ReadAlignment;
import org.opencb.biodata.models.alignment.RegionCoverage;
import org.opencb.biodata.tools.alignment.stats.AlignmentGlobalStats;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.QueryResponse;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.client.config.ClientConfiguration;
import org.opencb.opencga.client.rest.AbstractParentClient;

import java.io.IOException;

/**
 * Created by pfurio on 11/11/16.
 */
public class AlignmentClient extends AbstractParentClient {

    private static final String ALIGNMENT_URL = "analysis/alignment";

    public AlignmentClient(String userId, String sessionId, ClientConfiguration configuration) {
        super(userId, sessionId, configuration);
    }

    public QueryResponse<ReadAlignment> query(ObjectMap params) throws CatalogException, IOException {
        return execute(ALIGNMENT_URL, "query", params, GET, ReadAlignment.class);
    }

    public QueryResponse<AlignmentGlobalStats> stats(ObjectMap params) throws CatalogException, IOException {
        return execute(ALIGNMENT_URL, "stats", params, GET, AlignmentGlobalStats.class);
    }

    public QueryResponse<RegionCoverage> coverage(ObjectMap params) throws CatalogException, IOException {
        return execute(ALIGNMENT_URL, "coverage", params, GET, RegionCoverage.class);
    }
}
