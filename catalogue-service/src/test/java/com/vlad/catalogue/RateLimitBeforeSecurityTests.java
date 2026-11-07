package com.vlad.catalogue;

import com.vlad.catalogue.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "vlad.security.rate-limit.requests-per-minute=3",
        "vlad.security.rate-limit.window=PT1M"
})
class RateLimitBeforeSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void unauthenticatedRequestsAreAlsoCounted() throws Exception {
        for (int i = 0; i < 3; i++) {
            this.mockMvc.perform(post("/catalogue-api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Тест\",\"details\":\"Описание\"}"))
                    .andExpect(status().isUnauthorized());
        }

        this.mockMvc.perform(post("/catalogue-api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Тест\",\"details\":\"Описание\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }
}
