package com.ztna.service;

import com.ztna.model.User;
import com.ztna.repository.UserRepository;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

        private final UserRepository userRepository;
        private final BCryptPasswordEncoder passwordEncoder;
        private final LoginAttemptService loginAttemptService;

        public UserService(
                        UserRepository userRepository,
                        LoginAttemptService loginAttemptService) {

                this.userRepository = userRepository;
                this.loginAttemptService = loginAttemptService;

                this.passwordEncoder = new BCryptPasswordEncoder();
        }

        public User registerUser(User user) {

                if (userRepository.existsByUsername(
                                user.getUsername())) {

                        throw new RuntimeException(
                                        "Username already exists");
                }

                user.setPassword(
                                passwordEncoder.encode(
                                                user.getPassword()));

                if (user.getRole() == null ||
                                user.getRole().isBlank()) {

                        user.setRole("USER");
                }

                return userRepository.save(user);
        }

        public User loginUser(
                        String username,
                        String password) {

                User user = userRepository
                                .findByUsername(username)
                                .orElseThrow(() -> new RuntimeException(
                                                "Invalid username or password"));

                boolean passwordMatches = passwordEncoder.matches(
                                password,
                                user.getPassword());

                System.out.println(
                                "PASSWORD MATCH for "
                                                + username
                                                + " = "
                                                + passwordMatches);

                if (!passwordMatches) {

                        loginAttemptService.recordFailedAttempt(username);

                        throw new RuntimeException(
                                        "Invalid username or password");
                }

                // Successful login:
                // Do NOT reset failed attempts immediately.
                // The risk score persists for 24 hours unless overridden by an admin.
                return user;
        }

        public User getUserByUsername(
                        String username) {

                return userRepository
                                .findByUsername(username)
                                .orElseThrow(() -> new RuntimeException(
                                                "User not found"));
        }
}