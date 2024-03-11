package com.example.pact.demo;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith({PactConsumerTestExt.class, SpringExtension.class})
@PactTestFor(providerName = "user-service-omni", port="20999")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserServiceClientPactTest {
    @Autowired
    UserClient userClient;

    @DynamicPropertySource
    static  void registerProperties(DynamicPropertyRegistry dpr){
        dpr.add( "app.config.base-url" , ()-> "http://localhost:20999");
    }

    private ObjectMapper mapper = new ObjectMapper();

    @Pact(consumer = "user-service-omni-client")
    public RequestResponsePact createUser(PactDslWithProvider builder)
            throws JsonProcessingException {
        DslPart responseBody =
                newJsonBody(
                        (body) -> {
                            body.stringType("name", "Ellen");
                            body.integerType("age", 21);
                            body.stringType("email", "Ellen@test.com");
                        })
                        .build();

        return builder
                .given("the user service is up and running")
                .uponReceiving("a request to create a new user with required data")
                .path("/api/users")
                .method("POST")
                .body(mapper.writeValueAsString(createUserValidCommand()))
                .willRespondWith()
                .body(responseBody)
                .status(201)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createUser", pactVersion = PactSpecVersion.V3)
    void shouldCreateUser(MockServer mockServer) {
        System.out.println(mockServer.getUrl());
        UserDTO userDTO = userClient.createUser(createUserValidCommand()).getBody();
        assertThat(userDTO).isNotNull();
    }

    @Pact(consumer = "user-service-omni-client")
    public RequestResponsePact createUserWithInvalidAge(PactDslWithProvider builder)
            throws JsonProcessingException {

        DslPart responseBody =
                newJsonBody(
                        (body) -> {
                            body.stringType("type", "about:blank");
                            body.stringType("title", "Bad Request");
                            body.stringType("status", "400");
                            body.stringType("detail","Invalid request content.");
                            body.stringType("instance","/api/users");
                            body.minArrayLike("violations",1, violation -> {
                                violation.stringType("object","createUserRequest");
                                violation.stringType("field","age");
                                violation.stringType("rejectedValue","17");
                                violation.stringType("message","Age must be greater than or equal to 18");
                            });
                            body.stringType("id","f98809ee-f302-4c80-a69b-0b7c90b366ab");
                            body.stringType("timestamp","2024-03-10T08:27:05.984");
                            body.stringType("x-correlation-id","");
                        })
                        .build();

        return builder
                .given("the user service is up and running")
                .uponReceiving("a request to create a new user with invalid age")
                .path("/api/users")
                .method("POST")
                .body(mapper.writeValueAsString(createUserInvalidAgeCommand()))
                .willRespondWith()
                .body(responseBody)
                .status(400)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createUserWithInvalidAge", pactVersion = PactSpecVersion.V3)
    void shouldFailToCreateUserWithInvalidAge() {

        WebClientResponseException ex = assertThrows(WebClientResponseException.class, () -> userClient.createUser(createUserInvalidAgeCommand()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private CreateUserCommand createUserValidCommand() {
        return new CreateUserCommand("Ellen",21,"Ellen@test.com");
    }

    private CreateUserCommand createUserInvalidAgeCommand() {
        return new CreateUserCommand("unknown",17,"unknown@test.com");
    }
}
