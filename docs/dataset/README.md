# CAR-ZTNA Architecture Dataset Specification

This directory contains the reference dataset for the **Context-Aware Role-based Zero Trust Network Architecture (CAR-ZTNA)** project.

The dataset models a complete corporate Zero Trust environment under the guiding principle of **"Never Trust, Always Verify"**, operating on pure **Role-Based Access Control (RBAC)** and **Attribute-Based Access Control (ABAC)** contextual logic without arbitrary numerical risk/trust score heuristics.

---

## Dataset Files & Schema

### 1. `users.csv`
Represents the user identity store and account security state.
- **id**: Unique user identifier (Primary Key).
- **username**: Unique account identity (`alice`, `bob`, `carol`, `dave`, `superadmin`, `eve`, `frank`, `grace`).
- **password**: Secure BCrypt hash (cost factor 10).
- **role**: Authorization tier (`USER`, `ADMIN`, `SECURITY_ANALYST`).
- **status**: Account status (`ACTIVE`, `SUSPENDED`, `LOCKED`, `DELETED`).
- **locked**: Boolean lockout flag triggered by security threshold breaches.
- **lock_reason**: Reason for account lock (`FAILED_ATTEMPTS`, `IP_VIOLATION`, `ADMIN_SUSPENDED`, or empty).
- **created_at**: Timestamp of account onboarding.
- **last_login_at**: Timestamp of the latest authenticated session.

### 2. `devices.csv`
Hardware identity inventory and device posture verification.
- **id**: Unique device identifier (Primary Key).
- **user_id**: Foreign key mapping to `users.id`.
- **username**: Username of the assigned device owner.
- **device_fingerprint**: Cryptographic device fingerprint (SHA-256 derived from client characteristics).
- **device_name**: Human-readable device label.
- **device_type**: Form factor (`Workstation`, `Mobile`, `Tablet`, `Server`).
- **operating_system**: Host operating system platform.
- **browser**: User-agent client software.
- **status**: Device trust evaluation (`TRUSTED`, `UNREGISTERED`, `BLOCKED`).
- **label**: Usage context (`OFFICE`, `HOME`, `UNKNOWN`).
- **first_seen**: Timestamp when hardware was first observed.
- **last_seen**: Timestamp of the most recent interaction.
- **registered_at**: Timestamp when explicit trust was established (null if unregistered).
- **notes**: Security annotations and asset notes.

### 3. `network_rules.csv`
Network perimeter CIDR blocks and IP classifications for network context evaluation.
- **id**: Unique rule identifier.
- **ip_or_cidr**: IP address or CIDR subnet (e.g., `192.168.1.0/24`, `10.100.0.0/16`, `172.16.10.0/24`, `203.0.113.0/24`).
- **classification**: Environmental classification (`OFFICE`, `HOME`, `BLOCKED`, `UNKNOWN`).
- **description**: Administrative explanation of the subnet purpose.
- **active**: Boolean flag indicating whether the perimeter rule is currently enforced.
- **created_at**: Timestamp of rule configuration.

