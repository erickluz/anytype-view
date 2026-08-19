---
trigger: always_on
description: 
globs: 
---

# Regras para Use Cases

## Estrutura Obrigatória

### Anotações e Estrutura Base
```java
@Service
@RequiredArgsConstructor
public class ActionNameUC {
    
    private final RequiredGateway requiredGateway;
    private final AnotherGateway anotherGateway;
    private final ApplicationEventPublisher applicationEventPublisher;
    
    @Transactional
    public ReturnType execute(InputDTO input) {
        // Lógica de negócio
        // Publicação de eventos se necessário
        return result;
    }
}
```

## Regras Obrigatórias

### Método Execute
- **DEVE** ter apenas UM método público chamado `execute`
- **DEVE** receber parâmetros de entrada (DTO ou primitivos)
- **PODE** retornar resultado ou ser void
- **DEVE** conter toda a lógica do caso de uso

### Anotações
- **DEVE** usar `@Service` para registro no Spring
- **DEVE** usar `@RequiredArgsConstructor` para injeção de dependência
- **DEVE** usar `@Transactional` quando modificar dados
- **NÃO DEVE** usar `@Transactional(readOnly = true)` - deixar sem anotação para leitura

### Dependências
- **DEVE** depender apenas de interfaces de Gateway
- **NÃO PODE** depender de implementações concretas
- **NÃO PODE** chamar outros Use Cases diretamente
- **DEVE** usar `ApplicationEventPublisher` para comunicação com outros Use Cases

## Exemplos Práticos

### Use Case de Criação
```java
@Service
@RequiredArgsConstructor
public class RegisterUC {
    
    private final UserRepositoryGTW userRepositoryGTW;
    private final UserCryptPasswordGTW userCryptPasswordGTW;
    private final UserFactory userFactory;
    private final ApplicationEventPublisher applicationEventPublisher;
    
    @Transactional
    public void execute(UserRegisterDTO userRegisterDTO) {
        // 1. Criar entidade usando factory
        UserEntity user = userFactory.create(
            userRegisterDTO.getName(),
            userRegisterDTO.getEmail(),
            userRegisterDTO.getCpf(),
            userRegisterDTO.getPassword(),
            userRegisterDTO.getBirthDate(),
            userRegisterDTO.getPhone(),
            userRegisterDTO.getSpecialNeeds(),
            userRegisterDTO.getProgramKnowledgeSource(),
            userRegisterDTO.getProgramKnowledgeSourceOther()
        );
        
        // 2. Aplicar regras de negócio
        UserDetailsEntity userDetails = user.getUserDetails();
        user.setPassword(userCryptPasswordGTW.cryptPassword(user.getPassword()));
        user.setAuthorized(!userDetails.isMinor());
        
        // 3. Validações específicas
        if (StringUtil.isNullOrEmpty(userRegisterDTO.getAuthorizedToken())) {
            UserValidatorService.validateProgramKnowledgeSource(
                userDetails.getProgramKnowledgeSource(), 
                userDetails.getProgramKnowledgeSourceOther()
            );
        }
        
        // 4. Persistir dados
        boolean generateResponsibleLink = userDetails.isMinor() && 
            StringUtil.isNullOrEmpty(userRegisterDTO.getAuthorizedToken());
        userRepositoryGTW.save(user);
        
        // 5. Publicar evento para processamento assíncrono
        applicationEventPublisher.publishEvent(new RegisterUserEvent(
            this, user, userRegisterDTO.getAuthorizedToken(), generateResponsibleLink
        ));
    }
}
```

### Use Case de Consulta
```java
@Service
@RequiredArgsConstructor
public class GetCurrentUserUC {
    
    private final UserRepositoryGTW userRepositoryGTW;
    private final SecurityContextGTW securityContextGTW;
    
    // ✅ Sem @Transactional para operações de leitura
    public UserDTO execute() {
        // 1. Obter usuário autenticado
        String cpf = securityContextGTW.getCurrentUserCpf();
        
        // 2. Buscar usuário no repositório
        UserEntity user = userRepositoryGTW.findByCpf(cpf)
            .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado: " + cpf));
        
        // 3. Converter para DTO de resposta
        return UserDTO.fromEntity(user);
    }
}
```

