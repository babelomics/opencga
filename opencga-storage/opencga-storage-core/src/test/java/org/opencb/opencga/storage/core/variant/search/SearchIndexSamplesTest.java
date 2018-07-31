package org.opencb.opencga.storage.core.variant.search;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opencb.biodata.models.variant.StudyEntry;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.biodata.models.variant.stats.VariantStats;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.opencga.core.common.UriUtils;
import org.opencb.opencga.storage.core.exceptions.StorageEngineException;
import org.opencb.opencga.storage.core.exceptions.VariantSearchException;
import org.opencb.opencga.storage.core.metadata.StudyConfiguration;
import org.opencb.opencga.storage.core.variant.VariantStorageBaseTest;
import org.opencb.opencga.storage.core.variant.VariantStorageEngine;
import org.opencb.opencga.storage.core.variant.adaptors.VariantField;
import org.opencb.opencga.storage.core.variant.search.solr.VariantSearchManager;
import org.opencb.opencga.storage.core.variant.search.solr.VariantSearchUtils;
import org.opencb.opencga.storage.core.variant.search.solr.VariantSolrIterator;
import org.opencb.opencga.storage.core.variant.solr.SolrExternalResource;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.opencb.opencga.storage.core.variant.adaptors.VariantQueryParam.*;
import static org.opencb.opencga.storage.core.variant.adaptors.VariantQueryUtils.ALL;
import static org.opencb.opencga.storage.core.variant.adaptors.VariantQueryUtils.NONE;