### 4. `resources.csv`
Catalog of protected corporate assets governed under least-privilege enforcement.
- **id**: Unique resource identifier.
- **resource_name**: Resource code (`FINANCIAL_DATA`, `INTERNAL_DOCUMENTS`, `EMPLOYEE_DIRECTORY`, `HR_RECORDS`, `SOURCE_CODE_REPO`, `ADMIN_CONSOLE`, `NETWORK_TOPOLOGY`, `AUDIT_LOGS`).
- **display_name**: Human-readable resource name.
- **description**: Summary of contents and sensitivity.
- **sensitivity_level**: Classification tier (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
- **api_endpoint**: Protected backend REST API route.

### 5. `access_policies.csv`
Policy Decision Point (PDP) rules evaluated using deterministic RBAC and ABAC attributes.
- **id**: Unique policy identifier.
- **policy_name**: Descriptive policy rule label.
- **resource_name**: Targeted protected resource.
- **required_role**: RBAC condition (`USER`, `ADMIN`, `SECURITY_ANALYST`, `ANY`).
- **allowed_network**: ABAC network condition (`OFFICE`, `HOME`, `ANY`).
- **required_device_status**: ABAC device posture condition (`TRUSTED`, `UNREGISTERED`, `ANY`).
- **effect**: Evaluation result (`ALLOW`, `DENY`). Explicit DENY policies take absolute precedence over conflicting ALLOW policies.
- **priority**: Numeric priority ordering (lower values take higher precedence).
- **enabled**: Boolean flag toggling policy enforcement.
- **description**: Policy rationale and governance objective.
- **created_at / updated_at**: Auditing timestamps.

### 6. `sessions.csv`
Active and historical zero-trust session lifecycle data.
- **id**: Unique session row ID.
- **session_id**: Cryptographic session token/UUID.
- **user_id / username**: Associated user identity.
- **ip_address**: Originating network IP address.
- **device_fingerprint**: Originating hardware fingerprint.
- **device_info**: Hardware and client summary.
- **login_time / last_activity / expires_at**: Session expiration and activity timestamps.
- **active**: Boolean status (`true`, `false`).
- **status**: Lifecycle state (`ACTIVE`, `TERMINATED`).
- **network_classification**: Contextual classification (`OFFICE`, `HOME`, `UNKNOWN`, `BLOCKED`).
- **termination_reason**: Reason for closure (`LOGOUT`, `TIMEOUT`, `ACCOUNT_LOCKED`, `DEVICE_BLOCKED`, `NETWORK_BLOCKED`, `ADMIN`).

### 7. `context_snapshots.csv`
Immutable point-in-time contextual snapshots recorded on every Policy Enforcement Point (PEP) evaluation.
- **id**: Unique snapshot record ID.
- **username**: Evaluated identity.
- **session_id**: Active session ID.
- **ip_address / device_fingerprint / device_info**: Source contextual telemetry.
- **device_status**: Hardware trust posture (`TRUSTED`, `UNREGISTERED`, `BLOCKED`).
- **network_classification**: Classified network environment (`OFFICE`, `HOME`, `UNKNOWN`, `BLOCKED`).
- **requested_resource**: Target protected asset.
- **network_zone**: Ingress perimeter zone (`INTERNAL`, `EXTERNAL`, `VPN`, `BLOCKED`).
- **login_time / hour_of_day**: Temporal context.
- **failed_attempts**: Consecutive failed attempt counter at evaluation time.
- **session_activity**: Behavioral heuristic (`NORMAL`, `SUSPICIOUS`).
- **location_hint**: Subnet origin description.
- **captured_at**: Exact timestamp of evaluation.

### 8. `login_attempts.csv`
Tracks brute-force detection and failed authentication thresholds.
- **id**: Unique record ID.
- **username**: Target account username.
- **failed_attempts**: Number of consecutive failed attempts.
- **last_attempt_time**: Timestamp of the latest failed attempt.

### 9. `security_logs.csv`
Immutable audit log of all Policy Enforcement Point (PEP) decisions and security events.
- **id**: Unique log record ID.
- **username**: Acting identity or `SYSTEM`.
- **event_type**: Event code (`LOGIN_SUCCESS`, `LOGIN_FAILED`, `PEP_ALLOW`, `PEP_DENY`, `SUPERKEY_GENERATE`, `SUPERKEY_UNLOCK`, `ACCOUNT_LOCKED`, `DEVICE_BLOCKED`, `NETWORK_BLOCKED`, `SESSION_TERMINATED`).
- **ip_address**: Ingress IP address.
- **session_id**: Associated session ID.
- **description**: Comprehensive narrative of the security event.
- **severity**: Event alert priority (`INFO`, `WARN`, `HIGH`, `CRITICAL`).
- **event_time**: ISO event timestamp.

### 10. `super_recovery_keys.csv`
Audit store for single-use administrative Super Recovery Keys ("break-glass" account unlock mechanism).
- **id**: Unique key record ID.
- **admin_session_id**: Session ID of the authorizing administrator.
- **admin_username**: Username of the administrator executing the unlock.
- **target_username**: Target locked account.
- **hashed_key**: BCrypt hash of the single-use recovery passphrase.
- **expires_at**: Strict expiration timestamp.
- **used**: Boolean indicating if the key was redeemed.
- **used_at**: Timestamp when the key was consumed.
- **created_at**: Timestamp of key issuance.

---

## CAR-ZTNA Policy Decision Logic (Summary)

The evaluation flow executed by the **Policy Decision Point (PDP)**:
1. **Account Lockout Check**: If `user.locked == true`, access is immediately rejected (`DENY`).
2. **Network Perimeter Check**: If `networkClassification == "BLOCKED"`, access is rejected (`DENY`).
3. **Device Posture Check**: If `deviceStatus == "BLOCKED"`, access is rejected (`DENY`).
4. **Policy Retrieval & Default-Deny**: If no enabled policies exist for `requested_resource`, access is denied by default (`DENY`).
5. **Explicit Deny Precedence**: If any matching policy has `effect == "DENY"`, it immediately overrides any `ALLOW` policy (`DENY`).
6. **Allow Policy Evaluation**: If a matching policy with `effect == "ALLOW"` matches the user's role, network classification, and device posture, access is granted (`ALLOW`).
7. **Implicit Deny**: If no `ALLOW` policy is satisfied, access is denied (`DENY`).
