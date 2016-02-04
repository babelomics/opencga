/*
 * Copyright 2015 OpenCB
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

package org.opencb.opencga.catalog.db.api;

import org.opencb.commons.datastore.core.*;
import org.opencb.opencga.catalog.exceptions.CatalogDBException;
import org.opencb.opencga.catalog.models.Job;
import org.opencb.opencga.catalog.models.Tool;

import java.util.HashMap;
import java.util.Map;

import static org.opencb.commons.datastore.core.QueryParam.Type.*;

/**
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public interface CatalogJobDBAdaptor extends CatalogDBAdaptor<Job> {

    default boolean jobExists(int jobId) throws CatalogDBException {
        return count(new Query(QueryParams.ID.key(), jobId)).first() > 0;
    }

    default void checkJobId(int jobId) throws CatalogDBException {
        if (jobId < 0) {
            throw CatalogDBException.newInstance("Job id '{}' is not valid: ", jobId);
        }

        if (!jobExists(jobId)) {
            throw CatalogDBException.newInstance("Job id '{}' does not exist", jobId);
        }
    }

    QueryResult<Job> createJob(int studyId, Job job, QueryOptions options) throws CatalogDBException;

    @Deprecated
    default QueryResult<Job> deleteJob(int jobId) throws CatalogDBException {
        return delete(jobId);
    }

    default QueryResult<Job> getJob(int jobId, QueryOptions options) throws CatalogDBException {
        Query query = new Query(QueryParams.ID.key(), jobId);
        QueryResult<Job> jobQueryResult = get(query, options);
        if (jobQueryResult == null || jobQueryResult.getResult().size() == 0) {
            throw CatalogDBException.idNotFound("Job", jobId);
        }
        return jobQueryResult;
    }

    @Deprecated
    QueryResult<Job> getAllJobs(Query query, QueryOptions options) throws CatalogDBException;

    QueryResult<Job> getAllJobsInStudy(int studyId, QueryOptions options) throws CatalogDBException;

    String getJobStatus(int jobId, String sessionId) throws CatalogDBException;

    QueryResult<ObjectMap> incJobVisits(int jobId) throws CatalogDBException;

    QueryResult<Job> modifyJob(int jobId, ObjectMap parameters) throws CatalogDBException;

    int getStudyIdByJobId(int jobId) throws CatalogDBException;

    /*
     * Tool methods
     */

    QueryResult<Tool> createTool(String userId, Tool tool) throws CatalogDBException;

    QueryResult<Tool> getTool(int id) throws CatalogDBException;

    int getToolId(String userId, String toolAlias) throws CatalogDBException;

    QueryResult<Tool> getAllTools(Query query, QueryOptions queryOptions) throws CatalogDBException;

    /*
     * Experiments methods
     */

    boolean experimentExists(int experimentId);

//    public abstract QueryResult<Tool> searchTool(QueryOptions options);

    enum QueryParams implements QueryParam {
        ID("id", INTEGER_ARRAY, ""),
        NAME("name", TEXT_ARRAY, ""),
        USER_ID("userId", TEXT_ARRAY, ""),
        TOOL_NAME("toolName", TEXT_ARRAY, ""),
        DATE("date", TEXT_ARRAY, ""),
        DESCRIPTION("description", TEXT_ARRAY, ""),
        START_TIME("startTime", INTEGER_ARRAY, ""),
        END_TIME("endTime", INTEGER_ARRAY, ""),
        OUTPUT_ERROR("outputError", TEXT_ARRAY, ""),
        EXECUTION("execution", TEXT_ARRAY, ""),
        //PARAMS,
        COMMAND_LINE("commandLine", TEXT_ARRAY, ""),
        VISITS("visits", INTEGER_ARRAY, ""),
        STATUS("status", TEXT_ARRAY, ""),
        DISK_USAGE("diskUsage", DECIMAL, ""),
        OUT_DIR_ID("outDirId", INTEGER_ARRAY, ""),
        TMP_OUT_DIR_URI("tmpOutDirUri", TEXT_ARRAY, ""),
        INPUT("input", INTEGER_ARRAY, ""),
        OUTPUT("output", INTEGER_ARRAY, ""),
        TAGS("tags", TEXT_ARRAY, ""),
        ATTRIBUTES("attributes", TEXT, ""), // "Format: <key><operation><stringValue> where <operation> is [<|<=|>|>=|==|!=|~|!~]"
        NATTRIBUTES("nattributes", DECIMAL, ""), // "Format: <key><operation><numericalValue> where <operation> is [<|<=|>|>=|==|!=|~|!~]"
        BATTRIBUTES("battributes", BOOLEAN, ""), // "Format: <key><operation><true|false> where <operation> is [==|!=]"
        RESOURCE_MANAGER_ATTRIBUTES("resourceManagerAttributes", TEXT_ARRAY, ""),
        ERROR("error", TEXT_ARRAY, ""),
        ERROR_DESCRIPTION("errorDescription", TEXT_ARRAY, ""),

        STUDY_ID("studyId", INTEGER_ARRAY, "");

        private static Map<String, QueryParams> map = new HashMap<>();
        static {
            for (QueryParams params : QueryParams.values()) {
                map.put(params.key(), params);
            }
        }

        private final String key;
        private Type type;
        private String description;

        QueryParams(String key, Type type, String description) {
            this.key = key;
            this.type = type;
            this.description = description;
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public Type type() {
            return type;
        }

        @Override
        public String description() {
            return description;
        }

        public static Map<String, QueryParams> getMap() {
            return map;
        }

        public static QueryParams getParam(String key) {
            return map.get(key);
        }
    }

}
