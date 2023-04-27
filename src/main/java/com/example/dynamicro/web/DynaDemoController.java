package com.example.dynamicro.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class DynaDemoController {

//    @Autowired
//    Tracer otelTracer;

    @Autowired
    Tracer microTracer;

    @Autowired
    ObservationRegistry observationRegistry;

    RestTemplate restTemplate;

    RestTemplateBuilder restTemplateBuilder;

    WebClient webClient;

    ObjectMapper objectMapper;

    public DynaDemoController(RestTemplateBuilder restTemplateBuilder, WebClient.Builder webClientBuilder,
                              ObjectMapper objectMapper, @Value("${myURL:http://localhost:8080}") String url) {
        this.restTemplateBuilder = restTemplateBuilder;
        this.restTemplate = restTemplateBuilder.rootUri(url).build();
        this.webClient = webClientBuilder.baseUrl(url).build();
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/demo")
    public void headers(@RequestHeader HttpHeaders headers, @RequestParam(name = "internal", required = false) boolean internalSpan,
                        @RequestParam(required = false) boolean propagation, @RequestParam(required = false) String url) throws JsonProcessingException {
        Map<String, Object> logData = new HashMap<>();
        logData.put("control", "FIRST");
        logData.put("headers", objectMapper.writeValueAsString(headers));
        logData.put("MDC", objectMapper.writeValueAsString(MDC.getCopyOfContextMap()));
        var context = microTracer.currentSpan().context();
        logData.put("tracer", "{ \"traceId\": \"" + context.traceId() + "\", \"spanId\": \"" + context.spanId() + "\", \"parentId\": \"" + context.parentId() + "\"}");
        log.info(objectMapper.writeValueAsString(logData).replace("\\\"", "\"")
                .replace("\"{", "{").replace("}\"", "}"));
        logData.clear();

        if (internalSpan) {
            Observation internalObs = Observation.start("internalObservation", observationRegistry);
            try (Observation.Scope scope = internalObs.openScope()) {
                // Get context from internalObservation
                logData.put("control", "INTERNALSPAN");
                logData.put("MDC", objectMapper.writeValueAsString(MDC.getCopyOfContextMap()));
                context = microTracer.currentSpan().context();
                logData.put("tracer", "{ \"traceId\": \"" + context.traceId() + "\", \"spanId\": \"" + context.spanId() + "\", \"parentId\": \"" + context.parentId() + "\"}");
                log.info(objectMapper.writeValueAsString(logData).replace("\\\"", "\"")
                        .replace("\"{", "{").replace("}\"", "}"));
            }
        }
        if (propagation) {
            if (url == null) {
                restTemplate.getForObject("/demo", String.class);
            } else {
                restTemplateBuilder.rootUri(url).build().getForObject("/demo", String.class);
            }
        }
    }

}
