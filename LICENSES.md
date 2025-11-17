# License Compliance Report

This document provides a comprehensive overview of all licenses used in this project.

## Project License

**Encrypted SQLite MCP Server** is licensed under the **Apache License 2.0**.

Copyright 2025 rosch100

See [LICENSE](LICENSE) for the full license text.

## Third-Party Dependencies

### Runtime Dependencies

#### 1. sqlite-jdbc-crypt (version 3.50.1.0)
- **License**: Apache License 2.0
- **Source**: https://github.com/Willena/sqlite-jdbc-crypt
- **Usage**: SQLite JDBC driver with SQLCipher encryption support
- **License Compatibility**: ✅ Compatible (Apache 2.0)
- **Attribution**: Required (included in NOTICE file)

#### 2. Gson (version 2.11.0)
- **License**: Apache License 2.0
- **Source**: https://github.com/google/gson
- **Usage**: JSON serialization/deserialization library
- **License Compatibility**: ✅ Compatible (Apache 2.0)
- **Attribution**: Required (included in NOTICE file)

### Test Dependencies

#### 3. JUnit Jupiter (version 5.11.0)
- **License**: Eclipse Public License 2.0
- **Source**: https://junit.org/junit5/
- **Usage**: Unit testing framework (test scope only)
- **License Compatibility**: ✅ Compatible (EPL 2.0 is compatible with Apache 2.0 for distribution)
- **Attribution**: Required (included in NOTICE file)

## License Compatibility Analysis

All dependencies use licenses that are compatible with Apache License 2.0:

- **Apache License 2.0** (sqlite-jdbc-crypt, Gson): Fully compatible
- **Eclipse Public License 2.0** (JUnit): Compatible for distribution

## Compliance Checklist

- ✅ All dependencies have compatible licenses
- ✅ LICENSE file includes full Apache 2.0 text
- ✅ NOTICE file includes attribution for all third-party components
- ✅ Source code includes copyright notice
- ✅ README includes license information
- ✅ Third-party license information documented

## License Files Location

- Project License: [LICENSE](LICENSE)
- Third-Party Attributions: [NOTICE](NOTICE)
- Detailed License Info: This file (LICENSES.md)

## Verification

To verify licenses of dependencies:

```bash
# Check SQLite JDBC driver license
unzip -p libs/sqlite-jdbc-3.50.1.0.jar META-INF/maven/io.github.willena/sqlite-jdbc/LICENSE

# Check Gson license (from Maven Central)
curl https://raw.githubusercontent.com/google/gson/master/LICENSE

# Check JUnit license
curl https://raw.githubusercontent.com/junit-team/junit5/main/LICENSE.md
```

## Notes

- The SQLite JDBC driver is downloaded automatically during the build process
- All licenses are permissive and allow commercial use
- No copyleft licenses are used, ensuring maximum compatibility

