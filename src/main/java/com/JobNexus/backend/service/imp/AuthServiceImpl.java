package com.JobNexus.backend.service.imp;

import com.JobNexus.backend.entity.Company;
import com.JobNexus.backend.entity.JobSeekerProfile;
import com.JobNexus.backend.entity.Roles;
import com.JobNexus.backend.entity.Users;
import com.JobNexus.backend.exception.BadRequestException;
import com.JobNexus.backend.repositories.CompanyRepository;
import com.JobNexus.backend.repositories.JobSeekerProfileRepository;
import com.JobNexus.backend.repositories.RoleRepository;
import com.JobNexus.backend.repositories.UserRepository;
import com.JobNexus.backend.request.LoginRequest;
import com.JobNexus.backend.request.RegisterRequest;
import com.JobNexus.backend.request.VerifyOtpRequest;
import com.JobNexus.backend.response.AuthResponse;
import com.JobNexus.backend.service.AuthService;
import com.JobNexus.backend.service.EmailService;
import com.JobNexus.backend.service.PendingRegistrationService;
import com.JobNexus.backend.utils.JWTHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
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
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final CompanyRepository companyRepository;
    private final String DEFAULT_AVATAR_URL = "https://imgur.com/6VBx3io";
    @Override
    @Transactional
    public String register(RegisterRequest registerRequest) {
        if(userRepository.existsByEmail(registerRequest.getEmail())){
            throw new BadRequestException("Email đã được sử dụng");
        }
        if(pendingRegistrationService.hasPendingRegistration(registerRequest.getEmail())){
            throw new BadRequestException("Email đang chờ xác thực OTP. Vui lòng kiểm tra email hoặc gửi lại OTP.");
        }
        if(registerRequest.getPhone() !=null && userRepository.existsByPhone(registerRequest.getPhone())){
            throw new BadRequestException("Số điện thoại đã được sử dụng");
        }
        Roles role = roleRepository.findByRoleName(registerRequest.getRoleName())
                .orElseThrow(() -> new BadRequestException("Vai trò không tồn tại"));

        if(role.getRoleName().equals("ROLE_ADMIN")){
            throw new BadRequestException("Không thể đăng ký với vai trò ADMIN");
        }

        if(role.getRoleName().equals("ROLE_JOBSEEKER")){
            return registerJobSeeker(registerRequest, role);
        }else if (role.getRoleName().equals("ROLE_COMPANY")){
            return registerComapny(registerRequest);
        }
        throw new BadRequestException("Vai trò không hợp lệ");
    }

    private String registerJobSeeker(RegisterRequest request, Roles role){
        if(request.getFullName() == null || request.getFullName().isEmpty()){
            throw new BadRequestException("Họ và tên không được để trống");
        }
        Users user = new Users();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRole(role);
        user.setVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        JobSeekerProfile profile = new JobSeekerProfile();
        profile.setUser(user);
        profile.setAvatarUrl(DEFAULT_AVATAR_URL);
        profile.setBio(request.getBio());
        profile.setAddress(request.getAddress());
        profile.setDob(request.getDob());
        jobSeekerProfileRepository.save(profile);
        return "Đăng ký thành công! Bạn có thể đăng nhập ngay.";
    }

    private String registerComapny(RegisterRequest request){
        if (request.getCompanyName() == null || request.getCompanyName().isEmpty()) {
            throw new BadRequestException("Tên công ty không được để trống");
        }
        if(companyRepository.existsByCompanyName(request.getCompanyName())){
            throw new BadRequestException("Tên công ty đã được sử dụng");
        }
        String otp = String.format("%06d", new Random().nextInt(999999));
        pendingRegistrationService.savePendingRegistration(request.getEmail(), request, otp);
        try {
            emailService.sendOtpEmail(request.getEmail(), otp);
            return "Đăng ký thành công! Vui lòng kiểm tra email để xác thực OTP.";
        } catch (Exception e) {
            pendingRegistrationService.removePendingRegistration(request.getEmail());
            throw new BadRequestException("Gửi OTP thất bại: " + e.getMessage());
        }
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        Users user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("Email hoặc mật khẩu không đúng"));

        if(!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())){
            throw new BadRequestException("Email hoặc mật khẩu không đúng");
        }

        String token = jwtHelper.generateToken(user.getEmail());
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole().getRoleName());
        response.setUserId(user.getId());
        response.setPhone(user.getPhone());
        return response;
    }

    @Override
    public void verifyOtp(VerifyOtpRequest request) {
        if(!pendingRegistrationService.hasPendingRegistration(request.getEmail())){
            throw new BadRequestException("Không tìm thấy đăng ký chờ xác thực cho email này hoặc đã hết hạn. Vui lòng đăng ký lại.");
        }

        String storeOtp = pendingRegistrationService.getOtp(request.getEmail());
        if (storeOtp == null) {
            throw new BadRequestException("OTP đã hết hạn. Vui lòng yêu cầu gửi lại OTP.");
        }

        if(!storeOtp.equals(request.getOtp())){
            throw new BadRequestException("OTP không đúng");
        }

        RegisterRequest registerRequest = pendingRegistrationService.getPendingRegistration(request.getEmail());
        if(registerRequest == null){
            throw new BadRequestException("Không tìm thấy đăng ký chờ xác thực cho email này hoặc đã hết hạn. Vui lòng đăng ký lại.");
        }

        Roles role = roleRepository.findByRoleName("ROLE_COMPANY")
                .orElseThrow(() -> new BadRequestException("Vai trò không tồn tại"));
        Users user = new Users();
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFullName(registerRequest.getCompanyName());
        user.setPhone(registerRequest.getPhone());
        user.setRole(role);
        user.setVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        Company company = new Company();
        company.setUser(user);
        company.setCompanyName(registerRequest.getCompanyName());
        company.setLogoUrl(DEFAULT_AVATAR_URL);
        company.setWebsite(registerRequest.getWebsite());
        company.setDescription(registerRequest.getDescription());
        company.setIsVerified(true);
        companyRepository.save(company);
        pendingRegistrationService.removePendingRegistration(request.getEmail());
    }

    @Override
    public void resendOtp(String email) {
        if(!pendingRegistrationService.hasPendingRegistration(email)){
            throw new BadRequestException("Không tìm thấy đăng ký chờ xác thực cho email này. Vui lòng đăng ký lại.");
        }
        String newOtp = String.format("%06d", new Random().nextInt(999999));
        pendingRegistrationService.updateOtp(email, newOtp);
        try {
            emailService.sendOtpEmail(email, newOtp);
        } catch (Exception e) {
            throw new BadRequestException("Gửi OTP thất bại: " + e.getMessage());
        }
    }
}
