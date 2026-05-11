package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.dto.AdminUserDTO;
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
import com.JobsNow.backend.request.UpdateAdminUserRequest;
import com.JobsNow.backend.service.AdminUserService;
import com.JobsNow.backend.response.PagedResponse;
import com.JobsNow.backend.util.PagingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AdminUserDTO> listUsers(int page, int limit) {
        int p = PagingUtil.safePage(page);
        int lim = PagingUtil.safeLimit(limit, 100);
        Page<User> pg = userRepository.findAll(
                PageRequest.of(p - 1, lim, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<User> content = pg.getContent();
        Map<Integer, String> avatarByUserId = buildAvatarMap(content);

        return PagedResponse.<AdminUserDTO>builder()
                .items(content.stream()
                        .map(u -> toDto(u, avatarByUserId.get(u.getUserId())))
                        .collect(Collectors.toList()))
                .totalCount(pg.getTotalElements())
                .page(p)
                .limit(lim)
                .hasNext(pg.hasNext())
                .build();
    }

    @Override
    @Transactional
    public AdminUserDTO updateUser(Integer userId, UpdateAdminUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (request.getRoleName() != null && !request.getRoleName().isBlank()) {
            String rn = request.getRoleName().trim();
            Role role = roleRepository.findByRoleName(rn)
                    .orElseThrow(() -> new BadRequestException("Invalid role: " + rn));
            user.setRole(role);
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            String s = request.getStatus().trim().toUpperCase();
            try {
                user.setStatus(UserAccountStatus.valueOf(s));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("status must be ACTIVE or DISABLED");
            }
        }

        userRepository.save(user);
        return toDto(user, resolveAvatar(user));
    }

    private Map<Integer, String> buildAvatarMap(List<User> users) {
        if (users == null || users.isEmpty()) return Collections.emptyMap();
        List<Integer> ids = users.stream().map(User::getUserId).collect(Collectors.toList());

        Map<Integer, String> map = new HashMap<>();
        for (JobSeekerProfile profile : jobSeekerProfileRepository.findByUser_UserIdIn(ids)) {
            if (profile.getUser() != null && profile.getAvatarUrl() != null) {
                map.put(profile.getUser().getUserId(), profile.getAvatarUrl());
            }
        }
        for (Company company : companyRepository.findByUser_UserIdIn(ids)) {
            if (company.getUser() != null && company.getLogoUrl() != null) {
                map.putIfAbsent(company.getUser().getUserId(), company.getLogoUrl());
            }
        }
        return map;
    }

    private String resolveAvatar(User user) {
        if (user == null || user.getUserId() == null) return null;
        String role = user.getRole() != null ? user.getRole().getRoleName() : null;
        if ("ROLE_COMPANY".equals(role)) {
            return companyRepository.findByUser_UserId(user.getUserId())
                    .map(Company::getLogoUrl)
                    .orElse(null);
        }
        return jobSeekerProfileRepository.findByUser_UserId(user.getUserId())
                .map(JobSeekerProfile::getAvatarUrl)
                .orElse(null);
    }

    private AdminUserDTO toDto(User u, String avatar) {
        UserAccountStatus st = u.getStatus() != null ? u.getStatus() : UserAccountStatus.ACTIVE;
        return AdminUserDTO.builder()
                .userId(u.getUserId())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .phone(u.getPhone())
                .roleName(u.getRole() != null ? u.getRole().getRoleName() : null)
                .status(st.name())
                .isVerified(u.getIsVerified())
                .createdAt(u.getCreatedAt() != null ? u.getCreatedAt().toString() : null)
                .avatar(avatar)
                .build();
    }
}
