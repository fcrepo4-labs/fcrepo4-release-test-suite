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
import org.fcrepo.client.PutBuilder;

import org.apache.http.HttpStatus;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.Before;
import org.junit.Test;


/**
 * @author ylchen
 * @since 2016-09-06
 */
public class FusekiTest {

    String fedoraBaseURL;
    String fedoraAdminPassword = "";
    String fusekiURL;
    FcrepoClient testClient;
    int waitingtime;

    @Before
    public void setUp() throws IOException {

        final InputStream configFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("config.properties");

        final Properties p = new Properties();

        p.load(configFile);
        fedoraBaseURL = p.get("fedoraBaseURL").toString();
        fusekiURL = p.get("fusekiURL").toString();
        fedoraAdminPassword = p.get("fedoraadminpassword").toString();
        testClient = FcrepoClient.client().credentials("fedoraAdmin", fedoraAdminPassword).build();
        waitingtime = Integer.valueOf(System.getProperty("waitingtime"));

    }

    @Test
    public void testFuseki() throws IOException, FcrepoOperationFailedException, InterruptedException {

        // Create a container
        final InputStream turtleFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("object.ttl");
        final URI object1Uri = URI.create(fedoraBaseURL + "/object1");

        try (FcrepoResponse response = new PutBuilder(object1Uri, testClient)
                .body(turtleFile, "text/turtle")
                .perform()) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        }

        Thread.sleep(waitingtime);
        final String query =
                "SELECT ?subject WHERE { ?subject ?predicate <http://www.w3.org/ns/ldp#Container> . FILTER (?subject = <http://localhost:8080/fcrepo/rest/object1>) }";

        final QueryExecution q = QueryExecutionFactory.sparqlService(fusekiURL,
                query);
        final ResultSet results = q.execSelect();

        // Query that container in the Fuseki server
        while (results.hasNext()) {

            final QuerySolution soln = results.nextSolution();
            final RDFNode x = soln.get("subject");

            assertEquals("http://localhost:8080/fcrepo/rest/object1", x.toString());
        }

        // Delete a container
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

    }

}
