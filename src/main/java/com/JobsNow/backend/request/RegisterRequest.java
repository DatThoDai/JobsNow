package com.JobsNow.backend.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,20}$",
             message = "Password must be 6-20 characters with at least one uppercase, one lowercase and one digit")
    private String password;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "Invalid phone number")
    private String phone;

    @NotBlank(message = "Role is required")
    private String roleName;

    private String fullName;
    private String address;
    private LocalDate dob;
    private String bio;

    private String companyName;
    private String website;
    private String description;
    private String companyAddress;

    @JsonIgnore
    private MultipartFile logo;
}

