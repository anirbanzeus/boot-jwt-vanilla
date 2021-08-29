package com.policyportal.listner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

import com.policyportal.service.LoginAttemptService;

@Component
public class AuthenticationFailureListner {
	
	private LoginAttemptService loginAttemptService;
	
	@Autowired
	public AuthenticationFailureListner(LoginAttemptService loginAttemptService) {
		this.loginAttemptService = loginAttemptService;
	}
	
	@EventListener
	public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
		Object principle = event.getAuthentication().getPrincipal();
		if(principle instanceof String) {
			String userName = (String)event.getAuthentication().getPrincipal();
			loginAttemptService.addUserToLoginAttemptCache(userName);
		}
		
	}

}
