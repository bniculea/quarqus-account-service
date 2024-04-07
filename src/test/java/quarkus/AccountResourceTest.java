package quarkus;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import quarkus.domain.Account;
import quarkus.domain.AccountStatus;

import java.math.BigDecimal;
import java.net.http.HttpHeaders;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsEmptyCollection.empty;


@QuarkusTest
public class AccountResourceTest {

    @Test
    @Order(1)
    void testRetrieveAll() {
        Response result =
                given()
                        .when()
                        .get("/accounts")
                        .then()
                        .statusCode(200)
                        .body(
                                containsString("George Baird"),
                                containsString("Mary Taylor"),
                                containsString("Diana Rigg")
                        )
                        .extract()
                        .response();

        List<Account> accounts = result.jsonPath().getList("$");
        assertThat(accounts, not(empty()));
        assertThat(accounts, hasSize(3));
    }

    @Test
    @Order(2)
    void testGetAccount() {
        Account account =
                given()
                        .when()
                        .get("/accounts/{accountNumber}", 545454545)
                        .then()
                        .statusCode(200)
                        .extract()
                        .as(Account.class);

        assertThat(account.getAccountNumber(), equalTo(545454545));
        assertThat(account.getCustomerName(), equalTo("Diana Rigg"));
        assertThat(account.getBalance(), equalTo(422.0));
        assertThat(account.getAccountStatus(), equalTo(AccountStatus.OPEN));
    }

    @Test
    @Order(3)
    void testCreateAccount() {
        Account newAccount = new Account(324324L, 112244L, "Sandy Holmes", new BigDecimal("154.55"));


        Account returnedAccount =
                given()
                        .body(newAccount)
                        .contentType(ContentType.JSON)
                        .when()
                        .post("/accounts")
                        .then()
                        .statusCode(201)
                        .extract()
                        .as(Account.class);

        assertThat(returnedAccount, notNullValue());
        assertThat(returnedAccount, equalTo(newAccount));

        Response result =
                given()
                        .when().get("/accounts")
                        .then()
                        .statusCode(200)
                        .body(
                                containsString("George Baird"),
                                containsString("Mary Taylor"),
                                containsString("Diana Rigg"),
                                containsString("Sandy Holmes")
                        )
                        .extract()
                        .response();

        List<Account> accounts = result.jsonPath().getList("$");
        assertThat(accounts, not(empty()));
        assertThat(accounts, hasSize(4));
    }
}
