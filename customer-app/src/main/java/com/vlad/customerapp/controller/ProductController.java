package com.vlad.customerapp.controller;

import com.vlad.customerapp.client.ProductsClient;
import com.vlad.customerapp.controller.payload.NewProductReviewPayload;
import com.vlad.customerapp.entity.Product;
import com.vlad.customerapp.service.FavouriteProductsService;
import com.vlad.customerapp.service.ProductReviewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@Controller
@RequiredArgsConstructor
@RequestMapping("customer/products/{productId:\\d+}")
public class ProductController {

    private final ProductsClient productsClient;

    private final FavouriteProductsService favouriteProductsService;

    private final ProductReviewsService productReviewsService;

    @ModelAttribute(name = "product", binding = false)
    public Product loadProduct(@PathVariable("productId") int id) {
        return this.productsClient.findProduct(id)
                .orElseThrow(() -> new NoSuchElementException("customer.products.error.not_found"));
    }

    @GetMapping
    public String getProductPage(@PathVariable("productId") int id, Model model,
                                 @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        String userId = principal.getAttribute("preferred_username");
        model.addAttribute("inFavourite", false);
        model.addAttribute("reviews", this.productReviewsService.findProductReviewsByProduct(id));
        if (this.favouriteProductsService.findFavouriteProductByProduct(id, userId).isPresent()) {
            model.addAttribute("inFavourite", true);
        }
        return "customer/products/product";
    }

    @PostMapping("add-to-favourites")
    public String addProductToFavourites(@ModelAttribute("product") Product product,
                                         @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        String userId = principal.getAttribute("preferred_username");
        this.favouriteProductsService.addProductToFavourites(product.id(), userId);
        return "redirect:/customer/products/%d".formatted(product.id());
    }

    @PostMapping("remove-from-favourites")
    public String removeProductFromFavourites(@ModelAttribute("product") Product product,
                                              @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        String userId = principal.getAttribute("preferred_username");
        this.favouriteProductsService.removeProductFromFavourites(product.id(), userId);
        return "redirect:/customer/products/%d".formatted(product.id());
    }

    @PostMapping("create-review")
    public String createReview(@PathVariable("productId") int id,
                               @Valid NewProductReviewPayload payload,
                               BindingResult bindingResult,
                               Model model,
                               @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        String userId = principal.getAttribute("preferred_username");
        if (bindingResult.hasErrors()) {
            model.addAttribute("inFavourite", false);
            model.addAttribute("payload", payload);
            model.addAttribute("errors", bindingResult.getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .toList());
            if (this.favouriteProductsService.findFavouriteProductByProduct(id, userId).isPresent()) {
                model.addAttribute("inFavourite", true);
            }
            return "customer/products/product";
        } else {
            this.productReviewsService.createProductReview(id, payload.rating(), payload.review());
            return "redirect:/customer/products/%d".formatted(id);
        }
    }

    @ExceptionHandler(NoSuchElementException.class)
    public String handleNoSuchElementException(NoSuchElementException exception, Model model) {
        model.addAttribute("error", exception.getMessage());
        return "errors/404";
    }
}
