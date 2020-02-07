package io.precognito.services;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class SecurityResourceTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    public void testAuthFails() {
        given().urlEncodingEnabled(true)
                .param("username", "user@site.com")
                .param("password", "Pas54321")
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401);
    }
    @Test
    public void testAuthPass() {
        System.out.println("secret".hashCode());
        given().urlEncodingEnabled(true)
                .param("username", "user@precognito.io")
                .param("password", "secret")
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200);
    }

}