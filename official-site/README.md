# Nurtlina official and legal pages

Static, dependency-free English pages prepared for `https://nurtlina.app`.

## Routes

| File | Intended URL |
|---|---|
| `index.html` | `/` |
| `privacy.html` | `/privacy` |
| `terms.html` | `/terms` |
| `data-deletion.html` | `/data-deletion` |
| `data-safety.html` | `/data-safety` |
| `support.html` | `/support` |

The Android app currently links to `/privacy` and `/terms`. Enter
`https://nurtlina.app/data-deletion` as the account-deletion web resource in
Google Play Console after publishing.

These documents reflect the repository implementation reviewed on 18 August
2026. Before publication, confirm the developer/legal entity name, postal
address if required, hosting provider, production retention jobs, AdMob consent
flow, and Google Play Data safety answers with qualified counsel.

The repository currently initializes Google Mobile Ads but does not contain the
Google User Messaging Platform (UMP) consent SDK or an equivalent consent flow.
Resolve this before serving ads in the EEA, UK, or Switzerland, and expose a way
to revisit privacy choices where required.

If the chosen static host does not automatically map extensionless paths to
`.html` files, configure these rewrites:

```text
/privacy       -> /privacy.html
/terms         -> /terms.html
/data-deletion -> /data-deletion.html
/data-safety   -> /data-safety.html
/support       -> /support.html
```
