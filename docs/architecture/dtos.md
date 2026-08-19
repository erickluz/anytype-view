---
trigger: always_on
description: 
globs: 
---

# Regras para DTOs

## Estrutura Obrigatória

### Herança de BaseDTO
Todos os DTOs **DEVEM** estender `BaseDTO`:

```java
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisterDTO extends BaseDTO {
    
    private String name;
    
    private String email;
    
    private String cpf;
    
    private String password;
    
    private Date birthDate;
    
    private String phone;
    
    private Boolean specialNeeds;
    
    private EnumProgramKnowledgeSource programKnowledgeSource;
    
    private String programKnowledgeSourceOther;
    
    private String authorizedToken;
}
```

## Regras Obrigatórias

### Anotações Lombok
- **DEVE** usar `@Data` para getters, setters, toString, equals e hashCode
- **DEVE** usar `@EqualsAndHashCode(callSuper = false)` 
- **DEVE** usar `@AllArgsConstructor` e `@NoArgsConstructor`
- **NÃO DEVE** usar `@Builder` (usar construtores)

### Herança
- **DEVE** estender `BaseDTO`
- **NÃO DEVE** estender outros DTOs

## Tipos de DTOs

### DTOs de Entrada (Request)
```java
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticateDTO extends BaseDTO {
    
    private String cpf;
    
    private String password;
}

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class ChangePasswordDTO extends BaseDTO {
    
    private String currentPassword;
    
    private String newPassword;
    
    private String confirmNewPassword;
    
    public boolean isPasswordsMatch() {
        return Objects.equals(newPassword, confirmNewPassword);
    }
}
```

### DTOs de Saída (Response)
```java
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO extends BaseDTO {
    
    private UUID id;
    private String name;
    private String email;
    private String cpf;
    private Boolean active;
    private Boolean authorized;
    private Date createdAt;
    private UserDetailsDTO userDetails;
    
    // Método factory para conversão de entidade
    public static UserDTO fromEntity(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new UserDTO(
            entity.getId(),
            entity.getName(),
            entity.getEmail(),
            entity.getCpf(),
            entity.getActive(),
            entity.getAuthorized(),
            entity.getCreatedAt(),
            UserDetailsDTO.fromEntity(entity.getUserDetails())
        );
    }
}

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticateResponseDTO extends BaseDTO {
    
    private String token;
    private String userName;
    private Date expiresAt;
}
```

### DTOs de Consulta (Query)
```java
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class GetUsersDTO extends BaseDTO {
    
    @Min(value = 0, message = "Página deve ser maior ou igual a 0")
    private int page = 0;
    
    @Min(value = 1, message = "Tamanho da página deve ser maior que 0")
    @Max(value = 100, message = "Tamanho da página deve ser menor ou igual a 100")
    private int size = 20;
    
    private String name;
    private Boolean active;
    private String sortBy = "name";
    private String sortDirection = "ASC";
    
    public Pageable toPageable() {
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection) 
            ? Sort.Direction.DESC 
            : Sort.Direction.ASC;
        
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}
```

### DTOs Compostos
```java
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class DependentsLinkedListDTO extends BaseDTO {
    
    private List<UserDTO> pending = new ArrayList<>();
    private List<UserDTO> active = new ArrayList<>();
    
    public static DependentsLinkedListDTO fromEntities(List<UserEntity> pendingEntities, 
                                                      List<UserEntity> activeEntities) {
        List<UserDTO> pendingDTOs = pendingEntities.stream()
            .map(UserDTO::fromEntity)
            .collect(Collectors.toList());
            
        List<UserDTO> activeDTOs = activeEntities.stream()
            .map(UserDTO::fromEntity)
            .collect(Collectors.toList());
            
        return new DependentsLinkedListDTO(pendingDTOs, activeDTOs);
    }
}
```

## Records para Dados Simples

### Records de Resposta
```java
// Para dados simples e imutáveis
public record AuthorizedLinkGeneratedRecord(
    String token,
    Date expiresAt,
    String message
) {}

public record ErrorResponse(
    String message,
    List<String> errors,
    String timestamp
) {
    public ErrorResponse(String message, List<String> errors) {
        this(message, errors, Instant.now().toString());
    }
}

public record UserSummaryRecord(
    UUID id,
    String name,
    String cpf,
    Boolean active
) {
    public static UserSummaryRecord fromEntity(UserEntity entity) {
        return new UserSummaryRecord(
            entity.getId(),
            entity.getName(),
            entity.getCpf(),
            entity.getActive()
        );
    }
}
```

