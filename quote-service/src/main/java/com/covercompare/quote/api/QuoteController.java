package com.covercompare.quote.api;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.covercompare.quote.QuoteService;

@RestController
@RequestMapping("/api/v1/quotes")
class QuoteController {

	private final QuoteService quoteService;

	QuoteController(QuoteService quoteService) {
		this.quoteService = quoteService;
	}

	@PostMapping
	ResponseEntity<QuoteResponse> createQuote(@Valid @RequestBody QuoteRequest request) {
		QuoteResponse quote = quoteService.createQuote(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(quote.id())
			.toUri();
		return ResponseEntity.created(location).body(quote);
	}

	@GetMapping("/{id}")
	QuoteResponse getQuote(@PathVariable UUID id) {
		return quoteService.getQuote(id);
	}

}