### Use Case com Validações Complexas
```java
@Service
@RequiredArgsConstructor
public class LinkDependentUC {
    
    private final UserRepositoryGTW userRepositoryGTW;
    private final UserDependentRepositoryGTW userDependentRepositoryGTW;
    private final SecurityContextGTW securityContextGTW;
    private final UserDependentValidatorService userDependentValidatorService;
    
    @Transactional
    public void execute(LinkDependentDTO linkDependentDTO) {
        // 1. Obter usuário autenticado (responsável)
        String guardianCpf = securityContextGTW.getCurrentUserCpf();
        UserEntity guardian = userRepositoryGTW.findByCpf(guardianCpf)
            .orElseThrow(() -> new UserNotFoundException("Responsável não encontrado"));
        
        // 2. Buscar dependente
        UserEntity dependent = userRepositoryGTW.findByCpf(linkDependentDTO.getDependentCpf())
            .orElseThrow(() -> new UserNotFoundException("Dependente não encontrado"));
        
        // 3. Validações de negócio usando Domain Service
        userDependentValidatorService.validateLinkage(guardian, dependent);
        
        // 4. Verificar se já existe vinculação
        boolean alreadyLinked = userDependentRepositoryGTW
            .existsByGuardianAndDependent(guardian, dependent);
        if (alreadyLinked) {
            throw new DependentAlreadyLinkedException("Dependente já vinculado");
        }
        
        // 5. Criar vinculação
        UserDependentEntity userDependent = new UserDependentEntity(
            guardian, dependent, EnumDependentStatus.ACTIVE
        );
        userDependentRepositoryGTW.save(userDependent);
        
        // 6. Autorizar dependente
        dependent.setAuthorized(true);
        userRepositoryGTW.save(dependent);
    }
}
```

## Comunicação Entre Use Cases

### ❌ ERRADO - Chamada Direta
```java
@Service
@RequiredArgsConstructor
public class RegisterUC {
    
    private final LinkDependentUC linkDependentUC; // ❌ NÃO FAZER
    
    @Transactional
    public void execute(UserRegisterDTO dto) {
        // ... lógica de registro
        
        // ❌ CHAMADA DIRETA PROIBIDA
        linkDependentUC.execute(linkDto);
    }
}
```

### ✅ CORRETO - Comunicação via Eventos
```java
@Service
@RequiredArgsConstructor
public class RegisterUC {
    
    private final ApplicationEventPublisher applicationEventPublisher; // ✅ CORRETO
    
    @Transactional
    public void execute(UserRegisterDTO dto) {
        // ... lógica de registro
        
        // ✅ PUBLICAR EVENTO
        applicationEventPublisher.publishEvent(new UserRegisteredEvent(
            this, user, dto.getAuthorizedToken()
        ));
    }
}

// Handler do evento em outro componente
@Component
@RequiredArgsConstructor
public class UserRegisteredEventHandler {
    
    private final LinkDependentUC linkDependentUC;
    
    @Async
    @EventListener
    public void handle(UserRegisteredEvent event) {
        if (event.hasAuthorizedToken()) {
            LinkDependentDTO linkDto = new LinkDependentDTO(event.getAuthorizedToken());
            linkDependentUC.execute(linkDto);
        }
    }
}
```

## Tratamento de Erros

