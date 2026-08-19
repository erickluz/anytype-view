---
trigger: always_on
description: 
globs: 
---

# Regras para Eventos

## Conceito e Responsabilidade

### O que são Eventos
- **Comunicação assíncrona ou sincrona** entre Use Cases
- **Desacoplamento** de componentes
- **Processamento** de ações secundárias
- **Auditoria** e rastreamento de ações

### Padrão Publisher/Subscriber
- **Publisher**: Use Case que dispara o evento
- **Subscriber**: Listener que processa o evento
- **Event**: Objeto que carrega os dados do evento

## Estrutura Obrigatória

### Classe de Evento
```java
@Getter
public class RegisterUserEvent extends ApplicationEvent {
    
    private final UserEntity user;
    private final String authorizedToken;
    private final boolean generateResponsibleLink;
    
    public RegisterUserEvent(Object source, UserEntity user, String authorizedToken, boolean generateResponsibleLink) {
        super(source);
        this.user = user;
        this.authorizedToken = authorizedToken;
        this.generateResponsibleLink = generateResponsibleLink;
    }
    
    public boolean hasAuthorizedToken() {
        return authorizedToken != null && !authorizedToken.trim().isEmpty();
    }
}
```

### Listener de Evento
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class RegisterUserEventListener {
    
    private final UserDependentLinkUC userDependentLinkUC;
    private final GenerateResponsibleLinkUC generateResponsibleLinkUC;
    
    @EventListener
    @Transactional
    public void handleUserRegistration(RegisterUserEvent event) {
        log.info("Processando registro de usuário: {}", event.getUser().getCpf());
        
        try {
            // Processar vinculação de dependente se tiver token
            if (event.hasAuthorizedToken()) {
                userDependentLinkUC.linkDependentOnRegistration(
                    event.getUser(), 
                    event.getAuthorizedToken()
                );
            }
            
            // Gerar link de responsável se necessário
            if (event.isGenerateResponsibleLink()) {
                generateResponsibleLinkUC.execute(event.getUser());
            }
            
            log.info("Registro de usuário processado com sucesso: {}", event.getUser().getCpf());
            
        } catch (Exception e) {
            log.error("Erro ao processar registro de usuário: {}", event.getUser().getCpf(), e);
            // Não relançar exceção para não afetar o fluxo principal
        }
    }
}
```

## Regras Obrigatórias

### Eventos
- **DEVE** estender `ApplicationEvent`
- **DEVE** usar `@Getter` para campos
- **DEVE** ter construtor que chama `super(source)`
- **DEVE** ser imutável (campos `final`)
- **DEVE** ter nome terminado em "Event"

### Handlers
- **DEVE** ser anotado com `@Component`
- **DEVE** usar `@RequiredArgsConstructor` para injeção
- **DEVE** usar `@EventListener` nos métodos
- **PODE** usar `@Async` para processamento assíncrono
- **DEVE** usar `@Transactional` se modificar dados
- **DEVE** ter logging apropriado

## Exemplos Práticos

### Evento de Autenticação
```java
@Getter
public class UserAuthenticatedEvent extends ApplicationEvent {
    
    private final UserEntity user;
    private final String ipAddress;
    private final String userAgent;
    private final Date authenticatedAt;
    
    public UserAuthenticatedEvent(Object source, UserEntity user, String ipAddress, String userAgent) {
        super(source);
        this.user = user;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.authenticatedAt = new Date();
    }
}

@Component
@RequiredArgsConstructor
@Slf4j
public class UserAuthenticatedEventHandler {
    
    private final AuditLogGTW auditLogGTW;
    private final SecurityAlertGTW securityAlertGTW;
    
    @Async
    @EventListener
    public void handleUserAuthentication(UserAuthenticatedEvent event) {
        log.info("Usuário autenticado: {} de IP: {}", event.getUser().getCpf(), event.getIpAddress());
        
        // Registrar log de auditoria
        auditLogGTW.logAuthentication(
            event.getUser().getId(),
            event.getIpAddress(),
            event.getUserAgent(),
            event.getAuthenticatedAt()
        );
        
        // Verificar alertas de segurança
        securityAlertGTW.checkSuspiciousActivity(
            event.getUser().getId(),
            event.getIpAddress()
        );
    }
}
```

### Evento de Mudança de Senha
```java
@Getter
public class PasswordChangedEvent extends ApplicationEvent {
    
    private final UserEntity user;
    private final String ipAddress;
    private final Date changedAt;
    
    public PasswordChangedEvent(Object source, UserEntity user, String ipAddress) {
        super(source);
        this.user = user;
        this.ipAddress = ipAddress;
        this.changedAt = new Date();
    }
}

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordChangedEventHandler {
    
    private final EmailGTW emailGTW;
    private final AuditLogGTW auditLogGTW;
    
    @Async
    @EventListener
    public void handlePasswordChange(PasswordChangedEvent event) {
        log.info("Senha alterada para usuário: {}", event.getUser().getCpf());
        
        try {
            // Enviar email de notificação
            if (event.getUser().getEmail() != null) {
                emailGTW.sendPasswordChangeNotification(
                    event.getUser().getEmail(),
                    event.getUser().getName(),
                    event.getChangedAt()
                );
            }
            
            // Registrar log de auditoria
            auditLogGTW.logPasswordChange(
                event.getUser().getId(),
                event.getIpAddress(),
                event.getChangedAt()
            );
            
        } catch (Exception e) {
            log.error("Erro ao processar mudança de senha para usuário: {}", 
                event.getUser().getCpf(), e);
        }
    }
}
```

### Evento de Vinculação de Dependente
```java
@Getter
public class DependentLinkedEvent extends ApplicationEvent {
    
