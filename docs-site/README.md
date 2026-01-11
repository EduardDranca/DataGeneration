# DataGeneration Documentation Site

This directory contains the Docusaurus-based documentation website for DataGeneration.

## Development

### Prerequisites

- Node.js 18+ and npm

### Local Development

```bash
cd docs-site
npm install
npm start
```

This starts a local development server at `http://localhost:3000` with hot reloading.

### Build

```bash
npm run build
```

Generates static content into the `build` directory.

### Deployment

The site is automatically deployed to GitHub Pages when changes are pushed to the `main` branch.

Manual deployment:
```bash
npm run deploy
```

## Structure

```
docs-site/
├── docs/                    # Documentation markdown files
│   ├── getting-started/     # Installation, quick start, concepts
│   ├── dsl-reference/       # Complete DSL syntax reference
│   ├── generators/          # Individual generator documentation
│   ├── api/                 # Java API reference
│   ├── architecture/        # Architecture documentation
│   └── guides/              # Patterns and how-to guides
├── src/
│   ├── components/          # React components
│   ├── css/                 # Custom CSS
│   └── pages/               # Custom pages (home, etc.)
├── static/                  # Static assets (images, etc.)
├── docusaurus.config.ts     # Docusaurus configuration
└── sidebars.ts              # Sidebar navigation configuration
```

## Adding Documentation

### New Page

1. Create a markdown file in the appropriate `docs/` subdirectory
2. Add frontmatter if needed:
   ```markdown
   ---
   sidebar_position: 1
   ---
   # Page Title
   ```
3. Update `sidebars.ts` if using manual sidebar configuration

### New Generator Documentation

1. Create `docs/generators/generator-name.md`
2. Follow the template:
   - Description
   - Options
   - Examples
   - Output samples
3. Add to the generators sidebar in `sidebars.ts`

### Code Examples

Use fenced code blocks with language identifiers:

````markdown
```json
{
  "users": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"}
    }
  }
}
```

```java
Generation generation = DslDataGenerator.create()
    .fromJsonString(dsl)
    .generate();
```
````

## Configuration

### Site Metadata

Edit `docusaurus.config.ts`:
- `title` - Site title
- `tagline` - Site tagline
- `url` - Production URL
- `baseUrl` - Base URL path

### Navigation

Edit `sidebars.ts` to configure sidebar navigation.

### Theme

Customize in `src/css/custom.css`.

## Resources

- [Docusaurus Documentation](https://docusaurus.io/)
- [Markdown Features](https://docusaurus.io/docs/markdown-features)
- [Deployment Guide](https://docusaurus.io/docs/deployment)
