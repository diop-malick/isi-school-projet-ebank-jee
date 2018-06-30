package com.examen.GestionBanque.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.examen.GestionBanque.configuration.security.RolesConstants;
import com.examen.GestionBanque.dao.RoleRepository;
import com.examen.GestionBanque.dao.UserRepository;
import com.examen.GestionBanque.entities.Role;
import com.examen.GestionBanque.entities.User;

@Service
public class UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
    private PasswordEncoder passwordEncoder;

	public User findUserByEmail(String email) {
		return userRepository.findByEmail(email);
	}

	public void saveUser(User user) {
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		user.setActivated(true);
		
		Set<Role> roles = new HashSet<>();
		roleRepository.findById(RolesConstants.ADMIN).ifPresent(roles::add);
		user.setRoles(roles);
		
		userRepository.save(user);
	}

}