/**
 * Created on 20/07/18.
 *
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public abstract class SearchIndexSamplesTest extends VariantStorageBaseTest {

    protected static final String COLLECTION_1 = "opencga_variants_test_1_1";
    protected static final String COLLECTION_2 = "opencga_variants_test_1_2";
    @ClassRule
    public static SolrExternalResource solr = new SolrExternalResource();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static boolean loaded = false;
    private static List<String> samples1;
    private static List<String> samples2;
    private static List<String> files1;
    private static List<String> files2;

    @Before
    public void before() throws Exception {
        solr.configure(variantStorageEngine);
        if (!loaded) {
            load();
            loaded = true;
        }
    }

    protected void load() throws Exception {
        clearDB(DB_NAME);

        VariantStorageEngine variantStorageEngine = getVariantStorageEngine();

        StudyConfiguration studyConfiguration = newStudyConfiguration();
        runDefaultETL(smallInputUri, variantStorageEngine, studyConfiguration);

        Iterator<String> it = studyConfiguration.getSampleIds().keySet().iterator();

        samples1 = Arrays.asList(it.next(), it.next());
        files1 = Collections.singletonList(UriUtils.fileName(smallInputUri));
        variantStorageEngine.searchIndexSamples(studyConfiguration.getStudyName(), samples1);

        runDefaultETL(getPlatinumFile(12877), variantStorageEngine, studyConfiguration);
        runDefaultETL(getPlatinumFile(12878), variantStorageEngine, studyConfiguration);

        samples2 = Arrays.asList("NA12877", "NA12878");
        files2 = Arrays.asList(UriUtils.fileName(getPlatinumFile(12877)), UriUtils.fileName(getPlatinumFile(12878)));
        variantStorageEngine.searchIndexSamples(studyConfiguration.getStudyName(), samples2);
    }

    @Test
    public void testFailReindex() throws Exception {
        thrown.expectMessage("already in search index");
        variantStorageEngine.searchIndexSamples(STUDY_NAME, samples1);
    }

    @Test
    public void testFailReindexMix() throws Exception {
        thrown.expectMessage("already in search index");
        variantStorageEngine.searchIndexSamples(STUDY_NAME, Arrays.asList(samples1.get(0), samples2.get(0)));
    }

    @Test
    public void testDoQuerySearchManagerSpecificSearchIndexSamples() throws Exception {
        check(null, new Query(), new QueryOptions());
        check(null, new Query(), new QueryOptions(QueryOptions.EXCLUDE, VariantField.STUDIES)); // Missing filter
        check(null, new Query(SAMPLE.key(), samples1).append(INCLUDE_SAMPLE.key(), samples1.get(0) + "," + samples2.get(0)), new QueryOptions());   // Include not covered
        check(null, new Query(SAMPLE.key(), samples1).append(INCLUDE_SAMPLE.key(), samples2), new QueryOptions());  // Include samples2 not covered
        check(null, new Query(SAMPLE.key(), samples1).append(INCLUDE_SAMPLE.key(), ALL), new QueryOptions());   //  Include sample not covered
        check(null, new Query(SAMPLE.key(), "!" + samples1.get(0)), new QueryOptions());    // Only negated filters. Filters not covered
        check(null, new Query(GENOTYPE.key(), samples1.get(0) + ":!0/0"), new QueryOptions());
        check(null, new Query(INCLUDE_SAMPLE.key(), samples1), new QueryOptions());     // Missing filter
        check(null, new Query(INCLUDE_SAMPLE.key(), samples1.get(0)), new QueryOptions());      // Missing filter
        check(null, new Query(INCLUDE_SAMPLE.key(), samples1).append(INCLUDE_FILE.key(), files1), new QueryOptions()); // Missing filter
        check(null, new Query(SAMPLE.key(), samples1).append(INCLUDE_FILE.key(), ALL), new QueryOptions()); // Include file not covered
        check(null, new Query(SAMPLE.key(), samples1.get(0) + "," + samples2.get(0)), new QueryOptions()); // Filter mixed
        check(null, new Query(SAMPLE.key(), samples1).append(FILE.key(), files2), new QueryOptions()); // Filter mixed
        check(null, new Query(SAMPLE.key(), samples1).append(INCLUDE_FILE.key(), files2), new QueryOptions()); // Include file not covered
        check(null, new Query(FILE.key(), files1), new QueryOptions());  // About to return all samples from file1.
        check(null, new Query(SAMPLE.key(), "!" + samples1.get(0)).append(GENOTYPE.key(), samples1.get(1) + ":!0/0"), new QueryOptions()); // None positive filter
        check(null, new Query(FILE.key(), files1).append(SAMPLE.key(), "!" + samples2.get(0)), new QueryOptions()); // Filter sample2 not covered

        check(COLLECTION_1, new Query(SAMPLE.key(), samples1).append(INCLUDE_SAMPLE.key(), NONE), new QueryOptions());
        check(COLLECTION_1, new Query(SAMPLE.key(), samples1), new QueryOptions());
        check(COLLECTION_1, new Query(SAMPLE.key(), samples1.get(0)), new QueryOptions());
        check(COLLECTION_1, new Query(SAMPLE.key(), samples1.get(0) + ",!" + samples1.get(1)), new QueryOptions());
        check(COLLECTION_1, new Query(GENOTYPE.key(), samples1.get(0) + ":0/0"), new QueryOptions());
        check(COLLECTION_1, new Query(SAMPLE.key(), samples1.get(0)).append(GENOTYPE.key(), samples1.get(1) + ":!0/0"), new QueryOptions());
        check(COLLECTION_1, new Query(FILE.key(), files1).append(SAMPLE.key(), "!" + samples1.get(0)).append(GENOTYPE.key(), samples1.get(1) + ":!0/0"), new QueryOptions());
        check(COLLECTION_1, new Query(FILE.key(), files1).append(SAMPLE.key(), "!" + samples1.get(0)), new QueryOptions()); // Filter sample not covered
        check(COLLECTION_1, new Query(SAMPLE.key(), samples1).append(FILE.key(), files1), new QueryOptions());
        check(COLLECTION_1, new Query(FILE.key(), files1).append(INCLUDE_SAMPLE.key(), samples1), new QueryOptions());

        check(COLLECTION_2, new Query(SAMPLE.key(), samples2), new QueryOptions());
        check(COLLECTION_2, new Query(SAMPLE.key(), samples2.get(0)), new QueryOptions());
        check(COLLECTION_2, new Query(SAMPLE.key(), samples2.get(0)).append(FILE.key(), files2.get(1)), new QueryOptions());
        check(COLLECTION_2, new Query(SAMPLE.key(), samples2.get(0)).append(FILE.key(), files2), new QueryOptions());
        check(COLLECTION_2, new Query(FILE.key(), files2), new QueryOptions());
        check(COLLECTION_2, new Query(FILE.key(), files2.get(0)), new QueryOptions());
        check(COLLECTION_2, new Query(FILE.key(), files2.get(0) + ",!" + files2.get(1)), new QueryOptions());
    }

    protected void check(String collection, Query query, QueryOptions options) throws StorageEngineException {
        assertEquals(query.toJson() + " " + options.toJson(), collection, VariantSearchUtils.inferSpecificSearchIndexSamplesCollection(query, options, variantStorageEngine.getStudyConfigurationManager(), DB_NAME));
    }

    @Test
    public void checkLoadedData() throws Exception {
        checkLoadedData(COLLECTION_1, samples1);
        checkLoadedData(COLLECTION_2, samples2);
    }

    protected void checkLoadedData(String collection, List<String> samples)
            throws StorageEngineException, IOException, VariantSearchException {
        VariantSearchManager variantSearchManager = variantStorageEngine.getVariantSearchManager();

        Query query = new Query(SAMPLE.key(), samples);
        int expectedCount = variantStorageEngine.getDBAdaptor().count(query).first().intValue();
        assertEquals(expectedCount, variantSearchManager.query(collection, new Query(), new QueryOptions()).getNumTotalResults());


        VariantSolrIterator solrIterator = variantSearchManager.iterator(collection, new Query(), new QueryOptions(QueryOptions.SORT, true));

//        int i = 0;
        while (solrIterator.hasNext()) {
            Variant actual = solrIterator.next();
            Variant expected = variantStorageEngine.getDBAdaptor().get(query.append(ID.key(), actual.toString()), null).first();
//            System.out.println(collection + "[" + i + "] " + actual);
//            i++;

            assertNotNull(expected);
            assertEquals(expected.toString(), actual.toString());
            StudyEntry expectedStudy = expected.getStudies().get(0);
            StudyEntry actualStudy = actual.getStudies().get(0);

            Map<String, VariantStats> expectedStudyStats = expectedStudy.getStats();
            expectedStudy.setStats(Collections.emptyMap());
            Map<String, VariantStats> actualStudyStats = actualStudy.getStats();
            actualStudy.setStats(Collections.emptyMap());

            assertEquals(expected.toString(), expectedStudy, actualStudy);
            // FIXME
//            assertEquals(expected.toString(), expectedStudyStats, actualStudyStats);
        }

        assertFalse(solrIterator.hasNext());

    }
}
