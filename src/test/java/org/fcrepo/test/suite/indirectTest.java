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
public class indirectTest {

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
    public void testIndirect() throws IOException, FcrepoOperationFailedException {

        // Create a PCDM object
        final InputStream pcdmContainerFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("pcdm_container.ttl");
        final URI object1Uri = URI.create(fedoraBaseURL + "/object1");

        try (FcrepoResponse response = new PutBuilder(object1Uri, testClient)
                .body(pcdmContainerFile, "text/turtle")
                .perform()) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        }

        // Create a PCDM Collection
        final InputStream pcdmCollectionFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("pcdm_collection.ttl");
        final URI collectionUri = URI.create(fedoraBaseURL + "/collection");

        try (FcrepoResponse response = new PutBuilder(collectionUri, testClient)
                .body(pcdmCollectionFile, "text/turtle")
                .perform()) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        }

        // Create an indirect container inside collection called members
        final InputStream pcdmMembersFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("pcdm_indirect.ttl");
        final String pcdmMembersString = IOUtils.toString(pcdmMembersFile, "UTF-8");
        final InputStream pcdmMembersUpdate = IOUtils.toInputStream(pcdmMembersString.replace("{{PCDM_COLLECTION}}",
                collectionUri.toString()), "UTF-8");

        final URI membersUri = URI.create(fedoraBaseURL + "/collection/members");

        try (FcrepoResponse response = new PutBuilder(membersUri, testClient)
                .body(pcdmMembersUpdate, "text/turtle")
                .perform()) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        }

        // Create a proxy to the PCDM object inside members
        final InputStream pcdmProxyFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("pcdm_proxy.ttl");
        final URI proxy1Uri = URI.create(fedoraBaseURL + "/collection/members/proxy1");
        final String pcdmProxyString = IOUtils.toString(pcdmProxyFile, "UTF-8");
        final InputStream pcdmProxyUpdate = IOUtils.toInputStream(pcdmProxyString.replace("{{PROXY_FOR}}",
                proxy1Uri.toString()), "UTF-8");

        try (FcrepoResponse response = new PutBuilder(proxy1Uri, testClient)
                .body(pcdmProxyUpdate, "text/turtle")
                .perform()) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        }

        // Checking for pcdm:hasMember property on PCDM collection to PCDM object
        try (FcrepoResponse response = new GetBuilder(collectionUri, testClient)
                .perform()) {

            final String turtleContent = IOUtils.toString(response.getBody(), "UTF-8");
            // pending check use matcher
            assertThat(turtleContent, containsString("hasMember"));

        }

        // Delete object1 container
        try (FcrepoResponse response = new DeleteBuilder(object1Uri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

        // Delete object1 container permanently
        final URI object1TombstoneUri = URI.create(fedoraBaseURL + "/object1/fcr:tombstone");

        try (FcrepoResponse response = new DeleteBuilder(object1TombstoneUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

        // Delete collection container
        try (FcrepoResponse response = new DeleteBuilder(collectionUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

        // Delete collection container permanently
        final URI collectionTombstoneUri = URI.create(fedoraBaseURL + "/collection/fcr:tombstone");

        try (FcrepoResponse response = new DeleteBuilder(collectionTombstoneUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

    }

}
