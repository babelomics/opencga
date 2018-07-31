/*
 * Copyright 2015-2017 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.storage.core.variant.search.solr;

import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.opencga.storage.core.exceptions.StorageEngineException;
import org.opencb.opencga.storage.core.metadata.StudyConfiguration;
import org.opencb.opencga.storage.core.metadata.StudyConfigurationManager;
import org.opencb.opencga.storage.core.variant.adaptors.VariantField;
import org.opencb.opencga.storage.core.variant.adaptors.VariantQueryParam;
import org.opencb.opencga.storage.core.variant.adaptors.VariantQueryUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.opencb.opencga.storage.core.variant.adaptors.VariantQueryParam.INCLUDE_STUDY;
import static org.opencb.opencga.storage.core.variant.adaptors.VariantQueryParam.STUDY;
import static org.opencb.opencga.storage.core.variant.adaptors.VariantQueryUtils.*;

/**
 * Created on 06/07/17.
 *
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public class VariantSearchUtils {

    public static final Set<VariantQueryParam> UNSUPPORTED_QUERY_PARAMS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList(VariantQueryParam.FILE,
                    VariantQueryParam.FILTER,
                    VariantQueryParam.GENOTYPE,
                    VariantQueryParam.SAMPLE,
                    VariantQueryParam.COHORT,
                    VariantQueryParam.MISSING_ALLELES,
                    VariantQueryParam.MISSING_GENOTYPES)));

    public static final Set<VariantQueryParam> UNSUPPORTED_MODIFIERS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList(VariantQueryParam.INCLUDE_FILE,
                    VariantQueryParam.INCLUDE_SAMPLE,
//                    VariantQueryParam.INCLUDE_STUDY,
                    VariantQueryParam.UNKNOWN_GENOTYPE,
                    VariantQueryParam.INCLUDE_FORMAT,
                    VariantQueryParam.INCLUDE_GENOTYPE,
                    VariantQueryParam.SAMPLE_METADATA
                    // RETURNED_COHORTS
            )));

    private static final List<VariantField> UNSUPPORTED_VARIANT_FIELDS =
            Arrays.asList(VariantField.STUDIES_FILES,
                    VariantField.STUDIES_SAMPLES_DATA);

    public static boolean isQueryCovered(Query query) {
        for (VariantQueryParam nonCoveredParam : UNSUPPORTED_QUERY_PARAMS) {
            if (isValidParam(query, nonCoveredParam)) {
                return false;
            }
        }
        return true;
    }

    public static Collection<VariantQueryParam> coveredParams(Query query) {
        Set<VariantQueryParam> params = validParams(query);
        return coveredParams(params);
    }

    public static List<VariantQueryParam> coveredParams(Collection<VariantQueryParam> params) {
        List<VariantQueryParam> coveredParams = new ArrayList<>();

        for (VariantQueryParam param : params) {
            if (!UNSUPPORTED_QUERY_PARAMS.contains(param) && !UNSUPPORTED_MODIFIERS.contains(param)) {
                coveredParams.add(param);
            }
        }
        return coveredParams;
    }

    public static Collection<VariantQueryParam> uncoveredParams(Query query) {
        Set<VariantQueryParam> params = validParams(query);
        return uncoveredParams(params);
    }

    public static List<VariantQueryParam> uncoveredParams(Collection<VariantQueryParam> params) {
        List<VariantQueryParam> coveredParams = new ArrayList<>();

        for (VariantQueryParam param : params) {
            if (UNSUPPORTED_QUERY_PARAMS.contains(param) || UNSUPPORTED_MODIFIERS.contains(param)) {
                coveredParams.add(param);
            }
        }
        return coveredParams;
    }

    public static boolean isIncludeCovered(QueryOptions options) {
        Set<VariantField> returnedFields = VariantField.getIncludeFields(options);
        for (VariantField unsupportedVariantField : UNSUPPORTED_VARIANT_FIELDS) {
            if (returnedFields.contains(unsupportedVariantField)) {
                return false;
            }
        }
        return true;
    }

    public static Query getEngineQuery(Query query, QueryOptions options, StudyConfigurationManager scm)
            throws StorageEngineException {
        Collection<VariantQueryParam> uncoveredParams = uncoveredParams(query);
        Query engineQuery = new Query();
        for (VariantQueryParam uncoveredParam : uncoveredParams) {
            engineQuery.put(uncoveredParam.key(), query.get(uncoveredParam.key()));
        }
        // Despite STUDIES is a covered filter by Solr, it has to be in the underlying
        // query to be used as defaultStudy
        if (isValidParam(query, STUDY)) {
            if (!uncoveredParams.isEmpty()) {
                // This will set the default study, if needed
                engineQuery.put(STUDY.key(), query.get(STUDY.key()));
            } else if (!isValidParam(query, INCLUDE_STUDY)) {
                // If returned studies is not defined, we need to define it with the values from STUDIES
                List<Integer> studies = VariantQueryUtils.getIncludeStudies(query, options, scm);
                engineQuery.put(INCLUDE_STUDY.key(), studies);
            }
        }
        return engineQuery;
    }

    public static String buildSamplesIndexCollectionName(String dbName, StudyConfiguration sc, int id) {
        return dbName + '_' + sc.getStudyId() + '_' + id;
    }

    /**
     * Decide if a query should be resolved using the Specific Samples SearchManager or not.
     *
     * Will use it if:
     *  - UseSearchIndex is YES or AUTO
     *  - Included elements are covered by the specific search collection
     *      - INCLUDE_STUDY
     *      - INCLUDE_FILE
     *      - INCLUDE_SAMPLE
     *  - Filter only by one STUDY
     *  - Filter elements are covered by the specific search collection, and there is at least one of them
     *      - SAMPLE
     *      - GENOTYPE
     *      - FILE
     *
     * @param query                     Query
     * @param options                   QueryOptions
     * @param studyConfigurationManager StudyConfigurationManager
     * @param dbName                    DBName
     * @return          Name of the specific collection, or null if not found
     * @throws StorageEngineException StorageEngineException
     */
    public static String inferSpecificSearchIndexSamplesCollection(
            Query query, QueryOptions options, StudyConfigurationManager studyConfigurationManager, String dbName)
            throws StorageEngineException {
        if (!VariantSearchManager.UseSearchIndex.from(options).equals(VariantSearchManager.UseSearchIndex.NO)) { // YES or AUTO
            if (isValidParam(query, VariantQueryParam.STUDY)) {
                if (VariantQueryUtils.splitValue(query.getString(VariantQueryParam.STUDY.key())).getValue().size() > 1) {
                    // Multi study query
                    return null;
                }
            }

            boolean validGenotypeFilter = false;
            if (isValidParam(query, VariantQueryParam.GENOTYPE)) {
                HashMap<Object, List<String>> map = new HashMap<>();
                parseGenotypeFilter(query.getString(VariantQueryParam.GENOTYPE.key()), map);

                for (List<String> gts : map.values()) {
                    validGenotypeFilter |= !gts.stream().allMatch(VariantQueryUtils::isNegated);
                }
            }

            if (!isValidParam(query, VariantQueryParam.SAMPLE, true)
                    && !validGenotypeFilter
                    && !isValidParam(query, VariantQueryParam.FILE, true)) {
                // Specific search index will only be valid if at least one of this filters is present.
                return null;
            } else if (ALL.equals(query.getString(VariantQueryParam.INCLUDE_SAMPLE.key()))
                    || ALL.equals(query.getString(VariantQueryParam.INCLUDE_FILE.key()))) {
                // Discard if including all samples or files.
                // What if all samples are in the study are in the search index?
                return null;
            } else {
                // Check that all elements from the query are in the same search collection

                VariantQueryUtils.SelectVariantElements selectVariantElements =
                        VariantQueryUtils.parseSelectElements(query, options, studyConfigurationManager);

                if (selectVariantElements.getStudies().size() != 1) {
                    return null;
                }

                Integer studyId = selectVariantElements.getStudies().get(0);
                StudyConfiguration studyConfiguration = selectVariantElements.getStudyConfigurations().get(studyId);
                Set<String> samples = new HashSet<>();
                if (isValidParam(query, VariantQueryParam.SAMPLE)) {
                    String value = query.getString(VariantQueryParam.SAMPLE.key());
                    samples.addAll(splitValue(value).getValue());
                }
                if (isValidParam(query, VariantQueryParam.GENOTYPE)) {
                    HashMap<Object, List<String>> map = new HashMap<>();
                    parseGenotypeFilter(query.getString(VariantQueryParam.GENOTYPE.key()), map);
                    for (Object o : map.keySet()) {
                        samples.add(o.toString());
                    }
                }
                if (isValidParam(query, VariantQueryParam.INCLUDE_SAMPLE)) {
                    String value = query.getString(VariantQueryParam.INCLUDE_SAMPLE.key());
                    if (!value.equals(VariantQueryUtils.NONE)) {
                        samples.addAll(splitValue(value).getValue());
                    }
                }

                List<Integer> sampleIds;
                if (samples.isEmpty()) {
                    // None of the previous fields is defined. Returning all samples from study, or from the given samples
                    sampleIds = selectVariantElements.getSamples().get(studyId);
                } else {
                    sampleIds = samples.stream()
                            .map(sample -> isNegated(sample) ? removeNegation(sample) : sample)
                            .map(studyConfiguration.getSampleIds()::get).collect(Collectors.toList());
                }

                Integer sampleSet = null;
                for (Integer sampleId : sampleIds) {
                    Integer thisSampleSet = studyConfiguration.getSearchIndexedSampleSets().get(sampleId);
                    if (sampleSet == null) {
                        sampleSet = thisSampleSet;
                    } else if (!sampleSet.equals(thisSampleSet)) {
                        // Mix of sample sets
                        return null;
                    }
                }

                // Check that files are within the specific search collection, only if defined.
                // Otherwise, this is defined by the samples, so it is in the specific search collection
                Set<String> files = new HashSet<>();
                if (isValidParam(query, VariantQueryParam.FILE)) {
                    files.addAll(splitValue(query.getString(VariantQueryParam.FILE.key())).getValue());
                } else if (isValidParam(query, VariantQueryParam.INCLUDE_FILE)) {
                    files.addAll(splitValue(query.getString(VariantQueryParam.INCLUDE_FILE.key())).getValue());
                }

                for (String file : files) {
                    file = isNegated(file) ? removeNegation(file) : file;
                    Integer fileId = StudyConfigurationManager.getFileIdFromStudy(file, studyConfiguration);
                    if (fileId == null) {
                        // File not found
                        return null;
                    } else {
                        // Check if any of the samples of this file is in this collection. If so, the file will be too.
                        for (Integer sampleId : studyConfiguration.getSamplesInFiles().get(fileId)) {
                            Integer thisSampleSet = studyConfiguration.getSearchIndexedSampleSets().get(sampleId);
                            if (thisSampleSet != null) {
                                if (sampleSet == null) {
                                    // There may be another sampleSet that contains all required samples.
                                    // Take any. Could be improved in the future, if needed.
                                    sampleSet = thisSampleSet;
                                } else if (!Objects.equals(sampleSet, thisSampleSet)) {
                                    return null;
                                }
                            }
                        }
                    }
                }

                if (sampleSet == null) {
                    return null;
                } else {
                    return buildSamplesIndexCollectionName(dbName, studyConfiguration, sampleSet);
                }
            }
        }

        return null;
    }
}
