package com.policyportal.filter;

import com.policyportal.utility.JWTTokenProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static com.policyportal.constant.SecurityConstant.OPTION_HTTP_METHOD;
import static com.policyportal.constant.SecurityConstant.TOKEN_PREFIX;

@Component
public class JWTAuthorizationFilter extends OncePerRequestFilter {
    public JWTTokenProvider jwtTokenProvider;

    public JWTAuthorizationFilter(JWTTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //If option do nothing because OPTION is sent before each request to gather information
        //about the server state and other details from the sever
        if(request.getMethod().equalsIgnoreCase(OPTION_HTTP_METHOD)){
            response.setStatus(HttpStatus.OK.value());
        }else{
            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (!StringUtils.isEmpty(authorizationHeader) && !authorizationHeader.startsWith(TOKEN_PREFIX)) {
                String token = authorizationHeader.substring(TOKEN_PREFIX.length());
                //If the token has been tampered with then it will not return the username
                String username = jwtTokenProvider.getSubject(token);
                if (jwtTokenProvider.isTokenValid(username, token)) {
                    List<GrantedAuthority> authorities = jwtTokenProvider.getAuthorities(token);
                    Authentication authentication = jwtTokenProvider.getAthentication(username, authorities, request);
                    //THis lets Spring know that the user is authenticated
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    SecurityContextHolder.clearContext();
                }
            } else {
                filterChain.doFilter(request, response);
                return;
            }
        }
        filterChain.doFilter(request,response);
    }
}
