package com.vlad.customerapp.controller;

import com.vlad.customerapp.client.ProductsClient;
import com.vlad.customerapp.controller.payload.PaginatedProductsResponse;
import com.vlad.customerapp.entity.FavouriteProduct;
import com.vlad.customerapp.entity.Product;
import com.vlad.customerapp.service.FavouriteProductsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("customer/products")
public class ProductsController {

    private final ProductsClient productsClient;

    private final FavouriteProductsService favouriteProductsService;

    @GetMapping("list")
    public String getProductsListPage(Model model,
                                      @RequestParam(name = "filter", required = false) String filter,
                                      @RequestParam(name = "detailsFilter", required = false) String detailsFilter,
                                      @RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                      @RequestParam(name = "size", required = false, defaultValue = "10") int size) {
        log.info("GET /customer/products/list - filter: {}, detailsFilter: {}, page: {}, size: {}", filter, detailsFilter, page, size);
        PaginatedProductsResponse response = this.productsClient.findAllProductsPaginated(filter, detailsFilter, page, size);
        model.addAttribute("filter", filter);
        model.addAttribute("detailsFilter", detailsFilter);
        model.addAttribute("products", response.getContent());
        model.addAttribute("currentPage", response.getCurrentPage());
        model.addAttribute("pageSize", response.getPageSize());
        model.addAttribute("totalElements", response.getTotalElements());
        model.addAttribute("totalPages", response.getTotalPages());
        log.info("Returning {} products on page {}/{}", response.getContent().size(), response.getCurrentPage() + 1, response.getTotalPages());
        return "customer/products/list";
    }

    @GetMapping("favourites")
    public String getFavouriteProductsPage(Model model,
                                           @RequestParam(name = "filter", required = false) String filter,
                                           @RequestParam(name = "detailsFilter", required = false) String detailsFilter,
                                           @RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                           @RequestParam(name = "size", required = false, defaultValue = "10") int size,
                                           @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        String userId = principal.getAttribute("preferred_username");
        log.info("GET /customer/products/favourites - userId: {}, filter: {}, page: {}", userId, filter, page);
        model.addAttribute("filter", filter);
        model.addAttribute("detailsFilter", detailsFilter);
        List<FavouriteProduct> favouriteProducts = this.favouriteProductsService.findFavouriteProducts(userId);
        List<Integer> favouriteProductIds = favouriteProducts.stream()
                .map(FavouriteProduct::getProductId)
                .toList();
        
        PaginatedProductsResponse response = this.productsClient.findAllProductsPaginated(filter, detailsFilter, page, size);
        List<Product> filteredProducts = response.getContent().stream()
                .filter(product -> favouriteProductIds.contains(product.id()))
                .toList();
        
        model.addAttribute("products", filteredProducts);
        model.addAttribute("currentPage", response.getCurrentPage());
        model.addAttribute("pageSize", response.getPageSize());
        model.addAttribute("totalElements", filteredProducts.size());
        model.addAttribute("totalPages", (int) Math.ceil((double) filteredProducts.size() / size));
        log.info("Returning {} favourite products", filteredProducts.size());
        return "customer/products/favourites";
    }
}