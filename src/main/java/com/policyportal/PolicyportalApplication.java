package com.policyportal;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static com.policyportal.constant.FileConstant.USER_FOLDER;


@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class )
public class PolicyportalApplication {

	public static void main(String[] args) {
		SpringApplication.run(PolicyportalApplication.class, args);
		new File(USER_FOLDER).mkdirs();
	}

	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder(){
		return new BCryptPasswordEncoder();
	}
}


