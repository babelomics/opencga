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

package org.opencb.opencga.client.rest.catalog;

import org.codehaus.jackson.map.ObjectMapper;
import org.opencb.biodata.models.alignment.Alignment;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.commons.datastore.core.QueryResponse;
import org.opencb.opencga.client.config.ClientConfiguration;
import org.opencb.opencga.core.models.File;
import org.opencb.opencga.core.models.Job;
import org.opencb.opencga.core.models.Sample;
import org.opencb.opencga.core.models.Study;
import org.opencb.opencga.core.models.acls.permissions.StudyAclEntry;
import org.opencb.opencga.core.models.summaries.StudySummary;

import java.io.IOException;

/**
 * Created by swaathi on 10/05/16.
 */
public class StudyClient extends CatalogClient<Study, StudyAclEntry> {

    private static final String STUDY_URL = "studies";

    public StudyClient(String userId, String sessionId, ClientConfiguration configuration) {
        super(userId, sessionId, configuration);
        this.category = STUDY_URL;
        this.clazz = Study.class;
        this.aclClass = StudyAclEntry.class;
    }

    public QueryResponse<Study> create(String projectId, String studyId, String studyName, ObjectMap params) throws IOException {
        params = addParamsToObjectMap(params, "name", studyName, "id", studyId);
        ObjectMap p = new ObjectMap("body", params);
        p = addParamsToObjectMap(p, "projectId", projectId);
        return execute(STUDY_URL, "create", p, POST, Study.class);
    }

    public QueryResponse<StudySummary> getSummary(String studyId, QueryOptions options) throws IOException {
        return execute(STUDY_URL, studyId, "summary", options, GET, StudySummary.class);
    }

    public QueryResponse<Sample> getSamples(String studyId, QueryOptions options) throws IOException {
        return execute(STUDY_URL, studyId, "samples", options, GET, Sample.class);
    }

    public QueryResponse<File> getFiles(String studyId, QueryOptions options) throws IOException {
        return execute(STUDY_URL, studyId, "files", options, GET, File.class);
    }

    public QueryResponse<Job> getJobs(String studyId, QueryOptions options) throws IOException {
        return execute(STUDY_URL, studyId, "jobs", options, GET, Job.class);
    }

    public QueryResponse<ObjectMap> getStatus(String studyId, QueryOptions options) throws IOException {
        return execute(STUDY_URL, studyId, "status", options, GET, ObjectMap.class);
    }

    public QueryResponse<Variant> getVariants(String studyId, QueryOptions options) throws IOException {
        return execute(STUDY_URL, studyId, "variants", options, GET, Variant.class);
    }

    public QueryResponse<Long> countVariants(String studyId, QueryOptions options) throws IOException {
        return execute(STUDY_URL, studyId, "variants", options, GET, Long.class);
    }

    public QueryResponse<ObjectMap> getVariantsGeneric(String studyId, QueryOptions options) throws IOException {
        return execute(STUDY_URL, studyId, "variants", options, GET, ObjectMap.class);
    }

    public QueryResponse<Alignment> getAlignments(String studyId, String sampleId, String fileId, String region, Query query,
                                                  QueryOptions options) throws IOException {
        ObjectMap params = new ObjectMap(query);
        params.putAll(options);
        params = addParamsToObjectMap(params, "sampleId", sampleId, "fileId", fileId, "region", region);
        params.putIfAbsent("view_as_pairs", false);
        params.putIfAbsent("include_coverage", true);
        params.putIfAbsent("process_differences", true);
        params.putIfAbsent("histogram", false);
        params.putIfAbsent("interval", 200);
        return execute(STUDY_URL, studyId, "alignments", params, GET, Alignment.class);
    }

    public QueryResponse scanFiles(String studyId, QueryOptions options) throws IOException {
        return execute(STUDY_URL, studyId, "scanFiles", options, GET, Object.class);
    }

    public QueryResponse resyncFiles(String studyId, QueryOptions options) throws IOException {
        return execute(STUDY_URL, studyId, "resyncFiles", options, GET, Object.class);
    }

    public QueryResponse<ObjectMap> createGroup(String studyId, String groupId, String users) throws IOException {
        ObjectMap bodyParams = new ObjectMap();
        bodyParams.putIfNotEmpty("name", groupId);
        bodyParams.putIfNotEmpty("users", users);
        return execute(STUDY_URL, studyId, "groups", null, "create", new ObjectMap("body", bodyParams), POST, ObjectMap.class);
    }

    public QueryResponse<ObjectMap> deleteGroup(String studyId, String groupId, QueryOptions options) throws IOException {
        return execute(STUDY_URL, studyId, "groups", groupId, "delete", options, GET, ObjectMap.class);
    }

    public QueryResponse<ObjectMap> updateGroup(String studyId, String groupId, ObjectMap objectMap) throws IOException {
        ObjectMap bodyParams = new ObjectMap("body", objectMap);
        return execute(STUDY_URL, studyId, "groups", groupId, "update", bodyParams, POST, ObjectMap.class);
    }

    public QueryResponse<ObjectMap> updateGroupMember(String studyId, ObjectMap objectMap) throws IOException {
        ObjectMap bodyParams = new ObjectMap("body", objectMap);
        return execute(STUDY_URL, studyId, "groups", "members", "update", bodyParams, POST, ObjectMap.class);
    }

    public QueryResponse<ObjectMap> updateGroupAdmins(String studyId, ObjectMap objectMap) throws IOException {
        ObjectMap bodyParams = new ObjectMap("body", objectMap);
        return execute(STUDY_URL, studyId, "groups", "admins", "update", bodyParams, POST, ObjectMap.class);
    }

    public QueryResponse<ObjectMap> groups(String studyId, ObjectMap objectMap) throws IOException {
        ObjectMap params = new ObjectMap(objectMap);
        return execute(STUDY_URL, studyId, "groups", params, GET, ObjectMap.class);
    }

    public QueryResponse<Study> update(String studyId, String study, ObjectMap params) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(params);
        ObjectMap p = new ObjectMap("body", json);
        return execute(STUDY_URL, studyId, "update", p, POST, Study.class);
    }

    public QueryResponse<Study> delete(String studyId, ObjectMap params) throws IOException {
        return execute(STUDY_URL, studyId, "delete", params, GET, Study.class);
    }
}
