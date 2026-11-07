package com.vlad.catalogue;

import com.vlad.catalogue.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "vlad.security.rate-limit.requests-per-minute=5",
        "vlad.security.rate-limit.window=PT1M"
})
class RateLimitTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void requestsAboveLimitAreRejectedWith429() throws Exception {
        when(this.productService.findAllProducts(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        for (int i = 0; i < 5; i++) {
            this.mockMvc.perform(get("/catalogue-api/products"))
                    .andExpect(status().isOk());
        }

        this.mockMvc.perform(get("/catalogue-api/products"))
                .andExpect(status().isTooManyRequests());

        this.mockMvc.perform(get("/catalogue-api/products"))
                .andExpect(status().isTooManyRequests());
    }
}
