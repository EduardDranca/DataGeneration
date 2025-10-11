# Book Generator

Generates book-related data including titles, authors, publishers, and genres.

## Basic Usage

```json
{
  "book": {"gen": "book"}
}
```

**Output:** Returns an object with all book fields:
```json
{
  "title": "The Great Gatsby",
  "author": "F. Scott Fitzgerald",
  "publisher": "Scribner",
  "genre": "Fiction"
}
```

## Available Fields

Access specific fields using dot notation:

| Field | Description | Example |
|-------|-------------|---------|
| `title` | Book title | `The Great Gatsby` |
| `author` | Author name | `F. Scott Fitzgerald` |
| `publisher` | Publisher name | `Scribner` |
| `genre` | Book genre | `Fiction` |

## Options

This generator has **no options**.

## Examples

### Single Field

```json
{
  "title": {"gen": "book.title"}
}
```

**Output:** `"The Great Gatsby"`

### Multiple Fields

```json
{
  "title": {"gen": "book.title"},
  "author": {"gen": "book.author"},
  "genre": {"gen": "book.genre"}
}
```

### Full Book Object

```json
{
  "bookData": {"gen": "book"}
}
```

**Output:**
```json
{
  "bookData": {
    "title": "To Kill a Mockingbird",
    "author": "Harper Lee",
    "publisher": "J. B. Lippincott & Co.",
    "genre": "Southern Gothic"
  }
}
```

## Common Patterns

### Book Catalog

```json
{
  "books": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "title": {"gen": "book.title"},
      "author": {"gen": "book.author"},
      "publisher": {"gen": "book.publisher"},
      "genre": {"gen": "book.genre"},
      "isbn": {"gen": "string", "regex": "[0-9]{3}-[0-9]{10}"},
      "price": {"gen": "float", "min": 9.99, "max": 49.99, "decimals": 2}
    }
  }
}
```

### Library System

```json
{
  "books": {
    "count": 500,
    "item": {
      "id": {"gen": "uuid"},
      "title": {"gen": "book.title"},
      "author": {"gen": "book.author"},
      "genre": {"gen": "book.genre"},
      "available": {"gen": "boolean", "probability": 0.8}
    }
  },
  "loans": {
    "count": 200,
    "item": {
      "id": {"gen": "uuid"},
      "bookId": {"ref": "books[*].id"},
      "userId": {"gen": "uuid"},
      "dueDate": {"gen": "date", "from": "2024-01-01", "to": "2024-12-31"}
    }
  }
}
```

### Bookstore Inventory

```json
{
  "inventory": {
    "count": 200,
    "item": {
      "id": {"gen": "uuid"},
      "title": {"gen": "book.title"},
      "author": {"gen": "book.author"},
      "publisher": {"gen": "book.publisher"},
      "genre": {"gen": "book.genre"},
      "stock": {"gen": "number", "min": 0, "max": 50},
      "price": {"gen": "float", "min": 12.99, "max": 39.99, "decimals": 2},
      "rating": {"gen": "float", "min": 1.0, "max": 5.0, "decimals": 1}
    }
  }
}
```

### Author-Book Relationship

```json
{
  "authors": {
    "count": 20,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "email": {"gen": "internet.emailAddress"}
    }
  },
  "books": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "title": {"gen": "book.title"},
      "genre": {"gen": "book.genre"},
      "authorId": {"ref": "authors[*].id"},
      "publishedDate": {"gen": "date", "from": "2000-01-01", "to": "2024-12-31"}
    }
  }
}
```

### Book Reviews

```json
{
  "books": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "title": {"gen": "book.title"},
      "author": {"gen": "book.author"}
    }
  },
  "reviews": {
    "count": 500,
    "item": {
      "id": {"gen": "uuid"},
      "bookId": {"ref": "books[*].id"},
      "reviewer": {"gen": "name.fullName"},
      "rating": {"gen": "number", "min": 1, "max": 5},
      "comment": {"gen": "lorem.sentence"},
      "reviewDate": {"gen": "date"}
    }
  }
}
```

## Best Practices

1. **Use Specific Fields**: Access only the fields you need (e.g., `book.title`)
2. **Combine with Other Generators**: Mix book data with UUIDs, dates, and numbers
3. **Create Relationships**: Use references to link books with authors, reviews, or loans
4. **Add Custom Fields**: Combine generated book data with custom fields like ISBN, price, stock

## Use Cases

- **Library Management**: Generate book catalogs and loan records
- **Bookstore Systems**: Create inventory and sales data
- **Review Platforms**: Generate books with ratings and reviews
- **Publishing Systems**: Create author-book relationships
- **Testing**: Populate test databases with realistic book data

## Comparison with Other Generators

| Generator | Use Case |
|-----------|----------|
| **Book** | Book-related data |
| [Lorem](./lorem.md) | Generic placeholder text |
| [Name](./name.md) | Person names (can be used for authors) |
| [Company](./company.md) | Company names (can be used for publishers) |

## Next Steps

- [Name Generator](./name.md) - For author names
- [Lorem Generator](./lorem.md) - For book descriptions
- [References](../dsl-reference/references.md) - Create author-book relationships
