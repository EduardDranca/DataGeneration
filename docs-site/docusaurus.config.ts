import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

const config: Config = {
  title: 'DataGeneration',
  tagline: 'Generate complex, realistic test data with a declarative JSON DSL',
  favicon: 'img/favicon.ico',

  // Future flags, see https://docusaurus.io/docs/api/docusaurus-config#future
  future: {
    v4: true, // Improve compatibility with the upcoming Docusaurus v4
  },

  // Set the production url of your site here
  url: 'https://eduarddranca.github.io',
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: '/DataGeneration/',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: 'EduardDranca', // Usually your GitHub org/user name.
  projectName: 'DataGeneration', // Usually your repo name.

  onBrokenLinks: 'throw',

  // Even if you don't use internationalization, you can use this field to set
  // useful metadata like html lang. For example, if your site is Chinese, you
  // may want to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          editUrl:
            'https://github.com/EduardDranca/DataGeneration/tree/main/docs-site/',
        },
        blog: false, // Disable blog for now
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    // Replace with your project's social card
    image: 'img/docusaurus-social-card.jpg',
    colorMode: {
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: 'DataGeneration',
      logo: {
        alt: 'DataGeneration Logo',
        src: 'img/logo.svg',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'docsSidebar',
          position: 'left',
          label: 'Documentation',
        },
        {
          type: 'docSidebar',
          sidebarId: 'generatorsSidebar',
          position: 'left',
          label: 'Generators',
        },
        {
          type: 'docSidebar',
          sidebarId: 'guidesSidebar',
          position: 'left',
          label: 'Guides',
        },
        {
          href: 'https://github.com/EduardDranca/DataGeneration',
          label: 'GitHub',
          position: 'right',
        },
        {
          href: 'https://central.sonatype.com/artifact/io.github.eduarddranca/data-generation',
          label: 'Maven Central',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Documentation',
          items: [
            {
              label: 'Getting Started',
              to: '/docs/getting-started/installation',
            },
            {
              label: 'DSL Reference',
              to: '/docs/dsl-reference/overview',
            },
            {
              label: 'Generators',
              to: '/docs/generators/overview',
            },
          ],
        },
        {
          title: 'Resources',
          items: [
            {
              label: 'GitHub',
              href: 'https://github.com/EduardDranca/DataGeneration',
            },
            {
              label: 'Maven Central',
              href: 'https://central.sonatype.com/artifact/io.github.eduarddranca/data-generation',
            },
            {
              label: 'Issues',
              href: 'https://github.com/EduardDranca/DataGeneration/issues',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'Examples',
              href: 'https://github.com/EduardDranca/DataGeneration/tree/main/examples',
            },
            {
              label: 'Contributing',
              href: 'https://github.com/EduardDranca/DataGeneration/blob/main/CONTRIBUTING.md',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} Eduard Dranca. Built with Docusaurus.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['java', 'json', 'bash'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
