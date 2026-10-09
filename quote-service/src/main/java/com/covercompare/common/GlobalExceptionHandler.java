package com.covercompare.common;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.covercompare.quote.InvalidQuoteRequestException;
import com.covercompare.quote.QuoteNotFoundException;
import com.covercompare.quote.rating.RatingUnavailableException;

/**
 * Turns exceptions into RFC 9457 "problem detail" JSON so every error response has the same shape.
 */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final String RETRY_AFTER_SECONDS = "5";

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> fieldErrors = new TreeMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"One or more fields are invalid");
		problem.setTitle("Validation failed");
		problem.setProperty("errors", fieldErrors);
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(InvalidQuoteRequestException.class)
	ProblemDetail handleInvalidQuoteRequest(InvalidQuoteRequestException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT,
				"The quote request cannot be priced");
		problem.setTitle("Invalid quote request");
		problem.setProperty("problems", ex.getProblems());
		return problem;
	}

	@ExceptionHandler(QuoteNotFoundException.class)
	ProblemDetail handleQuoteNotFound(QuoteNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setTitle("Quote not found");
		return problem;
	}

	@ExceptionHandler(RatingUnavailableException.class)
	ResponseEntity<ProblemDetail> handleRatingUnavailable(RatingUnavailableException ex) {
		logger.warn("Pricing service unavailable", ex);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
				"Quotes cannot be priced right now. Please try again shortly.");
		problem.setTitle("Pricing temporarily unavailable");
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
			.header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
			.body(problem);
	}

}
