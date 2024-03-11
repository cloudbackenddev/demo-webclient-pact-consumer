package com.example.pact.demo;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class UserClient {
    private final WebClient webClient;
    private final Duration requestTimeout;
    private final int maxRequestRetryAttempts;

    public UserClient(
            WebClient.Builder webClientBuilder,
            @Value("${app.config.base-url}") String serviceBaseUrl,
            @Value("${service.client.timeout:10000}") int requestRetryTimeoutMs,
            @Value("${service.client.retry.maxAttempts:5}") int maxRequestRetryAttempts) {

        HttpClient httpClient =
                HttpClient.create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
                        .responseTimeout(Duration.ofMillis(500))
                        .doOnConnected(
                                conn ->
                                        conn.addHandlerLast(new ReadTimeoutHandler(500, TimeUnit.MILLISECONDS))
                                                .addHandlerLast(new WriteTimeoutHandler(500, TimeUnit.MILLISECONDS)));

        ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        this.webClient =
                webClientBuilder
                        .baseUrl(serviceBaseUrl)
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .clientConnector(connector)
                        .build();

        this.requestTimeout = Duration.ofMillis(requestRetryTimeoutMs);
        this.maxRequestRetryAttempts = maxRequestRetryAttempts;
    }
    public ResponseEntity<UserDTO> createUser(
            CreateUserCommand createUserCommand) {

        MediaType mediaTypeWithCharset =
                new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);

        return this.webClient
                .post()
                .uri("/api/users")
                .contentType(mediaTypeWithCharset)
                .bodyValue(createUserCommand)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<UserDTO>() {})
                .block();
    }
}
