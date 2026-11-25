package br.com.agroclima.service;

import br.com.agroclima.repository.UserRepository;
import br.com.agroclima.security.AgroUserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public AccountUserDetailsService(UserRepository users) { this.users = users; }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        return users.findWithRolesByEmailIgnoreCase(email.trim())
            .map(AgroUserPrincipal::from)
            .orElseThrow(() -> new UsernameNotFoundException("Conta não encontrada."));
    }
}
