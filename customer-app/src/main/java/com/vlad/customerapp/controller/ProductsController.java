package com.vlad.customerapp.controller;

import com.vlad.customerapp.client.ProductsClient;
import com.vlad.customerapp.entity.FavouriteProduct;
import com.vlad.customerapp.entity.Product;
import com.vlad.customerapp.service.FavouriteProductsService;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("customer/products")
public class ProductsController {

    private final ProductsClient productsClient;

    private final FavouriteProductsService favouriteProductsService;

    @GetMapping("list")
    public String getProductsListPage(Model model,
                                      @RequestParam(name = "filter", required = false) String filter) {
        model.addAttribute("filter", filter);
        model.addAttribute("products", this.productsClient.findAllProducts(filter));
        return "customer/products/list";
    }

    @GetMapping("favourites")
    public String getFavouriteProductsPage(Model model,
                                           @RequestParam(name = "filter", required = false) String filter,
                                           @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        String userId = principal.getAttribute("preferred_username");
        model.addAttribute("filter", filter);
        List<FavouriteProduct> favouriteProducts = this.favouriteProductsService.findFavouriteProducts(userId);
        List<Integer> favouriteProductIds = favouriteProducts.stream()
                .map(FavouriteProduct::getProductId)
                .toList();
        List<Product> products = this.productsClient.findAllProducts(filter).stream()
                .filter(product -> favouriteProductIds.contains(product.id()))
                .toList();
        model.addAttribute("products", products);
        return "customer/products/favourites";
    }
}