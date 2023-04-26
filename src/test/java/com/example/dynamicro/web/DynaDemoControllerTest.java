package com.example.dynamicro.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest
@AutoConfigureObservability
public class DynaDemoControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void testController() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/demo/headers")).andExpect(MockMvcResultMatchers.status().isOk());
    }
}
