package org.fcrepo.test.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses({ fixityTest.class, indirectTest.class, nestedTest.class, sparqlTest.class, transactionTest.class,
    versionTest.class })
public class fcrepo4TestSuite {

}
