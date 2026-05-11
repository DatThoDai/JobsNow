package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.AdminUserDTO;
import com.JobsNow.backend.request.UpdateAdminUserRequest;
import com.JobsNow.backend.response.PagedResponse;

public interface AdminUserService {
    PagedResponse<AdminUserDTO> listUsers(int page, int limit);

    AdminUserDTO updateUser(Integer userId, UpdateAdminUserRequest request);
}
