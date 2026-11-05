package com.shoplite.user;

import com.shoplite.common.exception.BusinessException;
import com.shoplite.common.exception.EmailAlreadyUsedException;
import com.shoplite.common.exception.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User register(RegistrationForm form) {
        String email = form.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyUsedException();
        }
        User user = new User(email, passwordEncoder.encode(form.getPassword()),
                form.getFullName().trim(), Role.CUSTOMER);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Override
    @Transactional
    public void updateProfile(String email, ProfileForm form) {
        User user = getByEmail(email);
        user.setFullName(form.getFullName().trim());
        String phone = form.getPhone() == null ? "" : form.getPhone().trim();
        user.setPhone(phone.isEmpty() ? null : phone);
    }

    @Override
    @Transactional
    public void changePassword(String email, PasswordChangeForm form) {
        User user = getByEmail(email);
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
    }
}
