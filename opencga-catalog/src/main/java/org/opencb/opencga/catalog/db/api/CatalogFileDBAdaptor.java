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
import org.opencb.opencga.catalog.db.AbstractCatalogDBAdaptor;
import org.opencb.opencga.catalog.exceptions.CatalogDBException;
import org.opencb.opencga.catalog.models.AclEntry;
import org.opencb.opencga.catalog.models.Dataset;
import org.opencb.opencga.catalog.models.File;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opencb.commons.datastore.core.QueryParam.Type.*;

/**
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public interface CatalogFileDBAdaptor extends CatalogDBAdaptor<File> {

    default boolean fileExists(int fileId) throws CatalogDBException {
        return count(new Query(QueryParams.ID.key(), fileId)).first() > 0;
    }

    default void checkFileId(int fileId) throws CatalogDBException {
        if (fileId < 0) {
            throw CatalogDBException.newInstance("File id '{}' is not valid: ", fileId);
        }

        long count = count(new Query(QueryParams.ID.key(), fileId)).first();
        if (count <= 0) {
            throw CatalogDBException.newInstance("File id '{}' does not exist", fileId);
        } else if (count > 1) {
            throw CatalogDBException.newInstance("'{}' documents found with the File id '{}'", count, fileId);
        }
    }

    int getFileId(int studyId, String path) throws CatalogDBException;

    int getStudyIdByFileId(int fileId) throws CatalogDBException;

    String getFileOwnerId(int fileId) throws CatalogDBException;

    QueryResult<File> createFile(int studyId, File file, QueryOptions options) throws CatalogDBException;

    QueryResult<File> getFile(int fileId, QueryOptions options) throws CatalogDBException;

    @Deprecated
    default QueryResult<File> getAllFiles(Query query, QueryOptions options) throws CatalogDBException {
        return get(query, options);
    }

    QueryResult<File> getAllFilesInStudy(int studyId, QueryOptions options) throws CatalogDBException;

    QueryResult<File> getAllFilesInFolder(int studyId, String path, QueryOptions options) throws CatalogDBException;

    @Deprecated
    QueryResult<File> modifyFile(int fileId, ObjectMap parameters) throws CatalogDBException;

    QueryResult<File> renameFile(int fileId, String filePath, QueryOptions options) throws CatalogDBException;

    @Deprecated
    QueryResult<File> deleteFile(int fileId) throws CatalogDBException;

    /*
     * ACL methods
     * ***************************
     */

    QueryResult<AclEntry> getFileAcl(int fileId, String userId) throws CatalogDBException;

    QueryResult<Map<String, Map<String, AclEntry>>> getFilesAcl(int studyId, List<String> filePaths, List<String> userIds) throws
            CatalogDBException;

    QueryResult<AclEntry> setFileAcl(int fileId, AclEntry newAcl) throws CatalogDBException;

    QueryResult<AclEntry> unsetFileAcl(int fileId, String userId) throws CatalogDBException;

    /*
     * Dataset methods
     * ***************************
     */

    int getStudyIdByDatasetId(int datasetId) throws CatalogDBException;

    QueryResult<Dataset> createDataset(int studyId, Dataset dataset, QueryOptions options) throws CatalogDBException;

    QueryResult<Dataset> getDataset(int datasetId, QueryOptions options) throws CatalogDBException;

    enum QueryParams implements QueryParam {
        DELETE_DATE("deleteDate", TEXT_ARRAY, ""),
        ID("id", INTEGER_ARRAY, ""),
        NAME("name", TEXT_ARRAY, ""),
        TYPE("type", TEXT_ARRAY, ""),
        FORMAT("format", TEXT_ARRAY, ""),
        BIOFORMAT("bioformat", TEXT_ARRAY, ""),
        URI("uri", TEXT_ARRAY, ""),
        PATH("path", TEXT_ARRAY, ""),
        OWNER_ID("ownerId", TEXT_ARRAY, ""),
        CREATION_DATE("creationDate", TEXT_ARRAY, ""),
        MODIFICATION_DATE("modificationDate", TEXT_ARRAY, ""),
        DESCRIPTION("description", TEXT_ARRAY, ""),
        STATUS("status", TEXT_ARRAY, ""),
        DISK_USAGE("diskUsage", INTEGER_ARRAY, ""),
        EXPERIMENT_ID("experimentId", INTEGER_ARRAY, ""),
        SAMPLE_IDS("sampleIds", INTEGER_ARRAY, ""),

        JOB_ID("jobId", INTEGER_ARRAY, ""),
        // TOCHECK: Pedro. Check parameter user_others_id.
        ACL_USER_ID("acl.userId", TEXT_ARRAY, ""),
        ACL_READ("acl.read", BOOLEAN, ""),
        ACL_WRITE("acl.write", BOOLEAN, ""),
        ACL_EXECUTE("acl.execute", BOOLEAN, ""),
        ACL_DELETE("acl.delete", BOOLEAN, ""),

        INDEX_USER_ID("index.userId", TEXT, ""),
        INDEX_DATE("index.date", TEXT, ""),
        INDEX_STATUS("index.status", TEXT_ARRAY, ""),
        INDEX_JOB_ID("index.jobId", TEXT, ""),

        ATTRIBUTES("attributes", TEXT, ""), // "Format: <key><operation><stringValue> where <operation> is [<|<=|>|>=|==|!=|~|!~]"
        NATTRIBUTES("nattributes", DECIMAL, ""), // "Format: <key><operation><numericalValue> where <operation> is [<|<=|>|>=|==|!=|~|!~]"
        BATTRIBUTES("battributes", BOOLEAN, ""), // "Format: <key><operation><true|false> where <operation> is [==|!=]"
        STATS("stats", TEXT, ""),
        NSTATS("nstats", DECIMAL, ""),

        DIRECTORY("directory", TEXT, ""),
        STUDY_ID("studyId", INTEGER_ARRAY, "");

        // Fixme: Index attributes
        private static Map<String, QueryParams> map = new HashMap<>();
        static {
            for (QueryParams param : QueryParams.values()) {
                map.put(param.key(), param);
            }
        }

        // TOCHECK: Pedro. Add annotation support?

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

    enum FileFilterOption implements AbstractCatalogDBAdaptor.FilterOption {
        studyId(Type.NUMERICAL, ""),
        directory(Type.TEXT, ""),

        id(Type.NUMERICAL, ""),
        name(Type.TEXT, ""),
        type(Type.TEXT, ""),
        format(Type.TEXT, ""),
        bioformat(Type.TEXT, ""),
        uri(Type.TEXT, ""),
        path(Type.TEXT, ""),
        ownerId(Type.TEXT, ""),
        creationDate(Type.TEXT, ""),
        modificationDate(Type.TEXT, ""),
        description(Type.TEXT, ""),
        status(Type.TEXT, ""),
        diskUsage(Type.NUMERICAL, ""),
        experimentId(Type.NUMERICAL, ""),
        sampleIds(Type.NUMERICAL, ""),
        jobId(Type.NUMERICAL, ""),
        acl(Type.TEXT, ""),
        bacl("acl", Type.BOOLEAN, ""),
        index("index", Type.TEXT, ""),

        stats(Type.TEXT, ""),
        nstats("stats", Type.NUMERICAL, ""),

        attributes(Type.TEXT, "Format: <key><operation><stringValue> where <operation> is [<|<=|>|>=|==|!=|~|!~]"),
        nattributes("attributes", Type.NUMERICAL, "Format: <key><operation><numericalValue> where <operation> is [<|<=|>|>=|==|!=|~|!~]"),
        battributes("attributes", Type.BOOLEAN, "Format: <key><operation><true|false> where <operation> is [==|!=]"),

        @Deprecated maxSize(Type.NUMERICAL, ""),
        @Deprecated minSize(Type.NUMERICAL, ""),
        @Deprecated startDate(Type.TEXT, ""),
        @Deprecated endDate(Type.TEXT, ""),
        @Deprecated like(Type.TEXT, ""),
        @Deprecated startsWith(Type.TEXT, "");

        private final String _key;
        private final String _description;
        private final Type _type;

        FileFilterOption(Type type, String description) {
            this._key = name();
            this._description = description;
            this._type = type;
        }

        FileFilterOption(String key, Type type, String description) {
            this._key = key;
            this._description = description;
            this._type = type;
        }

        @Override
        public String getDescription() {
            return _description;
        }

        @Override
        public Type getType() {
            return _type;
        }

        @Override
        public String getKey() {
            return _key;
        }
    }

}
