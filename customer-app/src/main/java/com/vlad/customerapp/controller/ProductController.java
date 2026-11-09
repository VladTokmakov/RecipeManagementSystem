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

import java.util.List;
import java.util.NoSuchElementException;

@Controller
@RequiredArgsConstructor
@RequestMapping("customer/products/{productId:\\d+}")
public class ProductController {

    private final ProductsClient productsClient;

    private final FavouriteProductsService favouriteProductsService;

    private final ProductReviewsService productReviewsService;

    private final org.springframework.context.MessageSource messageSource;

    @ModelAttribute(name = "product", binding = false)
    public Product loadProduct(@PathVariable("productId") int id) {
        return this.productsClient.findProduct(id)
                .orElseThrow(() -> new NoSuchElementException("customer.products.error.not_found"));
    }

    @GetMapping
    public String getProductPage(@PathVariable("productId") int id, Model model,
                                 @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        String userId = principal.getAttribute("sub");

        List<ProductReviewView> reviews = this.productReviewsService.findProductReviewsByProduct(id).stream()
                .map(review -> new ProductReviewView(
                        review.getRating(),
                        review.getReview(),
                        userId.equals(review.getUserId())))
                .toList();

        model.addAttribute("reviews", reviews);
        model.addAttribute("inFavourite",
                this.favouriteProductsService.findFavouriteProductByProduct(id, userId).isPresent());
        return "customer/products/product";
    }

    public record ProductReviewView(int rating, String review, boolean own) {
    }

    @PostMapping("add-to-favourites")
    public String addProductToFavourites(@ModelAttribute(name = "product", binding = false) Product product,
                                         @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        String userId = principal.getAttribute("sub");
        this.favouriteProductsService.addProductToFavourites(product.id(), userId);
        return "redirect:/customer/products/%d".formatted(product.id());
    }

    @PostMapping("remove-from-favourites")
    public String removeProductFromFavourites(@ModelAttribute(name = "product", binding = false) Product product,
                                              @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        String userId = principal.getAttribute("sub");
        this.favouriteProductsService.removeProductFromFavourites(product.id(), userId);
        return "redirect:/customer/products/%d".formatted(product.id());
    }

    @PostMapping("create-review")
    public String createReview(@PathVariable("productId") int id,
                               @Valid NewProductReviewPayload payload,
                               BindingResult bindingResult,
                               Model model,
                               @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        String userId = principal.getAttribute("sub");
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
            this.productReviewsService.createProductReview(id, userId, payload.rating(), payload.review());
            return "redirect:/customer/products/%d".formatted(id);
        }
    }

    @PostMapping("delete-review")
    public String deleteReview(@PathVariable("productId") int id,
                               @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        String userId = principal.getAttribute("sub");
        this.productReviewsService.deleteProductReview(id, userId);
        return "redirect:/customer/products/%d".formatted(id);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public String handleNoSuchElementException(NoSuchElementException exception, Model model,
                                               jakarta.servlet.http.HttpServletResponse response,
                                               jakarta.servlet.http.HttpServletRequest request) {
        response.setStatus(404);
        String message = exception.getMessage();
        String text = request.getLocale() == null || message == null
                ? message
                : this.messageSource.getMessage(message, null, message, request.getLocale());
        model.addAttribute("error", text);
        return "errors/404";
    }
}
