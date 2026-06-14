# Release Checklist

Run these checks before sharing an APK or AAB.

```bash
git status
./gradlew clean lint assembleRelease
```

Confirm that the repository does not contain generated app packages or local build files.

```bash
git ls-files | grep -Ei '\.(apk|aab|jks|keystore)$|local\.properties|keystore\.properties' || true
```

Expected result: no output.

Recommended GitHub settings:

- Enable secret scanning.
- Enable push protection when available.
- Enable Dependabot alerts.
- Enable Dependabot security updates.
