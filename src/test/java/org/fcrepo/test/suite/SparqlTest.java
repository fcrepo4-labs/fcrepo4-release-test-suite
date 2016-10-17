/**
 *
 */

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
import org.fcrepo.client.PatchBuilder;
import org.fcrepo.client.PutBuilder;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;


/**
 * @author ylchen
 * @since 2016-09-06
 */
public class SparqlTest {

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
    public void testSparql() throws IOException, FcrepoOperationFailedException {

        // Create a container
        final InputStream turtleFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("object.ttl");
        final URI objectUri = URI.create(fedoraBaseURL + "/object");

        try (FcrepoResponse response = new PutBuilder(objectUri, testClient)
                .body(turtleFile, "text/turtle")
                .perform()) {

            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

        }

        // Set dc:title with SPARQL
        final InputStream setTitleSparqlFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("set_title.sparql");
        final URI objectMetadataUri = URI.create(fedoraBaseURL + "/object/fcr:metadata");

        try (FcrepoResponse response = new PatchBuilder(objectMetadataUri, testClient)
                .body(setTitleSparqlFile)
                .perform()) {

            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

        }

        // Update dc:title with SPARQL
        final InputStream updateTitleSparqlFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("update_title.sparql");
        try (FcrepoResponse response = new PatchBuilder(objectMetadataUri, testClient)
                .body(updateTitleSparqlFile)
                .perform()) {

            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

        }

        // Create a binary
        final URI imageUri = URI.create(fedoraBaseURL + "/image");
        final InputStream imageFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("basic_image.jpg");
        try (FcrepoResponse response = new PutBuilder(imageUri, testClient)
                .body(imageFile, "image/jpeg")
                .perform()) {

            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

        }

        // Set dc:title with SPARQL
        final InputStream setBinaryTitleSparqlFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("set_title.sparql");
        final URI imageMetadataUri = URI.create(fedoraBaseURL + "/image/fcr:metadata");

        try (FcrepoResponse response = new PatchBuilder(imageMetadataUri, testClient)
                .body(setBinaryTitleSparqlFile)
                .perform()) {

            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

        }

        // Update dc:title with SPARQL
        final InputStream updateBinaryTitleSparqlFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("update_title.sparql");
        try (FcrepoResponse response = new PatchBuilder(imageMetadataUri, testClient)
                .body(updateBinaryTitleSparqlFile)
                .perform()) {

            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

        }

        // Delete a binary
        try (FcrepoResponse response = new DeleteBuilder(imageUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

        // Delete a binary permanently
        final URI binaryTombstoneUri = URI.create(fedoraBaseURL + "/image/fcr:tombstone");

        try (FcrepoResponse response = new DeleteBuilder(binaryTombstoneUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

        // Delete a container
        try (FcrepoResponse response = new DeleteBuilder(objectUri, testClient)
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
