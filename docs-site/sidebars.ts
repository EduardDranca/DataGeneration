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
        'dsl-reference/shadow-bindings',
        'dsl-reference/arrays',
        'dsl-reference/filtering',
        'dsl-reference/static-values',
      ],
    },
    {
      type: 'category',
      label: 'Generators',
      link: {
        type: 'doc',
        id: 'generators/overview',
      },
      items: [
        {
          type: 'category',
          label: 'Data Generators',
          collapsed: false,
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
          collapsed: false,
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
          collapsed: false,
          items: [
            'generators/lorem',
            'generators/sequence',
            'generators/choice',
            'generators/csv',
          ],
        },
      ],
    },
    {
      type: 'category',
      label: 'Guides',
      items: [
        'guides/overview',
        {
          type: 'category',
          label: 'How-To',
          items: [
            'guides/how-to/custom-generators',
            'guides/how-to/memory-optimization',
            'guides/how-to/runtime-computed-options',
          ],
        },
        {
          type: 'category',
          label: 'Patterns',
          items: [
            'guides/patterns/complex-scenarios',
          ],
        },
      ],
    },
    {
      type: 'category',
      label: 'API',
      items: [
        'api/java-api',
      ],
    },
    {
      type: 'category',
      label: 'Architecture',
      items: [
        'architecture/overview',
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
      label: 'How-To Guides',
      items: [
        'guides/how-to/custom-generators',
        'guides/how-to/memory-optimization',
        'guides/how-to/runtime-computed-options',
      ],
    },
    {
      type: 'category',
      label: 'Patterns',
      items: [
        'guides/patterns/complex-scenarios',
      ],
    },
  ],
};

export default sidebars;
