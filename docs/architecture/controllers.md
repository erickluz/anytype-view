---
trigger: always_on
description: 
globs: 
---

# Regras para Controllers

## Estrutura Obrigatória

### Interface e Implementação Separadas
- **DEVE** ter interface em `/inbound/http/interfaces/`
- **DEVE** ter implementação em `/inbound/http/`
- **Interface** contém todas as anotações e definições de endpoints
- **Implementação** apenas delega para Use Cases

## Interface do Controller

### Estrutura Base
```java
@RequestMapping(IUserController.PATH)
public interface IUserController {
    
    String PATH = PREFIX_PATH + "/user";
    
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    void register(@Valid @RequestBody UserRegisterDTO userRegisterDTO);
    
    @GetMapping("/current")
    @ResponseStatus(HttpStatus.OK)
    UserDTO getCurrentUser();
    
    @PostMapping("/regenerate-responsible-link")
    @ResponseStatus(HttpStatus.OK)
    AuthorizedLinkGeneratedRecord regenerateResponsibleLink();
}
```

### Regras para Interface
- **DEVE** usar `@RequestMapping` com constante PATH
- **DEVE** definir constante PATH usando `PREFIX_PATH`
- **DEVE** ter todas as anotações de mapeamento (`@PostMapping`, `@GetMapping`, etc.)
- **DEVE** usar `@ResponseStatus` para definir códigos HTTP
- **DEVE** usar `@Valid` para validação automática
- **DEVE** usar `@RequestBody` para dados de entrada
- **DEVE** usar `@PathVariable` para parâmetros de URL
- **DEVE** usar `@RequestParam` para query parameters

## Implementação do Controller

### Estrutura Base
```java
@RestController
@RequiredArgsConstructor
public class UserController implements IUserController {
    
    private final RegisterUC registerUC;
    private final GetCurrentUserUC getCurrentUserUC;
    private final GenerateResponsibleLinkUC generateResponsibleLinkUC;
    
    @Override
    public void register(UserRegisterDTO userRegisterDTO) {
        registerUC.execute(userRegisterDTO);
    }
    
    @Override
    public UserDTO getCurrentUser() {
        return getCurrentUserUC.execute();
    }
    
    @Override
    public AuthorizedLinkGeneratedRecord regenerateResponsibleLink() {
        return generateResponsibleLinkUC.execute();
    }
}
```

### Regras para Implementação
- **DEVE** usar apenas `@RestController` e `@RequiredArgsConstructor`
- **DEVE** implementar interface correspondente
- **DEVE** injetar apenas Use Cases
- **DEVE** apenas delegar chamadas para Use Cases
- **NÃO DEVE** conter lógica de negócio
- **NÃO DEVE** fazer validações além das automáticas

## Exemplos Práticos

### Controller CRUD Completo
```java
// Interface
@RequestMapping(IUserController.PATH)
public interface IUserController {
    
    String PATH = PREFIX_PATH + "/user";
    
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    void register(@Valid @RequestBody UserRegisterDTO userRegisterDTO);
    
    @GetMapping("/current")
    @ResponseStatus(HttpStatus.OK)
    UserDTO getCurrentUser();
    
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@Valid @RequestBody ChangePasswordDTO changePasswordDTO);
    
    @PostMapping("/regenerate-responsible-link")
    @ResponseStatus(HttpStatus.OK)
    AuthorizedLinkGeneratedRecord regenerateResponsibleLink();
    
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    UserDTO getUser(@PathVariable UUID id);
    
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    Page<UserDTO> getUsers(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String name);
}

// Implementação
@RestController
@RequiredArgsConstructor
public class UserController implements IUserController {
    
    private final RegisterUC registerUC;
    private final GetCurrentUserUC getCurrentUserUC;
    private final ChangePasswordUC changePasswordUC;
    private final GenerateResponsibleLinkUC generateResponsibleLinkUC;
    private final GetUserUC getUserUC;
    private final GetUsersUC getUsersUC;
    
    @Override
    public void register(UserRegisterDTO userRegisterDTO) {
        registerUC.execute(userRegisterDTO);
    }
    
    @Override
    public UserDTO getCurrentUser() {
        return getCurrentUserUC.execute();
    }
    
    @Override
    public void changePassword(ChangePasswordDTO changePasswordDTO) {
        changePasswordUC.execute(changePasswordDTO);
    }
    
    @Override
    public AuthorizedLinkGeneratedRecord regenerateResponsibleLink() {
        return generateResponsibleLinkUC.execute();
    }
    
    @Override
    public UserDTO getUser(UUID id) {
        return getUserUC.execute(new GetUserDTO(id));
    }
    
    @Override
    public Page<UserDTO> getUsers(int page, int size, String name) {
        GetUsersDTO dto = new GetUsersDTO(page, size, name);
        return getUsersUC.execute(dto);
    }
}
```

