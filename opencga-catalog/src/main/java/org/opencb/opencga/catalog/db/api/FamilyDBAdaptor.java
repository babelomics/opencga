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

package org.opencb.opencga.catalog.db.api;

import org.apache.commons.collections.map.LinkedMap;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.commons.datastore.core.QueryParam;
import org.opencb.commons.datastore.core.QueryResult;
import org.opencb.opencga.catalog.exceptions.CatalogDBException;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.managers.AnnotationSetManager;
import org.opencb.opencga.core.models.Family;
import org.opencb.opencga.core.models.VariableSet;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.opencb.commons.datastore.core.QueryParam.Type.*;

/**
 * Created by pfurio on 03/05/17.
 */
public interface FamilyDBAdaptor extends AnnotationSetDBAdaptor<Family> {

    enum QueryParams implements QueryParam {
        ID("id", TEXT, ""),
        UID("uid", INTEGER, ""),
        UUID("uuid", TEXT, ""),
        NAME("name", TEXT, ""),
        MEMBERS("members", TEXT_ARRAY, ""),
        MEMBER_UID("members.uid", INTEGER, ""),
        MEMBER_VERSION("members.version", INTEGER, ""),
        MEMBERS_PARENTAL_CONSANGUINITY("members.parentalConsanguinity", BOOLEAN, ""),
        CREATION_DATE("creationDate", DATE, ""),
        MODIFICATION_DATE("modificationDate", DATE, ""),
        DESCRIPTION("description", TEXT, ""),
        EXPECTED_SIZE("expectedSize", INTEGER, ""),
        ATTRIBUTES("attributes", TEXT, ""), // "Format: <key><operation><stringValue> where <operation> is [<|<=|>|>=|==|!=|~|!~]"
        NATTRIBUTES("nattributes", DECIMAL, ""), // "Format: <key><operation><numericalValue> where <operation> is [<|<=|>|>=|==|!=|~|!~]"
        BATTRIBUTES("battributes", BOOLEAN, ""), // "Format: <key><operation><true|false> where <operation> is [==|!=]"
        STATUS("status", TEXT_ARRAY, ""),
        STATUS_NAME("status.name", TEXT, ""),
        STATUS_MSG("status.msg", TEXT, ""),
        STATUS_DATE("status.date", TEXT, ""),
        RELEASE("release", INTEGER, ""),
        SNAPSHOT("snapshot", INTEGER, ""), // Last version of individual at release = snapshot
        VERSION("version", INTEGER, ""), // Version of the individual

        PHENOTYPES("phenotypes", TEXT_ARRAY, ""),
        PHENOTYPES_ID("phenotypes.id", TEXT, ""),
        PHENOTYPES_NAME("phenotypes.name", TEXT, ""),
        PHENOTYPES_SOURCE("phenotypes.source", TEXT, ""),

        STUDY_UID("studyUid", INTEGER_ARRAY, ""),
        STUDY("study", INTEGER_ARRAY, ""), // Alias to studyId in the database. Only for the webservices.

        ANNOTATION_SETS("annotationSets", TEXT_ARRAY, ""),
        ANNOTATION("annotation", TEXT_ARRAY, "");

        private static Map<String, QueryParams> map;
        static {
            map = new LinkedMap();
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

    enum UpdateParams {
        ID(QueryParams.ID.key()),
        NAME(QueryParams.NAME.key()),
        PHENOTYPES(QueryParams.PHENOTYPES.key()),
        MEMBERS(QueryParams.MEMBERS.key()),
        DESCRIPTION(QueryParams.DESCRIPTION.key()),
        EXPECTED_SIZE(QueryParams.EXPECTED_SIZE.key()),
        ATTRIBUTES(QueryParams.ATTRIBUTES.key()),
        ANNOTATION_SETS(QueryParams.ANNOTATION_SETS.key()),
        ANNOTATIONS(AnnotationSetManager.ANNOTATIONS);

        private static Map<String, UpdateParams> map;
        static {
            map = new LinkedMap();
            for (UpdateParams params : UpdateParams.values()) {
                map.put(params.key(), params);
            }
        }

        private final String key;

        UpdateParams(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }

        public static UpdateParams getParam(String key) {
            return map.get(key);
        }
    }

    default boolean exists(long familyId) throws CatalogDBException {
        return count(new Query(QueryParams.UID.key(), familyId)).first() > 0;
    }

    default void checkId(long familyId) throws CatalogDBException {
        if (familyId < 0) {
            throw CatalogDBException.newInstance("Family id '{}' is not valid: ", familyId);
        }

        if (!exists(familyId)) {
            throw CatalogDBException.newInstance("Family id '{}' does not exist", familyId);
        }
    }

    void nativeInsert(Map<String, Object> family, String userId) throws CatalogDBException;

    default QueryResult<Family> insert(long studyId, Family family, QueryOptions options) throws CatalogDBException {
        family.setAnnotationSets(Collections.emptyList());
        return insert(studyId, family, Collections.emptyList(), options);
    }

    QueryResult<Family> insert(long studyId, Family family, List<VariableSet> variableSetList, QueryOptions options)
            throws CatalogDBException;

    QueryResult<Family> get(long familyId, QueryOptions options) throws CatalogDBException;

    long getStudyId(long familyId) throws CatalogDBException;

    void updateProjectRelease(long studyId, int release) throws CatalogDBException;

    /**
     * Removes the mark of the permission rule (if existed) from all the entries from the study to notify that permission rule would need to
     * be applied.
     *
     * @param studyId study id containing the entries affected.
     * @param permissionRuleId permission rule id to be unmarked.
     * @throws CatalogException if there is any database error.
     */
    void unmarkPermissionRule(long studyId, String permissionRuleId) throws CatalogException;
}
