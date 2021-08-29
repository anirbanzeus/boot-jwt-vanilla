package com.policyportal.listner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.stereotype.Component;

import com.policyportal.domain.UserPrincipal;
import com.policyportal.service.LoginAttemptService;

@Component
public class AuthenticationSuccessListner implements ApplicationListener<AbstractAuthenticationEvent>{

	private LoginAttemptService loginAttemptService;

	@Autowired
	public AuthenticationSuccessListner(LoginAttemptService loginAttemptService) {
		this.loginAttemptService = loginAttemptService;
	}


	@Override
	public void onApplicationEvent(AbstractAuthenticationEvent event) {
		Object principle = event.getAuthentication().getPrincipal();
		if (principle instanceof UserPrincipal) {
			UserPrincipal user = (UserPrincipal) event.getAuthentication().getPrincipal();
			loginAttemptService.evictUserFromLoginAttemptCache(user.getUsername());
			//System.out.println(">>>>>>>>>>"+user.getUsername());
		}
		
	}

}
