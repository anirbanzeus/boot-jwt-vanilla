package com.policyportal.constant;

public class SecurityConstant {
    public static final long EXPIRATION_TIME = 432_000_000; // 5 days expressed in milliseconds
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String JWT_TOKEN_HEADER = "Jwt-Token";
    public static final String TOKEN_CANNOT_BE_VERIFIED = "Token cannot be verified !!";
    public static final String COMPANY_LLC = "Company LLC";
    public static final String COMPANY_ADMINISTRATION = "Policy Management Portal";
    public static final String AUTHORITIES = "authorities";
    public static final String FORBIDDEN_MESSAGE = "You need to login to access this page";
    public static final String ACCESS_DENIED_MESSAGE = "You donot have permission to access this page";
    public static final String OPTION_HTTP_METHOD = "OPTIONS";
    //public static final String[] PUBLIC_URLS = {"/user/login","user/register","user/resetpassword/**","/user/image/**"};
    public static final String[] PUBLIC_URLS = {"**"};

}