    private final UserEntity guardian;
    private final UserEntity dependent;
    private final EnumDependentStatus status;
    private final Date linkedAt;
    
    public DependentLinkedEvent(Object source, UserEntity guardian, UserEntity dependent, EnumDependentStatus status) {
        super(source);
        this.guardian = guardian;
        this.dependent = dependent;
        this.status = status;
        this.linkedAt = new Date();
    }
}

@Component
@RequiredArgsConstructor
@Slf4j
public class DependentLinkedEventListener {
    
    private final EmailGTW emailGTW;
    private final NotificationGTW notificationGTW;
    
    @Async
    @EventListener
    public void handleDependentLinked(DependentLinkedEvent event) {
        log.info("Dependente vinculado: {} -> {}", 
            event.getGuardian().getCpf(), event.getDependent().getCpf());
        
        try {
            // Enviar email para o responsável
            if (event.getGuardian().getEmail() != null) {
                emailGTW.sendDependentLinkedNotification(
                    event.getGuardian().getEmail(),
                    event.getGuardian().getName(),
                    event.getDependent().getName(),
                    event.getStatus()
                );
            }
            
            // Enviar email para o dependente
            if (event.getDependent().getEmail() != null) {
                emailGTW.sendGuardianLinkedNotification(
                    event.getDependent().getEmail(),
                    event.getDependent().getName(),
                    event.getGuardian().getName(),
                    event.getStatus()
                );
            }
            
            // Criar notificação no sistema
            notificationGTW.createDependentLinkedNotification(
                event.getGuardian().getId(),
                event.getDependent().getId(),
                event.getStatus()
            );
            
        } catch (Exception e) {
            log.error("Erro ao processar vinculação de dependente: {} -> {}", 
                event.getGuardian().getCpf(), event.getDependent().getCpf(), e);
        }
    }
}
```

## Publicação de Eventos

### No Use Case
```java
@Service
@RequiredArgsConstructor
public class RegisterUC {
    
    private final UserRepositoryGTW userRepositoryGTW;
    private final UserFactory userFactory;
    private final ApplicationEventPublisher applicationEventPublisher;
    
    @Transactional
    public void execute(UserRegisterDTO userRegisterDTO) {
        // 1. Lógica principal do use case
        UserEntity user = userFactory.create(/* parâmetros */);
        userRepositoryGTW.save(user);
        
        // 2. Publicar evento APÓS persistir dados
        applicationEventPublisher.publishEvent(new RegisterUserEvent(
            this, // source
            user,
            userRegisterDTO.getAuthorizedToken(),
            user.getUserDetails().isMinor()
        ));
        
        // 3. Não aguardar processamento do evento
        log.info("Usuário registrado e evento publicado: {}", user.getCpf());
    }
}
```

### Múltiplos Eventos
```java
@Service
@RequiredArgsConstructor
public class LinkDependentUC {
    
    private final UserDependentRepositoryGTW userDependentRepositoryGTW;
    private final ApplicationEventPublisher applicationEventPublisher;
    
    @Transactional
    public void execute(LinkDependentDTO dto) {
        // Lógica principal
        UserDependentEntity userDependent = new UserDependentEntity(/* parâmetros */);
        userDependentRepositoryGTW.save(userDependent);
        
        // Publicar múltiplos eventos
        applicationEventPublisher.publishEvent(new DependentLinkedEvent(
            this, guardian, dependent, EnumDependentStatus.ACTIVE
        ));
        
        applicationEventPublisher.publishEvent(new UserStatusChangedEvent(
            this, dependent, EnumUserStatus.ACTIVE
        ));
    }
}
```

## Tratamento de Erros

### Handler com Tratamento de Erro
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailEventListener {
    
    private final EmailGTW emailGTW;
    
    @Async
    @EventListener
    @Retryable(value = {EmailSendException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void handleEmailEvent(EmailEvent event) {
        try {
            emailGTW.sendEmail(event.getTo(), event.getSubject(), event.getBody());
            log.info("Email enviado com sucesso para: {}", event.getTo());
            
        } catch (EmailSendException e) {
            log.warn("Tentativa de envio de email falhou para: {} - Tentativa: {}", 
                event.getTo(), getCurrentAttempt(), e);
            throw e; // Relançar para retry
            
        } catch (Exception e) {
            log.error("Erro não recuperável ao enviar email para: {}", event.getTo(), e);
            // Não relançar - erro não recuperável
        }
    }
    
    @Recover
    public void recoverEmailSend(EmailSendException ex, EmailEvent event) {
        log.error("Falha definitiva ao enviar email após todas as tentativas para: {}", 
            event.getTo(), ex);
        
        // Registrar falha para análise posterior
        // Pode criar um evento de falha de email
    }
}
```

### Event Listener Condicional
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ConditionalEventListener {
    
    @EventListener(condition = "#event.user.active == true")
    public void handleActiveUserEvent(UserEvent event) {
        // Processa apenas usuários ativos
    }
    
    @EventListener(condition = "#event.user.userDetails.minor == true")
    public void handleMinorUserEvent(UserEvent event) {
        // Processa apenas usuários menores de idade
    }
}
```