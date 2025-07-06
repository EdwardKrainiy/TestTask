package com.example.testtask.service;

import com.example.testtask.dto.*;
import com.example.testtask.entity.*;
import com.example.testtask.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final EmailDataRepository emailDataRepository;
    private final PhoneDataRepository phoneDataRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserLookupService userLookupService;
    
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creating user: name={}, emails={}, phones={}", 
                request.getName(), 
                request.getEmails().stream().map(UserCreateRequest.EmailRequest::getEmail).toList(),
                request.getPhones().stream().map(UserCreateRequest.PhoneRequest::getPhone).toList());
        
        for (UserCreateRequest.EmailRequest emailReq : request.getEmails()) {
            if (emailDataRepository.existsByEmail(emailReq.getEmail())) {
                log.warn("User creation failed: email already exists - {}", emailReq.getEmail());
                throw new IllegalArgumentException("Email already exists: " + emailReq.getEmail());
            }
        }
        
        for (UserCreateRequest.PhoneRequest phoneReq : request.getPhones()) {
            if (phoneDataRepository.existsByPhone(phoneReq.getPhone())) {
                log.warn("User creation failed: phone already exists - {}", phoneReq.getPhone());
                throw new IllegalArgumentException("Phone already exists: " + phoneReq.getPhone());
            }
        }
        
        User user = new User();
        user.setName(request.getName());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        user = userRepository.save(user);
        
        Account account = new Account();
        account.setUserId(user.getId());
        account.setBalance(request.getInitialBalance());
        account.setInitialBalance(request.getInitialBalance());
        accountRepository.save(account);
        
        List<EmailData> emails = new ArrayList<>();
        for (UserCreateRequest.EmailRequest emailReq : request.getEmails()) {
            EmailData emailData = new EmailData();
            emailData.setUserId(user.getId());
            emailData.setEmail(emailReq.getEmail());
            emails.add(emailData);
        }
        emailDataRepository.saveAll(emails);
        
        List<PhoneData> phones = new ArrayList<>();
        for (UserCreateRequest.PhoneRequest phoneReq : request.getPhones()) {
            PhoneData phoneData = new PhoneData();
            phoneData.setUserId(user.getId());
            phoneData.setPhone(phoneReq.getPhone());
            phones.add(phoneData);
        }
        phoneDataRepository.saveAll(phones);
        
        log.info("User created successfully: ID={}, name={}, initialBalance={}", 
                user.getId(), user.getName(), request.getInitialBalance());
        return mapToUserResponse(user, account, emails, phones);
    }
    
    @Cacheable(value = "users", key = "#userId")
    public Optional<UserResponse> getUserById(Long userId) {
        log.debug("Fetching user by ID: {}", userId);
        
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            log.debug("User not found: ID={}", userId);
            return Optional.empty();
        }
        
        Optional<Account> account = accountRepository.findByUserId(userId);
        List<EmailData> emails = emailDataRepository.findByUserId(userId);
        List<PhoneData> phones = phoneDataRepository.findByUserId(userId);
        
        log.debug("User fetched successfully: ID={}", userId);
        return Optional.of(mapToUserResponse(user.get(), account.orElse(null), emails, phones));
    }
    
    @Transactional
    @CacheEvict(value = {"users", "usersByEmail", "usersByPhone"}, key = "#userId")
    public Optional<UserResponse> updateUser(Long userId, UserUpdateRequest request) {
        log.info("Updating user: ID={}, hasNameUpdate={}, hasEmailUpdate={}, hasPhoneUpdate={}", 
                userId, request.getName() != null, request.getEmails() != null, request.getPhones() != null);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User update failed: user not found - ID={}", userId);
            return Optional.empty();
        }
        
        User user = userOpt.get();
        
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            String oldName = user.getName();
            user.setName(request.getName());
            log.debug("User name updated: ID={}, oldName={}, newName={}", userId, oldName, request.getName());
        }
        
        if (request.getEmails() != null && !request.getEmails().isEmpty()) {
            for (UserUpdateRequest.EmailUpdateRequest emailReq : request.getEmails()) {
                if (emailReq.getEmail() != null && 
                    !emailDataRepository.findByEmail(emailReq.getEmail()).map(e -> e.getUserId().equals(userId)).orElse(true)) {
                    log.warn("User update failed: email already exists - {}", emailReq.getEmail());
                    throw new IllegalArgumentException("Email already exists: " + emailReq.getEmail());
                }
            }
            
            List<EmailData> oldEmails = emailDataRepository.findByUserId(userId);
            emailDataRepository.deleteByUserId(userId);
            
            List<EmailData> newEmails = new ArrayList<>();
            for (UserUpdateRequest.EmailUpdateRequest emailReq : request.getEmails()) {
                if (emailReq.getEmail() != null && !emailReq.getEmail().trim().isEmpty()) {
                    EmailData emailData = new EmailData();
                    emailData.setUserId(userId);
                    emailData.setEmail(emailReq.getEmail());
                    newEmails.add(emailData);
                }
            }
            emailDataRepository.saveAll(newEmails);
            
            log.info("User emails updated: ID={}, oldEmails={}, newEmails={}", 
                    userId, 
                    oldEmails.stream().map(EmailData::getEmail).toList(),
                    newEmails.stream().map(EmailData::getEmail).toList());
        }
        
        if (request.getPhones() != null && !request.getPhones().isEmpty()) {
            for (UserUpdateRequest.PhoneUpdateRequest phoneReq : request.getPhones()) {
                if (phoneReq.getPhone() != null && 
                    !phoneDataRepository.findByPhone(phoneReq.getPhone()).map(p -> p.getUserId().equals(userId)).orElse(true)) {
                    log.warn("User update failed: phone already exists - {}", phoneReq.getPhone());
                    throw new IllegalArgumentException("Phone already exists: " + phoneReq.getPhone());
                }
            }
            
            List<PhoneData> oldPhones = phoneDataRepository.findByUserId(userId);
            phoneDataRepository.deleteByUserId(userId);
            
            List<PhoneData> newPhones = new ArrayList<>();
            for (UserUpdateRequest.PhoneUpdateRequest phoneReq : request.getPhones()) {
                if (phoneReq.getPhone() != null && !phoneReq.getPhone().trim().isEmpty()) {
                    PhoneData phoneData = new PhoneData();
                    phoneData.setUserId(userId);
                    phoneData.setPhone(phoneReq.getPhone());
                    newPhones.add(phoneData);
                }
            }
            phoneDataRepository.saveAll(newPhones);
            
            log.info("User phones updated: ID={}, oldPhones={}, newPhones={}", 
                    userId, 
                    oldPhones.stream().map(PhoneData::getPhone).toList(),
                    newPhones.stream().map(PhoneData::getPhone).toList());
        }
        
        user = userRepository.save(user);
        
        Optional<Account> account = accountRepository.findByUserId(userId);
        List<EmailData> emails = emailDataRepository.findByUserId(userId);
        List<PhoneData> phones = phoneDataRepository.findByUserId(userId);
        
        log.info("User updated successfully: ID={}", userId);
        return Optional.of(mapToUserResponse(user, account.orElse(null), emails, phones));
    }
    
    public Page<UserResponse> searchUsers(LocalDate dateOfBirth, String phone, String name, String email, Pageable pageable) {
        log.info("Searching users: dateOfBirth={}, phone={}, name={}, email={}, page={}, size={}", 
                 dateOfBirth, phone, name, email, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<User> users;
        
        if (dateOfBirth != null) {
            users = userRepository.findByDateOfBirthAfter(dateOfBirth, pageable);
        } else if (phone != null && !phone.trim().isEmpty()) {
            Optional<User> userOpt = userRepository.findByPhone(phone.trim());
            List<User> content = userOpt.map(List::of).orElse(List.of());
            users = new PageImpl<>(content, pageable, content.size());
        } else if (name != null && !name.trim().isEmpty()) {
            users = userRepository.findByNameContainingIgnoreCase(name.trim(), pageable);
        } else if (email != null && !email.trim().isEmpty()) {
            Optional<User> userOpt = userRepository.findByEmail(email.trim());
            List<User> content = userOpt.map(List::of).orElse(List.of());
            users = new PageImpl<>(content, pageable, content.size());
        } else {
            users = userRepository.findAll(pageable);
        }
        
        List<UserResponse> userResponses = users.getContent().stream()
                .map(user -> {
                    Optional<Account> account = accountRepository.findByUserId(user.getId());
                    List<EmailData> emails = emailDataRepository.findByUserId(user.getId());
                    List<PhoneData> phones = phoneDataRepository.findByUserId(user.getId());
                    return mapToUserResponse(user, account.orElse(null), emails, phones);
                })
                .collect(Collectors.toList());
        
        log.info("User search completed: found {} users out of {} total", 
                userResponses.size(), users.getTotalElements());
        return new PageImpl<>(userResponses, pageable, users.getTotalElements());
    }
    
    public AuthResponse authenticate(AuthRequest request) {
        log.info("Authenticating user: login={}", request.getLogin());
        
        Optional<User> userOpt = userLookupService.findByEmail(request.getLogin());
        if (userOpt.isEmpty()) {
            userOpt = userLookupService.findByPhone(request.getLogin());
        }
        
        if (userOpt.isEmpty()) {
            log.warn("Authentication failed: user not found - login={}", request.getLogin());
            throw new IllegalArgumentException("Invalid credentials");
        }
        
        if (!passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
            log.warn("Authentication failed: invalid password - login={}", request.getLogin());
            throw new IllegalArgumentException("Invalid credentials");
        }
        
        String token = jwtService.generateToken(userOpt.get().getId());
        log.info("User authenticated successfully: userID={}, login={}", userOpt.get().getId(), request.getLogin());
        
        return new AuthResponse(token);
    }
    
    private UserResponse mapToUserResponse(User user, Account account, List<EmailData> emails, List<PhoneData> phones) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setDateOfBirth(user.getDateOfBirth());
        response.setBalance(account != null ? account.getBalance() : BigDecimal.ZERO);
        response.setEmails(emails.stream().map(EmailData::getEmail).collect(Collectors.toList()));
        response.setPhones(phones.stream().map(PhoneData::getPhone).collect(Collectors.toList()));
        return response;
    }
} 