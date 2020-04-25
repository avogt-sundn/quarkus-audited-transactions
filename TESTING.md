## Testing

### Testing with Postgresql in Docker

This project has a runtime dependency on postgresql database. This will be no test roundtrip overhead at all.
You need to have ```docker``` command installed.

Start the container with

    cd <project toplevel folder>
    docker-compose up   
    // showing live log and blocking, or do as daemon background mode:
    docker-compose up -d 
    // or use maven:
    mvn docker:start 
   
``mvn docker:start``uses fabric8 docker plugin. This is configured in the pom.xml as 
starting all containers etc. that are described on the project's``docker-compose.yaml``file.

You could also use H2 in memory database, but why not testing against the database you gonna use in real (or do you prefer SQL server, oracle xe, mysql, mariadb ...).

Do not stop the database, let in run in the background and tests will be quick. Quarkus application.properties configured hibernate to drop and create the database on each test run.
(doing this once for all tests in the suite, see below..)

### @QuarkusTest and JUnit5

Quarkus promotes framwork testing with the ```@QuarkusTest``` annotation.
Such a test will start the Quarkus container and apply all configuration that is found in the ``src/main/resources/application.properties`` file.
Properties that start with ``%test.`` can override the default values that do not have such prefix:

    %test.my.config-value=1
    my.config-value=1
    
The test profile is used when running tests in your IDE as well as when using command line:

    mvn test
    # runs in quarkus profile 'test'
    # runs all @QuarkusTest classes in one container
    
Quarkus is efficient about ``@QuarkusTest`` not only because 
Quarkus starts pretty quick, 
but also because the **Quarkus container will only be booted once
for all test classes** to be used one after the other.

   * make sure your test does not depend on order
   * make sure your test does not depend on data imported from import.sql being unmodified 
      
Quarkus by default comes with Juniper Junit 5.
    
    import org.junit.jupiter.api.BeforeAll;
    import org.junit.jupiter.api.MethodOrderer;
    import org.junit.jupiter.api.Order;
    import org.junit.jupiter.api.Test;
    import org.junit.jupiter.api.TestMethodOrder;

Use the right Junit 5 assertions:

    import org.junit.jupiter.api.Assertions;
    
    public void testThis(){ 
        Assertions.assertEquals(toBeSafe, oneValidId);
    }

### io.restassured

Quarkus comes with [Rest-assured (http://rest-assured.io/)](http://rest-assured.io/) framework.

Test for json payload response:

- io.restassured is bundled with Groovy GPath implementation

Therefore you cannot by default use:

- com.jayway.jsonpath
- and most internet jsonpath tester sites do not run GPath and will verify you syntax that does not work with your restassured tests!!

These examples are all _wrong_ :
- ``$.store.book[*].author``

See [https://www.james-willett.com/rest-assured-gpath-json/](https://www.james-willett.com/rest-assured-gpath-json/) for a nice tutorial with examples.

## The right way of developing gpath expressions:

1. open https://groovy-playground.appspot.com/ and copy this code into the left windows:
    
       import groovy.json.JsonSlurper
        
       def jsonSlurper = new JsonSlurper()
       def object = jsonSlurper.parseText(
          '[{"uuid":"b2f40a42-9748-4111-9b53-c205879d607b","name":"Cherry"}
          ,{"uuid":"b2f40a42-9748-4111-9b53-c205879d607c","name":"Apple"}
          ,{"uuid":"b2f40a42-9748-4111-9b53-c205879d607e","name":"Banana"}]'
       )
        
       def allDataForSingleTeam = object.find{ it.name == "Cherry" }.uuid

2. change the ``parseText()`` parameter to the json you expect in your test

3. tune the last line 
4. copy the last line beginning after ``object.`` to Java:

       final String oneValidId = given()
                       .when().get("/fruits")
                       .then()
                       .statusCode(200)
                       .body(
                               containsString("Cherry"),
                               containsString("Apple"),
                               containsString("Banana")
                       )
                       // 'find' is gpath special keyword introduncing a filter
                       // 'it' is a gpath special keyword referencing the current node
                       .extract().body().jsonPath().getString("find{ it.name == 'Cherry' }.uuid");
       
               Assertions.assertEquals(uuid, oneValidId);
              
              
              
              
## Sending java objects with PUT/POST 

Use ```.with().body(...)``` to define the payload for the PUT/POST request to be tested.

        @Test
        public void createAnew() {
            given()
                .with().body(new Fruit("id","its_me"))
                .contentType(ContentType.JSON)
                .when().post("/fruits")
                .then()
                .statusCode(200);
        }
        
* io.restassured can out-of-the-box serialize to XML. 
* To use Json You must add Jackson or Gson dependency:

    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.8.6</version>
        <scope>test</scope>
    </dependency>
    
    
## Examples for matching

https://github.com/rest-assured/rest-assured/wiki/Usage