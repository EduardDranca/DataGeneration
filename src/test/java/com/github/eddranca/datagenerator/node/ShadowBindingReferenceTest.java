package com.github.eddranca.datagenerator.node;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ShadowBindingReference")
class ShadowBindingReferenceTest {

    @Nested
    @DisplayName("parse()")
    class Parse {

        @Test
        @DisplayName("parses valid reference with simple field")
        void parsesValidReferenceWithSimpleField() {
            ShadowBindingReference ref = ShadowBindingReference.parse("$user.id");
            
            assertThat(ref.getBindingName()).isEqualTo("$user");
            assertThat(ref.getFieldPath()).isEqualTo("id");
        }

        @Test
        @DisplayName("parses valid reference with nested field path")
        void parsesValidReferenceWithNestedFieldPath() {
            ShadowBindingReference ref = ShadowBindingReference.parse("$user.profile.settings.theme");
            
            assertThat(ref.getBindingName()).isEqualTo("$user");
            assertThat(ref.getFieldPath()).isEqualTo("profile.settings.theme");
        }

        @Test
        @DisplayName("throws when reference doesn't start with $")
        void throwsWhenReferenceDoesntStartWithDollar() {
            assertThatThrownBy(() -> ShadowBindingReference.parse("user.id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must start with $");
        }

        @Test
        @DisplayName("throws when reference has no field path")
        void throwsWhenReferenceHasNoFieldPath() {
            assertThatThrownBy(() -> ShadowBindingReference.parse("$user"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must include field path");
        }

        @Test
        @DisplayName("throws when binding name is empty")
        void throwsWhenBindingNameIsEmpty() {
            assertThatThrownBy(() -> ShadowBindingReference.parse("$.id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be empty");
        }

        @Test
        @DisplayName("throws when field path is empty")
        void throwsWhenFieldPathIsEmpty() {
            assertThatThrownBy(() -> ShadowBindingReference.parse("$user."))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("field path cannot be empty");
        }
    }

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("creates reference with provided values")
        void createsReferenceWithProvidedValues() {
            ShadowBindingReference ref = new ShadowBindingReference("$binding", "field");
            
            assertThat(ref.getBindingName()).isEqualTo("$binding");
            assertThat(ref.getFieldPath()).isEqualTo("field");
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToString {

        @Test
        @DisplayName("returns binding name and field path joined by dot")
        void returnsBindingNameAndFieldPathJoinedByDot() {
            ShadowBindingReference ref = new ShadowBindingReference("$user", "regionId");
            
            assertThat(ref.toString()).isEqualTo("$user.regionId");
        }

        @Test
        @DisplayName("handles nested field paths")
        void handlesNestedFieldPaths() {
            ShadowBindingReference ref = new ShadowBindingReference("$user", "profile.email");
            
            assertThat(ref.toString()).isEqualTo("$user.profile.email");
        }
    }
}
