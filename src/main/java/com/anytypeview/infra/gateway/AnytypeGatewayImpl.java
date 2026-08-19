package com.anytypeview.infra.gateway;

import com.anytypeview.core.dto.AnytypeObjectSnapshotDTO;
import com.anytypeview.core.dto.AnytypeSchemaValidationDTO;
import com.anytypeview.core.dto.AnytypeSnapshotDataDTO;
import com.anytypeview.core.gateway.AnytypeGateway;
import com.anytypeview.infra.config.AnytypeProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AnytypeGatewayImpl implements AnytypeGateway {

    private static final int PAGE_LIMIT = 1000;
    private static final List<String> EXPECTED_TYPES = List.of(
        "Tema",
        "Conceito",
        "Checkpoint de Conhecimento",
        "Aplicacao"
    );
    private static final List<String> EXPECTED_PROPERTIES = List.of(
        "Tipo",
        "Links",
        "Prioridade",
        "Tag",
        "Categoria",
        "Veredito",
        "Entendimento",
        "Tradeoff",
        "Checkpoint",
        "Tema",
        "Conecta com",
        "Ultima Revisao",
        "Stack relacionada",
        "Lacunas",
        "Aplicacao Pratica",
        "Vendabilidade",
        "Nivel Percebido",
        "Status",
        "Nivel Implementacao",
        "GitHub"
    );

    private final RestClient restClient;
    private final AnytypeProperties anytypeProperties;
    private final ObjectMapper objectMapper;

    public AnytypeGatewayImpl(
        RestClient restClient,
        AnytypeProperties anytypeProperties,
        ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.anytypeProperties = anytypeProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public AnytypeSchemaValidationDTO validateExpectedSchema() {
        JsonNode space = findSpace();
        String spaceId = requiredText(space, "id");
        String spaceName = requiredText(space, "name");

        Set<String> typeNames = namesFromPaginatedEndpoint("/v1/spaces/" + spaceId + "/types");
        Set<String> propertyNames = namesFromPaginatedEndpoint("/v1/spaces/" + spaceId + "/properties");

        return new AnytypeSchemaValidationDTO(
            spaceId,
            spaceName,
            found(EXPECTED_TYPES, typeNames),
            missing(EXPECTED_TYPES, typeNames),
            found(EXPECTED_PROPERTIES, propertyNames),
            missing(EXPECTED_PROPERTIES, propertyNames)
        );
    }

    @Override
    public AnytypeSnapshotDataDTO loadSnapshotData() {
        JsonNode space = findSpace();
        String spaceId = requiredText(space, "id");
        String spaceName = requiredText(space, "name");
        Map<String, JsonNode> typesByExpectedName = expectedTypesByName(spaceId);
        List<String> typeKeys = typesByExpectedName.values().stream()
            .map(type -> requiredText(type, "key"))
            .toList();
        List<JsonNode> objects = searchObjects(spaceId, typeKeys);

        return new AnytypeSnapshotDataDTO(
            spaceId,
            spaceName,
            objects.stream().map(this::toObjectSnapshot).toList()
        );
    }

    private JsonNode findSpace() {
        List<JsonNode> spaces = fetchPaginated("/v1/spaces");
        String expectedSpaceName = normalizeName(anytypeProperties.spaceName());
        return spaces.stream()
            .filter(space -> expectedSpaceName.equals(normalizeName(requiredText(space, "name"))))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Espaco Anytype nao encontrado: " + anytypeProperties.spaceName()
                    + ". Espacos disponiveis: " + availableSpaceNames(spaces)
            ));
    }

    private String availableSpaceNames(List<JsonNode> spaces) {
        return spaces.stream()
            .map(space -> space.path("name").asText(""))
            .filter(name -> !name.isBlank())
            .sorted()
            .reduce((left, right) -> left + ", " + right)
            .orElse("nenhum");
    }

    private Set<String> namesFromPaginatedEndpoint(String path) {
        Set<String> names = new LinkedHashSet<>();
        for (JsonNode item : fetchPaginated(path)) {
            String name = item.path("name").asText(null);
            if (name != null && !name.isBlank()) {
                names.add(name);
            }
        }
        return names;
    }

    private Map<String, JsonNode> expectedTypesByName(String spaceId) {
        Map<String, JsonNode> typeIndex = new HashMap<>();
        for (JsonNode type : fetchPaginated("/v1/spaces/" + spaceId + "/types")) {
            String name = type.path("name").asText("");
            if (!name.isBlank()) {
                typeIndex.put(normalizeName(name), type);
            }
        }

        Map<String, JsonNode> result = new HashMap<>();
        for (String expectedType : EXPECTED_TYPES) {
            JsonNode type = typeIndex.get(normalizeName(expectedType));
            if (type != null) {
                result.put(expectedType, type);
            }
        }
        return result;
    }

    private List<JsonNode> searchObjects(String spaceId, List<String> typeKeys) {
        List<JsonNode> items = new ArrayList<>();
        int offset = 0;
        boolean hasMore;
        Map<String, Object> body = Map.of("types", typeKeys);

        do {
            JsonNode response = postSearch("/v1/spaces/" + spaceId + "/search", offset, body);
            JsonNode data = response.path("data");
            if (data.isArray()) {
                data.forEach(items::add);
            }

            JsonNode pagination = response.path("pagination");
            hasMore = pagination.path("has_more").asBoolean(false);
            offset += pagination.path("limit").asInt(PAGE_LIMIT);
        } while (hasMore);

        return items;
    }

    private AnytypeObjectSnapshotDTO toObjectSnapshot(JsonNode object) {
        JsonNode type = object.path("type");
        String propertiesJson = compactJson(object.path("properties"));
        return new AnytypeObjectSnapshotDTO(
            requiredText(object, "id"),
            type.path("id").asText(null),
            type.path("key").asText(null),
            type.path("name").asText(null),
            object.path("name").asText(""),
            object.path("archived").asBoolean(false),
            findDate(object.path("properties"), "created_date", "created date"),
            findDate(object.path("properties"), "last_modified_date", "last modified date"),
            sha256(propertiesJson),
            propertiesJson
        );
    }

    private String findDate(JsonNode properties, String expectedKey, String expectedName) {
        if (!properties.isArray()) {
            return null;
        }
        for (JsonNode property : properties) {
            String key = property.path("key").asText("");
            String name = property.path("name").asText("");
            if (expectedKey.equals(key) || expectedName.equals(normalizeName(name))) {
                return property.path("date").asText(null);
            }
        }
        return null;
    }

    private List<JsonNode> fetchPaginated(String path) {
        List<JsonNode> items = new ArrayList<>();
        int offset = 0;
        boolean hasMore;

        do {
            JsonNode response = get(path, offset);
            JsonNode data = response.path("data");
            if (data.isArray()) {
                data.forEach(items::add);
            }

            JsonNode pagination = response.path("pagination");
            hasMore = pagination.path("has_more").asBoolean(false);
            offset += pagination.path("limit").asInt(PAGE_LIMIT);
        } while (hasMore);

        return items;
    }

    private JsonNode get(String path, int offset) {
        return restClient.get()
            .uri(anytypeProperties.baseUrl() + path + "?offset={offset}&limit={limit}", offset, PAGE_LIMIT)
            .header("Anytype-Version", anytypeProperties.version())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + anytypeProperties.apiKey())
            .retrieve()
            .body(JsonNode.class);
    }

    private JsonNode postSearch(String path, int offset, Map<String, Object> body) {
        return restClient.post()
            .uri(anytypeProperties.baseUrl() + path + "?offset={offset}&limit={limit}", offset, PAGE_LIMIT)
            .header("Anytype-Version", anytypeProperties.version())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + anytypeProperties.apiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(JsonNode.class);
    }

    private String compactJson(JsonNode jsonNode) {
        try {
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nao foi possivel serializar propriedades do Anytype", exception);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponivel", exception);
        }
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asText(null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Campo obrigatorio ausente na resposta do Anytype: " + fieldName);
        }
        return value;
    }

    private List<String> found(List<String> expected, Set<String> actual) {
        Map<String, String> actualByNormalizedName = normalizedNameIndex(actual);
        return expected.stream()
            .filter(item -> actualByNormalizedName.containsKey(normalizeName(item)))
            .sorted(Comparator.naturalOrder())
            .toList();
    }

    private List<String> missing(List<String> expected, Set<String> actual) {
        Map<String, String> actualByNormalizedName = normalizedNameIndex(actual);
        return expected.stream()
            .filter(item -> !actualByNormalizedName.containsKey(normalizeName(item)))
            .sorted(Comparator.naturalOrder())
            .toList();
    }

    private Map<String, String> normalizedNameIndex(Set<String> names) {
        Map<String, String> index = new HashMap<>();
        for (String name : names) {
            index.put(normalizeName(name), name);
        }
        return index;
    }

    private String normalizeName(String name) {
        String withoutAccents = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return withoutAccents
            .trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", " ");
    }
}
