package com.covercompare.pricing;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("covercompare.pricing")
public record PricingProperties(
		@NotNull @DecimalMin("0.00") @DecimalMax("1.00") BigDecimal insurancePremiumTaxRate,
		@NotEmpty List<@Valid Insurer> insurers) {
}
