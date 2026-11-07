package com.vlad.managerapp.controller;

import com.vlad.managerapp.client.BadRequestException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Locale;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class WebExceptionAdvice {

    private final MessageSource messageSource;

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ModelAndView handleHandlerMethodValidation(HandlerMethodValidationException exception, Locale locale) {
        log.warn("Некорректные параметры запроса: {}", exception.getMessage());
        return errorView(HttpStatus.BAD_REQUEST, "errors.400.title", "errors.400.header", locale);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ModelAndView handleConstraintViolation(ConstraintViolationException exception, Locale locale) {
        log.warn("Некорректные параметры запроса: {}", exception.getMessage());
        return errorView(HttpStatus.BAD_REQUEST, "errors.400.title", "errors.400.header", locale);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView handleTypeMismatch(MethodArgumentTypeMismatchException exception, Locale locale) {
        log.warn("Некорректный тип параметра '{}': {}", exception.getName(), exception.getValue());
        return errorView(HttpStatus.BAD_REQUEST, "errors.400.title", "errors.400.header", locale);
    }

    @ExceptionHandler(BadRequestException.class)
    public ModelAndView handleBadRequest(BadRequestException exception, Locale locale) {
        log.warn("catalogue-service отклонил запрос: {}", exception.getErrors());
        ModelAndView modelAndView = errorView(HttpStatus.BAD_REQUEST,
                "errors.400.title", "errors.400.header", locale);
        modelAndView.addObject("errors", exception.getErrors());
        return modelAndView;
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ModelAndView handleClientError(HttpClientErrorException exception, Locale locale) {
        log.warn("catalogue-service ответил {}: {}", exception.getStatusCode(), exception.getStatusText());
        HttpStatus status = exception.getStatusCode().is4xxClientError()
                ? HttpStatus.valueOf(exception.getStatusCode().value())
                : HttpStatus.BAD_GATEWAY;
        return errorView(status, "errors.400.title", "errors.400.header", locale);
    }

    @ExceptionHandler(RestClientException.class)
    public ModelAndView handleRestClientError(RestClientException exception, Locale locale) {
        log.warn("Ошибка обращения к catalogue-service: {}", exception.getMessage());
        return errorView(HttpStatus.BAD_GATEWAY, "errors.502.title", "errors.502.header", locale);
    }

    private ModelAndView errorView(HttpStatus status, String titleCode, String headerCode, Locale locale) {
        ModelAndView modelAndView = new ModelAndView("errors/400");
        modelAndView.setStatus(status);
        modelAndView.addObject("errorTitle", message(titleCode, locale));
        modelAndView.addObject("errorHeader", message(headerCode, locale));
        modelAndView.addObject("errors", List.of());
        return modelAndView;
    }

    private String message(String code, Locale locale) {
        return this.messageSource.getMessage(code, new Object[0], code, locale);
    }
}
