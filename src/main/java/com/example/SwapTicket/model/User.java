package com.example.SwapTicket.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import jakarta.persistence.ElementCollection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 3, max = 15, message = "* It should be between 3–15 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "* It should be a proper 10-digit Indian mobile number")
    private String mobile;

    @Past(message = "* Enter a valid date of birth")
    @NotNull(message = "* DOB is required")
    private LocalDate dob;

    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^.*(?=.{8,})(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$",
        message = "* Password must have 1 uppercase, 1 lowercase, 1 number, 1 special character, and be at least 8 characters long"
    )
    private String password;

    @Transient
    @Pattern(
        regexp = "^.*(?=.{8,})(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$",
        message = "* Confirm password must match password rules"
    )
    private String confirmPassword;
    
    @Column(unique = true)
    private String referralCode;

    private String referredBy;
    @Column(nullable = false)
    private String status = "ACTIVE";
    
    @ElementCollection
    private Set<Integer> usedReferralDiscounts = new HashSet<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public LocalDate getDob() {
		return dob;
	}

	public void setDob(LocalDate dob) {
		this.dob = dob;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}

	public String getReferralCode() {
		return referralCode;
	}

	public void setReferralCode(String referralCode) {
		this.referralCode = referralCode;
	}

	public String getReferredBy() {
		return referredBy;
	}

	public void setReferredBy(String referredBy) {
		this.referredBy = referredBy;
	}
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	
	public Set<Integer> getUsedReferralDiscounts() {
		return usedReferralDiscounts;
	}

	public void setUsedReferralDiscounts(Set<Integer> usedReferralDiscounts) {
		this.usedReferralDiscounts = usedReferralDiscounts;
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", name=" + name + ", email=" + email + ", mobile=" + mobile + ", dob=" + dob
				+ ", password=" + password + ", confirmPassword=" + confirmPassword + ", referralCode=" + referralCode
				+ ", referredBy=" + referredBy + "]";
	}
    
    
}