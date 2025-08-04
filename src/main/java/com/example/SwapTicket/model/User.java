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

    @Pattern(
        regexp = "^.*(?=.{8,})(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$",
        message = "* Password must have 1 uppercase, 1 lowercase, 1 number, 1 special character, and be at least 8 characters long"
    )
    private String password; // Can be null for OAuth2 users

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
    
    // OAuth2 related fields
    private String provider; // "local" for registration, "google" for OAuth2
    private String providerId; // Google user ID
    private String profileImageUrl;

    @Override
    public String toString() {
        return "User [id=" + id + ", name=" + name + ", email=" + email + ", mobile=" + mobile + ", dob=" + dob
                + ", referralCode=" + referralCode + ", referredBy=" + referredBy + ", status=" + status + "]";
    }
}