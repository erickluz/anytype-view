---
trigger: always_on
description: 
globs: 
---

# Regras para Enums

## Estrutura Obrigatória

### Implementação da Interface IEnum
Todos os enums **DEVEM** implementar a interface `IEnum`:

```java
@Getter
public enum EnumProgramKnowledgeSource implements IEnum {
    FACEBOOK("Facebook", "Facebook"),
    INSTAGRAM("Instagram", "Instagram"),
    GOOGLE("Google", "Google"),
    WHATSAPP("WhatsApp", "WhatsApp"),
    EMAIL("E-mail", "E-mail"),
    OUTRO("Outro", "Outro");

    private final String key;

    private final String value;

    EnumProgramKnowledgeSource(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @jakarta.persistence.Converter(autoApply = true)
    static class Converter extends IEnum.Converter<EnumProgramKnowledgeSource, String> {

        @Override
        public EnumProgramKnowledgeSource[] getValues() {
            return EnumProgramKnowledgeSource.values();
        }

    }

    @Override
    public Class<? extends IEnum.Converter<EnumProgramKnowledgeSource, String>> getConverter() {
        return EnumProgramKnowledgeSource.Converter.class;
    }

}
```

## Regras Obrigatórias

### Anotações
- **DEVE** usar `@Getter` e `@RequiredArgsConstructor`
- **NÃO DEVE** usar `@AllArgsConstructor` ou `@NoArgsConstructor`

### Campos
- **DEVE** ter campo `key` (Integer, String ou Long)
- **DEVE** ter campo `value` (String descritivo)
- **DEVE** marcar campos como `final`

### Converter
- **DEVE** ter classe interna estática `Converter`
- **DEVE** anotar converter com `@jakarta.persistence.Converter(autoApply = true)`
- **DEVE** estender `IEnum.Converter<EnumType, KeyType>`
- **DEVE** implementar métodos de conversão

### Método getConverter
- **DEVE** implementar `getConverter()` retornando a classe do converter

## Exemplos Práticos

### Enum com Chave Integer
```java
@Getter
@RequiredArgsConstructor
public enum EnumDependentStatus implements IEnum {
    
    PENDING(1, "Pendente"),
    ACTIVE(2, "Ativo"),
    INACTIVE(3, "Inativo"),
    REJECTED(4, "Rejeitado");
    
    private final Integer key;
    private final String value;
    
    @Override
    public Class<? extends Converter<?, ?>> getConverter() {
        return EnumDependentStatusConverter.class;
    }
    
    @Component
    public static class EnumDependentStatusConverter extends IEnum.Converter<EnumDependentStatus, Integer> {
        @Override
        public EnumDependentStatus[] getValues() {
            return EnumDependentStatus.values();
        }
    }
}
```

### Enum com Chave String
```java
@Getter
public enum EnumTemplateEmail implements IEnum {

    RECOVERY("RECOVERY", "Recuperação", "Recuperação de senha");

    private final String key;

    private final String value;

    private final String subject;

    EnumTemplateEmail(String key, String value, String subject) {
        this.key = key;
        this.value = value;
        this.subject = subject;
    }

    @jakarta.persistence.Converter(autoApply = true)
    static class Converter extends IEnum.Converter<EnumTemplateEmail, String> {

        @Override
        public EnumTemplateEmail[] getValues() {
            return EnumTemplateEmail.values();
        }

    }

    @Override
    public Class<? extends IEnum.Converter<EnumTemplateEmail, String>> getConverter() {
        return Converter.class;
    }

}
```

## Uso em Entidades
```java
@NoArgsConstructor
@Getter
@EqualsAndHashCode(of = "id", callSuper = false)
@ToString(of = "id")
@Entity
@Table(name = "portal_template_email")
public class TemplateEmailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "template_email", nullable = false, unique = true)
    private EnumTemplateEmail templateEmail;

    @Column(name = "html", nullable = false)
    private String html;

}
```

### Validação em Construtores
```java
public UserDetailsEntity(Date birthDate, String phone, Boolean specialNeeds, 
                        EnumProgramKnowledgeSource programKnowledgeSource, 
                        String programKnowledgeSourceOther) {
    
    // Validações
    if (programKnowledgeSource == null) {
        throw new ValidatorException("Origem do conhecimento do programa é obrigatória");
    }
    
    if (programKnowledgeSource == EnumProgramKnowledgeSource.OTHER && 
        (programKnowledgeSourceOther == null || programKnowledgeSourceOther.trim().isEmpty())) {
        throw new ValidatorException("Descrição da origem 'Outro' é obrigatória");
    }
    
    // Atribuições
    this.birthDate = birthDate;
    this.phone = phone;
    this.specialNeeds = specialNeeds != null ? specialNeeds : false;
    this.programKnowledgeSource = programKnowledgeSource;
    this.programKnowledgeSourceOther = programKnowledgeSourceOther;
}
```

## Testes de Enums

### Teste do Enum
```java
@DisplayName("EnumProgramKnowledgeSource - Testes do enum")
class EnumProgramKnowledgeSourceTest {
    
    @Test
    @DisplayName("Deve ter todos os valores esperados")
    void deveTerTodosValoresEsperados() {
        // Given & When
        EnumProgramKnowledgeSource[] values = EnumProgramKnowledgeSource.values();
        
        // Then
        assertThat(values).hasSize(7);
        assertThat(values).contains(
            EnumProgramKnowledgeSource.INTERNET,
            EnumProgramKnowledgeSource.SOCIAL_MEDIA,
            EnumProgramKnowledgeSource.FRIENDS_FAMILY,
            EnumProgramKnowledgeSource.TELEVISION,
            EnumProgramKnowledgeSource.RADIO,
            EnumProgramKnowledgeSource.NEWSPAPER,
            EnumProgramKnowledgeSource.OTHER
        );
    }
    