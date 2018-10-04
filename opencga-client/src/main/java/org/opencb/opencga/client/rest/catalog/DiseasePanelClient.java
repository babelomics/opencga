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

import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.QueryResponse;
import org.opencb.opencga.client.config.ClientConfiguration;
import org.opencb.opencga.client.exceptions.ClientException;
import org.opencb.opencga.core.models.DiseasePanel;
import org.opencb.opencga.core.models.acls.permissions.DiseasePanelAclEntry;

import java.io.IOException;

/**
 * Created by pfurio on 10/06/16.
 */
public class DiseasePanelClient extends CatalogClient<DiseasePanel, DiseasePanelAclEntry> {
    private static final String PANEL_URL = "diseasePanels";

    public DiseasePanelClient(String userId, String sessionId, ClientConfiguration configuration) {
        super(userId, sessionId, configuration);

        this.category = PANEL_URL;
        this.clazz = DiseasePanel.class;
        this.aclClass = DiseasePanelAclEntry.class;
    }

    public QueryResponse<DiseasePanel> create(String studyId, ObjectMap bodyParams) throws IOException, ClientException {
        if (bodyParams == null || bodyParams.size() == 0) {
            throw new ClientException("Missing body parameters");
        }

        ObjectMap params = new ObjectMap("body", bodyParams);
        params.append("study", studyId);
        return execute(PANEL_URL, "create", params, POST, DiseasePanel.class);
    }

    public QueryResponse<ObjectMap> groupBy(String studyId, String fields, ObjectMap params) throws IOException {
        params = addParamsToObjectMap(params, "study", studyId, "fields", fields);
        return execute(PANEL_URL, "groupBy", params, GET, ObjectMap.class);
    }

}
