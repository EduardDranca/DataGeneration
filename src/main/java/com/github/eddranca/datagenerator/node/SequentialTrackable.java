package com.github.eddranca.datagenerator.node;

/**
 * Interface for nodes that can be tracked for sequential reference behavior.
 * This allows the GenerationContext to maintain separate counters for different reference nodes.
 */
public interface SequentialTrackable {
    /**
     * Returns whether this reference should use sequential (round-robin) behavior.
     */
    boolean isSequential();
}