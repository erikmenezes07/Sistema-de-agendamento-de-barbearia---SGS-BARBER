package com.sgsbarber.repository;

import com.sgsbarber.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<UserModel, Long> {
    List<UserModel> findByRoleIgnoreCase(String role);
}
