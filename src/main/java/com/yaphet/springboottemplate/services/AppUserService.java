package com.yaphet.springboottemplate.services;

import com.yaphet.springboottemplate.exceptions.EmailAlreadyExistsException;
import com.yaphet.springboottemplate.exceptions.EmailNotFoundException;
import com.yaphet.springboottemplate.exceptions.IdNotFoundException;
import com.yaphet.springboottemplate.models.AppUser;
import com.yaphet.springboottemplate.models.ConfirmationToken;
import com.yaphet.springboottemplate.models.Role;
import com.yaphet.springboottemplate.repositories.AppUserRepository;
import com.yaphet.springboottemplate.utilities.security.AppUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class AppUserService implements UserDetailsService {

    private final BCryptPasswordEncoder passwordEncoder;
    private final AppUserRepository appUserRepository;
    private final ConfirmationTokenService confirmationTokenService;
    private final String USER_NOT_FOUND_MSG = "user with %s not found";
    private final String RESOURCE_NAME = "App user";
    private final int TOKEN_EXPIRATION_DELAY = 15;

    @Autowired
    public AppUserService(BCryptPasswordEncoder passwordEncoder,
                          AppUserRepository appUserRepository,
                          ConfirmationTokenService confirmationTokenService) {
        this.passwordEncoder = passwordEncoder;
        this.appUserRepository = appUserRepository;
        this.confirmationTokenService = confirmationTokenService;
    }


    @Cacheable(cacheNames = "appUserList", key = "#root.methodName")
    public List<AppUser> getAppUsers() {
        return appUserRepository.findAll();
    }

    public Page<AppUser> getAppUsersPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return appUserRepository.findAll(pageable);
    }

    public AppUser getAppUser(Long id) {
        return appUserRepository
                .findById(id)
                .orElseThrow(() -> new IdNotFoundException(RESOURCE_NAME, id));
    }
    public AppUser getAppUser(String email) {
        return appUserRepository
                .findByEmail(email)
                .orElseThrow(() -> new EmailNotFoundException(email));
    }

    public void saveAppUser(AppUser appUser) {
        String email = appUser.getEmail();
        boolean emailExists  =   appUserRepository.findByEmail(email).isPresent();

        if(emailExists){
            throw new EmailAlreadyExistsException(email);
        }
        String encodedPassword = passwordEncoder.encode(appUser.getPassword());
        appUser.setPassword(encodedPassword);
        appUserRepository.save(appUser);
    }

    @Transactional
    public boolean updateAppUser(AppUser au) {
        boolean isUpdated = false;
        String newEmail = au.getEmail();
        String firstName = au.getFirstName();
        String lastName = au.getLastName();
        AppUser appUser = ((AppUserDetails)SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal()).getAppUser();

        if(newEmail != null &&
                newEmail.length() > 0 &&
                !Objects.equals(newEmail, appUser.getEmail())){
            if(appUserRepository.findByEmail(newEmail).isPresent()){
                throw new EmailAlreadyExistsException(newEmail);
            }
            appUser.setEmail(newEmail);
            isUpdated = true;
        }

        if(firstName != null &&
                firstName.length() > 0 &&
                !Objects.equals(appUser.getFirstName(), firstName)){
            appUser.setFirstName(firstName);
            isUpdated = true;
        }
        if(lastName != null &&
                lastName.length() > 0 &&
                !Objects.equals(appUser.getLastName(), lastName)){
            appUser.setLastName(lastName);
            isUpdated = true;
        }
        if(isUpdated){
            appUserRepository.save(appUser);
        }
        return isUpdated;
    }

    public void deleteAppUser(Long id) {
        AppUser appUser = appUserRepository
                .findById(id)
                .orElseThrow(() -> new IdNotFoundException(RESOURCE_NAME, id));
        Set<Role> appUserRoles = appUser.getRoles();
        for(Role role : appUserRoles){
            role.removeAppUser(appUser);
        }
        appUserRepository.deleteById(id);
    }

    @Transactional
    public void updateAppUserRole(AppUser appUser) {
        String email = appUser.getEmail();

        appUserRepository
                .findByEmail(email)
//                .filter(user -> Objects.equals(appUser.getRoles(), user.getRoles()))
                .map(appUserRepository::save)
                .orElseThrow(() -> new EmailNotFoundException(email));
    }

    public String signUpUser(AppUser appUser){
        String encodedPassword = passwordEncoder.encode(appUser.getPassword());
        String token = UUID.randomUUID().toString();

        saveAppUser(appUser);
        ConfirmationToken confirmationToken = new ConfirmationToken(token,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(TOKEN_EXPIRATION_DELAY),
                appUser
        );
        confirmationTokenService.saveConfirmationToken(confirmationToken);
        return token;
    }
    public void enableAppUser(String email) {
       appUserRepository
               .findByEmail(email)
               .orElseThrow(() -> new EmailNotFoundException(email));
        appUserRepository.enableAppUser(email);
    }
    @Override
    public UserDetails loadUserByUsername(String email) {
        return appUserRepository.findByEmail(email)
                .map(AppUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException(String.format(USER_NOT_FOUND_MSG, email)));
    }

}
