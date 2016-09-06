/**
 *
 */

package org.fcrepo.test.suite;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.fcrepo.client.DeleteBuilder;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.GetBuilder;
import org.fcrepo.client.PutBuilder;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ylchen
 * @since 2016-09-06
 */
public class fixityTest {

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
    public void testFixityCheck() throws FcrepoOperationFailedException, IOException {

        final String FIXITY_RESULT = "<urn:sha1:dec028a4400b4f7ed80ed1174e65179d6b57a0f2>";

        // Create a binary resource
        final URI binaryUri = URI.create(fedoraBaseURL + "/picture");
        final InputStream imageFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("basic_image.jpg");

        try (FcrepoResponse response = new PutBuilder(binaryUri, testClient)
                .body(imageFile, "image/jpeg")
                .perform()) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        }

        // Get a fixity result for that resource and compare that the SHA-1 hash matches the expected value
        final URI fixityUri = URI.create(fedoraBaseURL + "/picture/fcr:fixity");
        try (FcrepoResponse response = new GetBuilder(fixityUri, testClient)
                .perform()) {

            final String turtleContent = IOUtils.toString(response.getBody(), "UTF-8");
            assertThat(turtleContent, containsString(FIXITY_RESULT));

        }

        // Delete a binary
        try (FcrepoResponse response = new DeleteBuilder(binaryUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

        // Delete a binary permanently
        final URI binaryTombstoneUri = URI.create(fedoraBaseURL + "/picture/fcr:tombstone");

        try (FcrepoResponse response = new DeleteBuilder(binaryTombstoneUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

    }

}
