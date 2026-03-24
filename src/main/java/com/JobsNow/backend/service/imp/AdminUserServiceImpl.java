package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.dto.AdminUserDTO;
import com.JobsNow.backend.entity.Role;
import com.JobsNow.backend.entity.User;
import com.JobsNow.backend.entity.UserAccountStatus;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.repositories.RoleRepository;
import com.JobsNow.backend.repositories.UserRepository;
import com.JobsNow.backend.request.UpdateAdminUserRequest;
import com.JobsNow.backend.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserDTO> listUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
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
        return toDto(user);
    }

    private AdminUserDTO toDto(User u) {
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
                .build();
    }
}
