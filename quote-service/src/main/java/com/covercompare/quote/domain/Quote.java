package com.covercompare.quote.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * A customer's quote request together with every insurer's response to it. This is the aggregate
 * root: insurer quotes and rating factors are only ever saved and loaded through it.
 */
@Entity
@Table(name = "quote")
public class Quote {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant validUntil;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private CoverType coverType;

	@Column(nullable = false)
	private LocalDate driverDateOfBirth;

	@Column(nullable = false)
	private int licenceHeldYears;

	@Column(nullable = false)
	private int noClaimsYears;

	@Column(nullable = false, length = 8)
	private String postcode;

	@Column(nullable = false, length = 50)
	private String vehicleMake;

	@Column(nullable = false, length = 50)
	private String vehicleModel;

	@Column(nullable = false)
	private int vehicleYear;

	@Column(nullable = false)
	private int vehicleInsuranceGroup;

	@Column(nullable = false)
	private int voluntaryExcess;

	@ElementCollection
	@CollectionTable(name = "quote_rating_factor", joinColumns = @JoinColumn(name = "quote_id"))
	@OrderColumn(name = "display_order")
	private List<AppliedRatingFactor> ratingFactors = new ArrayList<>();

	@OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("displayOrder")
	private List<InsurerQuote> insurerQuotes = new ArrayList<>();

	protected Quote() {
	}

	public Quote(Instant createdAt, Instant validUntil, CoverType coverType, LocalDate driverDateOfBirth,
			int licenceHeldYears, int noClaimsYears, String postcode, String vehicleMake, String vehicleModel,
			int vehicleYear, int vehicleInsuranceGroup, int voluntaryExcess) {
		this.createdAt = createdAt;
		this.validUntil = validUntil;
		this.coverType = coverType;
		this.driverDateOfBirth = driverDateOfBirth;
		this.licenceHeldYears = licenceHeldYears;
		this.noClaimsYears = noClaimsYears;
		this.postcode = postcode;
		this.vehicleMake = vehicleMake;
		this.vehicleModel = vehicleModel;
		this.vehicleYear = vehicleYear;
		this.vehicleInsuranceGroup = vehicleInsuranceGroup;
		this.voluntaryExcess = voluntaryExcess;
	}

	public void addRatingFactor(AppliedRatingFactor ratingFactor) {
		ratingFactors.add(ratingFactor);
	}

	public void addInsurerQuote(InsurerQuote insurerQuote) {
		insurerQuote.attachTo(this, insurerQuotes.size());
		insurerQuotes.add(insurerQuote);
	}

	public UUID getId() {
		return id;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getValidUntil() {
		return validUntil;
	}

	public CoverType getCoverType() {
		return coverType;
	}

	public LocalDate getDriverDateOfBirth() {
		return driverDateOfBirth;
	}

	public int getLicenceHeldYears() {
		return licenceHeldYears;
	}

	public int getNoClaimsYears() {
		return noClaimsYears;
	}

	public String getPostcode() {
		return postcode;
	}

	public String getVehicleMake() {
		return vehicleMake;
	}

	public String getVehicleModel() {
		return vehicleModel;
	}

	public int getVehicleYear() {
		return vehicleYear;
	}

	public int getVehicleInsuranceGroup() {
		return vehicleInsuranceGroup;
	}

	public int getVoluntaryExcess() {
		return voluntaryExcess;
	}

	public List<AppliedRatingFactor> getRatingFactors() {
		return Collections.unmodifiableList(ratingFactors);
	}

	public List<InsurerQuote> getInsurerQuotes() {
		return Collections.unmodifiableList(insurerQuotes);
	}

}
