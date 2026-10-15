package com.rideapp.auth.service;

import com.rideapp.auth.dto.AuthResponse;
import com.rideapp.auth.dto.LoginRequest;
import com.rideapp.auth.dto.RegisterRequest;
import com.rideapp.auth.security.JwtService;
import com.rideapp.common.exception.ConflictException;
import com.rideapp.common.exception.ResourceNotFoundException;
import com.rideapp.driver.entity.Driver;
import com.rideapp.driver.repository.DriverRepository;
import com.rideapp.user.entity.User;
import com.rideapp.user.entity.UserRole;
import com.rideapp.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthService(
            UserRepository userRepository,
            DriverRepository driverRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already registered: " + request.getEmail());
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ConflictException("Phone number is already registered: " + request.getPhoneNumber());
        }

        if (request.getRole() == UserRole.DRIVER) {
            if (request.getDriverLicenseNumber() == null || request.getDriverLicenseNumber().isBlank()) {
                throw new IllegalArgumentException("Driver license number is required for driver registration");
            }
            if (driverRepository.findByLicenseNumber(request.getDriverLicenseNumber()).isPresent()) {
                throw new ConflictException("Driver license number is already registered: " + request.getDriverLicenseNumber());
            }
        }

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getPhoneNumber(),
                request.getGender(),
                request.getRole()
        );
        user = userRepository.save(user);

        if (request.getRole() == UserRole.DRIVER) {
            Driver driver = new Driver(user, request.getDriverLicenseNumber());
            driverRepository.save(driver);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails, user.getId(), user.getRole().name());

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getGender());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails, user.getId(), user.getRole().name());

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getGender());
    }
}
