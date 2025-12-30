package com.JobNexus.backend.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,20}$",
             message = "Mật khẩu phải từ 6-20 ký tự, bao gồm ít nhất một chữ hoa, một chữ thường và một chữ số")
    private String password;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Role không được để trống")
    private String roleName;

    private String fullName;
    private String address;
    private LocalDate dob;
    private String bio;

    private String companyName;
    private String website;
    private String description;
    private String companyAddress;
}

