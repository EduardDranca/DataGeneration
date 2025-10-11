import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

/**
 * Creating a sidebar enables you to:
 - create an ordered group of docs
 - render a sidebar for each doc of that group
 - provide next/previous navigation

 The sidebars can be generated from the filesystem, or explicitly defined here.

 Create as many sidebars as you want.
 */
const sidebars: SidebarsConfig = {
  docsSidebar: [
    'intro',
    {
      type: 'category',
      label: 'Getting Started',
      items: [
        'getting-started/installation',
        'getting-started/quick-start',
        'getting-started/core-concepts',
      ],
    },
    {
      type: 'category',
      label: 'DSL Reference',
      items: [
        'dsl-reference/overview',
        'dsl-reference/collections',
        'dsl-reference/references',
        'dsl-reference/arrays',
        'dsl-reference/filtering',
        'dsl-reference/static-values',
      ],
    },
    {
      type: 'category',
      label: 'API',
      items: [
        'api/java-api',
        'api/generation-modes',
        'api/output-formats',
      ],
    },
    {
      type: 'category',
      label: 'Architecture',
      items: [
        'architecture/overview',
        'architecture/visitor-pattern',
        'architecture/lazy-vs-eager',
      ],
    },
  ],
  generatorsSidebar: [
    'generators/overview',
    {
      type: 'category',
      label: 'Data Generators',
      items: [
        'generators/uuid',
        'generators/name',
        'generators/internet',
        'generators/address',
        'generators/company',
        'generators/country',
        'generators/book',
        'generators/finance',
        'generators/phone',
      ],
    },
    {
      type: 'category',
      label: 'Primitive Generators',
      items: [
        'generators/number',
        'generators/float',
        'generators/boolean',
        'generators/string',
        'generators/date',
      ],
    },
    {
      type: 'category',
      label: 'Utility Generators',
      items: [
        'generators/lorem',
        'generators/sequence',
        'generators/choice',
        'generators/csv',
      ],
    },
  ],
  guidesSidebar: [
    'guides/overview',
    {
      type: 'category',
      label: 'Common Patterns',
      items: [
        'guides/patterns/user-order-relationships',
        'guides/patterns/hierarchical-data',
        'guides/patterns/admin-users',
        'guides/patterns/audit-logs',
      ],
    },
    {
      type: 'category',
      label: 'How-To Guides',
      items: [
        'guides/how-to/memory-optimization',
        'guides/how-to/custom-generators',
        'guides/how-to/streaming',
        'guides/how-to/performance-tuning',
      ],
    },
  ],
};

export default sidebars;
