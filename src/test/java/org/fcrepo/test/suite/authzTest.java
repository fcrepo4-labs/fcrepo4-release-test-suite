package org.fcrepo.test.suite;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

import org.apache.http.HttpStatus;
import org.fcrepo.client.DeleteBuilder;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.GetBuilder;
import org.fcrepo.client.PatchBuilder;
import org.fcrepo.client.PutBuilder;
import org.junit.Before;
import org.junit.Test;


/**
 * @author ylchen
 * @since 2016-09-06
 */
public class authzTest {

    String fedoraBaseURL;

    String fedoraAdminPassword = "";

    String testpassword = "";

    String adminuserpassword = "";

    FcrepoClient testClient;

    @Before
    public void setUp() throws IOException {

        final InputStream configFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("config.properties");

        final Properties p = new Properties();

        p.load(configFile);
        fedoraBaseURL = p.get("fedoraBaseURL").toString();
        testpassword = p.get("testpassword").toString();
        fedoraAdminPassword = p.get("fedoraadminpassword").toString();
        adminuserpassword = p.get("adminuserpassword").toString();
        testClient = FcrepoClient.client().credentials("fedoraAdmin", fedoraAdminPassword).build();

    }

    @Test
    public void testAuth() throws IOException, FcrepoOperationFailedException {

        // Create a cover container
        final URI coverUri = URI.create(fedoraBaseURL + "/cover");

        try (FcrepoResponse response = new PutBuilder(coverUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        }

        // Make cover a pcdm:Object
        final InputStream cover2pcdmObjectFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("cover2pcdmObject.sparql");
        try (FcrepoResponse response = new PatchBuilder(coverUri, testClient)
                .body(cover2pcdmObjectFile)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

        // Create files inside cover
        final URI coverFileUri = URI.create(fedoraBaseURL + "/cover/files");
        try (FcrepoResponse response = new PutBuilder(coverFileUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        }

        // Create my-acls at root level
        final URI myAclsUri = URI.create(fedoraBaseURL + "/my-acls");
        try (FcrepoResponse response = new PutBuilder(myAclsUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        }

        // Create acl inside my-acls
        final URI aAclsUri = URI.create(fedoraBaseURL + "/my-acls/acl");
        try (FcrepoResponse response = new PutBuilder(aAclsUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        }

        // Create authorization inside acl
        final URI authorizationUri = URI.create(fedoraBaseURL + "/my-acls/acl/authorization");
        try (FcrepoResponse response = new PutBuilder(authorizationUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        }

        // Patch authorization with a WebAC Authorization
        final InputStream authorizationFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("authorization.sparql");
        try (FcrepoResponse response = new PatchBuilder(authorizationUri, testClient)
                .body(authorizationFile)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

        // Patch cover to add acl as an access control
        final InputStream linkAclPatchFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("link_acl_patch.sparql");
        try (FcrepoResponse response = new PatchBuilder(coverUri, testClient)
                .body(linkAclPatchFile)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

        // Verify Anonymous can't access cover
        final FcrepoClient anonymousClient = FcrepoClient.client().build();
        try (FcrepoResponse response = new GetBuilder(coverUri, anonymousClient)
                .perform()) {
            assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusCode());
        }

        // Verify fedoraAdmin can access cover
        final FcrepoClient fedoraAdminClient = FcrepoClient.client().credentials("fedoraAdmin", fedoraAdminPassword)
                .build();
        try (FcrepoResponse response = new GetBuilder(coverUri, fedoraAdminClient)
                .perform()) {
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        }

        // Verify adminuser can access cover
        final FcrepoClient adminuserClient = FcrepoClient.client().credentials("adminuser", adminuserpassword)
                .build();
        try (FcrepoResponse response = new GetBuilder(coverUri, adminuserClient)
                .perform()) {
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        }

        // Verify testuser can't access cover
        final FcrepoClient testuserClient = FcrepoClient.client().credentials("testuser", testpassword).build();
        try (FcrepoResponse response = new GetBuilder(coverUri, testuserClient)
                .perform()) {
            assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusCode());
        }

        // Delete cover container
        try (FcrepoResponse response = new DeleteBuilder(coverUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

        // Delete cover container permanently
        final URI coverTombstoneUri = URI.create(fedoraBaseURL + "/cover/fcr:tombstone");

        try (FcrepoResponse response = new DeleteBuilder(coverTombstoneUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

        // Delete my-acls container
        try (FcrepoResponse response = new DeleteBuilder(myAclsUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

        // Delete my-acls container permanently
        final URI myAclsTombstoneUri = URI.create(fedoraBaseURL + "/my-acls/fcr:tombstone");

        try (FcrepoResponse response = new DeleteBuilder(myAclsTombstoneUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

    }

}
