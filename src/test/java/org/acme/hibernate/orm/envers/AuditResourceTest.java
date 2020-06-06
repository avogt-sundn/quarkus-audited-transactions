package org.acme.hibernate.orm.envers;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.acme.hibernate.orm.panache.Fruit;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuditResourceTest {

    public static final String[] ALL_VALUES = {"one", "two", "three"};
    public static final String[] CHANGES = {"two", "three"};

    public static final UUID TEST_TARGET_UUID = UUID.randomUUID();
    public static final String FIRST_VALUE = ALL_VALUES[0];


    @Test
    @DisplayName("create test target")
    @Order(1)
    public void createNew() {

        // create a Fruit that the test can work with
        given().with().body(new Fruit(TEST_TARGET_UUID, true, FIRST_VALUE, "color")).contentType(ContentType.JSON)
                .when().post("/fruits")
                .then()
                .statusCode(201).body("name", Matchers.equalTo(FIRST_VALUE),
                "uuid", Matchers.equalTo(TEST_TARGET_UUID).toString());
    }

    @Test
    @DisplayName("history contains initial revision")
    @Order(3)
    public void beforeAudit() {

        RestAssured.given()
                .when().get("fruits/" + TEST_TARGET_UUID + "/revisions")
                .then()
                .statusCode(200)
                // there will be no revision present at start
                .body("history", Matchers.iterableWithSize(1));
    }

    @Test
    @DisplayName("do some changes")
    @Order(4)
    public void createRevisionsByChange() {
        // create two revisions (a revision is created by a change)
        for (String name : CHANGES
        ) {
            change(name);
        }
    }

    @Test
    @DisplayName("verify history reflects changes")
    @Order(9)
    public void checkHistory() {

        List<Object> list = given()
                .when().get("fruits/" + TEST_TARGET_UUID + "/revisions")
                .then()
                .statusCode(200)
                .body("history", Matchers.not(Matchers.empty()),
                        // after # of changes we will see same # of #revisions
                        "history", Matchers.iterableWithSize(ALL_VALUES.length),
                        "history.ref.name", Matchers.hasItems(FIRST_VALUE),
                        "history.ref.name", Matchers.hasItems(Matchers.in(ALL_VALUES)),
                        "history.info.username", Matchers.hasItems(Matchers.startsWith("your-name")))
                // since i could not figure how to test equality of all 'name' values against String array
                // extract the json response as list ..
                .extract().jsonPath().getList("history.ref.name");
        // .. and compare to the list of Strings from the array:
        Assertions.assertEquals(list, Arrays.asList(ALL_VALUES));
        ;


    }

    private void change(String newName) {

        // we use the fruit we know is there due to initial data set, with the uuid 'toBeChanged'
        final Fruit baseVersion = RestAssured.given()
                .when().get("/fruits/" + TEST_TARGET_UUID)
                .then()
                .statusCode(200)
                .extract().body().as(Fruit.class);

        // make a copy, make sure all fields are copied (null values will overwrite when merged!)
        final Fruit copy = baseVersion.copy();
        copy.name = newName;
        // now Change it with a PUT on /fruits/{id}
        given().with().body(copy).contentType(ContentType.JSON)
                .when().put("/fruits/" + TEST_TARGET_UUID)
                .then()
                .statusCode(201).
                body("name", Matchers.equalTo(newName),
                        "version", equalTo(baseVersion.version + 1));

    }

}
