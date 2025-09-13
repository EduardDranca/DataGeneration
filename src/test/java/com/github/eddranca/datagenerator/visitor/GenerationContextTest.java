package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.FilteringBehavior;
import com.github.eddranca.datagenerator.exception.FilteringException;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;
import com.github.eddranca.datagenerator.node.Sequential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationContextTest {

    @Mock
    private GeneratorRegistry mockGeneratorRegistry;

    @Mock
    private Random mockRandom;

    @Mock
    private Generator mockGenerator;

    @Mock
    private Sequential mockSequentialNode;

    private GenerationContext context;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        context = new GenerationContext(mockGeneratorRegistry, mockRandom, 5, FilteringBehavior.RETURN_NULL);
        mapper = new ObjectMapper();
    }

    @Test
    void testConstructorWithDefaults() {
        GenerationContext defaultContext = new GenerationContext(mockGeneratorRegistry, mockRandom);

        assertThat(defaultContext.getGeneratorRegistry()).isEqualTo(mockGeneratorRegistry);
        assertThat(defaultContext.getRandom()).isEqualTo(mockRandom);
        assertThat(defaultContext.getMapper()).isNotNull();
    }

    @Test
    void testConstructorWithCustomSettings() {
        GenerationContext customContext = new GenerationContext(
            mockGeneratorRegistry,
            mockRandom,
            10,
            FilteringBehavior.THROW_EXCEPTION
        );

        assertThat(customContext.getGeneratorRegistry()).isEqualTo(mockGeneratorRegistry);
        assertThat(customContext.getRandom()).isEqualTo(mockRandom);
    }

    @Test
    void testRegisterAndGetCollection() {
        JsonNode item1 = mapper.valueToTree("value1");
        JsonNode item2 = mapper.valueToTree("value2");
        List<JsonNode> collection = List.of(item1, item2);

        context.registerCollection("testCollection", collection);

        List<JsonNode> retrieved = context.getCollection("testCollection");
        assertThat(retrieved).hasSize(2);
        assertThat(retrieved.get(0).asText()).isEqualTo("value1");
        assertThat(retrieved.get(1).asText()).isEqualTo("value2");
    }

    @Test
    void testRegisterCollectionMergesExisting() {
        JsonNode item1 = mapper.valueToTree("value1");
        JsonNode item2 = mapper.valueToTree("value2");
        JsonNode item3 = mapper.valueToTree("value3");

        context.registerCollection("testCollection", List.of(item1));
        context.registerCollection("testCollection", List.of(item2, item3));

        List<JsonNode> retrieved = context.getCollection("testCollection");
        assertThat(retrieved).hasSize(3);
        assertThat(retrieved.get(0).asText()).isEqualTo("value1");
        assertThat(retrieved.get(1).asText()).isEqualTo("value2");
        assertThat(retrieved.get(2).asText()).isEqualTo("value3");
    }

    @Test
    void testGetCollectionReturnsEmptyForNonExistent() {
        List<JsonNode> retrieved = context.getCollection("nonExistent");
        assertThat(retrieved).isEmpty();
    }

    @Test
    void testRegisterAndGetReferenceCollection() {
        JsonNode item = mapper.valueToTree("refValue");
        List<JsonNode> collection = List.of(item);

        context.registerReferenceCollection("refCollection", collection);

        List<JsonNode> retrieved = context.getCollection("refCollection");
        assertThat(retrieved).hasSize(1);
        assertThat(retrieved.get(0).asText()).isEqualTo("refValue");
    }

    @Test
    void testReferenceCollectionTakesPrecedenceOverNamedCollection() {
        JsonNode namedItem = mapper.valueToTree("namedValue");
        JsonNode refItem = mapper.valueToTree("refValue");

        context.registerCollection("testCollection", List.of(namedItem));
        context.registerReferenceCollection("testCollection", List.of(refItem));

        List<JsonNode> retrieved = context.getCollection("testCollection");
        assertThat(retrieved).hasSize(1);
        assertThat(retrieved.get(0).asText()).isEqualTo("refValue");
    }

    @Test
    void testRegisterAndGetTaggedCollection() {
        JsonNode item1 = mapper.valueToTree("tagged1");
        JsonNode item2 = mapper.valueToTree("tagged2");
        List<JsonNode> collection = List.of(item1, item2);

        context.registerTaggedCollection("testTag", collection);

        List<JsonNode> retrieved = context.getTaggedCollection("testTag");
        assertThat(retrieved).hasSize(2);
        assertThat(retrieved.get(0).asText()).isEqualTo("tagged1");
        assertThat(retrieved.get(1).asText()).isEqualTo("tagged2");
    }

    @Test
    void testRegisterTaggedCollectionMergesExisting() {
        JsonNode item1 = mapper.valueToTree("tagged1");
        JsonNode item2 = mapper.valueToTree("tagged2");

        context.registerTaggedCollection("testTag", List.of(item1));
        context.registerTaggedCollection("testTag", List.of(item2));

        List<JsonNode> retrieved = context.getTaggedCollection("testTag");
        assertThat(retrieved).hasSize(2);
    }

    @Test
    void testGetTaggedCollectionReturnsEmptyForNonExistent() {
        List<JsonNode> retrieved = context.getTaggedCollection("nonExistentTag");
        assertThat(retrieved).isEmpty();
    }

    @Test
    void testRegisterAndGetPick() {
        JsonNode pickValue = mapper.valueToTree("pickValue");

        context.registerPick("testPick", pickValue);

        JsonNode retrieved = context.getNamedPick("testPick");
        assertThat(retrieved.asText()).isEqualTo("pickValue");
    }

    @Test
    void testGetNamedPickReturnsNullForNonExistent() {
        JsonNode retrieved = context.getNamedPick("nonExistentPick");
        assertThat(retrieved).isNull();
    }

    @Test
    void testGetNamedCollectionsReturnsCopy() {
        JsonNode item = mapper.valueToTree("value");
        context.registerCollection("testCollection", List.of(item));

        Map<String, List<JsonNode>> collections = context.getNamedCollections();
        assertThat(collections).hasSize(1);
        assertThat(collections.get("testCollection")).hasSize(1);

        // Modify the returned map - should not affect internal state
        collections.clear();
        assertThat(context.getCollection("testCollection")).hasSize(1);
    }

    @Test
    void testGetElementFromCollectionWithRandomSelection() {
        JsonNode item1 = mapper.valueToTree("item1");
        JsonNode item2 = mapper.valueToTree("item2");
        List<JsonNode> collection = List.of(item1, item2);

        when(mockRandom.nextInt(2)).thenReturn(1);

        JsonNode result = context.getElementFromCollection(collection, mockSequentialNode, false);
        assertThat(result.asText()).isEqualTo("item2");
    }

    @Test
    void testGetElementFromCollectionWithSequentialSelection() {
        JsonNode item1 = mapper.valueToTree("item1");
        JsonNode item2 = mapper.valueToTree("item2");
        List<JsonNode> collection = List.of(item1, item2);

        JsonNode result1 = context.getElementFromCollection(collection, mockSequentialNode, true);
        JsonNode result2 = context.getElementFromCollection(collection, mockSequentialNode, true);
        JsonNode result3 = context.getElementFromCollection(collection, mockSequentialNode, true);

        assertThat(result1.asText()).isEqualTo("item1");
        assertThat(result2.asText()).isEqualTo("item2");
        assertThat(result3.asText()).isEqualTo("item1"); // Wraps around
    }

    @Test
    void testGetElementFromEmptyCollectionReturnsNull() {
        List<JsonNode> emptyCollection = List.of();

        JsonNode result = context.getElementFromCollection(emptyCollection, mockSequentialNode, false);
        assertThat(result.isNull()).isTrue();
    }

    @Test
    void testSequentialIndexingWithDifferentNodes() {
        JsonNode item1 = mapper.valueToTree("item1");
        JsonNode item2 = mapper.valueToTree("item2");
        List<JsonNode> collection = List.of(item1, item2);

        Sequential node1 = mock(Sequential.class);
        Sequential node2 = mock(Sequential.class);

        // Different nodes should have independent counters
        JsonNode result1 = context.getElementFromCollection(collection, node1, true);
        JsonNode result2 = context.getElementFromCollection(collection, node2, true);
        JsonNode result3 = context.getElementFromCollection(collection, node1, true);

        assertThat(result1.asText()).isEqualTo("item1"); // node1 index 0
        assertThat(result2.asText()).isEqualTo("item1"); // node2 index 0
        assertThat(result3.asText()).isEqualTo("item2"); // node1 index 1
    }

    @Test
    void testGetNextSequentialIndex() {
        assertThat(context.getNextSequentialIndex(mockSequentialNode, 3)).isEqualTo(0);
        assertThat(context.getNextSequentialIndex(mockSequentialNode, 3)).isEqualTo(1);
        assertThat(context.getNextSequentialIndex(mockSequentialNode, 3)).isEqualTo(2);
        assertThat(context.getNextSequentialIndex(mockSequentialNode, 3)).isEqualTo(0); // Wraps around
    }

    @Test
    void testGetNextSequentialIndexWithZeroSize() {
        assertThat(context.getNextSequentialIndex(mockSequentialNode, 0)).isEqualTo(0);
    }

    @Test
    void testApplyFilteringWithEmptyFieldName() {
        JsonNode item1 = mapper.valueToTree("keep");
        JsonNode item2 = mapper.valueToTree("filter");
        JsonNode item3 = mapper.valueToTree("keep");
        List<JsonNode> collection = List.of(item1, item2, item3);

        JsonNode filterValue = mapper.valueToTree("filter");
        List<JsonNode> filterValues = List.of(filterValue);

        List<JsonNode> result = context.applyFiltering(collection, "", filterValues);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).asText()).isEqualTo("keep");
        assertThat(result.get(1).asText()).isEqualTo("keep");
    }

    @Test
    void testApplyFilteringWithFieldName() {
        JsonNode item1 = mapper.createObjectNode().put("name", "keep1").put("value", 1);
        JsonNode item2 = mapper.createObjectNode().put("name", "filter").put("value", 2);
        JsonNode item3 = mapper.createObjectNode().put("name", "keep2").put("value", 3);
        List<JsonNode> collection = List.of(item1, item2, item3);

        JsonNode filterValue = mapper.valueToTree("filter");
        List<JsonNode> filterValues = List.of(filterValue);

        List<JsonNode> result = context.applyFiltering(collection, "name", filterValues);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("name").asText()).isEqualTo("keep1");
        assertThat(result.get(1).get("name").asText()).isEqualTo("keep2");
    }

    @Test
    void testApplyFilteringWithEmptyCollection() {
        List<JsonNode> emptyCollection = List.of();
        JsonNode filterValue = mapper.valueToTree("filter");
        List<JsonNode> filterValues = List.of(filterValue);

        List<JsonNode> result = context.applyFiltering(emptyCollection, "field", filterValues);

        assertThat(result).isEmpty();
    }

    @Test
    void testApplyFilteringWithNullFilterValues() {
        JsonNode item = mapper.valueToTree("item");
        List<JsonNode> collection = List.of(item);

        List<JsonNode> result = context.applyFiltering(collection, "field", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(item);
    }

    @Test
    void testApplyFilteringOnField() {
        JsonNode item1 = mapper.createObjectNode().put("name", "keep1").put("value", 1);
        JsonNode item2 = mapper.createObjectNode().put("name", "filter").put("value", 2);
        JsonNode item3 = mapper.createObjectNode().put("name", "keep2").put("value", 3);
        List<JsonNode> collection = List.of(item1, item2, item3);

        JsonNode filterValue = mapper.valueToTree("filter");
        List<JsonNode> filterValues = List.of(filterValue);

        List<JsonNode> result = context.applyFilteringOnField(collection, "name", filterValues);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("name").asText()).isEqualTo("keep1");
        assertThat(result.get(1).get("name").asText()).isEqualTo("keep2");
    }

    @Test
    void testApplyFilteringOnFieldWithMissingField() {
        JsonNode item1 = mapper.createObjectNode().put("name", "keep1");
        JsonNode item2 = mapper.createObjectNode(); // Missing "name" field
        JsonNode item3 = mapper.createObjectNode().put("name", "keep2");
        List<JsonNode> collection = List.of(item1, item2, item3);

        JsonNode filterValue = mapper.valueToTree("filter");
        List<JsonNode> filterValues = List.of(filterValue);

        List<JsonNode> result = context.applyFilteringOnField(collection, "name", filterValues);

        // Item2 should be filtered out because it has a missing field
        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("name").asText()).isEqualTo("keep1");
        assertThat(result.get(1).get("name").asText()).isEqualTo("keep2");
    }

    @Test
    void testHandleFilteringFailureWithReturnNull() {
        GenerationContext nullContext = new GenerationContext(
            mockGeneratorRegistry,
            mockRandom,
            5,
            FilteringBehavior.RETURN_NULL
        );

        JsonNode result = nullContext.handleFilteringFailure("Test failure");

        assertThat(result.isNull()).isTrue();
    }

    @Test
    void testHandleFilteringFailureWithThrowException() {
        GenerationContext exceptionContext = new GenerationContext(
            mockGeneratorRegistry,
            mockRandom,
            5,
            FilteringBehavior.THROW_EXCEPTION
        );

        assertThatThrownBy(() -> exceptionContext.handleFilteringFailure("Test failure"))
            .isInstanceOf(FilteringException.class)
            .hasMessage("Test failure");
    }

    @Test
    void testGenerateWithFilterWithoutPath() {
        JsonNode options = mapper.createObjectNode();
        JsonNode expectedResult = mapper.valueToTree("generated");

        when(mockGenerator.generate(any())).thenReturn(expectedResult);

        JsonNode result = context.generateWithFilter(mockGenerator, options, null, null);

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void testGenerateWithFilterWithPath() {
        JsonNode options = mapper.createObjectNode();
        JsonNode expectedResult = mapper.valueToTree("pathGenerated");

        when(mockGenerator.generateAtPath(any(), any())).thenReturn(expectedResult);

        JsonNode result = context.generateWithFilter(mockGenerator, options, "testPath", null);

        assertThat(result).isEqualTo(expectedResult);
    }
}
