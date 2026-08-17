create table if not exists app_users (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    openid varchar(128) not null unique,
    nickname varchar(128) not null
);

create table if not exists session_tokens (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    token varchar(160) not null unique,
    user_id uuid not null,
    expires_at timestamp with time zone not null
);

create table if not exists families (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    name varchar(128) not null,
    owner_user_id uuid not null
);

create table if not exists family_members (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    family_id uuid not null,
    user_id uuid not null,
    role varchar(32) not null,
    relation varchar(32) not null,
    unique (family_id, user_id)
);

create table if not exists mother_profiles (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    family_id uuid not null,
    owner_user_id uuid not null,
    birthday date,
    height_cm integer,
    pre_pregnancy_weight_kg numeric(6,2),
    blood_type varchar(16)
);

create table if not exists pregnancy_profiles (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    family_id uuid not null,
    lmp_date date not null,
    due_date date not null,
    fetus_count integer not null,
    status varchar(32) not null
);

create table if not exists baby_profiles (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    family_id uuid not null,
    pregnancy_id uuid,
    name varchar(128) not null,
    gender varchar(32) not null,
    birth_date_time timestamp with time zone,
    birth_weight_kg numeric(6,2),
    birth_length_cm numeric(6,2)
);

create table if not exists records (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    family_id uuid not null,
    subject_type varchar(32) not null,
    subject_id uuid not null,
    record_type varchar(64) not null,
    occurred_at timestamp with time zone not null,
    payload_json text not null,
    privacy_level varchar(32) not null
);

create table if not exists medical_reports (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    family_id uuid not null,
    subject_type varchar(32) not null,
    subject_id uuid not null,
    report_type varchar(64) not null,
    title varchar(256) not null,
    examined_at date not null,
    indicators_json text not null
);

create table if not exists todos (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    family_id uuid not null,
    title varchar(256) not null,
    category varchar(64) not null,
    subject_type varchar(32),
    subject_id uuid,
    due_at timestamp with time zone,
    status varchar(32) not null
);

create table if not exists reminders (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    family_id uuid not null,
    title varchar(256) not null,
    scene varchar(64) not null,
    subject_type varchar(32),
    subject_id uuid,
    trigger_at timestamp with time zone not null,
    status varchar(32) not null
);

create table if not exists ai_audit_logs (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    family_id uuid,
    user_id uuid,
    purpose varchar(64) not null,
    provider varchar(64) not null,
    model varchar(128) not null,
    input_type varchar(32) not null,
    input_length integer not null,
    input_preview varchar(256) not null,
    risk_level varchar(32) not null,
    blocked boolean not null,
    fallback_used boolean not null,
    error_code varchar(64) not null,
    prompt_tokens integer not null,
    completion_tokens integer not null,
    total_tokens integer not null,
    latency_ms bigint not null,
    cost_currency varchar(16) not null,
    estimated_cost numeric(12,6) not null,
    status varchar(32) not null,
    policy_version varchar(64) not null default 'n/a',
    policy_configured boolean not null default false,
    safety_policy varchar(64) not null default 'draft_only',
    risk_reasons varchar(256) not null default ''
);

create table if not exists ai_configs (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    config_type varchar(32) not null,
    config_key varchar(128) not null,
    display_name varchar(160) not null,
    provider varchar(64) not null,
    status varchar(32) not null,
    config_json text not null,
    version_label varchar(64) not null,
    created_by varchar(128) not null
);

create table if not exists ai_draft_confirmations (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    family_id uuid not null,
    user_id uuid not null,
    subject_type varchar(32) not null,
    subject_id uuid not null,
    provider varchar(64) not null,
    model varchar(128) not null,
    purpose varchar(64) not null,
    draft_preview varchar(256) not null,
    record_ids_json text not null,
    report_ids_json text not null,
    todo_ids_json text not null,
    confirmed_at timestamp with time zone not null
);

create table if not exists ai_preprocess_audit_logs (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    purpose varchar(64) not null,
    provider varchar(64) not null,
    preprocessor varchar(64) not null,
    file_url_preview varchar(256) not null,
    text_length integer not null,
    fallback_used boolean not null,
    error_code varchar(64) not null,
    latency_ms bigint not null,
    status varchar(32) not null
);

create index if not exists idx_records_family_occurred_at on records (family_id, occurred_at desc);
create index if not exists idx_records_family_type_occurred_at on records (family_id, record_type, occurred_at asc);
create index if not exists idx_session_tokens_token on session_tokens (token);
create index if not exists idx_mother_profiles_family on mother_profiles (family_id);
create index if not exists idx_baby_profiles_family on baby_profiles (family_id);
create index if not exists idx_reports_family_examined_at on medical_reports (family_id, examined_at desc);
create index if not exists idx_todos_family_due_at on todos (family_id, due_at asc);
create index if not exists idx_todos_family_subject on todos (family_id, subject_type, subject_id);
create index if not exists idx_reminders_family_trigger_at on reminders (family_id, trigger_at asc);
create index if not exists idx_reminders_family_subject on reminders (family_id, subject_type, subject_id);
create index if not exists idx_ai_audit_logs_created_at on ai_audit_logs (created_at desc);
create index if not exists idx_ai_configs_type_created_at on ai_configs (config_type, created_at desc);
create index if not exists idx_ai_draft_confirmations_family_confirmed on ai_draft_confirmations (family_id, confirmed_at desc);
create index if not exists idx_ai_preprocess_audit_created_at on ai_preprocess_audit_logs (created_at desc);
