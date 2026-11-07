package com.vlad.managerapp.controller;

import com.vlad.managerapp.client.BadRequestException;
import com.vlad.managerapp.client.ProductsRestClient;
import com.vlad.managerapp.controller.payload.NewProductPayload;
import com.vlad.managerapp.controller.payload.PaginatedProductsResponse;
import com.vlad.managerapp.entity.Product;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("catalogue/products")
public class ProductsController {

    private final ProductsRestClient productsRestClient;

    @GetMapping("list")
    public String getProductsList(Model model,
                                 @Size(max = 100) @RequestParam(name = "filter", required = false) String filter,
                                 @Size(max = 100) @RequestParam(name = "detailsFilter", required = false) String detailsFilter,
                                 @Min(0) @RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                 @Min(1) @Max(100) @RequestParam(name = "size", required = false, defaultValue = "10") int size) {
        log.info("GET /catalogue/products/list - page: {}, size: {}", page, size);
        PaginatedProductsResponse response = this.productsRestClient.findAllProductsPaginated(filter, detailsFilter, page, size);
        model.addAttribute("products", response.getContent());
        model.addAttribute("filter", filter);
        model.addAttribute("detailsFilter", detailsFilter);
        model.addAttribute("currentPage", response.getCurrentPage());
        model.addAttribute("pageSize", response.getPageSize());
        model.addAttribute("totalElements", response.getTotalElements());
        model.addAttribute("totalPages", response.getTotalPages());
        log.info("Returning {} products on page {}/{}", response.getContent().size(), response.getCurrentPage() + 1, response.getTotalPages());
        return "catalogue/products/list";
    }

    @GetMapping("create")
    public String getNewProductPage() {
        log.info("GET /catalogue/products/create");
        return "catalogue/products/new_product";
    }

    @PostMapping("create")
    public String createProduct(@Valid NewProductPayload payload,
                                BindingResult bindingResult,
                                Model model) {
        log.info("POST /catalogue/products/create");

        if (bindingResult.hasErrors()) {
            model.addAttribute("payload", payload);
            model.addAttribute("errors", bindingResult.getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .toList());
            return "catalogue/products/new_product";
        }

        try {
            Product product = this.productsRestClient.createProduct(payload.title(), payload.details());
            log.info("Product created with id: {}, redirecting to detail page", product.id());
            return "redirect:/catalogue/products/%d".formatted(product.id());
        } catch (BadRequestException exception) {
            log.warn("Failed to create product - validation errors: {}", exception.getErrors());
            model.addAttribute("payload", payload);
            model.addAttribute("errors", exception.getErrors());
            return "catalogue/products/new_product";
        }
    }
}
