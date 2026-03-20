package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.entity.Company;
import com.JobsNow.backend.entity.JobSeekerProfile;
import com.JobsNow.backend.entity.Role;
import com.JobsNow.backend.entity.User;
import com.JobsNow.backend.entity.UserAccountStatus;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.repositories.CompanyRepository;
import com.JobsNow.backend.repositories.JobSeekerProfileRepository;
import com.JobsNow.backend.repositories.RoleRepository;
import com.JobsNow.backend.repositories.UserRepository;
import com.JobsNow.backend.request.LoginRequest;
import com.JobsNow.backend.request.RegisterRequest;
import com.JobsNow.backend.request.VerifyOtpRequest;
import com.JobsNow.backend.response.AuthResponse;
import com.JobsNow.backend.dto.OtpLoginData;
import com.JobsNow.backend.service.AuthService;
import com.JobsNow.backend.service.AwsS3Service;
import com.JobsNow.backend.service.EmailService;
import com.JobsNow.backend.service.GoogleTokenVerifier;
import com.JobsNow.backend.service.LoginOtpRedisService;
import com.JobsNow.backend.service.PendingRegistrationService;
import com.JobsNow.backend.utils.JWTHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTHelper jwtHelper;
    private final EmailService emailService;
    private final PendingRegistrationService pendingRegistrationService;
    private final LoginOtpRedisService loginOtpRedisService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final CompanyRepository companyRepository;
    private final AwsS3Service awsS3Service;
    private final String DEFAULT_AVATAR_URL = "https://jobsnow-upload.s3.us-east-1.amazonaws.com/avatars/default-avatar_1771699390597.png";
    @Override
    @Transactional
    public String register(RegisterRequest registerRequest) {
        if(userRepository.existsByEmail(registerRequest.getEmail())){
            throw new BadRequestException("Email already in use");
        }
        if(pendingRegistrationService.hasPendingRegistration(registerRequest.getEmail())){
            throw new BadRequestException("Email pending OTP verification. Please check email or resend OTP.");
        }
        if(registerRequest.getPhone() !=null && userRepository.existsByPhone(registerRequest.getPhone())){
            throw new BadRequestException("Phone number already in use");
        }
        Role role = roleRepository.findByRoleName(registerRequest.getRoleName())
                .orElseThrow(() -> new BadRequestException("Role not found"));

        if(role.getRoleName().equals("ROLE_ADMIN")){
            throw new BadRequestException("Cannot register as ADMIN");
        }

        if(role.getRoleName().equals("ROLE_JOBSEEKER")){
            return registerJobSeeker(registerRequest, role);
        }else if (role.getRoleName().equals("ROLE_COMPANY")){
            return registerComapny(registerRequest);
        }
        throw new BadRequestException("Invalid role");
    }

    private String registerJobSeeker(RegisterRequest request, Role role){
        if(request.getFullName() == null || request.getFullName().isEmpty()){
            throw new BadRequestException("Full name is required");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRole(role);
        user.setIsVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        JobSeekerProfile profile = new JobSeekerProfile();
        profile.setUser(user);
        profile.setAvatarUrl(DEFAULT_AVATAR_URL);
        profile.setBio(request.getBio());
        profile.setAddress(request.getAddress());
        profile.setDob(request.getDob());
        jobSeekerProfileRepository.save(profile);
        return "Registration successful! You can now login.";
    }

    private String registerComapny(RegisterRequest request){
        if (request.getCompanyName() == null || request.getCompanyName().isEmpty()) {
            throw new BadRequestException("Company name is required");
        }
        if(companyRepository.existsByCompanyName(request.getCompanyName())){
            throw new BadRequestException("Company name already in use");
        }
        String logoUrl = DEFAULT_AVATAR_URL;
        if(request.getLogo() != null && !request.getLogo().isEmpty()){
            try {
                String originalFileName = request.getLogo().getOriginalFilename();
                String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
                String baseName = originalFileName.substring(0, originalFileName.lastIndexOf("."));
                String s3Key = "logos/" + baseName + "_" + System.currentTimeMillis() + extension;
                logoUrl = awsS3Service.uploadFileToS3(
                        request.getLogo().getInputStream(),
                        s3Key,
                        request.getLogo().getContentType()
                );
            } catch (Exception e) {
                throw new BadRequestException("Logo upload failed: " + e.getMessage());
            }
        }
        String otp = String.format("%06d", new Random().nextInt(999999));
        pendingRegistrationService.savePendingRegistration(request.getEmail(), request, otp, logoUrl);
        try {
            emailService.sendOtpEmail(request.getEmail(), otp);
            return "Registration successful! Please check email to verify OTP.";
        } catch (Exception e) {
            pendingRegistrationService.removePendingRegistration(request.getEmail());
            throw new BadRequestException("Failed to send OTP: " + e.getMessage());
        }
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (user.getPasswordHash() == null) {
            throw new BadRequestException("Account uses Google sign-in. Please use Google to login.");
        }
        if(!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())){
            throw new BadRequestException("Invalid email or password");
        }
        assertAccountActive(user);

        String token = jwtHelper.generateToken(user.getEmail());
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole().getRoleName());
        response.setUserId(user.getUserId());
        response.setPhone(user.getPhone());
        String roleName = user.getRole().getRoleName();
        if(roleName.equals("ROLE_JOBSEEKER")) {
            JobSeekerProfile profile = jobSeekerProfileRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new BadRequestException("Profile not found"));
            response.setProfileId(profile.getProfileId());
            response.setAvatar(profile.getAvatarUrl());
        } else if (roleName.equals("ROLE_COMPANY")) {
            Company company = companyRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new BadRequestException("Company not found"));
            response.setCompanyId(company.getCompanyId());
            response.setCompanyName(company.getCompanyName());
            response.setAvatar(company.getLogoUrl());
        }
        return response;
    }

    @Override
    public void verifyOtp(VerifyOtpRequest request) {
        if(!pendingRegistrationService.hasPendingRegistration(request.getEmail())){
            throw new BadRequestException("No pending registration found. Please register again.");
        }

        String storeOtp = pendingRegistrationService.getOtp(request.getEmail());
        if (storeOtp == null) {
            throw new BadRequestException("OTP expired. Please request a new OTP.");
        }

        if(!storeOtp.equals(request.getOtp())){
            throw new BadRequestException("Invalid OTP");
        }

        RegisterRequest registerRequest = pendingRegistrationService.getPendingRegistration(request.getEmail());
        if(registerRequest == null){
            throw new BadRequestException("No pending registration found. Please register again.");
        }

        Role role = roleRepository.findByRoleName("ROLE_COMPANY")
                .orElseThrow(() -> new BadRequestException("Role not found"));
        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFullName(registerRequest.getCompanyName());
        user.setPhone(registerRequest.getPhone());
        user.setRole(role);
        user.setIsVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        Company company = new Company();
        company.setUser(user);
        company.setCompanyName(registerRequest.getCompanyName());
        company.setLogoUrl(pendingRegistrationService.getLogoUrl(request.getEmail()));
        company.setWebsite(registerRequest.getWebsite());
        company.setDescription(registerRequest.getDescription());
        company.setIsVerified(true);
        company.setJobPostCount(5);
        companyRepository.save(company);
        pendingRegistrationService.removePendingRegistration(request.getEmail());
    }

    @Override
    public void resendOtp(String email) {
        if(!pendingRegistrationService.hasPendingRegistration(email)){
            throw new BadRequestException("No pending registration found. Please register again.");
        }
        String newOtp = String.format("%06d", new Random().nextInt(999999));
        pendingRegistrationService.updateOtp(email, newOtp);
        try {
            emailService.sendOtpEmail(email, newOtp);
        } catch (Exception e) {
            throw new BadRequestException("Failed to send OTP: " + e.getMessage());
        }
    }

    @Override
    public boolean checkEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public void sendLoginOtp(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email not found. Please register first.");
        }
        if (loginOtpRedisService.hasCooldown(email)) {
            throw new BadRequestException("Please wait 60 seconds before requesting a new OTP.");
        }
        String otp = String.format("%06d", new Random().nextInt(999999));
        String hashOtp = passwordEncoder.encode(otp);
        loginOtpRedisService.saveOtp(email, hashOtp);
        loginOtpRedisService.setCooldown(email);
        try {
            emailService.sendLoginOtpEmail(email, otp);
        } catch (Exception e) {
            loginOtpRedisService.deleteOtp(email);
            throw new BadRequestException("Failed to send OTP: " + e.getMessage());
        }
    }

    @Override
    public AuthResponse verifyLoginOtp(String email, String otp, String clientIp) {
        if (loginOtpRedisService.hasIplock(clientIp)) {
            throw new BadRequestException("IP temporarily locked. Please try again later.");
        }
        OtpLoginData data = loginOtpRedisService.getOtpData(email);
        if (data == null) {
            throw new BadRequestException("OTP expired. Please request a new one.");
        }
        if (!passwordEncoder.matches(otp, data.getHashOtp())) {
            data.setAttempts(data.getAttempts() + 1);
            if (data.getAttempts() >= 5) {
                loginOtpRedisService.setIplock(clientIp);
                loginOtpRedisService.deleteOtp(email);
                throw new BadRequestException("Too many attempts. IP locked for 5 minutes.");
            }
            loginOtpRedisService.updateOtpData(email, data);
            throw new BadRequestException("Invalid OTP.");
        }
        loginOtpRedisService.deleteOtp(email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
        assertAccountActive(user);
        String token = jwtHelper.generateToken(user.getEmail());
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole().getRoleName());
        response.setUserId(user.getUserId());
        response.setPhone(user.getPhone());
        String roleName = user.getRole().getRoleName();
        if (roleName.equals("ROLE_JOBSEEKER")) {
            JobSeekerProfile profile = jobSeekerProfileRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new BadRequestException("Profile not found"));
            response.setProfileId(profile.getProfileId());
            response.setAvatar(profile.getAvatarUrl());
        } else if (roleName.equals("ROLE_COMPANY")) {
            Company company = companyRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new BadRequestException("Company not found"));
            response.setCompanyId(company.getCompanyId());
            response.setCompanyName(company.getCompanyName());
            response.setAvatar(company.getLogoUrl());
        }
        return response;
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        GoogleTokenVerifier.GoogleUserInfo info = googleTokenVerifier.verify(idToken);

        User user = userRepository.findByEmail(info.getEmail()).orElse(null);

        if (user == null) {
            Role role = roleRepository.findByRoleName("ROLE_JOBSEEKER")
                    .orElseThrow(() -> new BadRequestException("Role not found"));

            user = new User();
            user.setEmail(info.getEmail());
            user.setFullName(info.getName() != null ? info.getName() : info.getEmail());
            user.setRole(role);
            user.setIsVerified(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setPasswordHash(null);
            userRepository.save(user);

            JobSeekerProfile profile = new JobSeekerProfile();
            profile.setUser(user);
            profile.setAvatarUrl(info.getPicture() != null ? info.getPicture() : DEFAULT_AVATAR_URL);
            jobSeekerProfileRepository.save(profile);
        }

        return buildAuthResponse(user);
    }

    private void assertAccountActive(User user) {
        UserAccountStatus s = user.getStatus() != null ? user.getStatus() : UserAccountStatus.ACTIVE;
        if (s == UserAccountStatus.DISABLED) {
            throw new BadRequestException("Tài khoản đã bị vô hiệu hóa.");
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        assertAccountActive(user);
        String token = jwtHelper.generateToken(user.getEmail());
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole().getRoleName());
        response.setUserId(user.getUserId());
        response.setPhone(user.getPhone());
        String roleName = user.getRole().getRoleName();
        if (roleName.equals("ROLE_JOBSEEKER")) {
            JobSeekerProfile profile = jobSeekerProfileRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new BadRequestException("Profile not found"));
            response.setProfileId(profile.getProfileId());
            response.setAvatar(profile.getAvatarUrl());
        } else if (roleName.equals("ROLE_COMPANY")) {
            Company company = companyRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new BadRequestException("Company not found"));
            response.setCompanyId(company.getCompanyId());
            response.setCompanyName(company.getCompanyName());
            response.setAvatar(company.getLogoUrl());
        }
        return response;
    }

    @Override
    public AuthResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
        assertAccountActive(user);
        AuthResponse response = new AuthResponse();
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole().getRoleName());
        response.setUserId(user.getUserId());
        response.setPhone(user.getPhone());
        String roleName = user.getRole().getRoleName();
        if (roleName.equals("ROLE_JOBSEEKER")) {
            JobSeekerProfile profile = jobSeekerProfileRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new BadRequestException("Profile not found"));
            response.setProfileId(profile.getProfileId());
            response.setAvatar(profile.getAvatarUrl());
        } else if (roleName.equals("ROLE_COMPANY")) {
            Company company = companyRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new BadRequestException("Company not found"));
            response.setCompanyId(company.getCompanyId());
            response.setCompanyName(company.getCompanyName());
            response.setAvatar(company.getLogoUrl());
        }
        return response;
    }
}
