# CI History Note

The release branch intentionally removes the old reconstruction/phase-specific GitHub Actions workflows after their work is complete. Those workflow definitions remain recoverable from Git history and the frozen audit refs.

Release validation is consolidated into a single release-oriented workflow so active CI reflects the current product state instead of replaying reconstruction stages.
