---
trigger: always_on
description: 
globs: 
---

# Regras para Testes

## Frameworks Obrigatórios

### JUnit 5 e Mockito
- **SEMPRE** usar JUnit 5 (Jupiter)
- **SEMPRE** usar Mockito para mocks
- **SEMPRE** manter documentação atualizada dos testes
- **USAR** versões mais recentes compatíveis com Spring Boot

### Regras
- **SEMPRE** fazer em inglês metodos, váriaveis e etc
- **SEMPRE** comentários fazer em portugues-ptbr

### Dependências Maven
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

## Estrutura de Testes

### Exemplo
```java
@DisplayName("Testes de integração de registro de usuário")
class RegisterIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RegisterUC registerUC;

    @Autowired
    private UserRepositoryGTW userRepositoryGTW;

    @Autowired
    private UserDependentRepositoryGTW userDependentRepositoryGTW;

    @Autowired
    private ListDependentsLinkedUC listDependentsLinkedUC;

    @Test
    @DisplayName("Deve registrar um usuário adulto com sucesso")
    void shouldRegisterAdultUserSuccessfully() {
        // Given
        UserRegisterDTO userRegisterDTO = createValidUserRegisterDTO("adult@example.com", "11144477735");
        userRegisterDTO.setBirthDate(Date.from(LocalDate.now().minusYears(25).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        // When
        assertDoesNotThrow(() -> registerUC.execute(userRegisterDTO));

        // Then
        Optional<UserEntity> savedUser = userRepositoryGTW.findByCpf(userRegisterDTO.getCpf());
        assertTrue(savedUser.isPresent());
        UserEntity user = savedUser.get();
        assertTrue(user.getActive());
        assertTrue(user.getAuthorized()); // Deve estar autorizado
        assertNotNull(user.getPassword());
    }

    @Test
    @DisplayName("Deve registrar um usuário menor como nao autorizado")
    void shouldRegisterMinorUserAsUnauthorized() {
        // Given
        UserRegisterDTO userRegisterDTO = createValidUserRegisterDTO("minor@example.com", "22255588846");
        userRegisterDTO.setBirthDate(Date.from(LocalDate.now().minusYears(16).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        // When
        assertDoesNotThrow(() -> registerUC.execute(userRegisterDTO));

        // Then
        Optional<UserEntity> savedUser = userRepositoryGTW.findByCpf(userRegisterDTO.getCpf());
        assertTrue(savedUser.isPresent());
        UserEntity user = savedUser.get();
        assertFalse(user.getAuthorized()); // Menor nao deve estar autorizado
        assertTrue(user.getActive());
        assertNotNull(user.getResponsibleToken()); // Deve ter gerado o link para o responsável
    }

    @Test
    @DisplayName("Deve registrar um usuário responsavel com link enviado pelo usúario menor")
    void shouldRegisterResponsibleUser() {
        // Given
        // Registra o menor
        UserRegisterDTO userRegisterDTO = createValidUserRegisterDTO("minor2@example.com", "54403121020");
        userRegisterDTO.setBirthDate(Date.from(LocalDate.now().minusYears(16).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        // When
        assertDoesNotThrow(() -> registerUC.execute(userRegisterDTO));

        // Then
        Optional<UserEntity> savedUser = userRepositoryGTW.findByCpf(userRegisterDTO.getCpf());
        assertTrue(savedUser.isPresent());
        UserEntity minor = savedUser.get();
        assertFalse(minor.getAuthorized()); // Menor nao deve estar autorizado
        assertTrue(minor.getActive());
        assertNotNull(minor.getResponsibleToken()); // Deve ter gerado o link para o responsável

        // Given
        // Registra o responsável
        UserRegisterDTO DTOResponsible = createValidUserRegisterDTO("adult2@example.com", "06236654093");
        DTOResponsible.setBirthDate(Date.from(LocalDate.now().minusYears(25).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        DTOResponsible.setAuthorizedToken(minor.getResponsibleToken());

        // When
        assertDoesNotThrow(() -> registerUC.execute(DTOResponsible));

        // Then
        Optional<UserEntity> savedUserResponsible = userRepositoryGTW.findByCpf(DTOResponsible.getCpf());
        assertTrue(savedUserResponsible.isPresent());
        UserEntity userResponsible = savedUserResponsible.get();
        assertTrue(userResponsible.getActive());
        assertTrue(userResponsible.getAuthorized()); // Responsável deve estar autorizado
        Optional<UserDependentEntity> userDependent = userDependentRepositoryGTW.findByUserAndDependent(userResponsible, minor);
        assertTrue(userDependent.isPresent()); // Verifica se o menor foi vinculado ao responsável

        // When
        DependentsLinkedListDTO dependents = listDependentsLinkedUC.execute(userResponsible);

        // Then
        // Verifica se o menor foi adicionado na lista de pendentes dos dependentes
        assertEquals(1, dependents.getPending().size());
    }

    private UserRegisterDTO createValidUserRegisterDTO(String email, String cpf) {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setName("Usuario de teste");
        dto.setEmail(email);
        dto.setPassword("SenhaValida123!");
        dto.setBirthDate(Date.from(LocalDate.now().minusYears(25).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        dto.setPhone("11987654321");
        dto.setCpf(cpf);
        dto.setSpecialNeeds(false);
        dto.setProgramKnowledgeSource(EnumProgramKnowledgeSource.FACEBOOK);
        dto.setProgramKnowledgeSourceOther(null);
        dto.setAuthorizedToken(null);
        return dto;
    }

}
```

## Regras de Nomenclatura

### Nomes de Classes de Teste
- **DEVE** terminar com "Test"
- **DEVE** seguir padrão: `[ClasseTestada]Test`
- **EXEMPLO**: `RegisterUCTest`, `UserEntityTest`

### Nomes de Métodos de Teste
- **DEVE** usar inglês
- **DEVE** ser descritivo e explicar o cenário
- **DEVE** seguir padrão: `should[Ação]When[Condição]`
- **EXEMPLO**: `shouldRegisterResponsibleUser`

### DisplayName
- **SEMPRE** usar `@DisplayName` em classes e métodos
- **DEVE** ser em português brasileiro
- **DEVE** ser descritivo e claro

## Estrutura de Teste (Given-When-Then)

### Padrão Obrigatório
```java
@Test
void shouldExecuteActionWhenCondition() {
    // Given - Preparação dos dados
    InputDTO input = createValidInput();
    when(gateway.method()).thenReturn(expectedResult);
    
    // When - Execução da ação
    ResultType result = useCase.execute(input);
    
    // Then - Verificação dos resultados
    assertThat(result).isNotNull();
    verify(gateway).method();
}
```

## Assertions

### AssertJ (Recomendado)
```java
// ✅ Usar AssertJ
assertThat(result).isNotNull();
assertThat(result.getName()).isEqualTo("João");
assertThat(result.getList()).hasSize(3);
assertThat(result.getList()).containsExactly("A", "B", "C");

// ❌ Evitar JUnit assertions básicas
assertEquals("João", result.getName());
assertTrue(result.getList().size() == 3);
```

### Exceções
```java
// ✅ Correto
IllegalArgumentException exception = assertThrows(
    IllegalArgumentException.class,
    () -> useCase.execute(invalidInput)
);
assertThat(exception.getMessage()).contains("CPF inválido");

// ❌ Evitar
try {
    useCase.execute(invalidInput);
    fail("Deveria ter lançado exceção");
} catch (IllegalArgumentException e) {
    assertEquals("CPF inválido", e.getMessage());
}
```