## Validações Customizadas

### Validações Condicionais
```java
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisterDTO extends BaseDTO {
    
    // ... outros campos
    
    @NotNull(message = "Origem do conhecimento do programa é obrigatória")
    private EnumProgramKnowledgeSource programKnowledgeSource;
    
    private String programKnowledgeSourceOther;
    
    // Validação customizada
    @AssertTrue(message = "Descrição da origem 'Outro' é obrigatória quando selecionado 'Outro'")
    public boolean isProgramKnowledgeSourceOtherValid() {
        if (programKnowledgeSource == EnumProgramKnowledgeSource.OTHER) {
            return programKnowledgeSourceOther != null && !programKnowledgeSourceOther.trim().isEmpty();
        }
        return true;
    }
}
```

### Validador Customizado
```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordMatchValidator.class)
public @interface PasswordMatch {
    String message() default "Senhas não coincidem";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, ChangePasswordDTO> {
    
    @Override
    public boolean isValid(ChangePasswordDTO dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }
        
        return Objects.equals(dto.getNewPassword(), dto.getConfirmNewPassword());
    }
}

// Uso no DTO
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@PasswordMatch
public class ChangePasswordDTO extends BaseDTO {
    // ... campos
}
```

## Conversão Entre DTOs e Entidades

### Métodos Factory
```java
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailsDTO extends BaseDTO {
    
    private Date birthDate;
    private String phone;
    private Boolean specialNeeds;
    private EnumProgramKnowledgeSource programKnowledgeSource;
    private String programKnowledgeSourceOther;
    private Integer age;
    private Boolean isMinor;
    
    public static UserDetailsDTO fromEntity(UserDetailsEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new UserDetailsDTO(
            entity.getBirthDate(),
            entity.getPhone(),
            entity.getSpecialNeeds(),
            entity.getProgramKnowledgeSource(),
            entity.getProgramKnowledgeSourceOther(),
            entity.getAge(),
            entity.isMinor()
        );
    }
    
    public UserDetailsEntity toEntity() {
        return new UserDetailsEntity(
            this.birthDate,
            this.phone,
            this.specialNeeds,
            this.programKnowledgeSource,
            this.programKnowledgeSourceOther
        );
    }
}
```

### Conversão com Listas
```java
public class UserDTO extends BaseDTO {
    
    // ... campos
    
    public static List<UserDTO> fromEntities(List<UserEntity> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        
        return entities.stream()
            .map(UserDTO::fromEntity)
            .collect(Collectors.toList());
    }
    
    public static Page<UserDTO> fromEntities(Page<UserEntity> entityPage) {
        return entityPage.map(UserDTO::fromEntity);
    }
}
```

## DTOs para Diferentes Contextos

### DTOs do Core
```java
// /core/dto/UserRegisterDTO.java
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisterDTO extends BaseDTO {
    // Usado pelos Use Cases
    // Contém validações de negócio
    // Não expõe detalhes de implementação
}
```

### DTOs da Infraestrutura
```java
// /infra/dto/UserDTO.java
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO extends BaseDTO {
    // Usado pelos Controllers
    // Pode ter anotações específicas de framework
    // Pode ter formatações específicas
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "America/Sao_Paulo")
    private Date birthDate;
    
    @JsonProperty("user_name")
    private String name;
}
```

## Paginação

### DTO de Paginação
```java
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class PageRequestDTO extends BaseDTO {
    
    @Min(value = 0, message = "Página deve ser maior ou igual a 0")
    private int page = 0;
    
    @Min(value = 1, message = "Tamanho deve ser maior que 0")
    @Max(value = 100, message = "Tamanho deve ser menor ou igual a 100")
    private int size = 20;
    
    private String sortBy = "id";
    private String sortDirection = "ASC";
    
    public Pageable toPageable() {
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection) 
            ? Sort.Direction.DESC 
            : Sort.Direction.ASC;
            
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}