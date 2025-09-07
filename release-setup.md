# Release Setup Guide

This guide explains how to set up the project for releasing to Maven Central.

## Prerequisites

1. **Central Publisher Portal Account**
   - Create account at https://central.sonatype.com/
   - Verify your namespace (groupId) ownership
   - No ticket creation needed - automated verification process

2. **GPG Key for Signing**
   ```bash
   # Generate GPG key
   gpg --gen-key
   
   # List keys
   gpg --list-secret-keys --keyid-format LONG
   
   # Export public key to key servers
   gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
   gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
   ```

3. **Maven Settings**
   Add to `~/.m2/settings.xml`:
   ```xml
   <settings>
     <servers>
       <server>
         <id>central</id>
         <username>your-central-portal-username</username>
         <password>your-central-portal-token</password>
       </server>
     </servers>
     <profiles>
       <profile>
         <id>central</id>
         <activation>
           <activeByDefault>true</activeByDefault>
         </activation>
         <properties>
           <gpg.executable>gpg</gpg.executable>
           <gpg.passphrase>your-gpg-passphrase</gpg.passphrase>
         </properties>
       </profile>
     </profiles>
   </settings>
   ```

## GitHub Secrets

Configure these secrets in your GitHub repository:

- `CENTRAL_USERNAME`: Your Central Publisher Portal username
- `CENTRAL_TOKEN`: Your Central Publisher Portal token
- `GPG_PRIVATE_KEY`: Your GPG private key (export with `gpg --armor --export-secret-keys YOUR_KEY_ID`)
- `GPG_PASSPHRASE`: Your GPG key passphrase

## Release Process

### Automatic Release (Recommended)

1. **Create and push a tag:**
   ```bash
   git tag v0.1.0
   git push origin v0.1.0
   ```

2. **GitHub Actions will automatically:**
   - Run tests
   - Build the project
   - Deploy to Maven Central
   - Create GitHub release

### Manual Release

1. **Update version:**
   ```bash
   mvn versions:set -DnewVersion=0.1.0
   ```

2. **Deploy to staging:**
   ```bash
   mvn clean deploy -P release
   ```

3. **Release from Central Portal:**
   - Login to https://central.sonatype.com/
   - Monitor deployment status
   - Automatic promotion to Maven Central

## Verification

After release, verify:

1. **Maven Central**: Check https://search.maven.org/
2. **GitHub Release**: Verify release was created
3. **Documentation**: Update version numbers in README

## Troubleshooting

### Common Issues

1. **GPG Signing Fails**
   - Ensure GPG key is properly configured
   - Check passphrase is correct
   - Verify key is not expired

2. **Central Portal Validation Fails**
   - Ensure all required metadata is present
   - Check groupId matches verified namespace
   - Verify sources and javadoc JARs are included

3. **GitHub Actions Fails**
   - Check all secrets are configured
   - Verify workflow permissions
   - Review action logs for specific errors

### Getting Help

- Central Publisher Portal: https://central.sonatype.com/
- Maven Central Guide: https://central.sonatype.org/publish/
- Migration Guide: https://central.sonatype.org/publish/publish-portal-migration/
- GitHub Actions Docs: https://docs.github.com/en/actions

## Migration Notes

**Important**: As of June 30, 2025, OSSRH has been shut down and replaced by the Central Publisher Portal. Key changes:

- **No more JIRA tickets** - namespace verification is automated
- **Simplified process** - direct publishing through the portal
- **Better UI** - modern web interface for managing releases
- **Faster publishing** - streamlined deployment process

## Security Notes

- Never commit credentials to version control
- Use GitHub secrets for sensitive information
- Regularly rotate access tokens
- Keep GPG keys secure and backed up
