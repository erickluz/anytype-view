---
trigger: always_on
description: 
globs: 
---

# Regras para Entidades

## Estrutura Obrigatória

### Anotações Obrigatórias
Toda entidade **DEVE** ter as seguintes anotações:
```java
@NoArgsConstructor
@Getter
@EqualsAndHashCode(of = "id", callSuper = false)
@ToString(of = "id")
@Entity
@Table(name = "nome_da_tabela")
```

### Estrutura Base
```java
@NoArgsConstructor
@Getter
@EqualsAndHashCode(of = "id", callSuper = false)
@ToString(of = "id")
@Entity
@Table(name = "nome_da_tabela")
public class NomeEntity extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "campo_obrigatorio", nullable = false)
    private String campoObrigatorio;
    
    @Column(name = "campo_opcional")
    private String campoOpcional;
    
    // Construtor com validação obrigatório
    public NomeEntity(String campoObrigatorio, String campoOpcional) {
        // Validações
        if (campoObrigatorio == null || campoObrigatorio.trim().isEmpty()) {
            throw new IllegalArgumentException("Campo obrigatório não pode ser nulo ou vazio");
        }
        
        // Atribuições
        this.campoObrigatorio = campoObrigatorio;
        this.campoOpcional = campoOpcional;
    }
}
```

## Regras Obrigatórias

### Herança
- **DEVE** estender `BaseEntity`
- **NÃO DEVE** estender outras entidades (usar composição)

### Identificador
- **DEVE** usar UUID como chave primária
- **DEVE** usar `@GeneratedValue(strategy = GenerationType.UUID)`
- **DEVE** ter nome da coluna como "id"

### Anotações Lombok
- **DEVE** usar `@NoArgsConstructor` (obrigatório para JPA)
- **DEVE** usar `@Getter` (nunca `@Data` ou `@Setter`)
- **DEVE** usar `@EqualsAndHashCode(of = "id", callSuper = false)`
- **DEVE** usar `@ToString(of = "id")`
- **NÃO DEVE** usar `@Setter` (imutabilidade)

### Construtor com Validação
- **DEVE** ter construtor público com parâmetros obrigatórios
- **DEVE** validar todos os parâmetros no construtor
- **DEVE** lançar exceções específicas para validações falhas
- **PODE** ter múltiplos construtores para diferentes cenários

### Campos
- **DEVE** usar `@Column` com nome explícito
- **DEVE** definir `nullable = false` para campos obrigatórios
- **DEVE** definir `length` para campos de texto quando apropriado
- **DEVE** usar `@Temporal` para campos de data/hora

## Validações

### Validação no Construtor
```java
public UserEntity(String name, String email, String cpf) {
    // Validação de nome
    if (name == null || name.trim().isEmpty()) {
        throw new IllegalArgumentException("Nome é obrigatório");
    }
    if (name.length() > 150) {
        throw new IllegalArgumentException("Nome não pode ter mais de 150 caracteres");
    }
    
    // Validação de CPF
    if (cpf == null || !CpfUtil.isValid(cpf)) {
        throw new IllegalArgumentException("CPF inválido");
    }
    
    this.name = name.trim();
    this.email = email;
    this.cpf = cpf;
}
```

### Validações Complexas
Para validações complexas que requerem acesso a gateways, **DEVE** usar:
- **Domain Services** em `/core/domain/service/`
- **Factories** em `/core/domain/factory/`

```java
// Em um Domain Service
@Component
public class UserValidatorService {
    
    public static void validateCpf(String cpf) {
        if (cpf == null || !CpfUtil.isValid(cpf)) {
            throw new InvalidCpfException("CPF inválido: " + cpf);
        }
    }
    
    public void validateUniqueCpf(String cpf, UserRepositoryGTW userRepository) {
        if (userRepository.existsByCpf(cpf)) {
            throw new DuplicatedCpfException("CPF já cadastrado: " + cpf);
        }
    }
}
```

## Relacionamentos

### OneToOne
```java
@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
@JoinColumn(name = "user_details_id", nullable = false, unique = true)
private UserDetailsEntity userDetails;
```

### OneToMany
```java
@OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
private List<DependentEntity> dependents = new ArrayList<>();
```

### ManyToOne
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "guardian_id")
private UserEntity guardian;
```

## Métodos de Callback JPA

### PrePersist
```java
@Override
public void prePersist() {
    super.prePersist(); // Chama BaseEntity.prePersist()
    
    // Lógica específica da entidade
    if (this.active == null) {
        this.active = true;
    }
    
    if (this.authorized == null) {
        this.authorized = false;
    }
}
```

### PreUpdate
```java
@PreUpdate
public void preUpdate() {
    super.preUpdate(); // Chama BaseEntity.preUpdate()
    
    // Lógica específica da entidade
    // Validações antes da atualização
}
```

## Factories

### Quando Usar
Use factories quando:
- A criação da entidade requer acesso a gateways
- A lógica de criação é complexa
- Precisa de validações que dependem de dados externos

### Estrutura de Factory
```java
@Component
@RequiredArgsConstructor
public class UserFactory {
    
    private final UserRepositoryGTW userRepositoryGTW;
    private final UserValidatorService userValidatorService;
    
    public UserEntity create(String name, String email, String cpf, String password, 
                           Date birthDate, String phone, Boolean specialNeeds, 
                           EnumProgramKnowledgeSource programKnowledgeSource, 
                           String programKnowledgeSourceOther) {
        
        // Validações que requerem gateway
        userValidatorService.validateUniqueCpf(cpf, userRepositoryGTW);
        
        // Criação de entidades relacionadas
        UserDetailsEntity userDetails = new UserDetailsEntity(
            birthDate, phone, specialNeeds, programKnowledgeSource, programKnowledgeSourceOther
        );
        
        // Criação da entidade principal
        return new UserEntity(name, email, cpf, password, userDetails);
    }
}
```

## Boas Práticas

### Imutabilidade
- **NÃO** use `@Setter` global
- **USE** métodos específicos para mudanças de estado
- **USE** `@Setter` apenas em campos específicos quando necessário

```java
// ❌ Errado
@Setter
private String status;

// ✅ Correto
@Setter
private String password; // Apenas quando necessário

public void activate() {
    this.active = true;
}

public void deactivate() {
    this.active = false;
}
```

### Encapsulamento
- **MANTENHA** lógica de negócio dentro da entidade
- **USE** métodos para operações complexas
- **EVITE** expor detalhes internos

```java
public boolean isMinor() {
    return this.userDetails != null && this.userDetails.isMinor();
}

public boolean canBeAuthorized() {
    return !isMinor() || hasValidGuardian();
}
```

### Performance
- **USE** `FetchType.LAZY` por padrão
- **EVITE** relacionamentos bidirecionais desnecessários
- **CONSIDERE** usar `@BatchSize` para coleções

## Checklist para Entidades

- [ ] Estende `BaseEntity`
- [ ] Possui todas as anotações obrigatórias
- [ ] Usa UUID como ID com `@GeneratedValue(strategy = GenerationType.UUID)`
- [ ] Possui construtor com validação
- [ ] Valida parâmetros no construtor
- [ ] Usa `@EqualsAndHashCode(of = "id", callSuper = false)`
- [ ] Usa `@ToString(of = "id")`
- [ ] Não usa `@Setter` global
- [ ] Define nomes explícitos para colunas
- [ ] Define `nullable = false` para campos obrigatórios
- [ ] Implementa `prePersist()` se necessário
- [ ] Usa Factory se criação for complexa
