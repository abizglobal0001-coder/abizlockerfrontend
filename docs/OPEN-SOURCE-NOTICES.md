# ABIZ LOCKER — Open Source Notices

ABIZ Locker is built on top of MDMesh, which is itself a derivative of Headwind MDM.
All open-source obligations are preserved and documented here.

---

## Primary Foundation: MDMesh

| Field | Value |
|-------|-------|
| Project | MDMesh |
| Repository | https://github.com/MDMesh-app/MDMesh |
| License | Apache License, Version 2.0 |
| Usage | Foundation for Android MDM functionality |
| Modification | Extended with ABIZ Locker business layer |

### MDMesh NOTICE (preserved per Apache-2.0 section 4d)
```
=========================================================================
==  NOTICE file corresponding to the section 4 d of                    ==
==  the Apache License, Version 2.0                                    ==
=========================================================================

This product is a derivative work based on Headwind MDM (Apache-2.0).

Upstream attribution (retained per Apache-2.0 section 4d):

  Headwind MDM
  https://h-mdm.com
  Copyright 2019-2024 Headwind MDM contributors
  Licensed under the Apache License, Version 2.0
```

---

## Upstream: Headwind MDM

| Field | Value |
|-------|-------|
| Project | Headwind MDM |
| Repository | https://h-mdm.com |
| License | Apache License, Version 2.0 |
| Usage | Original MDM server codebase (MDMesh is a fork) |
| Modification | Substantially rewritten in MDMesh; patterns retained |

---

## Apache License 2.0 Text

The full Apache License 2.0 text is available at:
http://www.apache.org/licenses/LICENSE-2.0

Key obligations for ABIZ Locker:
1. Retain the NOTICE file with Headwind MDM attribution
2. Retain the Apache-2.0 LICENSE file
3. State changes made to the original code (done via ABIZ package namespaces)
4. Do not use the names of original contributors for endorsement

---

## ABIZ Locker — New Code

All new ABIZ business logic code (com.abiz.locker.* packages) is proprietary
commercial code owned by ABIZ. It is NOT covered by the Apache-2.0 license.

The ABIZ Locker Android agent (com.abiz.locker Android app) is based on and
extends the MDMesh agent (Apache-2.0) — it retains the Apache-2.0 license
for the portions derived from MDMesh, and proprietary license for ABIZ additions.

---

## Third-Party Dependencies (to be confirmed during build)

### Java Backend
- Jersey (JAX-RS) — CDDL/GPL with Classpath Exception
- Guice — Apache-2.0
- MyBatis — Apache-2.0
- PostgreSQL JDBC — BSD-2-Clause
- Jackson — Apache-2.0

### React Frontend
- React — MIT
- Vite — MIT
- react-router — MIT
- TypeScript — Apache-2.0
- Recharts (to be added) — MIT
- react-query (to be added) — MIT

### Android Agent
- Kotlin — Apache-2.0
- Hilt/Dagger — Apache-2.0
- Retrofit — Apache-2.0
- OkHttp — Apache-2.0
- Room — Apache-2.0
- WorkManager — Apache-2.0
- kotlinx.serialization — Apache-2.0
- kotlinx.coroutines — Apache-2.0

---

## Compliance Checklist

- [x] MDMesh Apache-2.0 LICENSE file retained
- [x] MDMesh NOTICE file retained (Headwind MDM attribution)
- [x] ABIZ Locker code in separate package namespaces (com.abiz.locker.*)
- [x] No removal of legally required notices
- [ ] Full dependency license audit (to be completed before production release)
- [ ] Android agent license header audit
- [ ] SBOM (Software Bill of Materials) generation for production

---

## Questions for Legal Review
1. Is the ABIZ Android agent license (derivative of Apache-2.0 MDMesh agent) acceptable for commercial distribution?
2. Are there any CDDL (Jersey) license concerns for the server deployment model?
3. Does the planned SaaS deployment model affect any open-source license obligations?
