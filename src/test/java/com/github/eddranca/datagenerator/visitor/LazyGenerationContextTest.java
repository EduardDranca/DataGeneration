package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LazyGenerationContextTest {

    @Mock
    private GeneratorRegistry mockGeneratorRegistry;

    @Mock
    private Random mockRandom;

    @Mock
    private Generator mockGenerator;

    @Mock
    private Sequential mockSequentialNode;


    @Mock
    private LazyItemProxy mockLazyItem;

    private LazyGenerationContext context;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        context = new LazyGenerationContext(mockGeneratorRegistry, mockRandom, 5, FilteringBehavior.RETURN_NULL);
        mapper = new ObjectMapper();
    }

    @Test
    void testConstructorWithDefaults() {
        LazyGenerationContext defaultContext = new LazyGenerationContext(mockGeneratorRegistry, mockRandom);

        assertThat(defaultContext.getGeneratorRegistry()).isEqualTo(mockGeneratorRegistry);
        assertThat(defaultContext.getRandom()).isEqualTo(mockRandom);
        assertThat(defaultContext.getMapper()).isNotNull();
        // LazyGenerationContext is used for memory optimization
    }

    @Test
    void testConstructorWithCustomSettings() {
        LazyGenerationContext customContext = new LazyGenerationContext(
            mockGeneratorRegistry,
            mockRandom,
            10,
            FilteringBehavior.THROW_EXCEPTION
        );

        assertThat(customContext.getGeneratorRegistry()).isEqualTo(mockGeneratorRegistry);
        assertThat(customContext.getRandom()).isEqualTo(mockRandom);
        // LazyGenerationContext is used for memory optimization
    }

    @Test
    void testSetReferencedPaths() {
        Map<String, Set<String>> paths = Map.of(
            "users", Set.of("id", "name"),
            "posts", Set.of("title")
        );

        // This method should not throw an exception
        context.setReferencedPaths(paths);

        // The actual behavior will be tested through integration tests
        // since getReferencedPaths is private
    }

    @Test
    void testRegisterLazyCollection() {
        List<LazyItemProxy> collection = List.of(mockLazyItem);
        context.registerCollection("testCollection", collection);

        Map<String, List<LazyItemProxy>> collections = context.getNamedCollections();
        assertThat(collections).containsKey("testCollection");
        assertThat(collections.get("testCollection")).hasSize(1);
        assertThat(collections.get("testCollection").get(0)).isEqualTo(mockLazyItem);
    }

    @Test
    void testRegisterLazyCollectionMergesExisting() {
        LazyItemProxy item1 = mock(LazyItemProxy.class);
        LazyItemProxy item2 = mock(LazyItemProxy.class);

        List<LazyItemProxy> collection1 = List.of(item1);
        List<LazyItemProxy> collection2 = List.of(item2);

        context.registerCollection("testCollection", collection1);
        context.registerCollection("testCollection", collection2);

        Map<String, List<LazyItemProxy>> collections = context.getNamedCollections();
        assertThat(collections).containsKey("testCollection");

        // Should be merged into a single list
        List<LazyItemProxy> merged = collections.get("testCollection");
        assertThat(merged).hasSize(2)
            .contains(item1, item2);
    }

    @Test
    void testRegisterLazyReferenceCollection() {
        List<LazyItemProxy> collection = List.of(mockLazyItem);
        context.registerReferenceCollection("refCollection", collection);

        // Should be accessible via getCollection
        ObjectNode refValue = mapper.createObjectNode().put("value", "refValue");
        when(mockLazyItem.getMaterializedCopy()).thenReturn(refValue);

        List<JsonNode> retrieved = context.getCollection("refCollection");
        assertThat(retrieved).hasSize(1);
        assertThat(retrieved.get(0).get("value").asText()).isEqualTo("refValue");
    }


    @Test
    void testGetCollectionMaterializesLazyCollection() {
        ObjectNode expectedValue = mapper.createObjectNode().put("value", "lazyValue");
        when(mockLazyItem.getMaterializedCopy()).thenReturn(expectedValue);

        List<LazyItemProxy> collection = List.of(mockLazyItem);
        context.registerCollection("testCollection", collection);

        List<JsonNode> retrieved = context.getCollection("testCollection");
        assertThat(retrieved).hasSize(1);
        assertThat(retrieved.get(0)).isEqualTo(expectedValue);
    }

    @Test
    void testGetCollectionCachesResults() {
        ObjectNode expectedValue = mapper.createObjectNode().put("value", "cachedValue");
        when(mockLazyItem.getMaterializedCopy()).thenReturn(expectedValue);

        List<LazyItemProxy> collection = List.of(mockLazyItem);
        context.registerCollection("testCollection", collection);

        // First call should materialize
        List<JsonNode> first = context.getCollection("testCollection");
        // Second call should return cached result
        List<JsonNode> second = context.getCollection("testCollection");

        assertThat(first).isSameAs(second); // Should be the same cached instance
    }

    @Test
    void testGetCollectionReturnsEmptyForNonExistent() {
        List<JsonNode> retrieved = context.getCollection("nonExistent");
        assertThat(retrieved).isEmpty();
    }


    @Test
    void testRegisterAndGetPick() {
        JsonNode pickValue = mapper.valueToTree("pickValue");

        context.registerPick("testPick", pickValue);

        JsonNode retrieved = context.getNamedPick("testPick");
        assertThat(retrieved).isEqualTo(pickValue);
    }

    @Test
    void testGetNamedPickReturnsNullForNonExistent() {
        JsonNode retrieved = context.getNamedPick("nonExistentPick");
        assertThat(retrieved).isNull();
    }

    @Test
    void testReferenceCollectionTakesPrecedenceOverNamedCollection() {
        ObjectNode refValue = mapper.createObjectNode().put("value", "refValue");

        LazyItemProxy namedItem = mock(LazyItemProxy.class);
        LazyItemProxy refItem = mock(LazyItemProxy.class);
        when(refItem.getMaterializedCopy()).thenReturn(refValue);

        List<LazyItemProxy> namedCollection = List.of(namedItem);
        List<LazyItemProxy> refCollection = List.of(refItem);

        context.registerCollection("testCollection", namedCollection);
        context.registerReferenceCollection("testCollection", refCollection);

        List<JsonNode> retrieved = context.getCollection("testCollection");
        assertThat(retrieved).hasSize(1);
        assertThat(retrieved.get(0)).isEqualTo(refValue);
    }

    @Test
    void testInheritedMethodsFromAbstractGenerationContext() {
        // Test that inherited methods work correctly
        JsonNode item1 = mapper.valueToTree("item1");
        JsonNode item2 = mapper.valueToTree("item2");
        List<JsonNode> collection = List.of(item1, item2);

        when(mockRandom.nextInt(2)).thenReturn(1);

        JsonNode result = context.getElementFromCollection(collection, mockSequentialNode, false);
        assertThat(result).isEqualTo(item2);
    }

    @Test
    void testSequentialIndexing() {
        JsonNode item1 = mapper.valueToTree("item1");
        JsonNode item2 = mapper.valueToTree("item2");
        List<JsonNode> collection = List.of(item1, item2);

        JsonNode result1 = context.getElementFromCollection(collection, mockSequentialNode, true);
        JsonNode result2 = context.getElementFromCollection(collection, mockSequentialNode, true);
        JsonNode result3 = context.getElementFromCollection(collection, mockSequentialNode, true);

        assertThat(result1).isEqualTo(item1);
        assertThat(result2).isEqualTo(item2);
        assertThat(result3).isEqualTo(item1); // Wraps around
    }

    @Test
    void testHandleFilteringFailureWithReturnNull() {
        LazyGenerationContext nullContext = new LazyGenerationContext(
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
        LazyGenerationContext exceptionContext = new LazyGenerationContext(
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

    @Test
    void testApplyFiltering() {
        JsonNode item1 = mapper.valueToTree("keep");
        JsonNode item2 = mapper.valueToTree("filter");
        JsonNode item3 = mapper.valueToTree("keep");
        List<JsonNode> collection = List.of(item1, item2, item3);

        JsonNode filterValue = mapper.valueToTree("filter");
        List<JsonNode> filterValues = List.of(filterValue);

        List<JsonNode> result = context.applyFiltering(collection, "", filterValues);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(item1);
        assertThat(result.get(1)).isEqualTo(item3);
    }

    @Test
    void testApplyFilteringOnField() {
        JsonNode item1 = mapper.createObjectNode().put("name", "keep1");
        JsonNode item2 = mapper.createObjectNode().put("name", "filter");
        JsonNode item3 = mapper.createObjectNode().put("name", "keep2");
        List<JsonNode> collection = List.of(item1, item2, item3);

        JsonNode filterValue = mapper.valueToTree("filter");
        List<JsonNode> filterValues = List.of(filterValue);

        List<JsonNode> result = context.applyFilteringOnField(collection, "name", filterValues);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("name").asText()).isEqualTo("keep1");
        assertThat(result.get(1).get("name").asText()).isEqualTo("keep2");
    }
}
