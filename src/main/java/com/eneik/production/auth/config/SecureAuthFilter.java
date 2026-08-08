package com.eneik.production.auth.config;

import com.eneik.production.auth.service.SessionAuthResolver;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
public class SecureAuthFilter implements Filter {

    private final SessionAuthResolver sessionAuthResolver;

    public SecureAuthFilter(SessionAuthResolver sessionAuthResolver) {
        this.sessionAuthResolver = sessionAuthResolver;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            SessionAuthResolver.AuthResult auth = sessionAuthResolver.resolveAuth(httpRequest);
            if (auth.isAuthenticated()) {
                // Wrap request to override headers with database-resolved values (or test fallback headers)
                HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(httpRequest) {
                    @Override
                    public String getHeader(String name) {
                        if ("X-User-Role".equalsIgnoreCase(name)) {
                            return auth.getRole();
                        }
                        if ("X-User-Id".equalsIgnoreCase(name)) {
                            return auth.getUserId() != null ? auth.getUserId().toString() : null;
                        }
                        if ("Authorization".equalsIgnoreCase(name)) {
                            return auth.getRole() != null ? "Bearer " + auth.getRole() : null;
                        }
                        return super.getHeader(name);
                    }

                    @Override
                    public Enumeration<String> getHeaders(String name) {
                        if ("X-User-Role".equalsIgnoreCase(name)) {
                            return Collections.enumeration(Collections.singletonList(auth.getRole()));
                        }
                        if ("X-User-Id".equalsIgnoreCase(name)) {
                            return Collections.enumeration(Collections.singletonList(
                                    auth.getUserId() != null ? auth.getUserId().toString() : null));
                        }
                        if ("Authorization".equalsIgnoreCase(name)) {
                            return Collections.enumeration(Collections.singletonList(
                                    auth.getRole() != null ? "Bearer " + auth.getRole() : null));
                        }
                        return super.getHeaders(name);
                    }
                };
                chain.doFilter(wrappedRequest, response);
                return;
            } else {
                // Any unauthenticated request must have all raw security/identity headers stripped
                HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(httpRequest) {
                    @Override
                    public String getHeader(String name) {
                        if ("X-User-Role".equalsIgnoreCase(name) || "X-User-Id".equalsIgnoreCase(name) || "Authorization".equalsIgnoreCase(name)) {
                            return null;
                        }
                        return super.getHeader(name);
                    }

                    @Override
                    public Enumeration<String> getHeaders(String name) {
                        if ("X-User-Role".equalsIgnoreCase(name) || "X-User-Id".equalsIgnoreCase(name) || "Authorization".equalsIgnoreCase(name)) {
                            return Collections.enumeration(Collections.emptyList());
                        }
                        return super.getHeaders(name);
                    }
                };
                chain.doFilter(wrappedRequest, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
