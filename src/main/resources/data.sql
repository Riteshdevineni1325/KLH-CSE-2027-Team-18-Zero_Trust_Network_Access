-- =============================================================
-- CAR-ZTNA  —  Schema migrations + minimal seed users
-- =============================================================

-- ─── 0. SCHEMA ADAPTATIONS ────────────────────────────────────
ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'ACTIVE';
ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS locked BOOLEAN DEFAULT FALSE;
ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS lock_reason VARCHAR(255);
ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;

ALTER TABLE IF EXISTS access_policies ADD COLUMN IF NOT EXISTS allowed_network VARCHAR(50) DEFAULT 'ANY';
ALTER TABLE IF EXISTS access_policies ADD COLUMN IF NOT EXISTS required_device_status VARCHAR(50) DEFAULT 'ANY';
ALTER TABLE IF EXISTS access_policies ADD COLUMN IF NOT EXISTS effect VARCHAR(50) DEFAULT 'ALLOW';
ALTER TABLE IF EXISTS access_policies ADD COLUMN IF NOT EXISTS priority INT DEFAULT 10;
ALTER TABLE IF EXISTS access_policies ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE IF EXISTS access_policies ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE IF EXISTS access_policies ALTER COLUMN sensitivity_level DROP NOT NULL;
ALTER TABLE IF EXISTS access_policies ALTER COLUMN required_network_zone DROP NOT NULL;
ALTER TABLE IF EXISTS access_policies ALTER COLUMN required_trust_level DROP NOT NULL;

-- Remove duplicate policies keeping the lowest id per policy_name
DELETE FROM access_policies a
USING access_policies b
WHERE a.id > b.id AND a.policy_name = b.policy_name;

-- Ensure unique index on policy_name
CREATE UNIQUE INDEX IF NOT EXISTS uq_access_policies_name ON access_policies (policy_name);

ALTER TABLE IF EXISTS sessions ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'ACTIVE';
ALTER TABLE IF EXISTS sessions ADD COLUMN IF NOT EXISTS network_classification VARCHAR(50) DEFAULT 'UNKNOWN';
ALTER TABLE IF EXISTS sessions ADD COLUMN IF NOT EXISTS termination_reason VARCHAR(255);

ALTER TABLE IF EXISTS context_snapshots ADD COLUMN IF NOT EXISTS network_classification VARCHAR(50) DEFAULT 'UNKNOWN';
ALTER TABLE IF EXISTS context_snapshots ADD COLUMN IF NOT EXISTS requested_resource VARCHAR(100);

ALTER TABLE IF EXISTS devices ADD COLUMN IF NOT EXISTS device_fingerprint VARCHAR(255);
ALTER TABLE IF EXISTS devices ADD COLUMN IF NOT EXISTS device_name VARCHAR(255);
ALTER TABLE IF EXISTS devices ADD COLUMN IF NOT EXISTS device_type VARCHAR(50);
ALTER TABLE IF EXISTS devices ADD COLUMN IF NOT EXISTS operating_system VARCHAR(100);
ALTER TABLE IF EXISTS devices ADD COLUMN IF NOT EXISTS browser VARCHAR(100);
ALTER TABLE IF EXISTS devices ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'UNREGISTERED';
ALTER TABLE IF EXISTS devices ADD COLUMN IF NOT EXISTS first_seen TIMESTAMP;
ALTER TABLE IF EXISTS devices ADD COLUMN IF NOT EXISTS last_seen TIMESTAMP;
ALTER TABLE IF EXISTS devices ADD COLUMN IF NOT EXISTS registered_at TIMESTAMP;
ALTER TABLE IF EXISTS devices ALTER COLUMN trusted DROP NOT NULL;
ALTER TABLE IF EXISTS devices ALTER COLUMN trusted SET DEFAULT false;

ALTER TABLE IF EXISTS users ALTER COLUMN ip_address DROP NOT NULL;
ALTER TABLE IF EXISTS users ALTER COLUMN office_ip DROP NOT NULL;
ALTER TABLE IF EXISTS users ALTER COLUMN home_ip DROP NOT NULL;

ALTER TABLE IF EXISTS security_logs ADD COLUMN IF NOT EXISTS severity VARCHAR(50) DEFAULT 'INFO';
UPDATE security_logs SET severity = 'INFO' WHERE severity IS NULL;
ALTER TABLE IF EXISTS security_logs ADD COLUMN IF NOT EXISTS session_id VARCHAR(255);
ALTER TABLE IF EXISTS security_logs ADD COLUMN IF NOT EXISTS ip_address VARCHAR(100);
ALTER TABLE IF EXISTS security_logs ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE IF EXISTS security_logs ADD COLUMN IF NOT EXISTS event_time TIMESTAMP;

