package com.example.dynamicro.web;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequestMapping("/demo")
@Slf4j
public class DynaDemoController {

//    @Autowired
//    Tracer otelTracer;

    @Autowired
    Tracer microTracer;

    @Autowired
    ObservationRegistry observationRegistry;

    RestTemplate restTemplate;

    WebClient webClient;

    public DynaDemoController(RestTemplateBuilder restTemplateBuilder, WebClient.Builder webClientBuilder) {
        final String URI = "http://localhost:8080";
        this.restTemplate = restTemplateBuilder.rootUri(URI).build();
        this.webClient = webClientBuilder.baseUrl(URI).build();
    }

    @GetMapping(value = "/headers")
    public void headers(@RequestHeader HttpHeaders headers) {
        var traceParent = headers.get("traceparent");
        var traceState = headers.get("tracestate");
        var context = microTracer.currentSpan().context();
        log.info("HEADERS(traceParent: {}, traceState:{}) - TRACER(traceId: {}, spanId: {}, parentId: {})",
                traceParent, traceState, context.traceId(), context.spanId(), context.parentId());
    }

    @GetMapping("/internalSpan")
    public void internalSpan() {
        // Get current context from initial observation
        var context = microTracer.currentSpan().context();
        log.info("FIRST TRACER(traceId: {}, spanId: {}, parentId: {})", context.traceId(), context.spanId(), context.parentId());
        Observation internalObs = Observation.start("internalObservation", observationRegistry);
        try (Observation.Scope scope = internalObs.openScope()) {
            // Get context from internalObservation
            context = microTracer.currentSpan().context();
            log.info("INTERNAL TRACER(traceId: {}, spanId: {}, parentId: {})", context.traceId(), context.spanId(), context.parentId());
        }
    }

    @GetMapping("/propagation")
    public void propagation(@RequestParam(name = "webClient", defaultValue = "false", required = false) boolean withWebClient) {
        log.info("FIRST ENDPOINT");
        if (withWebClient) {
            restTemplate.getForObject("/demo/headers", String.class);
        } else {
            webClient.get().uri("/demo/headers").retrieve().bodyToFlux(String.class).blockFirst();
        }
    }

}
