package com.policyportal.repository;

import com.policyportal.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository  extends JpaRepository<User, Long> {

    User findUserByUserName(String userName);
    User findUserByEmail(String email);
}
