package com.JobsNow.backend.request;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UpdateProfileRequest {
    private String fullName;
    @Pattern(regexp = "^\\+?[0-9]{10,11}$", message = "Phone must be 10-11 digits")
    private String phone;

    private String title;
    private String bio;
    private String address;
    private LocalDate dob;

    /** When present (including empty list), replaces all job seeker social links */
    private List<SocialLinkItem> socials;
}
