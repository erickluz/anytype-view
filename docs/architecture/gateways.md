---
trigger: always_on
description: 
globs: 
---

# Regras para Gateways

## Conceito e Responsabilidade

### O que são Gateways
- **Abstraem** dependências externas (banco de dados, APIs, serviços)
- **Isolam** o domínio core de frameworks e bibliotecas
- **Permitem** fácil troca de implementações
- **Garantem** testabilidade através de mocks

### Padrão Interface/Implementação
- **Interface** em `/core/gateway/`
- **Implementação** em `/infra/(varia o local)`
- **Core** depende apenas da interface
- **Infra** implementa a interface

## Estrutura Obrigatória

### Interface do Gateway
```java
// /core/gateway/UserRepositoryGTW.java
public interface UserRepositoryGTW {
    
    UserEntity save(UserEntity user);
    
    Optional<UserEntity> findById(UUID id);
    
    Optional<UserEntity> findByCpf(String cpf);
    
    boolean existsByCpf(String cpf);
    
    boolean existsByCpfAndIdIsNot(String cpf, UUID id);
    
    void delete(UserEntity user);
    
    Page<UserEntity> findAll(Pageable pageable);
    
    List<UserEntity> findByNameContainingIgnoreCase(String name);
}
```

### Implementação do Gateway
```java
// /infra/gateway/UserRepositoryGTWImpl.java
@Component
@RequiredArgsConstructor
public class UserRepositoryGTWImpl implements UserRepositoryGTW {
    
    private final UserRepository userRepository;
    
    @Override
    public UserEntity save(UserEntity user) {
        return userRepository.save(user);
    }
    
    @Override
    public Optional<UserEntity> findById(UUID id) {
        return userRepository.findById(id);
    }
    
    @Override
    public Optional<UserEntity> findByCpf(String cpf) {
        return userRepository.findByCpf(cpf);
    }
    
    @Override
    public boolean existsByCpf(String cpf) {
        return userRepository.existsByCpf(cpf);
    }
    
    @Override
    public boolean existsByCpfAndIdIsNot(String cpf, UUID id) {
        return userRepository.existsByCpfAndIdIsNot(cpf, id);
    }
    
    @Override
    public void delete(UserEntity user) {
        userRepository.delete(user);
    }
    
    @Override
    public Page<UserEntity> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    @Override
    public List<UserEntity> findByNameContainingIgnoreCase(String name) {
        return userRepository.findByNameContainingIgnoreCase(name);
    }
}
```

## Tipos de Gateways

### Repository Gateway
```java
// Interface
public interface UserRepositoryGTW {
    
    // CRUD básico
    UserEntity save(UserEntity user);
    Optional<UserEntity> findById(UUID id);
    void delete(UserEntity user);
    
    // Consultas específicas
    Optional<UserEntity> findByCpf(String cpf);
    List<UserEntity> findByActive(boolean active);
    
    // Verificações de existência
    boolean existsByCpf(String cpf);
    boolean existsByEmail(String email);
    
    // Consultas com paginação
    Page<UserEntity> findAll(Pageable pageable);
    Page<UserEntity> findByNameContaining(String name, Pageable pageable);
    
    // Consultas agregadas
    long countByActive(boolean active);
    List<UserEntity> findTop10ByOrderByCreatedAtDesc();
}

// Implementação
@Component
@RequiredArgsConstructor
public class UserRepositoryGTWImpl implements UserRepositoryGTW {
    
    private final UserRepository userRepository;
    
    @Override
    public UserEntity save(UserEntity user) {
        return userRepository.save(user);
    }
    
    @Override
    public Optional<UserEntity> findById(UUID id) {
        return userRepository.findById(id);
    }
    
    @Override
    public Optional<UserEntity> findByCpf(String cpf) {
        return userRepository.findByCpf(cpf);
    }
    
    @Override
    public boolean existsByCpf(String cpf) {
        return userRepository.existsByCpf(cpf);
    }
    
    @Override
    public Page<UserEntity> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    // Implementar todos os outros métodos...
}
```

### External Service Gateway
```java
// Interface
public interface EmailGTW {
    
    void sendEmail(String to, String subject, String body);
    
    void sendTemplateEmail(String to, EmailTemplate template, Map<String, Object> variables);
    
    void sendRecoveryEmail(String to, String token);
    
    void sendAuthorizationEmail(String to, String guardianName, String dependentName, String token);
}

// Implementação
@Component
@RequiredArgsConstructor
public class EmailGTWImpl implements EmailGTW {
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new EmailSendException("Erro ao enviar email para: " + to, e);
        }
    }
    
    @Override
    public void sendTemplateEmail(String to, EmailTemplate template, Map<String, Object> variables) {
        try {
            String body = templateEngine.process(template.getTemplateName(), 
                new Context(Locale.getDefault(), variables));
            sendEmail(to, template.getSubject(), body);
        } catch (Exception e) {
            throw new EmailSendException("Erro ao enviar email template para: " + to, e);
        }
    }
    
    @Override
    public void sendRecoveryEmail(String to, String token) {
        Map<String, Object> variables = Map.of("token", token);
        sendTemplateEmail(to, EmailTemplate.PASSWORD_RECOVERY, variables);
    }
    
    @Override
    public void sendAuthorizationEmail(String to, String guardianName, String dependentName, String token) {
        Map<String, Object> variables = Map.of(
            "guardianName", guardianName,
            "dependentName", dependentName,
            "token", token
        );
        sendTemplateEmail(to, EmailTemplate.AUTHORIZATION_REQUEST, variables);
    }
}
```

### Cryptography Gateway
```java
// Interface
public interface UserCryptPasswordGTW {
    
    String cryptPassword(String plainPassword);
    
    boolean matches(String plainPassword, String encryptedPassword);
}

// Implementação
@Component
@RequiredArgsConstructor
public class UserCryptPasswordGTWImpl implements UserCryptPasswordGTW {
    
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public String cryptPassword(String plainPassword) {
        if (plainPassword == null) {
            throw new IllegalArgumentException("Senha não pode ser nula");
        }
        return passwordEncoder.encode(plainPassword);
    }
    
    @Override
    public boolean matches(String plainPassword, String encryptedPassword) {
        if (plainPassword == null || encryptedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(plainPassword, encryptedPassword);
    }
}
```

### Token Gateway
```java
// Interface
public interface TokenGTW {
    
    String generateToken(String cpf);
    
    String generateRecoveryToken(String cpf);
    
    String generateAuthorizedLinkToken(String guardianCpf, String dependentCpf);
    
    boolean validateToken(String token);
    
    String extractCpfFromToken(String token);
    
    Date getExpirationFromToken(String token);
}

// Implementação
@Component
@RequiredArgsConstructor
public class TokenGTWImpl implements TokenGTW {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    @Value("${jwt.recovery.expiration}")
    private Long recoveryExpiration;
    
    @Override
    public String generateToken(String cpf) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setSubject(cpf)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }
    
    @Override
    public String generateRecoveryToken(String cpf) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + recoveryExpiration);
        
        return Jwts.builder()
                .setSubject(cpf)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("type", "recovery")
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }
    
    @Override
    public String generateAuthorizedLinkToken(String guardianCpf, String dependentCpf) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + recoveryExpiration);
        
        return Jwts.builder()
                .setSubject(guardianCpf)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("type", "authorization")
                .claim("dependentCpf", dependentCpf)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }
    
    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    @Override
    public String extractCpfFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
    
    @Override
    public Date getExpirationFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration();
    }
}
```