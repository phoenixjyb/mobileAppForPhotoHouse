# Phone Home mode

The owner explicitly requested no-sign-in phone access on the home LAN, matching
TV. Phone versionCode 6 (`0.7-phone-home`) adds two explicit entry choices:
**At home** and **Sign in**. The latter retains the existing authenticated phone
module, original permissions and opt-in protected discovery/photo delivery.

Home mode consumes the existing published home v3 catalog and readiness extension
pinned at backend `a72aa320801787cd066c6e04c33764e9271c9ab5`. No new backend route,
protected grant or contract field is needed. It offers touch readiness/media filters,
pagination and numeric page jump, literal captions, retryable previews, optimized
photos and explicit permitted original quality, pinch/zoom/pan/fit/fill/fullscreen,
current-page slideshow and prepared or admitted direct video streaming/seek.

The Home Activity owns only HomeStore/HttpsCatalogApi. The account Activity owns
only ConnectedStore/HttpsPhotoHouseApi. The launcher constructs neither client;
neither transport falls back to the other. The account module's core has no home
dependency. Only the APK composition layer joins the two destinations. Their shared
player receives an owned byte-reader interface, never an origin, bearer or URL.

Home mode requires its own `photohousePhoneHomeOrigin` and
`photohousePhoneHomeLanAddress` private build properties. The LAN address is the server address, not the phone
address or a DNS server. Both default empty and
invalid/missing values fail closed. HomeOrigin enforces normal HTTPS, HomeLanAddress
accepts only numeric RFC1918 IPv4, and its existing DNS mapping/no-proxy policy
prevents public-DNS fallback while retaining hostname and system certificate checks.
Neither field inherits `photohouseOrigin`, which remains the protected account origin.

Home access is enforced by the server's allowed peer policy, not by SSID, an Android
Wi-Fi indicator or merely having this APK. The current v13 server's narrow approved
peer list is not automatically expanded by installing this build. A new phone needs
its LAN address admitted by the owner before real home testing. Broad subnet access,
public ingress and account authentication changes are not implemented here.

All Activities retain secure-window and no-backup policy. Home background/exit
cancels readers/requests and clears catalog/media; foreground revalidates through a
fresh catalog. The account Activity retains its existing independent lifecycle.
No persistent media/session cache, server auto-discovery, background downloads,
automatic login, TV discovery, advanced-search enablement or 4K negotiation is added.

The shared phone player now distinguishes native failure from expected teardown,
closes the source before reporting one failure, exposes audio-focus refusal and
keeps Home errors on the selected media with a retry action. Account failure
callbacks remain bound to their original reader. Home callbacks also verify reader
identity, so late callbacks cannot fail another media selection.

Validation and artifact evidence: [return](../docs/evidence/android/phone-home/RETURN.md).
