package com.example.testtask;

import com.example.testtask.dto.UserCreateRequest;
import com.example.testtask.dto.UserResponse;
import com.example.testtask.entity.User;
import com.example.testtask.entity.Account;
import com.example.testtask.entity.EmailData;
import com.example.testtask.entity.PhoneData;
import com.example.testtask.repository.UserRepository;
import com.example.testtask.repository.AccountRepository;
import com.example.testtask.repository.EmailDataRepository;
import com.example.testtask.repository.PhoneDataRepository;
import com.example.testtask.service.UserService;
import com.example.testtask.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private EmailDataRepository emailDataRepository;
    
    @Mock
    private PhoneDataRepository phoneDataRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    private UserCreateRequest createRequest;
    private User user;
    private Account account;

    @BeforeEach
    void setUp() {
        createRequest = new UserCreateRequest();
        createRequest.setName("John Doe");
        createRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));
        createRequest.setPassword("password123");
        createRequest.setInitialBalance(BigDecimal.valueOf(1000));
        
        UserCreateRequest.EmailRequest emailRequest = new UserCreateRequest.EmailRequest();
        emailRequest.setEmail("john@example.com");
        createRequest.setEmails(List.of(emailRequest));
        
        UserCreateRequest.PhoneRequest phoneRequest = new UserCreateRequest.PhoneRequest();
        phoneRequest.setPhone("79201234567");
        createRequest.setPhones(List.of(phoneRequest));

        user = new User();
        user.setId(1L);
        user.setName("John Doe");
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));
        user.setPassword("encodedPassword");

        account = new Account();
        account.setId(1L);
        account.setUserId(1L);
        account.setBalance(BigDecimal.valueOf(1000));
        account.setInitialBalance(BigDecimal.valueOf(1000));
    }

    @Test
    void createUser_Success() {
        // Given
        when(emailDataRepository.existsByEmail(anyString())).thenReturn(false);
        when(phoneDataRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(emailDataRepository.saveAll(any())).thenReturn(List.of(new EmailData()));
        when(phoneDataRepository.saveAll(any())).thenReturn(List.of(new PhoneData()));

        // When
        UserResponse response = userService.createUser(createRequest);

        // Then
        assertNotNull(response);
        assertEquals("John Doe", response.getName());
        assertEquals(BigDecimal.valueOf(1000), response.getBalance());
        
        verify(userRepository).save(any(User.class));
        verify(accountRepository).save(any(Account.class));
        verify(emailDataRepository).saveAll(any());
        verify(phoneDataRepository).saveAll(any());
    }

    @Test
    void createUser_EmailAlreadyExists_ThrowsException() {
        // Given
        when(emailDataRepository.existsByEmail("john@example.com")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.createUser(createRequest)
        );
        
        assertEquals("Email already exists: john@example.com", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserById_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(account));
        when(emailDataRepository.findByUserId(1L)).thenReturn(List.of(new EmailData()));
        when(phoneDataRepository.findByUserId(1L)).thenReturn(List.of(new PhoneData()));

        // When
        Optional<UserResponse> response = userService.getUserById(1L);

        // Then
        assertTrue(response.isPresent());
        assertEquals("John Doe", response.get().getName());
        assertEquals(BigDecimal.valueOf(1000), response.get().getBalance());
    }

    @Test
    void getUserById_UserNotFound_ReturnsEmpty() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Optional<UserResponse> response = userService.getUserById(1L);

        // Then
        assertFalse(response.isPresent());
    }


} 