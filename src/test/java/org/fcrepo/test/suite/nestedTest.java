/**
 *
 */

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
import org.fcrepo.client.PutBuilder;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ylchen
 * @since 2016-09-06
 */
public class nestedTest {

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
    public void testNestedCreation() throws IOException, FcrepoOperationFailedException, InterruptedException {

        // Create a container
        final InputStream turtleFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("object.ttl");
        final URI object1Uri = URI.create(fedoraBaseURL + "/object1");

        try (FcrepoResponse response = new PutBuilder(object1Uri, testClient)
                .body(turtleFile, "text/turtle")
                .perform()) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        }

        // Create a container in a container
        final InputStream anotherTurtleFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("object.ttl");
        final URI nestedUri = URI.create(fedoraBaseURL + "/object1/object2");
        try (FcrepoResponse response = new PutBuilder(nestedUri, testClient)
                .body(anotherTurtleFile, "text/turtle")
                .perform()) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        }

        // Create binary inside a container inside a container
        final URI nestedBinaryUri = URI.create(fedoraBaseURL + "/object1/object2/picture");
        final InputStream imageFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("basic_image.jpg");
        try (FcrepoResponse response = new PutBuilder(nestedBinaryUri, testClient)
                .body(imageFile, "image/jpeg")
                .perform()) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        }

        // Delete binary
        try (FcrepoResponse response = new DeleteBuilder(nestedBinaryUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

        // Delete container with a container inside it
        try (FcrepoResponse response = new DeleteBuilder(object1Uri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

        // Delete container with a container inside it permanently
        final URI object1TombstoneUri = URI.create(fedoraBaseURL + "/object1/fcr:tombstone");

        try (FcrepoResponse response = new DeleteBuilder(object1TombstoneUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

    }

}
