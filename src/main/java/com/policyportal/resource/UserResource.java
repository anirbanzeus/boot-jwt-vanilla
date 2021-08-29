package com.policyportal.resource;

import com.policyportal.domain.HttpResponse;
import com.policyportal.domain.User;
import com.policyportal.domain.UserPrincipal;
import com.policyportal.exception.domain.*;
import com.policyportal.service.UserService;
import com.policyportal.utility.JWTTokenProvider;

import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static com.policyportal.constant.SecurityConstant.JWT_TOKEN_HEADER;
import static org.springframework.http.HttpStatus.*;
import static com.policyportal.constant.FileConstant.*;
import static com.policyportal.constant.ValidationConstant.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.mail.MessagingException;

@RestController
@RequestMapping(path = {"/","/user"})
public class UserResource {
    private static final String USER_DELETED_SUCCESSFULLY = "User deleted successfully !!!";
	private static final String RESET_EMAIL_SENT = "Password reset email sent successfully to :: ";
	@Autowired
    private UserService userService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JWTTokenProvider jwttokenProvider;
    
    private EmailValidator emailValidator = EmailValidator.getInstance();


    @GetMapping("/home")
    public String showUser() throws EmailExistsException{
        return "I am a user";
        //throw new EmailExistsException("This email is allready existing");
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) throws UserNotFoundException, UserNameExistsException, EmailExistsException, MessagingException, ValidationException {
        
    	if(emailValidator.isValid(user.getEmail())) {
    		User newUser = userService.register(user.getFirstName(), user.getLastName(), user.getUserName(), user.getEmail());
    		return new ResponseEntity<User>(newUser, OK);
        }else {
        	throw new ValidationException(INVALID_EMAIL);
        }
    	
        
    }
    
    @PostMapping("/add")
    public ResponseEntity<User> addNewUser( @RequestParam ("firstName")String firstName,
								    		@RequestParam ("lastName")String lastName,
								    		@RequestParam ("userName")String userName,
								    		@RequestParam ("email")String email,
								    		@RequestParam ("role")String role,
								    		@RequestParam ("isNonLocker")String isNonLocker,
								    		@RequestParam ("isActive")String isActive,
								    		@RequestParam (value="profileImage", required=false)MultipartFile profileImage) 
    				throws UserNotFoundException, UserNameExistsException, EmailExistsException, MessagingException, IOException, ValidationException {
    	if(emailValidator.isValid(email)) {
    		User newUser = userService.addUser(firstName, lastName, userName, email, role, Boolean.parseBoolean(userName), Boolean.parseBoolean(isActive), profileImage);
    		return new ResponseEntity<User>(newUser, OK);
        }else {
        	throw new ValidationException(INVALID_EMAIL);
        }
        
        
    }
    
    @PostMapping("/update")
    public ResponseEntity<User> updateUser( @RequestParam ("currentUserName")String currentUserName,
    										@RequestParam ("firstName")String firstName,
								    		@RequestParam ("lastName")String lastName,
								    		@RequestParam ("userName")String userName,
								    		@RequestParam ("email")String email,
								    		@RequestParam ("role")String role,
								    		@RequestParam ("isNonLocker")String isNonLocker,
								    		@RequestParam ("isActive")String isActive,
								    		@RequestParam (value="profileImage", required=false)MultipartFile profileImage) 
    				throws UserNotFoundException, UserNameExistsException, EmailExistsException, MessagingException, IOException, ValidationException {
    	if(emailValidator.isValid(email)) {
    		User newUser = userService.updateUser(currentUserName, firstName, lastName, userName, email, role, Boolean.parseBoolean(userName), Boolean.parseBoolean(isActive), profileImage);
    		return new ResponseEntity<User>(newUser, OK);
        }else {
        	throw new ValidationException(INVALID_EMAIL);
        }
        
        
    }
    
    @PostMapping("/updateProfileImage")
    public ResponseEntity<User> updateUser( @RequestParam ("userName")String userName,
								    		@RequestParam (value="profileImage", required=false)MultipartFile profileImage) 
    				throws UserNotFoundException, UserNameExistsException, EmailExistsException, MessagingException, IOException {
        User user = userService.updateProfileImage(userName, profileImage);
        return new ResponseEntity<User>(user, OK);
    }
    
    @GetMapping("/find/{userName}")
    public ResponseEntity<User> getUserByUserName( @PathVariable ("userName")String userName) 
    				throws UserNotFoundException, UserNameExistsException, EmailExistsException, MessagingException, IOException {
        User user = userService.findUserByUserName(userName);
        return new ResponseEntity<User>(user, OK);
    }
    
    @GetMapping("/list")
    public ResponseEntity<List<User>> getAllUsers() 
    				throws UserNotFoundException, UserNameExistsException, EmailExistsException, MessagingException, IOException {
        List<User> users = userService.getUsers();
        return new ResponseEntity<List<User>>(users, OK);
    }
    
    @GetMapping("/resetPassword/{email}")
    public ResponseEntity<HttpResponse> resetPassword(@PathVariable ("email")String email) throws EmailNotFoundException, MessagingException {
       userService.resetPassword(email);
        return response(HttpStatus.OK,RESET_EMAIL_SENT+email);
    }
    

	@PostMapping("/delete/{userId}")
	@PreAuthorize("hasAnyAuthority('user:delete')")
    public ResponseEntity<HttpResponse> deleteUser( @RequestParam ("userId")String userId) 
    				throws UserNotFoundException, UserNameExistsException, EmailExistsException, MessagingException, IOException {
        userService.deleteUser(Long.parseLong(userId));
        return response(NO_CONTENT, USER_DELETED_SUCCESSFULLY);
    }
	
	

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody User user) throws UserNotFoundException{
        authenticate(user.getUserName(), user.getPassword());
        User loginUser = userService.findUserByUserName(user.getUserName());
        UserPrincipal userPrincipal = new UserPrincipal(loginUser);
        HttpHeaders jwtHeaders = getJwtHeader(userPrincipal);
        return new ResponseEntity<User>(loginUser,jwtHeaders, HttpStatus.OK);
    }
    
    @GetMapping(path = "/image/profile/{userName}", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] getProfileImage(@PathVariable ("userName")String userName) throws IOException {
    	URL url = new URL(TEMP_PROFILE_IMAGE_BASE_URL + userName);
    	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    	
    	try(InputStream stream = url.openStream()){
    		int bytesRead;
    		byte[] chunk = new byte[1024];
    		while((bytesRead = stream.read(chunk))>0) {
    			byteArrayOutputStream.write(chunk, 0, bytesRead);
    		}
    	}
		return byteArrayOutputStream.toByteArray();  	
    }
    
    
    @GetMapping(path = "/image/{userName}/{fileName}", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] getTemporaryProfileImage(@PathVariable ("userName")String userName,
    							  @PathVariable ("fileName")String fileName) throws IOException {
		return Files.readAllBytes(Paths.get(USER_FOLDER + userName + FORWARD_SLASH + fileName));  	
    }

    private HttpHeaders getJwtHeader(UserPrincipal userPrincipal) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(JWT_TOKEN_HEADER, jwttokenProvider.generateJwtToken(userPrincipal));
        return headers;
    }

    private void authenticate(String userName, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userName, password));
    }
    
    private ResponseEntity<HttpResponse> response(HttpStatus httpStatus, String message) {
        return new ResponseEntity<>(new HttpResponse(httpStatus.value(), httpStatus, httpStatus.getReasonPhrase().toUpperCase(),
                message), httpStatus);
    }
}