### Exceções de Negócio
```java
@Service
@RequiredArgsConstructor
public class AuthenticateUC {
    
    private final UserRepositoryGTW userRepositoryGTW;
    private final UserCryptPasswordGTW userCryptPasswordGTW;
    private final TokenGTW tokenGTW;
    
    public AuthenticateResponseDTO execute(AuthenticateDTO authenticateDTO) {
        // 1. Buscar usuário
        UserEntity user = userRepositoryGTW.findByCpf(authenticateDTO.getCpf())
            .orElseThrow(() -> new InvalidCredentialsException("Credenciais inválidas"));
        
        // 2. Validar senha
        if (!userCryptPasswordGTW.matches(authenticateDTO.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Credenciais inválidas");
        }
        
        // 3. Validar status do usuário
        if (!user.getActive()) {
            throw new UserInactiveException("Usuário inativo");
        }
        
        if (!user.getAuthorized()) {
            throw new UserNotAuthorizedException("Usuário não autorizado pelo responsável");
        }
        
        // 4. Gerar token
        String token = tokenGTW.generateToken(user.getCpf());
        
        return new AuthenticateResponseDTO(token, user.getName());
    }
}
```

### Validação de Entrada
```java
@Service
@RequiredArgsConstructor
public class ChangePasswordUC {
    
    private final UserRepositoryGTW userRepositoryGTW;
    private final UserCryptPasswordGTW userCryptPasswordGTW;
    private final SecurityContextGTW securityContextGTW;
    
    @Transactional
    public void execute(ChangePasswordDTO changePasswordDTO) {
        // 1. Validar entrada
        if (changePasswordDTO == null) {
            throw new IllegalArgumentException("Dados de alteração de senha são obrigatórios");
        }
        
        if (StringUtil.isNullOrEmpty(changePasswordDTO.getCurrentPassword())) {
            throw new IllegalArgumentException("Senha atual é obrigatória");
        }
        
        if (StringUtil.isNullOrEmpty(changePasswordDTO.getNewPassword())) {
            throw new IllegalArgumentException("Nova senha é obrigatória");
        }
        
        if (changePasswordDTO.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("Nova senha deve ter pelo menos 6 caracteres");
        }
        
        // 2. Obter usuário atual
        String cpf = securityContextGTW.getCurrentUserCpf();
        UserEntity user = userRepositoryGTW.findByCpf(cpf)
            .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
        
        // 3. Validar senha atual
        if (!userCryptPasswordGTW.matches(changePasswordDTO.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCurrentPasswordException("Senha atual incorreta");
        }
        
        // 4. Alterar senha
        String newPasswordEncrypted = userCryptPasswordGTW.cryptPassword(changePasswordDTO.getNewPassword());
        user.setPassword(newPasswordEncrypted);
        userRepositoryGTW.save(user);
    }
}
```

## Padrões de Nomenclatura

### Nomes de Use Cases
- **DEVE** terminar com "UC"
- **DEVE** usar verbo no infinitivo + substantivo
- **EXEMPLOS**: `RegisterUC`, `AuthenticateUC`, `GetCurrentUserUC`, `ChangePasswordUC`

### Nomes de Métodos
- **SEMPRE** usar `execute` como nome do método público
- **NÃO** usar outros nomes como `run`, `process`, `handle`

### Pacotes
- **DEVE** organizar por contexto de negócio
- **ESTRUTURA**: `/core/domain/usecase/[contexto]/`
- **EXEMPLOS**: 
  - `/core/domain/usecase/user/RegisterUC.java`
  - `/core/domain/usecase/user/AuthenticateUC.java`
  - `/core/domain/usecase/dependent/LinkDependentUC.java`

## Transações

### Quando Usar @Transactional
```java
// ✅ Operações que modificam dados
@Transactional
public void execute(CreateDTO dto) {
    // Salvar, atualizar, deletar
}

// ✅ Operações de leitura SEM anotação
public DataDTO execute(GetDTO dto) {
    // Apenas consultas
}

// ✅ Operações mistas (leitura + escrita)
@Transactional
public ResultDTO execute(ProcessDTO dto) {
    // Consultar + modificar dados
}
```

### Propagação de Transação
```java
// ✅ Padrão (REQUIRED)
@Transactional
public void execute(DTO dto) {
    // Usa transação existente ou cria nova
}

// ✅ Para operações independentes
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void execute(DTO dto) {
    // Sempre cria nova transação
}
```