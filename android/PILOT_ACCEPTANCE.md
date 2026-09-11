# Android browsing pilot — configuration and final acceptance

The [local MVP](MVP_STATUS.md) is implemented. This document specifies the remaining
live acceptance work; none is executed or authorized merely by documenting it.
Initial audience/media must be synthetic and **original access remains off**.
Keep all actual origins, account/library references and credentials in a private
operator handoff, not this public repository, its logs or screenshots.

## Coordinator inputs, in execution order

1. **Reviewed backend release and cutover plan.** Supply the selected immutable
   source/package digest, runtime lock, schema `b6e3f9a5c721`, Windows interpreter/
   runtime/ACL evidence, backup/recovery provenance, service identity, explicit
   protected launcher and rehearsed cutover/rollback procedure. Identify and close
   legacy/standalone ingress. Current coordinator checkpoint is
   `ceff3ab7ea89020b8909eff988177fa65a196179`, implementation
   `5103abdd0861c2dbb7553d6edeccddbb170deed7`, with reported 277 security tests and
   53-file package smoke. Those reports are not deployment. Existing launch scripts
   selecting `app.main:app` intentionally start closed; they require deployment
   review, not an Android workaround. The backend default branch is `master`;
   mergeability or commits ahead establish neither rollout nor endpoint readiness.
2. **Approved served HTTPS and client route.** Supply the exact HTTPS origin,
   hostname/chain verification from the intended client route, deployed release
   identity at that origin, ingress restrictions and certificate reload/renewal
   evidence. No path prefix/query/userinfo/fragment. DNS/DuckDNS or certificate
   preparation does not establish a served origin, NAT/VPN routing or authorization.
   Normal Android system trust is required; no user-CA/debug/cleartext bypass.
3. **Synthetic owner, audience, library and prepared previews.** Supply private
   references for one reviewed owner/library, new viewer phone login, finite
   sample assets/captions and already-cached thumbnails. Confirm viewer original
   grants are zero (`originals_allowed=false`). First-owner recovery is implemented
   but needs operator review and a fresh protected password. It revokes other
   memberships/original grants; a new invitation cannot recover an already-existing
   disabled viewer. Use a new synthetic phone account or a separately reviewed
   recovery path. Issue the owner-controlled phone-bound invitation privately;
   never use a phone number as proof of identity or an invitation as a media URL.
4. **Configured artifact and build authorization.** Supply approval for the chosen
   Android source, private origin, local build lane and pilot artifact handling.
   Then set `photohouseOrigin` in ignored `android/local.properties` (or the reviewed
   equivalent Gradle property), build the connected debug app and record source SHA,
   origin reference, package/version, APK SHA-256, signer certificate digest and
   fresh build/lint result. Verify the frozen pin and selected backend compatibility
   rather than substituting its newer HEAD into the strict verifier. Record which
   interpreter/lock is used for any authorized adapter/TLS replay. Do not put
   passwords, bearer tokens or invitations in Gradle inputs. Keep a configured APK
   private because it embeds the origin. The prior empty-origin APK is not this
   artifact; UI tests expecting an empty origin require a separate unconfigured build.
5. **Explicit phone/run authorization and acceptance operator.** Supply the exact
   target phone/OS, selected artifact and authorized operator/test window. After
   installation, verify actual package, version, signer and launch state. Execute
   the scenario matrix below on the approved route; record redacted server receipts
   separately from phone observations. No token/code/password or private-media
   capture is acceptable evidence. Complete TalkBack/large-text/landscape, keyboard,
   back navigation, cold start, real Activity background/foreground, connectivity
   transitions and performance checks. Assign an owner to fix any observed defect;
   green source/JVM results do not waive a device failure.
6. **Outside-home route and final acceptance.** If outside-home use is intended,
   obtain separate authorization for that route/ingress test and repeat admission,
   protected reads and denial without widening access implicitly. Record certificate
   and route identity, offline behavior, logout/revocation and explicit family
   acceptance of the exact installed build. Audio/codecs/original grants are a later
   separately authorized addition; keep them off for this first browsing acceptance.

## Scenario matrix for the authorized run

Each row needs observed expected behavior, actual source/artifact/service identity,
an operator and a pass/fail record. Use only synthetic data; reference confidential
receipts privately instead of recording credentials or reusable URLs here.

| Scenario | Required observation |
| --- | --- |
| Fresh invited registration | Valid owner-issued invitation, correct phone and protected new password create viewer access only to the invited library; no open signup or automatic ownership. |
| Explicit logout, then normal login | Local protected state clears immediately; server acknowledgement is recorded separately. A fresh phone/password login retrieves only current memberships. No SMS/WeChat identity claim. |
| Browse | Own library -> paged gallery -> detail -> literal caption -> authenticated cached thumbnail; refresh, next/previous and Back retain appropriate page. Check EN/ZH, empty page/caption/date and missing preview. |
| Initial originals-off policy | No Open original/Open video control or original/Range request. Missing thumbnail stays a placeholder; no provider/original fallback. |
| Admission refusal | Invalid, consumed and wrong-phone invitation attempts fail without granting an account/library session or automatic retries. Distinguish server rejection evidence from a client-only simulated error. |
| Scope denial | Another library/asset and unavailable/closed membership are denied; prior approval/cached content does not grant access. Verify rejection before media/provider access on the backend. |
| Logout/revocation/disable | Backend rejects the old session; on subsequent protected read/revalidation the client clears credentials and private content. Late protected responses cannot restore them. No immediate push-revocation guarantee is claimed for already displayed content. |
| Privacy/lifecycle | Background content is covered; foreground revalidates before exposure; cold start is signed out; expiry clears state. Verify task-switcher/keyboard/rotation behavior and no persistent media/session residue. |
| Errors and recovery | Offline/503 retain an appropriate covered/unavailable state; 401 rechecks once; closed boundary stays closed; 429 waits for server cooldown and explicit retry. An uncertain invitation submission is not automatically repeated. |
| Accessibility and acceptance | Controls remain reachable with 200% text, TalkBack, portrait/landscape and the selected keyboard. Complete the actual phone flow and record family acceptance/performance limits. |

## Reproduction lanes and result record

The current no-listener allowance covers [direct cached JVM checks](readiness/README.md)
and [in-process contract replay](../docs/evidence/android/locked-profile-replay/RETURN.md)
when a concrete source/dependency change warrants replay. Preserve evidence in a new
Android-owned directory; do not overwrite historical reports or regenerate fixtures.

Ordinary Gradle, MockWebServer, actual-backend TLS and emulator scripts open local
connections or perform device actions. They are outside that allowance, including
Gradle `--no-daemon`. Use them only in a later explicitly approved build/device lane.
No command here configures an origin or begins an installation.

A final private acceptance record should bind: Android source and APK/signer;
backend release/package/runtime lock/schema; served origin/chain/route; synthetic
owner/viewer/library/cached-preview references and originals-off policy; each matrix
result; server revocation receipt versus client clearing; accessibility observations;
remaining defects and explicit acceptance. Leave incomplete gates marked pending.
