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
import org.fcrepo.client.GetBuilder;
import org.fcrepo.client.PostBuilder;
import org.fcrepo.client.PutBuilder;
import org.junit.Before;
import org.junit.Test;


/**
 * @author ylchen
 * @since 2016-09-06
 */
public class TransactionTest {

    String fedoraBaseURL;

    URI transactionUri;

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
    public void testCommitTransaction() throws IOException, FcrepoOperationFailedException {

        // Create a transaction
        final URI uri = URI.create(fedoraBaseURL + "/fcr:tx");
        try (FcrepoResponse response = new PostBuilder(uri, testClient)
                .perform()) {

            transactionUri = response.getLocation();
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

        }

        // Get status of transaction
        try (FcrepoResponse response = new GetBuilder(transactionUri, testClient).perform()) {

            assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        }

        // Create an container in the transaction
        final URI objectUri = URI.create(transactionUri.toString() + "/transactionObj");

        try (FcrepoResponse response = new PutBuilder(objectUri, testClient)
                .perform()) {

            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

        }

        // Verify that container is available inside the transaction
        try (FcrepoResponse response = new GetBuilder(objectUri, testClient).perform()) {

            assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        }

        // Verify that container is not available outside the transaction
        final URI outsideObjectUri = URI.create(fedoraBaseURL + "/transactionObj");
        try (FcrepoResponse response = new GetBuilder(outsideObjectUri, testClient).perform()) {

            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());

        }

        // Commit transaction
        final URI commitUri = URI.create(transactionUri.toString() + "/fcr:tx/fcr:commit");
        try (FcrepoResponse response = new PostBuilder(commitUri, testClient)
                .perform()) {

            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

        }

        // Verify that container is now available outside the transaction
        try (FcrepoResponse response = new GetBuilder(outsideObjectUri, testClient).perform()) {

            assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        }

        // Delete a container
        try (FcrepoResponse response = new DeleteBuilder(outsideObjectUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

        // Delete a container permanently
        final URI objectTombstoneUri = URI.create(fedoraBaseURL + "/transactionObj/fcr:tombstone");

        try (FcrepoResponse response = new DeleteBuilder(objectTombstoneUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

    }

    @Test
    public void testRollbackTransaction() throws IOException, FcrepoOperationFailedException {

        // Create a second transaction
        final URI uri = URI.create(fedoraBaseURL + "/fcr:tx");
        try (FcrepoResponse response = new PostBuilder(uri, testClient)
                .perform()) {

            transactionUri = response.getLocation();
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

        }

        // Get status of transaction
        try (FcrepoResponse response = new GetBuilder(transactionUri, testClient).perform()) {

            assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        }

        // Create an container in the transaction
        final URI objectUri = URI.create(transactionUri.toString() + "/transactionObj");

        try (FcrepoResponse response = new PutBuilder(objectUri, testClient)
                .perform()) {

            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

        }

        // Verify that container is available inside the transaction
        try (FcrepoResponse response = new GetBuilder(objectUri, testClient).perform()) {

            assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        }

        // Verify that container not available outside the transaction
        final URI outsideObjectUri = URI.create(fedoraBaseURL + "/transactionObj");
        try (FcrepoResponse response = new GetBuilder(outsideObjectUri, testClient).perform()) {

            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());

        }

        // Rollback transaction
        final URI rollbackUri = URI.create(transactionUri.toString() + "/fcr:tx/fcr:rollback");
        try (FcrepoResponse response = new PostBuilder(rollbackUri, testClient)
                .perform()) {

            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

        }

        // Verify that container is still not available outside the transaction
        try (FcrepoResponse response = new GetBuilder(outsideObjectUri, testClient).perform()) {

            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());

        }

    }

}