### Controller com Autenticação
```java
// Interface
@RequestMapping(IAuthenticationController.PATH)
public interface IAuthenticationController {
    
    String PATH = PREFIX_PATH + "/auth";
    
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    AuthenticateResponseDTO authenticate(@Valid @RequestBody AuthenticateDTO authenticateDTO);
    
    @PostMapping("/recovery")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void generateRecovery(@Valid @RequestBody GenerateRecoveryDTO generateRecoveryDTO);
    
    @PostMapping("/recovery/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void confirmRecovery(@Valid @RequestBody ConfirmRecoveryDTO confirmRecoveryDTO);
}

// Implementação
@RestController
@RequiredArgsConstructor
public class AuthenticationController implements IAuthenticationController {
    
    private final AuthenticateUC authenticateUC;
    private final GenerateRecoveryUC generateRecoveryUC;
    private final ConfirmRecoveryUC confirmRecoveryUC;
    
    @Override
    public AuthenticateResponseDTO authenticate(AuthenticateDTO authenticateDTO) {
        return authenticateUC.execute(authenticateDTO);
    }
    
    @Override
    public void generateRecovery(GenerateRecoveryDTO generateRecoveryDTO) {
        generateRecoveryUC.execute(generateRecoveryDTO);
    }
    
    @Override
    public void confirmRecovery(ConfirmRecoveryDTO confirmRecoveryDTO) {
        confirmRecoveryUC.execute(confirmRecoveryDTO);
    }
}
```

### Controller com Relacionamentos
```java
// Interface
@RequestMapping(IDependentController.PATH)
public interface IDependentController {
    
    String PATH = PREFIX_PATH + "/dependent";
    
    @PostMapping("/link")
    @ResponseStatus(HttpStatus.CREATED)
    void linkDependent(@Valid @RequestBody LinkDependentDTO linkDependentDTO);
    
    @PostMapping("/recognize")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void recognizeDependent(@Valid @RequestBody RecognizeDependentDTO recognizeDependentDTO);
    
    @GetMapping("/linked")
    @ResponseStatus(HttpStatus.OK)
    DependentsLinkedListDTO listDependentsLinked();
}

// Implementação
@RestController
@RequiredArgsConstructor
public class DependentController implements IDependentController {
    
    private final LinkDependentUC linkDependentUC;
    private final RecognizeDependentUC recognizeDependentUC;
    private final ListDependentsLinkedUC listDependentsLinkedUC;
    
    @Override
    public void linkDependent(LinkDependentDTO linkDependentDTO) {
        linkDependentUC.execute(linkDependentDTO);
    }
    
    @Override
    public void recognizeDependent(RecognizeDependentDTO recognizeDependentDTO) {
        recognizeDependentUC.execute(recognizeDependentDTO);
    }
    
    @Override
    public DependentsLinkedListDTO listDependentsLinked() {
        return listDependentsLinkedUC.execute();
    }
}
```

## Códigos de Status HTTP

### Mapeamento Padrão
```java
// Criação de recursos
@PostMapping("/register")
@ResponseStatus(HttpStatus.CREATED) // 201
void register(@Valid @RequestBody UserRegisterDTO dto);

// Consulta de recursos
@GetMapping("/current")
@ResponseStatus(HttpStatus.OK) // 200
UserDTO getCurrentUser();

// Atualização de recursos
@PutMapping("/password")
@ResponseStatus(HttpStatus.NO_CONTENT) // 204
void changePassword(@Valid @RequestBody ChangePasswordDTO dto);

// Operações sem retorno
@PostMapping("/action")
@ResponseStatus(HttpStatus.NO_CONTENT) // 204
void performAction(@Valid @RequestBody ActionDTO dto);

// Operações com retorno
@PostMapping("/generate")
@ResponseStatus(HttpStatus.OK) // 200
ResultDTO generateSomething(@Valid @RequestBody GenerateDTO dto);
```

### Códigos por Operação
- **POST (criação)**: `HttpStatus.CREATED` (201)
- **GET**: `HttpStatus.OK` (200)
- **PUT/PATCH**: `HttpStatus.NO_CONTENT` (204)
- **DELETE**: `HttpStatus.NO_CONTENT` (204)
- **POST (ação)**: `HttpStatus.OK` (200) se retorna dados, `HttpStatus.NO_CONTENT` (204) se não retorna

## Validação de Entrada

### Bean Validation
```java
// ✅ Validação automática com @Valid
@PostMapping("/register")
@ResponseStatus(HttpStatus.CREATED)
void register(@Valid @RequestBody UserRegisterDTO userRegisterDTO);

// ✅ Validação de path variables
@GetMapping("/{id}")
@ResponseStatus(HttpStatus.OK)
UserDTO getUser(@PathVariable @NotNull UUID id);

// ✅ Validação de query parameters
@GetMapping
@ResponseStatus(HttpStatus.OK)
Page<UserDTO> getUsers(@RequestParam(defaultValue = "0") @Min(0) int page,
                      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size);
```

### DTOs com Validação
```java
// DTO com validações
public class UserRegisterDTO extends BaseDTO {
    
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 150, message = "Nome não pode ter mais de 150 caracteres")
    private String name;
    
    @Email(message = "Email deve ter formato válido")
    private String email;
    
    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "CPF deve ter 11 dígitos")
    private String cpf;
    
    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter pelo menos 6 caracteres")
    private String password;
}
```

## Tratamento de Erros

### Exception Handler Global
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.toList());
        
        return new ErrorResponse("Dados inválidos", errors);
    }
    
    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleUserNotFoundException(UserNotFoundException ex) {
        return new ErrorResponse("Usuário não encontrado", List.of(ex.getMessage()));
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex) {
        return new ErrorResponse("Dados inválidos", List.of(ex.getMessage()));
    }
}
```