ALTER TABLE IF EXISTS login_attempts ADD COLUMN IF NOT EXISTS failed_attempts INT DEFAULT 0;
ALTER TABLE IF EXISTS login_attempts ADD COLUMN IF NOT EXISTS last_attempt_time TIMESTAMP;

CREATE TABLE IF NOT EXISTS network_rules (
    id BIGSERIAL PRIMARY KEY,
    ip_or_cidr VARCHAR(255) UNIQUE NOT NULL,
    classification VARCHAR(50) NOT NULL,
    description TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS resources (
    id BIGSERIAL PRIMARY KEY,
    resource_name VARCHAR(100) UNIQUE NOT NULL,
    display_name VARCHAR(255),
    description TEXT,
    sensitivity_level VARCHAR(50),
    api_endpoint VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS super_recovery_keys (
    id BIGSERIAL PRIMARY KEY,
    admin_session_id VARCHAR(255) NOT NULL,
    admin_username VARCHAR(255) NOT NULL,
    target_username VARCHAR(255) NOT NULL,
    hashed_key VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS admin_promotion_requests (
    id BIGSERIAL PRIMARY KEY,
    requester_username VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    reason TEXT,
    approved_by_admin VARCHAR(255),
    second_admin_username VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    resolved_at TIMESTAMP
);

-- ─── 1. CLEANUP TRANSIENT RUNTIME STATE ONLY ──────────────────
-- Clean up transient sessions and recovery keys on startup if desired,
-- but NEVER truncate persistent entities (users, devices, policies, rules, logs).
TRUNCATE TABLE super_recovery_keys CASCADE;
TRUNCATE TABLE sessions CASCADE;

-- Tables created by Hibernate that may not exist on first run — use DELETE to avoid
-- parsing issues with DO $$ blocks in Spring's SQL script runner.


-- ─── 2. SEED USERS ────────────────────────────────────────────
-- Passwords (BCrypt, cost 10):
--   alice / bob / dave  →  password123
--   superadmin          →  admin999
INSERT INTO users (username, password, role, status, locked, lock_reason, created_at)
VALUES
  ('alice',
   '$2a$10$7EqJtq98hPqEX7fNZaFWoOe2JDbEeFwHI6UQWF7dVSmE7bGcC9.sG',
   'USER', 'ACTIVE', false, NULL, NOW()),

  ('bob',
   '$2a$10$7EqJtq98hPqEX7fNZaFWoOe2JDbEeFwHI6UQWF7dVSmE7bGcC9.sG',
   'USER', 'ACTIVE', false, NULL, NOW()),

  ('dave',
   '$2a$10$7EqJtq98hPqEX7fNZaFWoOe2JDbEeFwHI6UQWF7dVSmE7bGcC9.sG',
   'USER', 'LOCKED', true, 'FAILED_ATTEMPTS', NOW()),

  ('superadmin',
   '$2a$10$Dow8berTR/lGMBGHHT4XuOy3Xr4pAXFbnkFTCv3FHBJVHJnJ.LXz2',
   'ADMIN', 'ACTIVE', false, NULL, NOW()),

  ('carol',
   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LnS37oXTteW',
   'ADMIN', 'ACTIVE', false, NULL, NOW()),

  ('eve',
   '$2a$10$7EqJtq98hPqEX7fNZaFWoOe2JDbEeFwHI6UQWF7dVSmE7bGcC9.sG',
   'USER', 'SUSPENDED', true, 'IP_VIOLATION', NOW()),

  ('frank',
   '$2a$10$7EqJtq98hPqEX7fNZaFWoOe2JDbEeFwHI6UQWF7dVSmE7bGcC9.sG',
   'USER', 'ACTIVE', false, NULL, NOW()),

  ('grace',
   '$2a$10$7EqJtq98hPqEX7fNZaFWoOe2JDbEeFwHI6UQWF7dVSmE7bGcC9.sG',
   'USER', 'ACTIVE', false, NULL, NOW())
ON CONFLICT (username) DO NOTHING;

-- ─── 3. LOGIN ATTEMPTS ────────────────────────────────────────
INSERT INTO login_attempts (username, failed_attempts, last_attempt_time)
VALUES
  ('alice',      0, NULL),
  ('bob',        0, NULL),
  ('carol',      0, NULL),
  ('dave',       4, NOW()),
  ('superadmin', 0, NULL),
  ('eve',        5, NOW()),
  ('frank',      0, NULL),
  ('grace',      0, NULL)
ON CONFLICT (username) DO NOTHING;

-- ─── 4. RESOURCE CATALOG ──────────────────────────────────────
INSERT INTO resources (resource_name, display_name, description, sensitivity_level, api_endpoint)
VALUES
  ('FINANCIAL_DATA',     'Financial Accounts',     'Q3 Ledgers, Audit Reports, Banking Info',     'HIGH',     '/api/protected/financial-data'),
  ('INTERNAL_DOCUMENTS', 'Internal Documentation', 'Engineering Specs, Architecture Blueprints',  'MEDIUM',   '/api/protected/internal-documents'),
  ('EMPLOYEE_DIRECTORY', 'Employee Directory',     'Staff Directory, Contact Info',                'LOW',      '/api/protected/employee-directory'),
  ('HR_RECORDS',         'HR Records',             'Employee Compensation and Personnel Files',    'HIGH',     '/api/protected/hr-records'),
  ('SOURCE_CODE_REPO',   'Source Code Repository', 'Proprietary Core Git Repositories',            'HIGH',     '/api/protected/source-code-repo'),
  ('ADMIN_CONSOLE',      'Admin Console',          'Root Configuration and Security Controls',     'CRITICAL', '/api/protected/admin-console'),
  ('NETWORK_TOPOLOGY',   'Network Topology',       'Internal Routing and Firewall Infrastructure', 'CRITICAL', '/api/protected/network-topology'),
  ('AUDIT_LOGS',         'Security Audit Logs',    'Immutable PEP Decision Records and Logs',      'HIGH',     '/api/audit/logs')
ON CONFLICT (resource_name) DO NOTHING;

-- ─── 5. ACCESS POLICIES ───────────────────────────────────────
INSERT INTO access_policies
  (policy_name, resource_name, required_role, allowed_network,
   required_device_status, effect, priority, enabled, description, created_at, updated_at)
VALUES
  ('Employee Directory — All Users',
   'EMPLOYEE_DIRECTORY', 'ANY',   'ANY',    'ANY',          'ALLOW', 50, true,
   'All authenticated users can view the employee directory', NOW(), NOW()),

  ('Internal Docs — Office',
   'INTERNAL_DOCUMENTS', 'USER',  'OFFICE', 'ANY',          'ALLOW', 40, true,
   'Internal documents accessible from the office network',   NOW(), NOW()),

  ('Internal Docs — Trusted Home',
   'INTERNAL_DOCUMENTS', 'USER',  'HOME',   'TRUSTED',      'ALLOW', 40, true,
   'Internal documents from home on a trusted device',        NOW(), NOW()),

  ('Financial Data — Office Trusted',
   'FINANCIAL_DATA',     'USER',  'OFFICE', 'TRUSTED',      'ALLOW', 20, true,
   'Financial data: office network + trusted device',         NOW(), NOW()),

  ('Financial Data — Home Trusted',
   'FINANCIAL_DATA',     'USER',  'HOME',   'TRUSTED',      'ALLOW', 20, true,
   'Financial data from home on a trusted device',            NOW(), NOW()),

  ('HR Records — Office Trusted',
   'HR_RECORDS',         'USER',  'OFFICE', 'TRUSTED',      'ALLOW', 20, true,
   'HR records: office network + trusted hardware',           NOW(), NOW()),

  ('Source Code — Trusted Device',
   'SOURCE_CODE_REPO',   'USER',  'ANY',    'TRUSTED',      'ALLOW', 30, true,
   'Source code requires a trusted workstation',              NOW(), NOW()),

  ('Admin Console — Admin Trusted',
   'ADMIN_CONSOLE',      'ADMIN', 'ANY',    'TRUSTED',      'ALLOW', 10, true,
   'Admin console: ADMIN role + trusted device',              NOW(), NOW()),

  ('Admin Console — Admin Office',
   'ADMIN_CONSOLE',      'ADMIN', 'OFFICE', 'ANY',          'ALLOW', 10, true,
   'Admin console: ADMIN from office network',                NOW(), NOW()),

  ('Network Topology — Admin Office Trusted',
   'NETWORK_TOPOLOGY',   'ADMIN', 'OFFICE', 'TRUSTED',      'ALLOW', 10, true,
   'Network topology: ADMIN + office + trusted device',       NOW(), NOW()),

  ('Audit Logs — Admin Trusted',
   'AUDIT_LOGS',         'ADMIN', 'ANY', 'TRUSTED', 'ALLOW', 15, true,
   'Security logs inspection allowed for administrators on trusted devices', NOW(), NOW()),

  ('Deny Unregistered — Financial',
   'FINANCIAL_DATA',     'ANY',   'ANY',    'UNREGISTERED', 'DENY',   5, true,
   'Block financial data access from unregistered devices',   NOW(), NOW()),

  ('Deny External — Network Topology',
   'NETWORK_TOPOLOGY',   'ANY',   'UNKNOWN','ANY',          'DENY',   2, true,
   'Prohibit network topology queries from unknown/external networks', NOW(), NOW())
ON CONFLICT (policy_name) DO NOTHING;

-- ─── 6. NETWORK RULES ─────────────────────────────────────────
INSERT INTO network_rules (ip_or_cidr, classification, description, active, created_at)
VALUES
  ('192.168.1.0/24',  'OFFICE',  'HQ Corporate Ethernet & Secured Wi-Fi Subnet',       true, NOW()),
  ('10.100.0.0/16',   'OFFICE',  'Internal Corporate Datacenter & Engineering Network', true, NOW()),
  ('127.0.0.1/32',    'OFFICE',  'Localhost Direct Management Interface',              true, NOW()),
  ('172.16.10.0/24',  'HOME',    'Approved Teleworker & Secure Remote Access Subnet',  true, NOW()),
  ('10.0.0.55/32',    'HOME',    'Designated Remote Home Workstation Static Route',    true, NOW()),
  ('203.0.113.0/24',  'BLOCKED', 'Identified Malicious External Reconnaissance Subnet', true, NOW()),
  ('198.51.100.99/32','BLOCKED', 'Hostile IP flagged for Brute Force & Stuffing',     true, NOW()),
  ('185.220.101.5/32','BLOCKED', 'Anonymizing Malicious Tor Exit Node',                 true, NOW())
ON CONFLICT (ip_or_cidr) DO NOTHING;

-- ─── 7. SEED DEVICES (idempotent: only insert if not already present) ───
INSERT INTO devices (user_id, device_fingerprint, device_name, device_type, operating_system, browser, status, label, first_seen, last_seen, registered_at, notes)
SELECT u.id, 'a1b2c3d4e5f601a2b3c4d5e6f708a9b0c1d2e3f4a5b6c7d8e9f0123456789abc', 'Corporate ThinkPad T14', 'Workstation', 'Windows 11 Pro', 'Chrome 128.0', 'TRUSTED', 'OFFICE', NOW(), NOW(), NOW(), 'Primary corporate workstation'
FROM users u WHERE u.username = 'alice'
  AND NOT EXISTS (SELECT 1 FROM devices d WHERE d.device_fingerprint = 'a1b2c3d4e5f601a2b3c4d5e6f708a9b0c1d2e3f4a5b6c7d8e9f0123456789abc');

INSERT INTO devices (user_id, device_fingerprint, device_name, device_type, operating_system, browser, status, label, first_seen, last_seen, registered_at, notes)
SELECT u.id, 'c3d4e5f601a2b3c4d5e6f708a9b0c1d2e3f4a5b6c7d8e9f0123456789abcdef0', 'Bob Linux Workstation', 'Workstation', 'Ubuntu 24.04 LTS', 'Firefox 130.0', 'TRUSTED', 'OFFICE', NOW(), NOW(), NOW(), 'Standard engineering workstation'
FROM users u WHERE u.username = 'bob'
  AND NOT EXISTS (SELECT 1 FROM devices d WHERE d.device_fingerprint = 'c3d4e5f601a2b3c4d5e6f708a9b0c1d2e3f4a5b6c7d8e9f0123456789abcdef0');

INSERT INTO devices (user_id, device_fingerprint, device_name, device_type, operating_system, browser, status, label, first_seen, last_seen, registered_at, notes)
SELECT u.id, 'e5f601a2b3c4d5e6f708a9b0c1d2e3f4a5b6c7d8e9f0123456789abcdef01234', 'SecOps Hardened Terminal', 'Workstation', 'Fedora 40 Security', 'Chrome 128.0', 'TRUSTED', 'OFFICE', NOW(), NOW(), NOW(), 'Dedicated SOC analyst workstation'
FROM users u WHERE u.username = 'carol'
  AND NOT EXISTS (SELECT 1 FROM devices d WHERE d.device_fingerprint = 'e5f601a2b3c4d5e6f708a9b0c1d2e3f4a5b6c7d8e9f0123456789abcdef01234');

INSERT INTO devices (user_id, device_fingerprint, device_name, device_type, operating_system, browser, status, label, first_seen, last_seen, registered_at, notes)
SELECT u.id, 'f601a2b3c4d5e6f708a9b0c1d2e3f4a5b6c7d8e9f0123456789abcdef0123456', 'Dave Corporate Dell Latitude', 'Workstation', 'Windows 11 Enterprise', 'Edge 128.0', 'TRUSTED', 'OFFICE', NOW(), NOW(), NOW(), 'Corporate laptop under security investigation'
FROM users u WHERE u.username = 'dave'
  AND NOT EXISTS (SELECT 1 FROM devices d WHERE d.device_fingerprint = 'f601a2b3c4d5e6f708a9b0c1d2e3f4a5b6c7d8e9f0123456789abcdef0123456');

INSERT INTO devices (user_id, device_fingerprint, device_name, device_type, operating_system, browser, status, label, first_seen, last_seen, registered_at, notes)
SELECT u.id, '01a2b3c4d5e6f708a9b0c1d2e3f4a5b6c7d8e9f0123456789abcdef012345678', 'Root Admin Secure Console', 'Workstation', 'Windows 11 Enterprise', 'Chrome 128.0', 'TRUSTED', 'OFFICE', NOW(), NOW(), NOW(), 'High-security jumpbox'
FROM users u WHERE u.username = 'superadmin'
  AND NOT EXISTS (SELECT 1 FROM devices d WHERE d.device_fingerprint = '01a2b3c4d5e6f708a9b0c1d2e3f4a5b6c7d8e9f0123456789abcdef012345678');

INSERT INTO devices (user_id, device_fingerprint, device_name, device_type, operating_system, browser, status, label, first_seen, last_seen, registered_at, notes)
SELECT u.id, '12a3b4c5d6e7f809a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3', 'Compromised Foreign Rig', 'Workstation', 'Kali Linux 2024.2', 'Firefox ESR', 'BLOCKED', 'UNKNOWN', NOW(), NOW(), NULL, 'Blacklisted attacker host'
FROM users u WHERE u.username = 'eve'
  AND NOT EXISTS (SELECT 1 FROM devices d WHERE d.device_fingerprint = '12a3b4c5d6e7f809a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3');

-- ─── 8. SEED SECURITY AUDIT LOGS (idempotent: only seed on initial empty log table) ───
INSERT INTO security_logs (username, event_type, ip_address, session_id, description, severity, event_time)
SELECT 'carol', 'LOGIN_SUCCESS', '192.168.1.25', 'sess-c303-carol-admin', 'Administrator carol authenticated successfully from office workstation', 'INFO', NOW() - INTERVAL '3 hours'
WHERE NOT EXISTS (SELECT 1 FROM security_logs WHERE session_id = 'sess-c303-carol-admin');

INSERT INTO security_logs (username, event_type, ip_address, session_id, description, severity, event_time)
SELECT 'carol', 'PEP_ALLOW', '192.168.1.25', 'sess-c303-carol-admin', 'Policy Enforcement: Allowed access to AUDIT_LOGS under policy ''Audit Logs — Admin Trusted''', 'INFO', NOW() - INTERVAL '2 hours 55 minutes'
WHERE NOT EXISTS (SELECT 1 FROM security_logs WHERE session_id = 'sess-c303-carol-admin' AND event_type = 'PEP_ALLOW');

INSERT INTO security_logs (username, event_type, ip_address, session_id, description, severity, event_time)
SELECT 'alice', 'LOGIN_SUCCESS', '192.168.1.10', 'sess-a101-alice-off', 'User authenticated successfully from office network', 'INFO', NOW() - INTERVAL '2 hours 30 minutes'
WHERE NOT EXISTS (SELECT 1 FROM security_logs WHERE session_id = 'sess-a101-alice-off');

INSERT INTO security_logs (username, event_type, ip_address, session_id, description, severity, event_time)
SELECT 'alice', 'PEP_ALLOW', '192.168.1.10', 'sess-a101-alice-off', 'Policy Enforcement: Allowed access to FINANCIAL_DATA under policy ''Financial Data — Office Trusted''', 'INFO', NOW() - INTERVAL '2 hours'
WHERE NOT EXISTS (SELECT 1 FROM security_logs WHERE session_id = 'sess-a101-alice-off' AND event_type = 'PEP_ALLOW');

INSERT INTO security_logs (username, event_type, ip_address, session_id, description, severity, event_time)
SELECT 'bob', 'LOGIN_SUCCESS', '172.16.10.15', 'sess-b202-bob-home', 'User authenticated via approved teleworker VPN', 'INFO', NOW() - INTERVAL '1 hour 45 minutes'
WHERE NOT EXISTS (SELECT 1 FROM security_logs WHERE session_id = 'sess-b202-bob-home');

INSERT INTO security_logs (username, event_type, ip_address, session_id, description, severity, event_time)
SELECT 'bob', 'PEP_ALLOW', '172.16.10.15', 'sess-b202-bob-home', 'Policy Enforcement: Allowed access to INTERNAL_DOCUMENTS under policy ''Internal Docs — Trusted Home''', 'INFO', NOW() - INTERVAL '1 hour 30 minutes'
WHERE NOT EXISTS (SELECT 1 FROM security_logs WHERE session_id = 'sess-b202-bob-home' AND event_type = 'PEP_ALLOW');

INSERT INTO security_logs (username, event_type, ip_address, session_id, description, severity, event_time)
SELECT 'superadmin', 'LOGIN_SUCCESS', '127.0.0.1', 'sess-s505-superadmin', 'Administrator authenticated into root management console', 'INFO', NOW() - INTERVAL '1 hour'
WHERE NOT EXISTS (SELECT 1 FROM security_logs WHERE session_id = 'sess-s505-superadmin');

INSERT INTO security_logs (username, event_type, ip_address, session_id, description, severity, event_time)
SELECT 'superadmin', 'PEP_ALLOW', '127.0.0.1', 'sess-s505-superadmin', 'Policy Enforcement: Allowed access to ADMIN_CONSOLE under policy ''Admin Console — Admin Office''', 'INFO', NOW() - INTERVAL '55 minutes'
WHERE NOT EXISTS (SELECT 1 FROM security_logs WHERE session_id = 'sess-s505-superadmin' AND event_type = 'PEP_ALLOW');

INSERT INTO security_logs (username, event_type, ip_address, session_id, description, severity, event_time)
SELECT 'eve', 'LOGIN_FAILED', '203.0.113.99', NULL, 'Failed login attempt from blacklisted subnet 203.0.113.0/24', 'HIGH', NOW() - INTERVAL '40 minutes'
WHERE NOT EXISTS (SELECT 1 FROM security_logs WHERE username = 'eve' AND event_type = 'LOGIN_FAILED');

INSERT INTO security_logs (username, event_type, ip_address, session_id, description, severity, event_time)
SELECT 'eve', 'NETWORK_BLOCKED', '203.0.113.99', NULL, 'Connection attempt terminated immediately by perimeter rule', 'CRITICAL', NOW() - INTERVAL '39 minutes'
WHERE NOT EXISTS (SELECT 1 FROM security_logs WHERE username = 'eve' AND event_type = 'NETWORK_BLOCKED');

INSERT INTO security_logs (username, event_type, ip_address, session_id, description, severity, event_time)
SELECT 'eve', 'DEVICE_BLOCKED', '203.0.113.99', NULL, 'Hardware fingerprint blacklisted after malicious exploit payload detected', 'CRITICAL', NOW() - INTERVAL '38 minutes'
WHERE NOT EXISTS (SELECT 1 FROM security_logs WHERE username = 'eve' AND event_type = 'DEVICE_BLOCKED');

INSERT INTO security_logs (username, event_type, ip_address, session_id, description, severity, event_time)
SELECT 'eve', 'PEP_DENY', '203.0.113.99', 'sess-e601-eve-intrud', 'Policy Enforcement: Access denied to ADMIN_CONSOLE due to NETWORK_BLOCK_POLICY', 'CRITICAL', NOW() - INTERVAL '37 minutes'
WHERE NOT EXISTS (SELECT 1 FROM security_logs WHERE session_id = 'sess-e601-eve-intrud' AND event_type = 'PEP_DENY');


