package com.policyportal.service;

import com.policyportal.domain.User;
import com.policyportal.exception.domain.EmailExistsException;
import com.policyportal.exception.domain.EmailNotFoundException;
import com.policyportal.exception.domain.UserNameExistsException;
import com.policyportal.exception.domain.UserNotFoundException;

import java.io.IOException;
import java.util.List;

import javax.mail.MessagingException;

import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    User register(String firstName, String lastName, String userName, String email) throws UserNotFoundException, UserNameExistsException, EmailExistsException, MessagingException;

    List<User> getUsers();

    User findUserByUserName(String userName);

    User findUserByEmail(String email);
    
    User addUser(String firstName, String lastName, String userName, String email, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage) throws UserNotFoundException, UserNameExistsException, EmailExistsException, IOException;

    User updateUser(String currentUserName, String newFirstName, String newLastName,String newUserName, String newEmail, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage) throws UserNotFoundException, UserNameExistsException, EmailExistsException, IOException;

    void deleteUser(long id);
    
    void resetPassword(String email) throws EmailNotFoundException, MessagingException;
    
    User updateProfileImage(String userName, MultipartFile newImage) throws UserNotFoundException, UserNameExistsException, EmailExistsException, IOException;
}
