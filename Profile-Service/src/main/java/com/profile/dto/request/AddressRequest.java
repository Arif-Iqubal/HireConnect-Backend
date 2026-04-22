package com.profile.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressRequest {

	@NotBlank
	private String houseNo;

	@NotBlank
	private String street;

	@NotBlank
	private String city;

	@NotBlank
	private String state;

	@Min(100000)
	@Max(999999)
	private int pincode;
}