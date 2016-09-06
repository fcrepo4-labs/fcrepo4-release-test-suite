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
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
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
public class solrTest {

    String fedoraBaseURL;
    String fedoraAdminPassword = "";
    String solrURL;
    FcrepoClient testClient;
    int waitingtime;

    @Before
    public void setUp() throws IOException {

        final InputStream configFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("config.properties");

        final Properties p = new Properties();

        p.load(configFile);
        fedoraBaseURL = p.get("fedoraBaseURL").toString();
        solrURL = p.get("solrURL").toString();
        fedoraAdminPassword = p.get("fedoraadminpassword").toString();
        testClient = FcrepoClient.client().credentials("fedoraAdmin", fedoraAdminPassword).build();
        waitingtime = Integer.valueOf(System.getProperty("waitingtime"));
    }

    @Test
    public void testSolr() throws SolrServerException, IOException, FcrepoOperationFailedException,
    InterruptedException {

        // Create a container
        final InputStream turtleFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("object.ttl");
        final URI object1Uri = URI.create(fedoraBaseURL + "/solrObject");

        try (FcrepoResponse response = new PutBuilder(object1Uri, testClient)
                .body(turtleFile, "text/turtle")
                .perform()) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        }

        // Search that container in the Solr server
        Thread.sleep(waitingtime);

        final SolrClient solr = new HttpSolrClient.Builder(solrURL).build();

        final SolrQuery query = new SolrQuery();
        query.set("q", "id:\"http://localhost:8080/fcrepo/rest/solrObject\"");

        final QueryResponse solrResponse = solr.query(query);
        final SolrDocumentList list = solrResponse.getResults();

        assertEquals(1, list.size());

        // Delete a container
        try (FcrepoResponse response = new DeleteBuilder(object1Uri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

        // Delete object1 container permanently
        final URI object1TombstoneUri = URI.create(fedoraBaseURL + "/solrObject/fcr:tombstone");

        try (FcrepoResponse response = new DeleteBuilder(object1TombstoneUri, testClient)
                .perform()) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        }

    }

}