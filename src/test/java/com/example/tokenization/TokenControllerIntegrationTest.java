package com.example.tokenization;

import com.example.tokenization.dao.TokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TokenController.
 *
 * @SpringBootTest with DEFINED_PORT starts the full Spring application on port 3000
 * (as configured in application.yml), so tests exercise the real HTTP stack end-to-end.
 *
 * @AutoConfigureRestTestClient is required in Spring Boot 4 — test HTTP clients are no
 * longer auto-configured by default. It wires a RestTestClient pointed at the running server.
 *
 * @ActiveProfiles("dev") activates the dev profile which uses an H2 in-memory database
 * with ddl-auto: create-drop, giving each test run a clean schema automatically.
 *
 * Note @SpringBootTest(webEnvironment = ...DEFINED_PORT) basically bootstraps the entire ApplicationContext to start
 * an Embedded Web Server (Tomcat or Jetty) on the specified port 3000 to run all the tests against the live local server.
 * Then shuts down the server as soon as the test finishes.
 *
 * Note integrating AssertJ with JUnit 5 below is actually considered a best practice in modern Java development.
 * While JUnit 5 comes with its own assertions, most engineers prefer AssertJ for several key reasons.
 *
 * 1. Readability (The "Fluent" API)
 *    JUnit's native assertions often feel "backward," making them harder to read at a glance.
 *    - JUnit 5: assertEquals(3, tokens.size()); (Reads as: "Assert equals 3, the tokens size")
 *    - AssertJ: assertThat(tokens).hasSize(3); (Reads as: "Assert that tokens has size 3")
 *
 * 2. Superior Error Messages
 *    When a test fails at 3:00 AM, you want the error message to tell you exactly what went wrong without debugging.
 *    - JUnit 5 Failure: Expected <3> but was <2> (You have to go back to the code to remember which is which).
 *    - AssertJ Failure: > Expected size:<3> but was:<2> in: <["token1", "token2"]>
 *
 *    AssertJ automatically includes the actual values of the collection in the error message, which is a lifesaver for integration testing.
 *
 * 3. Powerful Collection Handling
 *    This is where AssertJ wins for dealing with a List<String>, AssertJ allows you to chain complex requirements that JUnit simply can't do in one line.
 *    Example
 *       assertThat(tokens)
 *          .hasSize(3)
 *          .doesNotContainNull()
 *          .allMatch(t -> t.length() == 32)
 *          .isSubsetOf(expectedPool);
 *
 * How they work together
 * It’s important to understand that they aren't "competing."
 *   - JUnit 5 is the Engine: It handles the lifecycle (@Test, @BeforeEach, @SpringBootTest).
 *   - AssertJ is the Verifyer: It handles the logic of checking the results.
 */
