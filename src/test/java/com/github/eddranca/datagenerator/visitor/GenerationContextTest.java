package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.FilteringBehavior;
import com.github.eddranca.datagenerator.FilteringException;
import com.github.eddranca.datagenerator.exception.InvalidReferenceException;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;
import com.github.eddranca.datagenerator.node.ReferenceFieldNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GenerationContextTest {
    private GenerationContext context;
    private ObjectMapper mapper;
    private Random random;

    @Mock
    private GeneratorRegistry generatorRegistry;

    @Mock
    private Generator mockGenerator;

    @Mock
    private ReferenceFieldNode mockReferenceNode;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mapper = new ObjectMapper();
        random = new Random(42);
        context = new GenerationContext(generatorRegistry, random, 100, FilteringBehavior.RETURN_NULL);
    }

    @Test
    void testConstructorAndBasicGetters() {
        // Test constructor with all parameters
        assertThat(context.getGeneratorRegistry()).isEqualTo(generatorRegistry);
        assertThat(context.getRandom()).isEqualTo(random);
        assertThat(context.getMapper()).isNotNull();

        // Test constructor with defaults
        GenerationContext defaultContext = new GenerationContext(generatorRegistry, random);
        assertThat(defaultContext.getGeneratorRegistry()).isEqualTo(generatorRegistry);
        assertThat(defaultContext.getRandom()).isEqualTo(random);
        assertThat(defaultContext.getMapper()).isNotNull();
    }

    @Test
    void testRegisterAndGetCollection() {
        List<JsonNode> collection = Arrays.asList(
            mapper.valueToTree("item1"),
            mapper.valueToTree("item2"));

        context.registerCollection("testCollection", collection);

        List<JsonNode> retrieved = context.getCollection("testCollection");
        assertThat(retrieved)
            .isNotNull()
            .hasSize(2);
        assertThat(retrieved.get(0).asText()).isEqualTo("item1");
        assertThat(retrieved.get(1).asText()).isEqualTo("item2");
    }

    @Test
    void testRegisterCollectionMerging() {
        List<JsonNode> collection1 = Collections.singletonList(mapper.valueToTree("item1"));
        List<JsonNode> collection2 = Collections.singletonList(mapper.valueToTree("item2"));

        context.registerCollection("testCollection", collection1);
        context.registerCollection("testCollection", collection2);

        List<JsonNode> merged = context.getCollection("testCollection");
        assertThat(merged).hasSize(2);
        assertThat(merged.get(0).asText()).isEqualTo("item1");
        assertThat(merged.get(1).asText()).isEqualTo("item2");
    }

    @Test
    void testRegisterAndGetReferenceCollection() {
        List<JsonNode> collection = Arrays.asList(
            mapper.valueToTree("ref1"),
            mapper.valueToTree("ref2"));

        context.registerReferenceCollection("refCollection", collection);

        // Reference collections should be accessible via getCollection
        List<JsonNode> retrieved = context.getCollection("refCollection");
        assertThat(retrieved)
            .isNotNull()
            .hasSize(2);
        assertThat(retrieved.get(0).asText()).isEqualTo("ref1");
        assertThat(retrieved.get(1).asText()).isEqualTo("ref2");
    }

    @Test
    void testReferenceCollectionTakesPrecedence() {
        List<JsonNode> namedCollection = Collections.singletonList(mapper.valueToTree("named"));
        List<JsonNode> refCollection = Collections.singletonList(mapper.valueToTree("reference"));

        context.registerCollection("sameName", namedCollection);
        context.registerReferenceCollection("sameName", refCollection);

        // Reference collection should take precedence
        List<JsonNode> retrieved = context.getCollection("sameName");
        assertThat(retrieved.get(0).asText()).isEqualTo("reference");
    }

    @Test
    void testRegisterAndGetTaggedCollection() {
        List<JsonNode> collection = Arrays.asList(
            mapper.valueToTree("tagged1"),
            mapper.valueToTree("tagged2"));

        context.registerTaggedCollection("testTag", collection);

        List<JsonNode> retrieved = context.getTaggedCollection("testTag");
        assertThat(retrieved)
            .isNotNull()
            .hasSize(2);
        assertThat(retrieved.get(0).asText()).isEqualTo("tagged1");
    }

    @Test
    void testTaggedCollectionMerging() {
        List<JsonNode> collection1 = Collections.singletonList(mapper.valueToTree("tagged1"));
        List<JsonNode> collection2 = Collections.singletonList(mapper.valueToTree("tagged2"));

        context.registerTaggedCollection("testTag", collection1);
        context.registerTaggedCollection("testTag", collection2);

        List<JsonNode> merged = context.getTaggedCollection("testTag");
        assertThat(merged).hasSize(2);
        assertThat(merged.get(0).asText()).isEqualTo("tagged1");
        assertThat(merged.get(1).asText()).isEqualTo("tagged2");
    }

    @Test
    void testGetNamedCollections() {
        List<JsonNode> collection1 = Collections.singletonList(mapper.valueToTree("item1"));
        List<JsonNode> collection2 = Collections.singletonList(mapper.valueToTree("item2"));

        context.registerCollection("collection1", collection1);
        context.registerCollection("collection2", collection2);

        Map<String, List<JsonNode>> namedCollections = context.getNamedCollections();
        assertThat(namedCollections)
            .hasSize(2)
            .containsKeys("collection1", "collection2");

        // Should be a copy, not the original map
        namedCollections.clear();
        assertThat(context.getNamedCollections()).hasSize(2);
    }

    @Test
    void testGetTaggedCollections() {
        List<JsonNode> collection = Collections.singletonList(mapper.valueToTree("tagged1"));

        context.registerTaggedCollection("tag1", collection);

        Map<String, List<JsonNode>> taggedCollections = context.getTaggedCollections();
        assertThat(taggedCollections)
            .hasSize(1)
            .containsKey("tag1");

        // Should be a copy
        taggedCollections.clear();
        assertThat(context.getTaggedCollections()).hasSize(1);
    }

    @Test
    void testSequentialIndexing() {
        // Test that sequential indexing works correctly with wrap-around
        int collectionSize = 3;

        assertThat(context.getNextSequentialIndex(mockReferenceNode, collectionSize)).isEqualTo(0);
        assertThat(context.getNextSequentialIndex(mockReferenceNode, collectionSize)).isEqualTo(1);
        assertThat(context.getNextSequentialIndex(mockReferenceNode, collectionSize)).isEqualTo(2);
        assertThat(context.getNextSequentialIndex(mockReferenceNode, collectionSize)).isEqualTo(0); // Wrap around
        assertThat(context.getNextSequentialIndex(mockReferenceNode, collectionSize)).isEqualTo(1);
    }

    @Test
    void testSequentialIndexingWithDifferentNodes() {
        // Different nodes should have independent counters
        ReferenceFieldNode node1 = mock(ReferenceFieldNode.class);
        ReferenceFieldNode node2 = mock(ReferenceFieldNode.class);

        assertThat(context.getNextSequentialIndex(node1, 5)).isEqualTo(0);
        assertThat(context.getNextSequentialIndex(node2, 5)).isEqualTo(0);
        assertThat(context.getNextSequentialIndex(node1, 5)).isEqualTo(1);
        assertThat(context.getNextSequentialIndex(node2, 5)).isEqualTo(1);
    }

    @Test
    void testSequentialIndexingWithZeroSize() {
        assertThat(context.getNextSequentialIndex(mockReferenceNode, 0)).isEqualTo(0);
    }

    @Test
    void testClearFilteredCollectionCache() {
        // Set up a collection and create a filtered version using field extraction
        List<JsonNode> collection = Arrays.asList(
            createTestObject("name1", "value1"),
            createTestObject("name2", "value2"));
        context.registerCollection("testCollection", collection);

        List<JsonNode> filterValues = Collections.singletonList(mapper.valueToTree("value1"));

        // Create filtered collection using field extraction (should be cached)
        List<JsonNode> filtered1 = context.getOrCreateFilteredCollection("testCollection[*].value", null, filterValues);
        List<JsonNode> filtered2 = context.getOrCreateFilteredCollection("testCollection[*].value", null, filterValues);

        // Should be the same instance (cached)
        assertThat(filtered1).isSameAs(filtered2);

        // Clear cache
        context.clearFilteredCollectionCache();

        // Should create a new instance
        List<JsonNode> filtered3 = context.getOrCreateFilteredCollection("testCollection[*].value", null, filterValues);
        assertThat(filtered1)
            .isNotSameAs(filtered3)
            .hasSameSizeAs(filtered3); // But same content
    }

    @Test
    void testWildcardReferenceResolution() {
        List<JsonNode> collection = Arrays.asList(
            mapper.valueToTree("item1"),
            mapper.valueToTree("item2"),
            mapper.valueToTree("item3"));
        context.registerCollection("testCollection", collection);

        JsonNode result = context.resolveReferenceWithFiltering("testCollection[*]", null, null, null, false);

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).isIn("item1", "item2", "item3");
    }

    @Test
    void testSequentialReferenceResolution() {
        List<JsonNode> collection = Arrays.asList(
            mapper.valueToTree("item1"),
            mapper.valueToTree("item2"),
            mapper.valueToTree("item3"));
        context.registerCollection("testCollection", collection);

        // Sequential access should return items in order
        JsonNode result1 = context.resolveReferenceWithFiltering("testCollection[*]", null, null, mockReferenceNode,
            true);
        JsonNode result2 = context.resolveReferenceWithFiltering("testCollection[*]", null, null, mockReferenceNode,
            true);
        JsonNode result3 = context.resolveReferenceWithFiltering("testCollection[*]", null, null, mockReferenceNode,
            true);
        JsonNode result4 = context.resolveReferenceWithFiltering("testCollection[*]", null, null, mockReferenceNode,
            true);

        assertThat(result1.asText()).isEqualTo("item1");
        assertThat(result2.asText()).isEqualTo("item2");
        assertThat(result3.asText()).isEqualTo("item3");
        assertThat(result4.asText()).isEqualTo("item1"); // Wrap around
    }

    @Test
    void testThisReferenceResolution() {
        ObjectNode currentItem = mapper.createObjectNode();
        currentItem.put("name", "John");
        currentItem.put("age", 30);

        JsonNode nameResult = context.resolveReferenceWithFiltering("this.name", currentItem, null, null, false);
        JsonNode ageResult = context.resolveReferenceWithFiltering("this.age", currentItem, null, null, false);
        JsonNode missingResult = context.resolveReferenceWithFiltering("this.missing", currentItem, null, null, false);

        assertThat(nameResult.asText()).isEqualTo("John");
        assertThat(ageResult.asInt()).isEqualTo(30);
        assertThat(missingResult.isNull() || missingResult.isMissingNode()).isTrue();
    }

    @Test
    void testPickReferenceResolution() {
        ObjectNode pickValue = mapper.createObjectNode();
        pickValue.put("name", "John");
        pickValue.put("age", 30);

        context.registerPick("testPick", pickValue);

        JsonNode directResult = context.resolveReferenceWithFiltering("testPick", null, null, null, false);
        JsonNode fieldResult = context.resolveReferenceWithFiltering("testPick.name", null, null, null, false);

        assertThat(directResult).isEqualTo(pickValue);
        assertThat(fieldResult.asText()).isEqualTo("John");
    }

    @Test
    void testTagReferenceResolution() {
        List<JsonNode> taggedCollection = Arrays.asList(
            createTestObject("name1", "value1"),
            createTestObject("name2", "value2"));
        context.registerTaggedCollection("testTag", taggedCollection);

        JsonNode result = context.resolveReferenceWithFiltering("byTag[testTag]", null, null, null, false);

        assertThat(result).isNotNull();
        assertThat(result.isObject()).isTrue();
    }

    @Test
    void testTagReferenceWithThisExpression() {
        ObjectNode currentItem = mapper.createObjectNode();
        currentItem.put("category", "electronics");

        List<JsonNode> taggedCollection = Arrays.asList(
            createTestObject("product1", "phone"),
            createTestObject("product2", "laptop"));
        context.registerTaggedCollection("electronics", taggedCollection);

        JsonNode result = context.resolveReferenceWithFiltering("byTag[this.category]", currentItem, null, null, false);

        assertThat(result).isNotNull();
        assertThat(result.isObject()).isTrue();
    }

    @Test
    void testTagReferenceWithFieldExtraction() {
        List<JsonNode> taggedCollection = Arrays.asList(
            createTestObject("name1", "value1"),
            createTestObject("name2", "value2"));
        context.registerTaggedCollection("testTag", taggedCollection);

        JsonNode result = context.resolveReferenceWithFiltering("byTag[testTag].name", null, null, null, false);

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).isIn("name1", "name2");
    }

    @Test
    void testIndexedReferenceResolution() {
        List<JsonNode> collection = Arrays.asList(
            createTestObject("name1", "value1"),
            createTestObject("name2", "value2"),
            createTestObject("name3", "value3"));
        context.registerCollection("testCollection", collection);

        JsonNode result = context.resolveReferenceWithFiltering("testCollection[1]", null, null, null, false);

        assertThat(result).isNotNull();
        assertThat(result.get("name").asText()).isEqualTo("name2");
    }

    @Test
    void testIndexedReferenceWithFieldExtraction() {
        List<JsonNode> collection = Arrays.asList(
            createTestObject("name1", "value1"),
            createTestObject("name2", "value2"));
        context.registerCollection("testCollection", collection);

        JsonNode result = context.resolveReferenceWithFiltering("testCollection[1].name", null, null, null, false);

        assertThat(result.asText()).isEqualTo("name2");
    }

    @Test
    void testInvalidFieldExtractionThrowsException() {
        List<JsonNode> collection = Arrays.asList(
            createTestObject("name1", "value1"),
            createTestObject("name2", "value2"));
        context.registerCollection("testCollection", collection);

        // Old field extraction syntax should throw exception (validation bug)
        assertThatThrownBy(() -> context.resolveReferenceWithFiltering("testCollection[name]", null, null, null, false))
            .isInstanceOf(InvalidReferenceException.class)
            .hasMessageContaining("Invalid reference pattern: 'testCollection[name]'");
    }

    @Test
    void testArrayFieldReference() {
        List<JsonNode> collection = Arrays.asList(
            createTestObject("name1", "value1"),
            createTestObject("name2", "value2"));
        context.registerCollection("testCollection", collection);

        JsonNode result = context.resolveReferenceWithFiltering("testCollection[*].name", null, null, null, false);

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).isIn("name1", "name2");
    }

    @Test
    void testReferenceResolutionWithFiltering() {
        List<JsonNode> collection = Arrays.asList(
            createTestObject("name1", "value1"),
            createTestObject("name2", "value2"),
            createTestObject("name3", "value3"));
        context.registerCollection("testCollection", collection);

        // Filter by field values using correct field extraction syntax
        List<JsonNode> filterValues = Arrays.asList(
            mapper.valueToTree("value1"),
            mapper.valueToTree("value2"));

        JsonNode result = context.resolveReferenceWithFiltering("testCollection[*].value", null, filterValues, null,
            false);

        // Field extraction returns the field value, not the full object
        assertThat(result).isNotNull();
        assertThat(result.asText()).isEqualTo("value3");
    }

    @Test
    void testReferenceResolutionWithAllFiltered() {
        List<JsonNode> collection = List.of(createTestObject("name1", "value1"));
        context.registerCollection("testCollection", collection);

        List<JsonNode> filterValues = Collections.singletonList(
            mapper.valueToTree("value1"));

        JsonNode result = context.resolveReferenceWithFiltering("testCollection[*].value", null, filterValues, null,
            false);

        assertThat(result.isNull()).isTrue();
    }

    @Test
    void testReferenceResolutionWithAllFilteredThrowException() {
        GenerationContext throwContext = new GenerationContext(generatorRegistry, random, 100,
            FilteringBehavior.THROW_EXCEPTION);

        List<JsonNode> collection = List.of(createTestObject("name1", "value1"));
        throwContext.registerCollection("testCollection", collection);

        List<JsonNode> filterValues = Collections.singletonList(
            mapper.valueToTree("value1"));

        assertThatThrownBy(() -> throwContext.resolveReferenceWithFiltering("testCollection[*].value", null,
            filterValues, null, false)).isInstanceOf(FilteringException.class);
    }

    @Test
    void testGenerateWithFilterReturnNull() {
        when(mockGenerator.supportsFiltering()).thenReturn(true);
        when(mockGenerator.generateWithFilter(any(), anyList())).thenThrow(new FilteringException("All filtered"));

        ObjectNode options = mapper.createObjectNode();
        List<JsonNode> filterValues = Collections.singletonList(mapper.valueToTree("filtered"));

        JsonNode result = context.generateWithFilter(mockGenerator, options, null, filterValues);

        assertThat(result.isNull()).isTrue();
    }

    @Test
    void testGenerateWithFilterThrowException() {
        GenerationContext throwContext = new GenerationContext(generatorRegistry, random, 100,
            FilteringBehavior.THROW_EXCEPTION);

        when(mockGenerator.supportsFiltering()).thenReturn(true);
        when(mockGenerator.generateWithFilter(any(), anyList())).thenThrow(new FilteringException("All filtered"));

        ObjectNode options = mapper.createObjectNode();
        List<JsonNode> filterValues = Collections.singletonList(mapper.valueToTree("filtered"));

        assertThatThrownBy(() -> throwContext.generateWithFilter(mockGenerator, options, null, filterValues))
            .isInstanceOf(FilteringException.class);
    }

    @Test
    void testGenerateWithFilterSuccess() {
        JsonNode expectedResult = mapper.valueToTree("generated");
        when(mockGenerator.supportsFiltering()).thenReturn(true);
        when(mockGenerator.generateWithFilter(any(), anyList())).thenReturn(expectedResult);

        ObjectNode options = mapper.createObjectNode();
        List<JsonNode> filterValues = Collections.singletonList(mapper.valueToTree("filtered"));

        JsonNode result = context.generateWithFilter(mockGenerator, options, null, filterValues);

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void testGenerateWithFilterAtPath() {
        JsonNode expectedResult = mapper.valueToTree("generated");
        when(mockGenerator.supportsFiltering()).thenReturn(true);
        when(mockGenerator.generateAtPathWithFilter(any(), eq("testPath"), anyList())).thenReturn(expectedResult);

        ObjectNode options = mapper.createObjectNode();
        List<JsonNode> filterValues = Collections.singletonList(mapper.valueToTree("filtered"));

        JsonNode result = context.generateWithFilter(mockGenerator, options, "testPath", filterValues);

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void testEdgeCaseCollectionsReturnNull() {
        // Non-existent collection should return null
        JsonNode result1 = context.resolveReferenceWithFiltering("nonExistent[*]", null, null, null, false);
        assertThat(result1.isNull()).isTrue();

        // Empty collection should return null
        List<JsonNode> emptyCollection = List.of();
        context.registerCollection("emptyCollection", emptyCollection);
        JsonNode result2 = context.resolveReferenceWithFiltering("emptyCollection[*]", null, null, null, false);
        assertThat(result2.isNull()).isTrue();
    }

    @Test
    void testInvalidReferencePatternThrowsException() {
        List<JsonNode> collection = Collections.singletonList(mapper.valueToTree("item1"));
        context.registerCollection("testCollection", collection);

        // Simple collection name without brackets should throw exception (validation
        // bug)
        assertThatThrownBy(() -> context.resolveReferenceWithFiltering("testCollection", null, null, null, false))
            .isInstanceOf(InvalidReferenceException.class)
            .hasMessageContaining("Invalid reference pattern: 'testCollection'");
    }

    @Test
    void testFilteredCollectionCaching() {
        List<JsonNode> collection = Arrays.asList(
            createTestObject("name1", "value1"),
            createTestObject("name2", "value2"));
        context.registerCollection("testCollection", collection);

        List<JsonNode> filterValues = Collections.singletonList(mapper.valueToTree("value1"));

        // First call should create and cache using field extraction
        List<JsonNode> filtered1 = context.getOrCreateFilteredCollection("testCollection[*].value", null, filterValues);

        // Second call should return cached version
        List<JsonNode> filtered2 = context.getOrCreateFilteredCollection("testCollection[*].value", null, filterValues);

        assertThat(filtered1)
            .isSameAs(filtered2)
            .hasSize(1);
        assertThat(filtered1.get(0).get("name").asText()).isEqualTo("name2");
    }

    // Helper method to create test objects
    private ObjectNode createTestObject(String name, String value) {
        ObjectNode obj = mapper.createObjectNode();
        obj.put("name", name);
        obj.put("value", value);
        return obj;
    }
}
