import type {ReactNode} from 'react';
import clsx from 'clsx';
import Heading from '@theme/Heading';
import styles from './styles.module.css';

type FeatureItem = {
  title: string;
  description: ReactNode;
};

const FeatureList: FeatureItem[] = [
  {
    title: 'Declarative DSL',
    description: (
      <>
        Define complex data structures in simple JSON. No code required for basic scenarios.
        Create realistic test data with relationships, arrays, and filtering.
      </>
    ),
  },
  {
    title: 'Built-in Generators',
    description: (
      <>
        UUID, Name, Internet, Address, Company, Phone, Date, and more.
        Generate realistic data for testing, development, and demos.
      </>
    ),
  },
  {
    title: 'Memory Efficient',
    description: (
      <>
        Lazy generation mode streams data without loading everything into memory.
        Perfect for large datasets and performance-critical scenarios.
      </>
    ),
  },
];

function Feature({title, description}: FeatureItem) {
  return (
    <div className={clsx('col col--4')}>
      <div className="text--center padding-horiz--md">
        <Heading as="h3">{title}</Heading>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures(): ReactNode {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
