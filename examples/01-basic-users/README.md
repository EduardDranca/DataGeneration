# Basic Users Example

This example demonstrates the simplest use case of the DataGeneration library - generating basic user data.

## What it generates

- **5 users** with basic profile information
- Each user has: ID, first name, last name, email, age, and active status

## Key features demonstrated

- **UUID generation** for unique identifiers
- **Name generators** for realistic names
- **Email generation** for valid email addresses
- **Number ranges** for age constraints (18-65)
- **Boolean choices** for true/false values

## Files in this example

- `BasicUsersExample.java` - Main Java program
- `dsl.json` - DSL definition in clean JSON format

## Expected output

- JSON with 5 user objects
- SQL INSERT statements for a `users` table

This is perfect for getting started with the library and understanding the basic DSL structure.
