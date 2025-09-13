package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Context for collection operations during reference resolution.
 */
public class CollectionContext {
    private final JsonNode currentItem;
    private final List<JsonNode> filterValues;
    private List<JsonNode> filteredCollection;

    public CollectionContext(JsonNode currentItem, List<JsonNode> filterValues) {
        this.currentItem = currentItem;
        this.filterValues = filterValues;
    }

    public JsonNode getCurrentItem() {
        return currentItem;
    }

    public List<JsonNode> getFilterValues() {
        return filterValues;
    }

    public boolean hasFilteredCollection() {
        return filteredCollection != null;
    }

    public List<JsonNode> getFilteredCollection() {
        return filteredCollection;
    }

    public void setFilteredCollection(List<JsonNode> filteredCollection) {
        this.filteredCollection = filteredCollection;
    }
}
