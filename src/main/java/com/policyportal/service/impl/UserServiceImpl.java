package com.policyportal.service.impl;

import static com.policyportal.constant.FileConstant.*;
import static com.policyportal.constant.UserImplConstant.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;
import javax.transaction.Transactional;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.policyportal.domain.User;
import com.policyportal.domain.UserPrincipal;
import com.policyportal.enumeration.Role;
import com.policyportal.exception.domain.*;
import com.policyportal.repository.UserRepository;
import com.policyportal.service.EmailService;
import com.policyportal.service.LoginAttemptService;
import com.policyportal.service.UserService;

@Service
@Transactional
@Qualifier("userDetailsService")
public class UserServiceImpl implements UserService, UserDetailsService {
	private UserRepository userRepository;
	private UserPrincipal userPrincipal;
	private LoginAttemptService loginAttemptService;
	private Logger logger = LoggerFactory.getLogger(getClass());
	private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private EmailService emailService;

	@Autowired
	public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder,
			LoginAttemptService loginAttemptService, EmailService emailService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.loginAttemptService = loginAttemptService;
		this.emailService = emailService;
	}

	@Override
	public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
		User user = userRepository.findUserByUserName(userName);
		if (user == null) {
			logger.error(NO_USER_FOUND_BY_USERNAME + userName);
			throw new UsernameNotFoundException(NO_USER_FOUND_BY_USERNAME + userName);
		} else {
			validateLoginAttempt(user);
			user.setLastLoginDateDisplay(user.getLastLoginDate());
			user.setLastLoginDate(new Date());
			userPrincipal = new UserPrincipal(user);
			logger.info(FOUND_USER_BY_USER_NAME + userPrincipal.getUsername());
			return userPrincipal;
		}
	}

	

	@Override
	public User register(String firstName, String lastName, String userName, String email)
			throws UserNotFoundException, UserNameExistsException, EmailExistsException, MessagingException {
		validateNewUsernameAndEmail(StringUtils.EMPTY, userName, email);
		User user = new User();
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setUserName(userName);
		user.setEmail(email);
		user.setJoinDate(new Date());
		user.setActive(true);
		user.setNotLocked(true);
		user.setRoles(Role.ROLE_USER.name());
		user.setAuthorities(Role.ROLE_USER.getAuthorities());
		user.setProfileImageUrl(getTemporaryProfileImageUrl(userName));
		user.setUserId(generateUserId());
		String password = generatePassword();
		logger.info("Password generated is :: "+password);
		user.setPassword(encodePassword(password));
		userRepository.save(user);
		emailService.sendNewPasswordEmail(firstName, password, email);
		logger.info("New user created successfully!!");

		return user;
	}



	@Override
	public List<User> getUsers() {
		return userRepository.findAll();
	}

	@Override
	public User findUserByUserName(String userName) {
		return userRepository.findUserByUserName(userName);
	}

	@Override
	public User findUserByEmail(String email) {
		return userRepository.findUserByEmail(email);
	}

	@Override
	public User addUser(String firstName, String lastName, String userName, String email, String role,
			boolean isNonLocked, boolean isActive, MultipartFile profileImage)
			throws UserNotFoundException, UserNameExistsException, EmailExistsException, IOException {
		validateNewUsernameAndEmail(StringUtils.EMPTY, userName, email);
		User user = new User();
		String password = generatePassword();
		String encodedPassword = encodePassword(password);
		user.setUserId(generateUserId());
		user.setPassword(encodedPassword);
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setUserName(userName);
		user.setEmail(email);
		user.setJoinDate(new Date());
		user.setActive(isActive);
		user.setNotLocked(isNonLocked);
		user.setRoles(getRoleEnumName(role).name());
		user.setAuthorities(getRoleEnumName(role).getAuthorities());
		user.setProfileImageUrl(getTemporaryProfileImageUrl(userName));
		userRepository.save(user);
		saveProfileImage(user, profileImage);
		return user;
	}

	

	@Override
	public User updateUser(String currentUserName, String newFirstName, String newLastName, String newUserName,
			String newEmail, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage)
			throws UserNotFoundException, UserNameExistsException, EmailExistsException, IOException {

		User currentUser = validateNewUsernameAndEmail(currentUserName, newUserName, newEmail);
		currentUser.setUserId(generateUserId());
		currentUser.setFirstName(newFirstName);
		currentUser.setLastName(newLastName);
		currentUser.setUserName(newUserName);
		currentUser.setEmail(newEmail);
		currentUser.setActive(isActive);
		currentUser.setNotLocked(isNonLocked);
		currentUser.setRoles(getRoleEnumName(role).name());
		currentUser.setAuthorities(getRoleEnumName(role).getAuthorities());
		userRepository.save(currentUser);
		saveProfileImage(currentUser, profileImage);
		return currentUser;
	}

	@Override
	public void deleteUser(long id) {
		userRepository.deleteById(id);

	}

	@Override
	public void resetPassword(String email) throws EmailNotFoundException, MessagingException {
		User user = userRepository.findUserByEmail(email);
		if (user != null) {
			String password = generatePassword();
			user.setPassword(password);
			userRepository.save(user);
			emailService.sendNewPasswordEmail(user.getFirstName(), password, user.getEmail());
		} else {
			throw new EmailNotFoundException(NO_USER_FOUND_BY_EMAIL);
		}

	}

	@Override
	public User updateProfileImage(String userName, MultipartFile newImage)
			throws UserNotFoundException, UserNameExistsException, EmailExistsException, IOException {
		User user = validateNewUsernameAndEmail(userName, null, null);
		saveProfileImage(user, newImage);
		return user;
	}
	
	private String setProfileImageUrl(String userName) {
		return ServletUriComponentsBuilder.fromCurrentContextPath().path(USER_IMAGE_PATH + userName + FORWARD_SLASH + userName + DOT + JPG_EXTENSION)
				.toUriString();
	}

	private Role getRoleEnumName(String role) {
		return Role.valueOf(role);
	}
	
	private void saveProfileImage(User user, MultipartFile profileImage) throws IOException {
		if (profileImage != null) {
			Path userFolder = Paths.get(USER_FOLDER + user.getUserName()).toAbsolutePath().normalize();
			if (Files.exists(userFolder)) {
				Files.createDirectories(userFolder);
				logger.info(DIRECTORY_CREATED);
			}
			Files.deleteIfExists(Paths.get(userFolder + user.getUserName() + DOT + JPG_EXTENSION));
			Files.copy(profileImage.getInputStream(), userFolder.resolve(user.getUserName() + DOT + JPG_EXTENSION),
					REPLACE_EXISTING);
			user.setProfileImageUrl(setProfileImageUrl(user.getUserName()));
			userRepository.save(user);
			logger.info(FILE_SAVED_IN_FILE_SYSTEM + profileImage.getOriginalFilename());
		}

	}
	
	private String getTemporaryProfileImageUrl(String userName) {
		return ServletUriComponentsBuilder.fromCurrentContextPath().path(DEFAULT_USER_IMAGE_PATH + userName)
				.toUriString();
	}

	private String encodePassword(String password) {
		return passwordEncoder.encode(password);
	}

	private String generatePassword() {
		return RandomStringUtils.randomAlphanumeric(10);
	}

	private String generateUserId() {
		return RandomStringUtils.randomNumeric(10);
	}

	private User validateNewUsernameAndEmail(String currentUserName, String newUserName, String email)
			throws UserNotFoundException, UserNameExistsException, EmailExistsException {
		User userByNewUserName = findUserByUserName(newUserName);
		User userByNewEmail = findUserByEmail(email);

		if (StringUtils.isNotBlank(currentUserName)) {
			User currentUser = findUserByUserName(currentUserName);
			if (currentUser == null) {
				throw new UserNotFoundException(NO_USER_FOUND_BY_USERNAME + currentUserName);
			}
			if (userByNewUserName != null && !currentUser.getId().equals(userByNewUserName.getId())) {
				throw new UserNameExistsException(USERNAME_ALREADY_EXISTS_);
			}
			if (userByNewEmail != null && !currentUser.getEmail().equals(userByNewEmail.getEmail())) {
				throw new EmailExistsException(EMAIL_ALREADY_EXISTS_);
			}
			return currentUser;
		} else {
			if (userByNewUserName != null) {
				throw new UserNameExistsException(USERNAME_ALREADY_EXISTS_);
			}
			if (userByNewEmail != null) {
				throw new EmailExistsException(EMAIL_ALREADY_EXISTS_);
			}
			return null;
		}
	}
	
	private void validateLoginAttempt(User user) {
		if (user.isNotLocked()) {
			if (loginAttemptService.hasExceededMaxAttempts(user.getUserName())) {
				user.setNotLocked(false);
			} else {
				user.setNotLocked(true);
			}
		} else {
			loginAttemptService.evictUserFromLoginAttemptCache(user.getUserName());
		}

	}
}
