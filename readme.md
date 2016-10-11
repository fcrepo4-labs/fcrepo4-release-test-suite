# Fedora 4 Release Test Suite

A set of integration tests that runs against a given version of fcrepo-webapp, Solr, and Fuseki server. You can use this test suite to verify a fresh instance using [fcrepo4-vagrant](https://github.com/fcrepo4-exts/fcrepo4-vagrant) or [Fedora 4 Docker](https://github.com/yinlinchen/fcrepo4-docker).

It is a Java version of [Fedora 4 Tests](https://github.com/fcrepo4-labs/fcrepo4-tests)(shell scripts). This program will create, update and delete resources in the repository, so you may **not** want to use it on a production Fedora server.

Note that there is a time delay for the new Fedora object to be indexed by Solr or Fuseki. Thus you may need to customize the waiting time (Default: 1mins). 

## Configuration 

Edit the `config.properties` file, example below
```
fedoraBaseURL=http://localhost:8080/fcrepo/rest
fusekiURL=http://127.0.0.1:8080/fuseki/test/query
solrURL=http://localhost:8080/solr/collection1
fedoraadminpassword=secret3
testpassword=password1
adminuserpassword=password2
```

## To run with Maven:

### Run all tests with default settings on a Fedora 4 vagrant
```
mvn clean -Dtest=vagrantTestSuite test
```

### Run tests with custom waiting time
```
mvn clean -Dtest=vagrantTestSuite -Dwaitingtime=3000 test
```

### Run a single test with custom waiting time
```
mvn clean -Dtest=fusekiTest -Dwaitingtime=3000 test
```

### Run tests on a Fedora 4 instance only.
```
mvn clean -Dtest=fcrepo4TestSuite test
```

## Test Cases

### authzTest
1. Create a container called **cover**
2. Patch it to a pcdm:Object
3. Create a container inside **cover** called **files**
4. Create a container called **my-acls**
5. Create a container called **acl** inside **my-acls**
6. Create a container called **authorization** inside **acl**
7. Patch **authorization** with a WebAC Authorization.
8. Patch **cover** to add **acl** as an access control.
9. Verify Anonymous can't access **cover**
10. Verify fedoraAdmin can access **cover**
11. Verify testadmin can access **cover**
12. Verify testuser can't access **cover**

### fixityTest
1. Create a binary resource
2. Get a fixity result for that resource and compare that the SHA-1 hash matches the expected value

### fusekiTest
1. Create a container
2. Query that container in the Fuseki server

### indirectTest
1. Create a pcdm:Object
2. Create a pcdm:Collection
3. Create an indirect container "members" inside the pcdm:Collection
4. Create a proxy object for the pcdm:Object inside the **members** indirectContainer
5. Verify that the pcdm:Collection has the memberRelation property added pointing to the pcdm:Object

### nestedTest
1. Create a container
2. Create a container inside the container from step 1
3. Create a binary inside the container from step 2
4. Delete the binary
5. Delete the container from step 1

### solrTest
1. Create a container
2. Search that container in the Solr server

### sparqlTest
1. Create a container
2. Set the dc:title of the container with a Patch request
3. Update the dc:title of the container with a Patch request
4. Create a binary
2. Set the dc:title of the binary with a Patch request
3. Update the dc:title of the binary with a Patch request

### transactionTest
1. Create a transaction
2. Get the status of the transaction
3. Create a container in the transaction
4. Verify the container is available in the transaction
5. Verify the container is **not** available outside the transaction
6. Commit the transaction
7. Verify the container is now available outside the transaction
8. Create a second transaction
9. Create a container in the transaction
10. Verify the container is available in the transaction
11. Verify the container is **not** available outside the transaction
12. Rollback the transaction
13. Verify the container is still **not** available outside the transaction

### versionTest
1. Create a container
2. Check for versions of the container
3. Create a version of the container (version1)
4. Try to create another version with the same label
5. Update the container with a PATCH request
6. Create another version of the container (version2)
7. Try to delete the current version
8. Revert to the previous version (version1)
9. Delete the newer version (version2)

## Maintainers

Current maintainers:

* [Yinlin Chen](https://github.com/yinlinchen)
