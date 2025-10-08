package com.github.eddranca.datagenerator.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.exception.FilteringException;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilteringGeneratorAdapterTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Faker faker = new Faker();

    @Mock
    private Generator mockGenerator;
    private FilteringGeneratorAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new FilteringGeneratorAdapter(mockGenerator, 3);
    }

    @Test
    void testGenerateDelegatesToUnderlying() {
        JsonNode options = mapper.valueToTree("test");
        GeneratorContext context = new GeneratorContext(faker, options, mapper);
        JsonNode expected = mapper.valueToTree("result");
        when(mockGenerator.generate(context)).thenReturn(expected);

        JsonNode result = adapter.generate(context);

        assertThat(result).isEqualTo(expected);
        verify(mockGenerator).generate(context);
    }

    @Test
    void testGenerateAtPathDelegatesToUnderlying() {
        JsonNode options = mapper.valueToTree("test");
        GeneratorContext context = new GeneratorContext(faker, options, mapper);
        JsonNode expected = mapper.valueToTree("result");
        when(mockGenerator.generateAtPath(context, "field")).thenReturn(expected);

        JsonNode result = adapter.generateAtPath(context, "field");

        assertThat(result).isEqualTo(expected);
        verify(mockGenerator).generateAtPath(context, "field");
    }

    @Test
    void testSupportsFilteringAlwaysTrue() {
        assertThat(adapter.supportsFiltering()).isTrue();
    }

    @Test
    void testGenerateWithFilterNativeSupport() {
        JsonNode options = mapper.valueToTree("test");
        GeneratorContext context = new GeneratorContext(faker, options, mapper);
        List<JsonNode> filterValues = Collections.singletonList(mapper.valueToTree("filtered"));
        JsonNode expected = mapper.valueToTree("result");

        when(mockGenerator.supportsFiltering()).thenReturn(true);
        when(mockGenerator.generateWithFilter(context, filterValues)).thenReturn(expected);

        JsonNode result = adapter.generateWithFilter(context, filterValues);

        assertThat(result).isEqualTo(expected);
        verify(mockGenerator).generateWithFilter(context, filterValues);
        verify(mockGenerator, never()).generate(any());
    }

    @Test
    void testGenerateWithFilterRetryLogicSuccess() {
        JsonNode options = mapper.valueToTree("test");
        GeneratorContext context = new GeneratorContext(faker, options, mapper);
        List<JsonNode> filterValues = Collections.singletonList(mapper.valueToTree("filtered"));
        JsonNode filteredResult = mapper.valueToTree("filtered");
        JsonNode validResult = mapper.valueToTree("valid");

        when(mockGenerator.supportsFiltering()).thenReturn(false);
        when(mockGenerator.generate(context))
            .thenReturn(filteredResult)  // First attempt - filtered
            .thenReturn(validResult);    // Second attempt - valid

        JsonNode result = adapter.generateWithFilter(context, filterValues);

        assertThat(result).isEqualTo(validResult);
        verify(mockGenerator, times(2)).generate(context);
    }

    @Test
    void testGenerateWithFilterRetryLogicMaxRetriesThrowsException() {
        JsonNode options = mapper.valueToTree("test");
        GeneratorContext context = new GeneratorContext(faker, options, mapper);
        List<JsonNode> filterValues = Collections.singletonList(mapper.valueToTree("filtered"));
        JsonNode filteredResult = mapper.valueToTree("filtered");

        when(mockGenerator.supportsFiltering()).thenReturn(false);
        when(mockGenerator.generate(context)).thenReturn(filteredResult); // Always return filtered value

        assertThatThrownBy(() -> adapter.generateWithFilter(context, filterValues))
            .isInstanceOf(FilteringException.class)
            .hasMessageContaining("Generator filtering failed to generate a valid value after 3 retries");

        verify(mockGenerator, times(3)).generate(context); // Should retry 3 times (maxFilteringRetries)
    }


    @Test
    void testGenerateWithFilterNoFiltering() {
        JsonNode options = mapper.valueToTree("test");
        GeneratorContext context = new GeneratorContext(faker, options, mapper);
        JsonNode expected = mapper.valueToTree("result");

        when(mockGenerator.supportsFiltering()).thenReturn(false);
        when(mockGenerator.generate(context)).thenReturn(expected);

        JsonNode result = adapter.generateWithFilter(context, null);

        assertThat(result).isEqualTo(expected);
        verify(mockGenerator, times(1)).generate(context);
    }

    @Test
    void testGenerateWithFilterEmptyFiltering() {
        JsonNode options = mapper.valueToTree("test");
        GeneratorContext context = new GeneratorContext(faker, options, mapper);
        JsonNode expected = mapper.valueToTree("result");

        when(mockGenerator.supportsFiltering()).thenReturn(false);
        when(mockGenerator.generate(context)).thenReturn(expected);

        JsonNode result = adapter.generateWithFilter(context, Collections.emptyList());

        assertThat(result).isEqualTo(expected);
        verify(mockGenerator, times(1)).generate(context);
    }

    @Test
    void testGenerateAtPathWithFilterNativeSupport() {
        JsonNode options = mapper.valueToTree("test");
        GeneratorContext context = new GeneratorContext(faker, options, mapper);
        List<JsonNode> filterValues = Collections.singletonList(mapper.valueToTree("filtered"));
        JsonNode expected = mapper.valueToTree("result");

        when(mockGenerator.supportsFiltering()).thenReturn(true);
        when(mockGenerator.generateAtPathWithFilter(context, "field", filterValues)).thenReturn(expected);

        JsonNode result = adapter.generateAtPathWithFilter(context, "field", filterValues);

        assertThat(result).isEqualTo(expected);
        verify(mockGenerator).generateAtPathWithFilter(context, "field", filterValues);
        verify(mockGenerator, never()).generateAtPath(any(), any());
    }

    @Test
    void testGenerateAtPathWithFilterRetryLogic() {
        JsonNode options = mapper.valueToTree("test");
        GeneratorContext context = new GeneratorContext(faker, options, mapper);
        List<JsonNode> filterValues = Collections.singletonList(mapper.valueToTree("filtered"));
        JsonNode filteredResult = mapper.valueToTree("filtered");
        JsonNode validResult = mapper.valueToTree("valid");

        when(mockGenerator.supportsFiltering()).thenReturn(false);
        when(mockGenerator.generateAtPath(context, "field"))
            .thenReturn(filteredResult)  // First attempt - filtered
            .thenReturn(validResult);    // Second attempt - valid

        JsonNode result = adapter.generateAtPathWithFilter(context, "field", filterValues);

        assertThat(result).isEqualTo(validResult);
        verify(mockGenerator, times(2)).generateAtPath(context, "field");
    }

    @Test
    void testGenerateAtPathWithFilterRetryLogicMaxRetriesThrowsException() {
        JsonNode options = mapper.valueToTree("test");
        GeneratorContext context = new GeneratorContext(faker, options, mapper);
        List<JsonNode> filterValues = Collections.singletonList(mapper.valueToTree("filtered"));
        JsonNode filteredResult = mapper.valueToTree("filtered");

        when(mockGenerator.supportsFiltering()).thenReturn(false);
        when(mockGenerator.generateAtPath(context, "field")).thenReturn(filteredResult);

        assertThatThrownBy(() -> adapter.generateAtPathWithFilter(context, "field", filterValues))
            .isInstanceOf(FilteringException.class)
            .hasMessageContaining("Generator path filtering failed to generate a valid value after 3 retries");

        verify(mockGenerator, times(3)).generateAtPath(context, "field");
    }

    @Test
    void testFilteringWithNullValues() {
        JsonNode options = mapper.valueToTree("test");
        GeneratorContext context = new GeneratorContext(faker, options, mapper);
        List<JsonNode> filterValues = Collections.singletonList(mapper.nullNode());

        when(mockGenerator.supportsFiltering()).thenReturn(false);
        when(mockGenerator.generate(context)).thenReturn(mapper.nullNode());

        assertThatThrownBy(() -> adapter.generateWithFilter(context, filterValues))
            .isInstanceOf(FilteringException.class)
            .hasMessageContaining("Generator filtering failed to generate a valid value after 3 retries");

        verify(mockGenerator, times(3)).generate(context);
    }

    @Test
    void testFilteringWithMultipleFilterValues() {
        JsonNode options = mapper.valueToTree("test");
        GeneratorContext context = new GeneratorContext(faker, options, mapper);
        List<JsonNode> filterValues = Arrays.asList(
            mapper.valueToTree("filtered1"),
            mapper.valueToTree("filtered2"),
            mapper.valueToTree("filtered3")
        );
        JsonNode validResult = mapper.valueToTree("valid");

        when(mockGenerator.supportsFiltering()).thenReturn(false);
        when(mockGenerator.generate(context))
            .thenReturn(mapper.valueToTree("filtered1"))  // First attempt - filtered
            .thenReturn(mapper.valueToTree("filtered2"))  // Second attempt - filtered
            .thenReturn(validResult);                     // Third attempt - valid

        JsonNode result = adapter.generateWithFilter(context, filterValues);

        assertThat(result).isEqualTo(validResult);
        verify(mockGenerator, times(3)).generate(context);
    }
}
