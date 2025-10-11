# Lorem Generator

Generates Lorem Ipsum placeholder text in various formats.

## Basic Usage

```json
{
  "lorem": {"gen": "lorem"}
}
```

**Output:** Returns an object with all lorem fields:
```json
{
  "word": "lorem",
  "words": "lorem ipsum dolor sit amet",
  "sentence": "Lorem ipsum dolor sit amet.",
  "sentences": "Lorem ipsum dolor. Sit amet consectetur. Adipiscing elit sed.",
  "paragraph": "Lorem ipsum dolor sit amet...",
  "paragraphs": "Lorem ipsum dolor...\n\nSit amet consectetur..."
}
```

## Available Fields

Access specific fields using dot notation:

| Field | Description | Example |
|-------|-------------|---------|
| `word` | Single word | `lorem` |
| `words` | 5 words | `lorem ipsum dolor sit amet` |
| `sentence` | Single sentence | `Lorem ipsum dolor sit amet.` |
| `sentences` | 3 sentences | Multiple sentences |
| `paragraph` | Single paragraph | Full paragraph |
| `paragraphs` | 2 paragraphs | Multiple paragraphs |

## Options

| Option | Type | Description |
|--------|------|-------------|
| `words` | integer | Generate N words |
| `sentences` | integer | Generate N sentences |
| `paragraphs` | integer | Generate N paragraphs |

## Examples

### Single Word

```json
{
  "tag": {"gen": "lorem.word"}
}
```

**Output:** `lorem`

### Multiple Words

```json
{
  "title": {"gen": "lorem", "words": 3}
}
```

**Output:** `lorem ipsum dolor`

### Custom Word Count

```json
{
  "description": {"gen": "lorem", "words": 10}
}
```

### Single Sentence

```json
{
  "summary": {"gen": "lorem.sentence"}
}
```

**Output:** `Lorem ipsum dolor sit amet.`

### Multiple Sentences

```json
{
  "description": {"gen": "lorem", "sentences": 5}
}
```

### Single Paragraph

```json
{
  "content": {"gen": "lorem.paragraph"}
}
```

### Multiple Paragraphs

```json
{
  "article": {"gen": "lorem", "paragraphs": 3}
}
```

## Common Patterns

### Blog Posts

```json
{
  "posts": {
    "count": 20,
    "item": {
      "id": {"gen": "uuid"},
      "title": {"gen": "lorem", "words": 5},
      "excerpt": {"gen": "lorem.sentence"},
      "content": {"gen": "lorem", "paragraphs": 3},
      "author": {"gen": "name.fullName"}
    }
  }
}
```

### Product Descriptions

```json
{
  "products": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem", "words": 2},
      "shortDescription": {"gen": "lorem.sentence"},
      "fullDescription": {"gen": "lorem.paragraph"},
      "price": {"gen": "float", "min": 9.99, "max": 999.99, "decimals": 2}
    }
  }
}
```

### Comments

```json
{
  "comments": {
    "count": 500,
    "item": {
      "id": {"gen": "uuid"},
      "author": {"gen": "name.fullName"},
      "text": {"gen": "lorem", "sentences": 3},
      "createdAt": {"gen": "date"}
    }
  }
}
```

### Articles

```json
{
  "articles": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "headline": {"gen": "lorem", "words": 6},
      "subheading": {"gen": "lorem.sentence"},
      "body": {"gen": "lorem", "paragraphs": 5},
      "author": {"gen": "name.fullName"}
    }
  }
}
```

### Tags and Labels

```json
{
  "items": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "tags": {
        "array": {
          "size": 3,
          "item": {"gen": "lorem.word"}
        }
      }
    }
  }
}
```

## Best Practices

1. **Use Specific Fields**: Access only what you need (e.g., `lorem.word`)
2. **Appropriate Length**: Match text length to use case
3. **Titles**: Use 3-6 words for titles
4. **Descriptions**: Use 1-2 sentences for short descriptions
5. **Content**: Use paragraphs for body content

## Use Cases

- **Prototyping**: Fill UI with realistic-looking text
- **Testing**: Test text rendering and layout
- **Demos**: Create convincing demo content
- **Development**: Populate databases with placeholder content
- **Load Testing**: Generate text data for performance tests

## Next Steps

- [String Generator](./string.md) - For random strings
- [Name Generator](./name.md) - For realistic names
- [Internet Generator](./internet.md) - For emails and URLs
