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
      label: 'Generators',
      link: {
        type: 'doc',
        id: 'generators/overview',
      },
      items: [
        'generators/uuid',
        'generators/name',
        'generators/number',
        'generators/choice',
        'generators/sequence',
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
    'generators/uuid',
    'generators/name',
    'generators/number',
    'generators/choice',
    'generators/sequence',
  ],
  guidesSidebar: [
    'guides/overview',
  ],
};

export default sidebars;
