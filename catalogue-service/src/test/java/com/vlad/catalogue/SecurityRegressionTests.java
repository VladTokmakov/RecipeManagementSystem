package com.vlad.catalogue;

import com.vlad.catalogue.entity.Product;
import com.vlad.catalogue.repository.ProductRepository;
import com.vlad.catalogue.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityRegressionTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    private static final String EDIT_CATALOGUE = "SCOPE_edit_catalogue";
    private static final String MANAGER = "ROLE_MANAGER";

    @Test
    void anonymousCanReadCatalogue() throws Exception {
        when(this.productService.findAllProducts(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new Product(1, "Тест", "Описание"))));

        this.mockMvc.perform(get("/catalogue-api/products"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCannotCreateProduct() throws Exception {
        this.mockMvc.perform(post("/catalogue-api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Тест\",\"details\":\"Описание\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void scopeWithoutManagerRoleCannotCreateProduct() throws Exception {
        this.mockMvc.perform(post("/catalogue-api/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority(EDIT_CATALOGUE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Тест\",\"details\":\"Описание\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerRoleWithoutScopeCannotCreateProduct() throws Exception {
        this.mockMvc.perform(post("/catalogue-api/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority(MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Тест\",\"details\":\"Описание\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerWithBothCanCreateProduct() throws Exception {
        when(this.productService.createProduct(any(), any()))
                .thenReturn(new Product(1, "Тест", "Описание"));

        this.mockMvc.perform(post("/catalogue-api/products")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority(EDIT_CATALOGUE),
                                new SimpleGrantedAuthority(MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Тест\",\"details\":\"Описание\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void oversizedPageSizeIsRejected() throws Exception {
        this.mockMvc.perform(get("/catalogue-api/products").param("size", "1000000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negativePageIsRejected() throws Exception {
        this.mockMvc.perform(get("/catalogue-api/products").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oversizedFilterIsRejected() throws Exception {
        this.mockMvc.perform(get("/catalogue-api/products").param("filter", "a".repeat(500)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletingMissingProductReturns404() throws Exception {
        org.mockito.Mockito.doThrow(new java.util.NoSuchElementException("catalogue.errors.product.not_found"))
                .when(this.productService).deleteProduct(999);

        this.mockMvc.perform(delete("/catalogue-api/products/999")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority(EDIT_CATALOGUE),
                                new SimpleGrantedAuthority(MANAGER))))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingProductReturns404WithoutInternals() throws Exception {
        when(this.productService.findProduct(999)).thenReturn(Optional.empty());

        this.mockMvc.perform(get("/catalogue-api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void tooShortTitleIsRejected() throws Exception {
        this.mockMvc.perform(post("/catalogue-api/products")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority(EDIT_CATALOGUE),
                                new SimpleGrantedAuthority(MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"ab\",\"details\":\"Описание\"}"))
                .andExpect(status().isBadRequest());
    }
}
