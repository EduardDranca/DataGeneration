package com.github.eddranca.datagenerator.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JsonNodeUtils")
class JsonNodeUtilsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Should extract simple field")
    void shouldExtractSimpleField() throws Exception {
        String json = "{\"name\": \"John\", \"age\": 30}";
        JsonNode node = mapper.readTree(json);
        
        JsonNode result = JsonNodeUtils.extractNestedField(node, "name");
        
        assertThat(result.asText()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should extract nested field with dot notation")
    void shouldExtractNestedField() throws Exception {
        String json = "{\"user\": {\"profile\": {\"name\": \"John\"}}}";
        JsonNode node = mapper.readTree(json);
        
        JsonNode result = JsonNodeUtils.extractNestedField(node, "user.profile.name");
        
        assertThat(result.asText()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should return original node when path is null")
    void shouldReturnOriginalNodeWhenPathIsNull() throws Exception {
        String json = "{\"name\": \"John\"}";
        JsonNode node = mapper.readTree(json);
        
        JsonNode result = JsonNodeUtils.extractNestedField(node, null);
        
        assertThat(result).isSameAs(node);
    }

    @Test
    @DisplayName("Should return original node when path is empty")
    void shouldReturnOriginalNodeWhenPathIsEmpty() throws Exception {
        String json = "{\"name\": \"John\"}";
        JsonNode node = mapper.readTree(json);
        
        JsonNode result = JsonNodeUtils.extractNestedField(node, "");
        
        assertThat(result).isSameAs(node);
    }

    @Test
    @DisplayName("Should return missing node when field does not exist")
    void shouldReturnMissingNodeWhenFieldDoesNotExist() throws Exception {
        String json = "{\"name\": \"John\"}";
        JsonNode node = mapper.readTree(json);
        
        JsonNode result = JsonNodeUtils.extractNestedField(node, "age");
        
        assertThat(result.isMissingNode()).isTrue();
    }

    @Test
    @DisplayName("Should return missing node when nested field does not exist")
    void shouldReturnMissingNodeWhenNestedFieldDoesNotExist() throws Exception {
        String json = "{\"user\": {\"name\": \"John\"}}";
        JsonNode node = mapper.readTree(json);
        
        JsonNode result = JsonNodeUtils.extractNestedField(node, "user.profile.name");
        
        assertThat(result.isMissingNode()).isTrue();
    }

    @Test
    @DisplayName("Should handle deeply nested paths")
    void shouldHandleDeeplyNestedPaths() throws Exception {
        String json = "{\"a\": {\"b\": {\"c\": {\"d\": {\"value\": 42}}}}}";
        JsonNode node = mapper.readTree(json);
        
        JsonNode result = JsonNodeUtils.extractNestedField(node, "a.b.c.d.value");
        
        assertThat(result.asInt()).isEqualTo(42);
    }

    @Test
    @DisplayName("Should extract field from array element")
    void shouldExtractFieldFromArrayElement() throws Exception {
        String json = "{\"items\": [{\"name\": \"Item1\"}, {\"name\": \"Item2\"}]}";
        JsonNode node = mapper.readTree(json);
        JsonNode firstItem = node.get("items").get(0);
        
        JsonNode result = JsonNodeUtils.extractNestedField(firstItem, "name");
        
        assertThat(result.asText()).isEqualTo("Item1");
    }

    @Test
    @DisplayName("Should handle null node in path")
    void shouldHandleNullNodeInPath() throws Exception {
        String json = "{\"user\": {\"profile\": null}}";
        JsonNode node = mapper.readTree(json);
        
        JsonNode result = JsonNodeUtils.extractNestedField(node, "user.profile.name");
        
        assertThat(result.isNull()).isTrue();
    }
}
