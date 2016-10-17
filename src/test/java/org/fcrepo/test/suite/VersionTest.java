
package org.fcrepo.test.suite;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

import org.fcrepo.client.DeleteBuilder;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.GetBuilder;
import org.fcrepo.client.PatchBuilder;
import org.fcrepo.client.PostBuilder;
import org.fcrepo.client.PutBuilder;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ylchen
 * @since 2016-09-06
 */
public class VersionTest {

    String fedoraBaseURL;

    String fedoraAdminPassword = "";

    FcrepoClient testClient;

    @Before
    public void setUp() throws IOException {

        final InputStream configFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("config.properties");

        final Properties p = new Properties();

        p.load(configFile);
        fedoraBaseURL = p.get("fedoraBaseURL").toString();
        fedoraAdminPassword = p.get("fedoraadminpassword").toString();
        testClient = FcrepoClient.client().credentials("fedoraAdmin", fedoraAdminPassword).build();

    }

    @Test
    public void testVersionOperations() throws IOException, FcrepoOperationFailedException {

        // Create a container
        final InputStream turtleFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("object.ttl");
        final URI object1Uri = URI.create(fedoraBaseURL + "/object");

        try (FcrepoResponse response = new PutBuilder(object1Uri, testClient)
                .body(turtleFile, "text/turtle")
                .perform()) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        }

        // Check for versions of the container
        final URI object1VersionUri = URI.create(fedoraBaseURL + "/object/fcr:versions");
        try (FcrepoResponse response = new GetBuilder(object1VersionUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());
        }

        // Create a version of the container
        try (FcrepoResponse response = new PostBuilder(object1VersionUri, testClient)
                .slug("version1")
                .perform()) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        }

        // Try to create a version of the container with the same label
        try (FcrepoResponse response = new PostBuilder(object1VersionUri, testClient)
                .slug("version1")
                .perform()) {
            assertEquals(HttpStatus.SC_CONFLICT, response.getStatusCode());
        }

        // Update the container with a Patch request
        final InputStream sparqlFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("update_title.sparql");
        try (FcrepoResponse response = new PatchBuilder(object1Uri, testClient)
                .body(sparqlFile)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

        // Create another version of the container
        try (FcrepoResponse response = new PostBuilder(object1VersionUri, testClient)
                .slug("version2")
                .perform()) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        }

        // Try to delete the current version
        final URI object1Version2Uri = URI.create(fedoraBaseURL + "/object/fcr:versions/version2");
        try (FcrepoResponse response = new DeleteBuilder(object1Version2Uri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        }

        // Revert to previous version
        final URI object1Version1Uri = URI.create(fedoraBaseURL + "/object/fcr:versions/version1");
        try (FcrepoResponse response = new PatchBuilder(object1Version1Uri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

        // Delete the newer version
        try (FcrepoResponse response = new DeleteBuilder(object1Version2Uri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

        // Delete a container
        try (FcrepoResponse response = new DeleteBuilder(object1Uri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

        // Delete a container permanently
        final URI objectTombstoneUri = URI.create(fedoraBaseURL + "/object/fcr:tombstone");
        try (FcrepoResponse response = new DeleteBuilder(objectTombstoneUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

    }

}
