package com.example.registrationmodule.service;

import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.Admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IAdminService {
    boolean existsById(UUID adminId);
    Optional<Admin> getAdminById(UUID adminId);
    Admin saveAdmin(Admin admin);
    void deleteById(UUID adminId);
    ResponseLoginDTO login(LoginAdminDTO loginAdminDTO);
    void logout(AdminLogoutDTO adminLogoutDTO);

    TokenDTO refreshToken(String refreshToken);

    List<AdminDTO> getAllAdmins();
}
