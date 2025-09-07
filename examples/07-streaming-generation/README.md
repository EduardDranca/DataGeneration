# Streaming Generation Example

This example demonstrates how to use the streaming generation feature to generate large datasets efficiently without loading everything into memory at once.

## Features Demonstrated

- **Streaming SQL Generation**: Generate SQL INSERT statements on-demand
- **Memory Efficiency**: Process large datasets without memory constraints
- **Reference Data**: Handle dependencies between collections
- **Batch Processing**: Stream data in manageable chunks

## DSL Structure

The example includes:
- `reference_data`: A small collection of reference data (5 items)
- `large_dataset`: A large collection that references the reference data (1000 items)

## Key Benefits

1. **Memory Efficient**: Only keeps reference collections in memory
2. **Scalable**: Can handle datasets of any size
3. **SQL Ready**: Generates properly formatted SQL INSERT statements
4. **Dependency Aware**: Automatically handles collection dependencies

This approach is ideal for:
- Large data migrations
- Database seeding
- Performance testing with big datasets
- ETL processes
