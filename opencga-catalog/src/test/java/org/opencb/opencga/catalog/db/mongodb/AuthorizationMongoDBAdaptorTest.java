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

package org.opencb.opencga.catalog.db.mongodb;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.commons.datastore.core.QueryResult;
import org.opencb.opencga.catalog.auth.authorization.AuthorizationDBAdaptor;
import org.opencb.opencga.catalog.db.DBAdaptorFactory;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.utils.UUIDUtils;
import org.opencb.opencga.core.common.Entity;
import org.opencb.opencga.core.config.Configuration;
import org.opencb.opencga.core.models.*;
import org.opencb.opencga.core.models.acls.permissions.AbstractAclEntry;
import org.opencb.opencga.core.models.acls.permissions.SampleAclEntry;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Created by pfurio on 21/04/17.
 */
public class AuthorizationMongoDBAdaptorTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private AuthorizationDBAdaptor aclDBAdaptor;
    private DBAdaptorFactory dbAdaptorFactory;
    private User user1;
    private User user2;
    private User user3;
    private long studyId;
    private Sample s1;
    private SampleAclEntry acl_s1_user1;
    private SampleAclEntry acl_s1_user2;

    @AfterClass
    public static void afterClass() {
        MongoDBAdaptorTest.afterClass();
    }

    @Before
    public void before() throws IOException, CatalogException {
        MongoDBAdaptorTest dbAdaptorTest = new MongoDBAdaptorTest();
        dbAdaptorTest.before();

        Configuration configuration = Configuration.load(getClass().getResource("/configuration-test.yml").openStream());

        user1 = MongoDBAdaptorTest.user1;
        user2 = MongoDBAdaptorTest.user2;
        user3 = MongoDBAdaptorTest.user3;
        dbAdaptorFactory = MongoDBAdaptorTest.catalogDBAdaptor;
        aclDBAdaptor = new AuthorizationMongoDBAdaptor(configuration);

        studyId = user3.getProjects().get(0).getStudies().get(0).getUid();
        s1 = dbAdaptorFactory.getCatalogSampleDBAdaptor().insert(studyId, new Sample("s1", "", new Individual(), "", "", false, 1, 1,
                Collections.emptyList(), new ArrayList<>(), Collections.emptyMap(), Collections.emptyMap()), QueryOptions.empty()).first();
        acl_s1_user1 = new SampleAclEntry(user1.getId(), Arrays.asList());
        acl_s1_user2 = new SampleAclEntry(user2.getId(), Arrays.asList(
                SampleAclEntry.SamplePermissions.VIEW.name(),
                SampleAclEntry.SamplePermissions.VIEW_ANNOTATIONS.name(),
                SampleAclEntry.SamplePermissions.UPDATE.name()
        ));
        aclDBAdaptor.setAcls(Arrays.asList(s1.getUid()), Arrays.asList(acl_s1_user1, acl_s1_user2), Entity.SAMPLE);
    }

    @Test
    public void addSetGetAndRemoveAcls() throws Exception {

        aclDBAdaptor.resetMembersFromAllEntries(studyId, Arrays.asList(user1.getId(), user2.getId()));

        aclDBAdaptor.addToMembers(Arrays.asList(s1.getUid()), Arrays.asList("user1", "user2", "user3"), Arrays.asList("VIEW", "UPDATE"),
                Entity.SAMPLE);
        aclDBAdaptor.addToMembers(Arrays.asList(s1.getUid()), Arrays.asList("user4"), Collections.emptyList(),
                Entity.SAMPLE);
        // We attempt to store the same permissions
        aclDBAdaptor.addToMembers(Arrays.asList(s1.getUid()), Arrays.asList("user1", "user2", "user3"), Arrays.asList("VIEW", "UPDATE"),
                Entity.SAMPLE);

        QueryResult<SampleAclEntry> sampleAcl = aclDBAdaptor.get(s1.getUid(), null, Entity.SAMPLE);
        assertEquals(4, sampleAcl.getNumResults());

        sampleAcl = aclDBAdaptor.get(s1.getUid(), Arrays.asList("user1", "user2"), Entity.SAMPLE);
        assertEquals(2, sampleAcl.getNumResults());
        for (SampleAclEntry sampleAclEntry : sampleAcl.getResult()) {
            assertTrue(sampleAclEntry.getPermissions().contains(SampleAclEntry.SamplePermissions.valueOf("VIEW")));
            assertTrue(sampleAclEntry.getPermissions().contains(SampleAclEntry.SamplePermissions.valueOf("UPDATE")));
        }

        // Todo: Remove this in 1.4
        List<String> allSamplePermissions = EnumSet.allOf(SampleAclEntry.SamplePermissions.class)
                .stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
        aclDBAdaptor.setToMembers(Arrays.asList(s1.getUid()), Arrays.asList("user1"), Arrays.asList("DELETE"), allSamplePermissions,
                Entity.SAMPLE);
        sampleAcl = aclDBAdaptor.get(s1.getUid(), Arrays.asList("user1", "user2"), Entity.SAMPLE);
        assertEquals(2, sampleAcl.getNumResults());
        for (SampleAclEntry sampleAclEntry : sampleAcl.getResult()) {
            if (sampleAclEntry.getMember().equals("user1")) {
                assertTrue(sampleAclEntry.getPermissions().contains(SampleAclEntry.SamplePermissions.valueOf("DELETE")));
            } else {
                assertEquals("user2", sampleAclEntry.getMember());
                assertTrue(sampleAclEntry.getPermissions().contains(SampleAclEntry.SamplePermissions.valueOf("VIEW")));
                assertTrue(sampleAclEntry.getPermissions().contains(SampleAclEntry.SamplePermissions.valueOf("UPDATE")));
            }
        }

        // Remove one permission from one user
        aclDBAdaptor.removeFromMembers(Arrays.asList(s1.getUid()), Arrays.asList("user1"), Arrays.asList("DELETE"),
                Entity.SAMPLE);
        sampleAcl = aclDBAdaptor.get(s1.getUid(), Arrays.asList("user1"), Entity.SAMPLE);
        assertEquals(1, sampleAcl.getNumResults());
        assertEquals(0, sampleAcl.first().getPermissions().size());

        // Reset user
        aclDBAdaptor.removeFromMembers(Arrays.asList(s1.getUid()), Arrays.asList("user1"), null,
                Entity.SAMPLE);
        sampleAcl = aclDBAdaptor.get(s1.getUid(), Arrays.asList("user1"), Entity.SAMPLE);
        assertEquals(0, sampleAcl.getNumResults());

        // Remove from all samples (there is only one) in study
        aclDBAdaptor.removeFromStudy(studyId, "user3", Entity.SAMPLE);
        sampleAcl = aclDBAdaptor.get(s1.getUid(), Arrays.asList("user3"), Entity.SAMPLE);
        assertEquals(0, sampleAcl.getNumResults());

        sampleAcl = aclDBAdaptor.get(s1.getUid(), null, Entity.SAMPLE);
        assertEquals(2, sampleAcl.getNumResults());
        for (SampleAclEntry sampleAclEntry : sampleAcl.getResult()) {
            if (sampleAclEntry.getMember().equals("user2")) {
                assertTrue(sampleAclEntry.getPermissions().contains(SampleAclEntry.SamplePermissions.valueOf("VIEW")));
                assertTrue(sampleAclEntry.getPermissions().contains(SampleAclEntry.SamplePermissions.valueOf("UPDATE")));
            } else {
                assertEquals("user4", sampleAclEntry.getMember());
                assertEquals(0, sampleAclEntry.getPermissions().size());
            }
        }

        // Reset user4
        aclDBAdaptor.removeFromMembers(Arrays.asList(s1.getUid()), Arrays.asList("user4"), null, Entity.SAMPLE);
        sampleAcl = aclDBAdaptor.get(s1.getUid(), null, Entity.SAMPLE);
        assertEquals(1, sampleAcl.getNumResults());
        assertEquals("user2", sampleAcl.first().getMember());
        assertTrue(sampleAcl.first().getPermissions().contains(SampleAclEntry.SamplePermissions.valueOf("VIEW")));
        assertTrue(sampleAcl.first().getPermissions().contains(SampleAclEntry.SamplePermissions.valueOf("UPDATE")));
    }

    @Test
    public void getSampleAcl() throws Exception {
        QueryResult<SampleAclEntry> sampleAcl = aclDBAdaptor.get(s1.getUid(), Arrays.asList(user1.getId()),
                Entity.SAMPLE);
        SampleAclEntry acl = sampleAcl.first();
        assertNotNull(acl);
        assertEquals(acl_s1_user1.getPermissions(), acl.getPermissions());

        acl = (SampleAclEntry) aclDBAdaptor.get(s1.getUid(), Arrays.asList(user2.getId()), Entity.SAMPLE).first();
        assertNotNull(acl);
        assertEquals(acl_s1_user2.getPermissions(), acl.getPermissions());
    }

    @Test
    public void getSampleAclWrongUser() throws Exception {
        QueryResult<SampleAclEntry> wrongUser = aclDBAdaptor.get(s1.getUid(), Arrays.asList("wrongUser"),
                Entity.SAMPLE);
        assertEquals(0, wrongUser.getNumResults());
    }

    @Test
    public void getSampleAclFromUserWithoutAcl() throws Exception {
        QueryResult<SampleAclEntry> sampleAcl = aclDBAdaptor.get(s1.getUid(), Arrays.asList(user3.getId()),
                Entity.SAMPLE);
        assertTrue(sampleAcl.getResult().isEmpty());
    }

    // Remove some concrete permissions
    @Test
    public void unsetSampleAcl2() throws Exception {
        // Unset permissions
        QueryResult<SampleAclEntry> sampleAcl = aclDBAdaptor.get(s1.getUid(), Arrays.asList(user2.getId()),
                Entity.SAMPLE);
        assertEquals(1, sampleAcl.getNumResults());
        assertEquals(3, sampleAcl.first().getPermissions().size());
        aclDBAdaptor.removeFromMembers(Arrays.asList(s1.getUid()), Arrays.asList(user2.getId()),
                Arrays.asList("VIEW_ANNOTATIONS", "DELETE", "VIEW"), Entity.SAMPLE);
//        sampleDBAdaptor.unsetSampleAcl(s1.getId(), Arrays.asList(user2.getId()),
//                Arrays.asList("VIEW_ANNOTATIONS", "DELETE", "VIEW"));
        sampleAcl = aclDBAdaptor.get(s1.getUid(), Arrays.asList(user2.getId()), Entity.SAMPLE);
        assertEquals(1, sampleAcl.getNumResults());
        assertEquals(1, sampleAcl.first().getPermissions().size());
        assertTrue(sampleAcl.first().getPermissions().containsAll(Arrays.asList(SampleAclEntry.SamplePermissions.UPDATE)));

    }

    @Test
    public void setSampleAclOverride() throws Exception {
        assertEquals(acl_s1_user2.getPermissions(),
                aclDBAdaptor.get(s1.getUid(), Arrays.asList(user2.getId()), Entity.SAMPLE).first().getPermissions());

        SampleAclEntry newAcl = new SampleAclEntry(user2.getId(), Arrays.asList(SampleAclEntry.SamplePermissions.DELETE.name()));
        assertTrue(!acl_s1_user2.getPermissions().equals(newAcl.getPermissions()));
        // Todo: Remove this in 1.4
        List<String> allSamplePermissions = EnumSet.allOf(SampleAclEntry.SamplePermissions.class)
                .stream()
                .map(String::valueOf)
                .collect(Collectors.toList());

        aclDBAdaptor.setToMembers(Arrays.asList(s1.getUid()), Arrays.asList(user2.getId()),
                Arrays.asList(SampleAclEntry.SamplePermissions.DELETE.name()), allSamplePermissions,
                Entity.SAMPLE);
//        sampleDBAdaptor.setSampleAcl(s1.getId(), newAcl, true);

        assertEquals(newAcl.getPermissions(), aclDBAdaptor.get(s1.getUid(), Arrays.asList(user2.getId()),
                Entity.SAMPLE).first().getPermissions());
    }

    @Test
    public void testPermissionRulesPlusManualPermissions() throws CatalogException {
        // We create a new sample s2
        Sample s2 = dbAdaptorFactory.getCatalogSampleDBAdaptor().insert(studyId, new Sample("s2", "", new Individual(), "", "", false,
                1, 1, Collections.emptyList(), new ArrayList<>(), Collections.emptyMap(), Collections.emptyMap()),
                QueryOptions.empty()).first();

        // We create a new permission rule
        PermissionRule pr = new PermissionRule("myPermissionRule", new Query(), Arrays.asList(user3.getId()),
                Arrays.asList(SampleAclEntry.SamplePermissions.VIEW.name()));
        dbAdaptorFactory.getCatalogStudyDBAdaptor().createPermissionRule(studyId, Study.Entity.SAMPLES, pr);

        // Apply the permission rule
        aclDBAdaptor.applyPermissionRules(studyId, pr, Study.Entity.SAMPLES);

        // All the samples should have view permissions for user user2
        List<QueryResult<AbstractAclEntry>> queryResults = aclDBAdaptor.get(Arrays.asList(s1.getUid(), s2.getUid()),
                Arrays.asList(user3.getId()), Entity.SAMPLE);
        for (QueryResult<AbstractAclEntry> queryResult : queryResults) {
            assertEquals(1, queryResult.first().getPermissions().size());
            assertTrue(queryResult.first().getPermissions().contains(SampleAclEntry.SamplePermissions.VIEW));
        }

        // Assign a manual permission to s2
        aclDBAdaptor.addToMembers(Arrays.asList(s2.getUid()), Arrays.asList(user3.getId()),
                Arrays.asList(SampleAclEntry.SamplePermissions.DELETE.name()), Entity.SAMPLE);

    }

}