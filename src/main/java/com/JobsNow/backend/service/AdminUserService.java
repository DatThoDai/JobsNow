package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.AdminUserDTO;
import com.JobsNow.backend.request.UpdateAdminUserRequest;

import java.util.List;

public interface AdminUserService {
    List<AdminUserDTO> listUsers();

    AdminUserDTO updateUser(Integer userId, UpdateAdminUserRequest request);
}
