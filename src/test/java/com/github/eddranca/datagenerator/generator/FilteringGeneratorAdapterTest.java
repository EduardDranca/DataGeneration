package com.github.eddranca.datagenerator.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.exception.FilteringException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FilteringGeneratorAdapterTest {
    private ObjectMapper mapper;
    private FilteringGeneratorAdapter adapter;

    @Mock
    private Generator mockGenerator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mapper = new ObjectMapper();
        adapter = new FilteringGeneratorAdapter(mockGenerator, 3);
    }

    @Test
    void testGenerateDelegatesToUnderlying() {
        JsonNode options = mapper.valueToTree("test");
        JsonNode expected = mapper.valueToTree("result");
        when(mockGenerator.generate(options)).thenReturn(expected);

        JsonNode result = adapter.generate(options);

        assertThat(result).isEqualTo(expected);
        verify(mockGenerator).generate(options);
    }

    @Test
    void testGenerateAtPathDelegatesToUnderlying() {
        JsonNode options = mapper.valueToTree("test");
        JsonNode expected = mapper.valueToTree("result");
        when(mockGenerator.generateAtPath(options, "field")).thenReturn(expected);

        JsonNode result = adapter.generateAtPath(options, "field");

        assertThat(result).isEqualTo(expected);
        verify(mockGenerator).generateAtPath(options, "field");
    }

    @Test
    void testGetFieldSuppliersDelegatesToUnderlying() {
        JsonNode options = mapper.valueToTree("test");
        Map<String, Supplier<JsonNode>> expected = Collections.emptyMap();
        when(mockGenerator.getFieldSuppliers(options)).thenReturn(expected);

        Map<String, Supplier<JsonNode>> result = adapter.getFieldSuppliers(options);

        assertThat(result).isEqualTo(expected);
        verify(mockGenerator).getFieldSuppliers(options);
    }

    @Test
    void testSupportsFilteringAlwaysTrue() {
        // Regardless of what the delegate returns, adapter always supports filtering
        when(mockGenerator.supportsFiltering()).thenReturn(false);

        assertThat(adapter.supportsFiltering()).isTrue();
    }

    @Test
    void testGenerateWithFilterNativeSupport() {
        JsonNode options = mapper.valueToTree("test");
        List<JsonNode> filterValues = Collections.singletonList(mapper.valueToTree("filtered"));
        JsonNode expected = mapper.valueToTree("result");

        when(mockGenerator.supportsFiltering()).thenReturn(true);
        when(mockGenerator.generateWithFilter(options, filterValues)).thenReturn(expected);

        JsonNode result = adapter.generateWithFilter(options, filterValues);

        assertThat(result).isEqualTo(expected);
        verify(mockGenerator).generateWithFilter(options, filterValues);
        verify(mockGenerator, never()).generate(any());
    }

    @Test
    void testGenerateWithFilterRetryLogicSuccess() {
        JsonNode options = mapper.valueToTree("test");
        List<JsonNode> filterValues = Collections.singletonList(mapper.valueToTree("filtered"));
        JsonNode filteredResult = mapper.valueToTree("filtered");
        JsonNode validResult = mapper.valueToTree("valid");

        when(mockGenerator.supportsFiltering()).thenReturn(false);
        when(mockGenerator.generate(options))
            .thenReturn(filteredResult)  // First attempt - filtered
            .thenReturn(validResult);    // Second attempt - valid

        JsonNode result = adapter.generateWithFilter(options, filterValues);

        assertThat(result).isEqualTo(validResult);
        verify(mockGenerator, times(2)).generate(options);
    }

    @Test
    void testGenerateWithFilterRetryLogicMaxRetriesThrowsException() {
        JsonNode options = mapper.valueToTree("test");
        List<JsonNode> filterValues = Collections.singletonList(mapper.valueToTree("filtered"));
        JsonNode filteredResult = mapper.valueToTree("filtered");

        when(mockGenerator.supportsFiltering()).thenReturn(false);
        when(mockGenerator.generate(options)).thenReturn(filteredResult); // Always return filtered value

        assertThatThrownBy(() -> adapter.generateWithFilter(options, filterValues))
            .isInstanceOf(FilteringException.class)
            .hasMessageContaining("Generator filtering failed to generate a valid value after 3 retries");

        verify(mockGenerator, times(3)).generate(options); // Should retry 3 times (maxFilteringRetries)
    }


    @Test
    void testGenerateWithFilterNoFiltering() {
        JsonNode options = mapper.valueToTree("test");
        JsonNode expected = mapper.valueToTree("result");

        when(mockGenerator.supportsFiltering()).thenReturn(false);
        when(mockGenerator.generate(options)).thenReturn(expected);

        JsonNode result = adapter.generateWithFilter(options, null);

        assertThat(result).isEqualTo(expected);
        verify(mockGenerator, times(1)).generate(options);
    }

    @Test
    void testGenerateWithFilterEmptyFiltering() {
        JsonNode options = mapper.valueToTree("test");
        JsonNode expected = mapper.valueToTree("result");

        when(mockGenerator.supportsFiltering()).thenReturn(false);
        when(mockGenerator.generate(options)).thenReturn(expected);

        JsonNode result = adapter.generateWithFilter(options, Collections.emptyList());

        assertThat(result).isEqualTo(expected);
        verify(mockGenerator, times(1)).generate(options);
    }

    @Test
    void testGenerateAtPathWithFilterNativeSupport() {
        JsonNode options = mapper.valueToTree("test");
        List<JsonNode> filterValues = Collections.singletonList(mapper.valueToTree("filtered"));
        JsonNode expected = mapper.valueToTree("result");

        when(mockGenerator.supportsFiltering()).thenReturn(true);
        when(mockGenerator.generateAtPathWithFilter(options, "field", filterValues)).thenReturn(expected);

        JsonNode result = adapter.generateAtPathWithFilter(options, "field", filterValues);

        assertThat(result).isEqualTo(expected);
        verify(mockGenerator).generateAtPathWithFilter(options, "field", filterValues);
        verify(mockGenerator, never()).generateAtPath(any(), any());
    }

    @Test
    void testGenerateAtPathWithFilterRetryLogic() {
        JsonNode options = mapper.valueToTree("test");
        List<JsonNode> filterValues = Collections.singletonList(mapper.valueToTree("filtered"));
        JsonNode filteredResult = mapper.valueToTree("filtered");
        JsonNode validResult = mapper.valueToTree("valid");

        when(mockGenerator.supportsFiltering()).thenReturn(false);
        when(mockGenerator.generateAtPath(options, "field"))
            .thenReturn(filteredResult)  // First attempt - filtered
            .thenReturn(validResult);    // Second attempt - valid

        JsonNode result = adapter.generateAtPathWithFilter(options, "field", filterValues);

        assertThat(result).isEqualTo(validResult);
        verify(mockGenerator, times(2)).generateAtPath(options, "field");
    }

    @Test
    void testGenerateAtPathWithFilterRetryLogicMaxRetriesThrowsException() {
        JsonNode options = mapper.valueToTree("test");
        List<JsonNode> filterValues = Collections.singletonList(mapper.valueToTree("filtered"));
        JsonNode filteredResult = mapper.valueToTree("filtered");

        when(mockGenerator.supportsFiltering()).thenReturn(false);
        when(mockGenerator.generateAtPath(options, "field")).thenReturn(filteredResult);

        assertThatThrownBy(() -> adapter.generateAtPathWithFilter(options, "field", filterValues))
            .isInstanceOf(FilteringException.class)
            .hasMessageContaining("Generator path filtering failed to generate a valid value after 3 retries");

        verify(mockGenerator, times(3)).generateAtPath(options, "field");
    }

    @Test
    void testFilteringWithNullValues() {
        JsonNode options = mapper.valueToTree("test");
        List<JsonNode> filterValues = Collections.singletonList(mapper.nullNode());

        when(mockGenerator.supportsFiltering()).thenReturn(false);
        when(mockGenerator.generate(options)).thenReturn(mapper.nullNode());

        assertThatThrownBy(() -> adapter.generateWithFilter(options, filterValues))
            .isInstanceOf(FilteringException.class)
            .hasMessageContaining("Generator filtering failed to generate a valid value after 3 retries");

        verify(mockGenerator, times(3)).generate(options);
    }

    @Test
    void testFilteringWithMultipleFilterValues() {
        JsonNode options = mapper.valueToTree("test");
        List<JsonNode> filterValues = Arrays.asList(
            mapper.valueToTree("filtered1"),
            mapper.valueToTree("filtered2"),
            mapper.valueToTree("filtered3")
        );
        JsonNode validResult = mapper.valueToTree("valid");

        when(mockGenerator.supportsFiltering()).thenReturn(false);
        when(mockGenerator.generate(options))
            .thenReturn(mapper.valueToTree("filtered1"))  // First attempt - filtered
            .thenReturn(mapper.valueToTree("filtered2"))  // Second attempt - filtered
            .thenReturn(validResult);                     // Third attempt - valid

        JsonNode result = adapter.generateWithFilter(options, filterValues);

        assertThat(result).isEqualTo(validResult);
        verify(mockGenerator, times(3)).generate(options);
    }
}