@Slf4j
@ActiveProfiles("dev")
@AutoConfigureRestTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TokenControllerIntegrationTest {

   private static final String TOKENIZE_URL   = "/tokens";
   private static final String DETOKENIZE_URL = "/tokens/resolve";
   private static final int    TOKEN_LENGTH   = 32;

   // RestTestClient is Spring Boot 4's replacement for TestRestTemplate.
   // It is auto-configured by @AutoConfigureRestTestClient to target the running server.
   @Autowired
   private RestTestClient restTestClient;

   // Injected to verify database state directly, independent of the API.
   @Autowired
   private TokenRepository tokenRepository;

   /**
    * Wipe the token table before each test so tests are fully independent
    * and do not bleed state into one another.
    */
   @BeforeEach
   void cleanDatabase() {
      tokenRepository.deleteAll();
   }

   // -------------------------------------------------------------------------
   // POST /tokens  (tokenize)
   // -------------------------------------------------------------------------

   /**
    * Verify tokens can be returned successfully at HTTP Post /tokens with valid inputs of account numbers.
    */
   @Test
   void test_tokenizeAccountNumbers_toReturnTokens_success() {
      // Given - a list of account numbers to tokenize
      List<String> accountNumbers = List.of(
              "4111-1111-1111-1111",
              "4444-3333-2222-1111",
              "4444-1111-2222-3333"
      );

      // When - call Http Post /tokens
      postForList( TOKENIZE_URL, accountNumbers )
           .value(returnedTokens -> {
              log.info("returnedTokens: {}", returnedTokens);

              // Inside here, tokens is a List, so AssertJ works perfectly
              assertThat( returnedTokens ).hasSize( accountNumbers.size() );
              assertThat( returnedTokens ).doesNotHaveDuplicates();
           });
   }

   /**
    * Verify tokens cannot be returned successfully at HTTP Post /tokens with duplicate input of account number.
    */
   @Test
   void test_tokenize_duplicateAccountNumber_returnsErrorMessageForDuplicate_success() {
      // Given - the account number is already tokenized
      String duplicateAccount = "4111-1111-1111-1111";

      restTestClient.post().uri( TOKENIZE_URL )
              .contentType( MediaType.APPLICATION_JSON )
              .body( List.of( duplicateAccount ) )
              .exchange()
              .expectStatus().isOk();

      // When - the same account number is tokenized again alongside a new one
      postForList( TOKENIZE_URL, List.of( duplicateAccount, "4444-3333-2222-1111" ) )
           .value(results -> {
              log.info("returnedTokens: {}", results);

              // First result (duplicate) should be an error message, not a token
              assertThat( results.get(0) ).contains("Account number exists already");

              // Second result (new account) should be a valid 32-char token
              assertThat( results.get(1) ).hasSize( TOKEN_LENGTH )
                      .matches("[A-Za-z0-9]{32}");
           });
   }

   /**
    * Verify no token is returned successfully at HTTP Post /tokens with empty list of account numbers as request body.
    */
   @Test
   void test_tokenize_emptyList_returnsEmptyList_success() {
      // Given / When / Then - empty input returns empty output, nothing saved to DB
      postForList( DETOKENIZE_URL, List.of() )
           .value(results -> {
              log.info("returnedTokens: {}", results);

              // Empty list should return empty list
              assertThat( results ).isEmpty();
           });

      assertThat( tokenRepository.count() ).isZero();
   }

   /**
    * Verify account numbers are successfully detokenized from the database.
    */
   @Test
   void test_detokenize_retrieveTokensFromDatabase_success() {
      // Given - account numbers that have already been tokenized
      List<String> accountNumbers = List.of(
              "4111-1111-1111-1111",
              "4444-3333-2222-1111",
              "4444-1111-2222-3333"
      );

      // Tokenize first so tokens exist in the DB — extract the body for use below
      List<String> tokens = restTestClient.post().uri(TOKENIZE_URL)
              .contentType( MediaType.APPLICATION_JSON )
              .body( accountNumbers )
              .exchange()
              .expectStatus().isOk()
              .expectBody( new ParameterizedTypeReference<List<String>>() {} )
              .returnResult()
              .getResponseBody();

      log.info("Tokens: " + tokens);
      assertThat( tokens ).isNotNull().hasSize( accountNumbers.size() );

      // When / Then - POST /tokens/resolve with the tokens to detokenize
      postForList( DETOKENIZE_URL, tokens )
           .value(resolvedAccounts -> {
                  log.info( "resolvedAccounts: " + resolvedAccounts );
                  assertThat( resolvedAccounts ).containsExactlyInAnyOrderElementsOf( accountNumbers );
           });
   }

   /**
    * Verify detokenization of an unknown token returns null successfully.
    */
   @Test
   void test_detokenize_unknownToken_returnsNull_success() {
      // Given - a token that was never stored in the database
      List<String> unknownTokens = List.of( "UnknownToken00000000000000000000" );

      // When / Then - HTTP 200 but resolved account is null (no match found)
      postForList( DETOKENIZE_URL, unknownTokens )
           .value(resolvedAccounts ->
                   assertThat(resolvedAccounts.get(0)).isNull()
           );
   }


   // Reusable method for HTTP POST /tokens or /tokens/resolve
   private RestTestClient.BodySpec<List<String>, ?> postForList(String uri, List<String> body) {
      // When - call Http Post /tokens
      return restTestClient.post()
              .uri( uri )
              .contentType( MediaType.APPLICATION_JSON )
              .body( body )
              .exchange()
              // Then - assert response below
              .expectStatus().isOk()
              .expectBody(new ParameterizedTypeReference<>() {});
   }

/*
   // Experimenting with Generics to create the most flexible version
   private <T>WebTestClient.BodySpec<List<T>, ?> postForList(String uri, Object body, Class<T> responseType) {
      return (WebTestClient.BodySpec<List<T>, ?>) restTestClient.post()
              .uri(uri)
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .exchange()
              .expectStatus().isOk()
              .expectBody( ParameterizedTypeReference.forType(
                      ResolvableType.forClassWithGenerics(List.class, responseType).getType()
              ));
   }
   */
}
