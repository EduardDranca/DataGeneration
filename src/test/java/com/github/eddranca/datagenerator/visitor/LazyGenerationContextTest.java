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

import java.util.HashMap;
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
    private LazyItemCollection mockLazyCollection;

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
        assertThat(defaultContext.isMemoryOptimizationEnabled()).isTrue();
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
        assertThat(customContext.isMemoryOptimizationEnabled()).isTrue();
    }

    @Test
    void testIsMemoryOptimizationEnabled() {
        assertThat(context.isMemoryOptimizationEnabled()).isTrue();
    }

    @Test
    void testRegisterLazyCollection() {
        context.registerLazyCollection("testCollection", mockLazyCollection);

        Map<String, List<LazyItemProxy>> collections = context.getLazyNamedCollections();
        assertThat(collections).containsKey("testCollection");
        assertThat(collections.get("testCollection")).isEqualTo(mockLazyCollection);
    }

    @Test
    void testRegisterLazyCollectionMergesExisting() {
        LazyItemCollection collection1 = mock(LazyItemCollection.class);
        LazyItemCollection collection2 = mock(LazyItemCollection.class);

        context.registerLazyCollection("testCollection", collection1);
        context.registerLazyCollection("testCollection", collection2);

        Map<String, List<LazyItemProxy>> collections = context.getLazyNamedCollections();
        assertThat(collections).containsKey("testCollection");

        // Should be a CompositeLazyItemCollection now
        List<LazyItemProxy> merged = collections.get("testCollection");
        assertThat(merged).isInstanceOf(CompositeLazyItemCollection.class);
    }

    @Test
    void testRegisterLazyReferenceCollection() {
        context.registerLazyReferenceCollection("refCollection", mockLazyCollection);

        // Should be accessible via getCollection
        ObjectNode refValue = mapper.createObjectNode().put("value", "refValue");
        when(mockLazyItem.getMaterializedCopy()).thenReturn(refValue);
        when(mockLazyCollection.iterator()).thenReturn(List.of(mockLazyItem).iterator());

        List<JsonNode> retrieved = context.getCollection("refCollection");
        assertThat(retrieved).hasSize(1);
        assertThat(retrieved.get(0).get("value").asText()).isEqualTo("refValue");
    }

    @Test
    void testRegisterLazyTaggedCollection() {
        context.registerLazyTaggedCollection("testTag", mockLazyCollection);

        ObjectNode taggedValue = mapper.createObjectNode().put("value", "taggedValue");
        when(mockLazyItem.getMaterializedCopy()).thenReturn(taggedValue);
        when(mockLazyCollection.iterator()).thenReturn(List.of(mockLazyItem).iterator());

        List<JsonNode> retrieved = context.getTaggedCollection("testTag");
        assertThat(retrieved).hasSize(1);
        assertThat(retrieved.get(0).get("value").asText()).isEqualTo("taggedValue");
    }

    @Test
    void testGetCollectionMaterializesLazyCollection() {
        ObjectNode expectedValue = mapper.createObjectNode().put("value", "lazyValue");
        when(mockLazyItem.getMaterializedCopy()).thenReturn(expectedValue);
        when(mockLazyCollection.iterator()).thenReturn(List.of(mockLazyItem).iterator());

        context.registerLazyCollection("testCollection", mockLazyCollection);

        List<JsonNode> retrieved = context.getCollection("testCollection");
        assertThat(retrieved).hasSize(1);
        assertThat(retrieved.get(0)).isEqualTo(expectedValue);
    }

    @Test
    void testGetCollectionCachesResults() {
        ObjectNode expectedValue = mapper.createObjectNode().put("value", "cachedValue");
        when(mockLazyItem.getMaterializedCopy()).thenReturn(expectedValue);
        when(mockLazyCollection.iterator()).thenReturn(List.of(mockLazyItem).iterator());

        context.registerLazyCollection("testCollection", mockLazyCollection);

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
    void testGetTaggedCollectionMaterializesLazyCollection() {
        ObjectNode expectedValue = mapper.createObjectNode().put("value", "taggedValue");
        when(mockLazyItem.getMaterializedCopy()).thenReturn(expectedValue);
        when(mockLazyCollection.iterator()).thenReturn(List.of(mockLazyItem).iterator());

        context.registerLazyTaggedCollection("testTag", mockLazyCollection);

        List<JsonNode> retrieved = context.getTaggedCollection("testTag");
        assertThat(retrieved).hasSize(1);
        assertThat(retrieved.get(0)).isEqualTo(expectedValue);
    }

    @Test
    void testGetTaggedCollectionCachesResults() {
        ObjectNode expectedValue = mapper.createObjectNode().put("value", "cachedTagValue");
        when(mockLazyItem.getMaterializedCopy()).thenReturn(expectedValue);
        when(mockLazyCollection.iterator()).thenReturn(List.of(mockLazyItem).iterator());

        context.registerLazyTaggedCollection("testTag", mockLazyCollection);

        // First call should materialize
        List<JsonNode> first = context.getTaggedCollection("testTag");
        // Second call should return cached result
        List<JsonNode> second = context.getTaggedCollection("testTag");

        assertThat(first).isSameAs(second); // Should be the same cached instance
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
        assertThat(retrieved).isEqualTo(pickValue);
    }

    @Test
    void testGetNamedPickReturnsNullForNonExistent() {
        JsonNode retrieved = context.getNamedPick("nonExistentPick");
        assertThat(retrieved).isNull();
    }

    @Test
    void testGetNamedCollectionsMaterializesAllCollections() {
        ObjectNode value1 = mapper.createObjectNode().put("value", "value1");
        ObjectNode value2 = mapper.createObjectNode().put("value", "value2");

        LazyItemProxy item1 = mock(LazyItemProxy.class);
        LazyItemProxy item2 = mock(LazyItemProxy.class);
        when(item1.getMaterializedCopy()).thenReturn(value1);
        when(item2.getMaterializedCopy()).thenReturn(value2);

        LazyItemCollection collection1 = mock(LazyItemCollection.class);
        LazyItemCollection collection2 = mock(LazyItemCollection.class);
        when(collection1.iterator()).thenReturn(List.of(item1).iterator());
        when(collection2.iterator()).thenReturn(List.of(item2).iterator());

        context.registerLazyCollection("collection1", collection1);
        context.registerLazyCollection("collection2", collection2);

        Map<String, List<JsonNode>> collections = context.getNamedCollections();

        assertThat(collections).hasSize(2);
        assertThat(collections.get("collection1")).hasSize(1);
        assertThat(collections.get("collection1").get(0)).isEqualTo(value1);
        assertThat(collections.get("collection2")).hasSize(1);
        assertThat(collections.get("collection2").get(0)).isEqualTo(value2);
    }

    @Test
    void testEagerCollectionRegistrationThrowsException() {
        JsonNode item = mapper.valueToTree("item");
        List<JsonNode> collection = List.of(item);

        assertThatThrownBy(() -> context.registerCollection("test", collection))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("Lazy generation context does not support eager collection registration");
    }

    @Test
    void testEagerReferenceCollectionRegistrationThrowsException() {
        JsonNode item = mapper.valueToTree("item");
        List<JsonNode> collection = List.of(item);

        assertThatThrownBy(() -> context.registerReferenceCollection("test", collection))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("Lazy generation context does not support eager collection registration");
    }

    @Test
    void testEagerTaggedCollectionRegistrationThrowsException() {
        JsonNode item = mapper.valueToTree("item");
        List<JsonNode> collection = List.of(item);

        assertThatThrownBy(() -> context.registerTaggedCollection("test", collection))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("Lazy generation context does not support eager collection registration");
    }

    @Test
    void testEnableMemoryOptimization() {
        Map<String, Set<String>> referencedPaths = new HashMap<>();
        referencedPaths.put("users", Set.of("name", "email"));
        referencedPaths.put("companies", Set.of("name"));

        context.enableMemoryOptimization(referencedPaths);

        assertThat(context.getReferencedPaths("users")).containsExactlyInAnyOrder("name", "email");
        assertThat(context.getReferencedPaths("companies")).containsExactly("name");
        assertThat(context.getReferencedPaths("nonExistent")).isEmpty();
    }

    @Test
    void testSetAndGetReferencedPaths() {
        Set<String> paths = Set.of("field1", "field2");

        context.setReferencedPaths("testCollection", paths);

        assertThat(context.getReferencedPaths("testCollection")).isEqualTo(paths);
    }

    @Test
    void testGetReferencedPathsReturnsEmptyForNonExistent() {
        assertThat(context.getReferencedPaths("nonExistent")).isEmpty();
    }

    @Test
    void testGetReferencedPathsWithNullReferencedPaths() {
        // Before enableMemoryOptimization is called, referencedPaths is null
        assertThat(context.getReferencedPaths("anyCollection")).isEmpty();
    }

    @Test
    void testReferenceCollectionTakesPrecedenceOverNamedCollection() {
        ObjectNode refValue = mapper.createObjectNode().put("value", "refValue");

        LazyItemProxy refItem = mock(LazyItemProxy.class);
        when(refItem.getMaterializedCopy()).thenReturn(refValue);

        LazyItemCollection namedCollection = mock(LazyItemCollection.class);
        LazyItemCollection refCollection = mock(LazyItemCollection.class);
        when(refCollection.iterator()).thenReturn(List.of(refItem).iterator());

        context.registerLazyCollection("testCollection", namedCollection);
        context.registerLazyReferenceCollection("testCollection", refCollection);

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
    void testHandleReferenceFailureWithReturnNull() {
        LazyGenerationContext nullContext = new LazyGenerationContext(
            mockGeneratorRegistry,
            mockRandom,
            5,
            FilteringBehavior.RETURN_NULL
        );

        JsonNode result = nullContext.handleReferenceFailure("Reference failure");

        assertThat(result.isNull()).isTrue();
    }

    @Test
    void testHandleReferenceFailureWithThrowException() {
        LazyGenerationContext exceptionContext = new LazyGenerationContext(
            mockGeneratorRegistry,
            mockRandom,
            5,
            FilteringBehavior.THROW_EXCEPTION
        );

        assertThatThrownBy(() -> exceptionContext.handleReferenceFailure("Reference failure"))
            .isInstanceOf(FilteringException.class)
            .hasMessage("Reference resolution failed: Reference failure");
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
