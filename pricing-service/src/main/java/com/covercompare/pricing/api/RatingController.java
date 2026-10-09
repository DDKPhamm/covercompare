package com.covercompare.pricing.api;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.covercompare.pricing.RatingEngine;

/**
 * Rating creates nothing and stores nothing, so it returns 200 rather than 201. It is a POST only
 * because the risk profile is too structured to pass as query parameters.
 */
@RestController
@RequestMapping("/api/v1/ratings")
class RatingController {

	private final RatingEngine ratingEngine;

	RatingController(RatingEngine ratingEngine) {
		this.ratingEngine = ratingEngine;
	}

	@PostMapping
	RatingResponse rate(@Valid @RequestBody RatingRequest request) {
		return RatingResponse.from(ratingEngine.rate(request.toRiskProfile()));
	}

}
