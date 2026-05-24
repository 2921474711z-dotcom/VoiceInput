create table if not exists uploaded_asset (
    id varchar(36) primary key,
    original_file_name varchar(255) not null,
    object_key varchar(512) not null,
    content_type varchar(128) not null,
    size_bytes bigint not null,
    duration_seconds numeric(10, 2),
    created_at timestamp not null,
    updated_at timestamp not null
);

create table if not exists processing_task (
    id varchar(36) primary key,
    scene_type varchar(32) not null,
    status varchar(16) not null,
    file_name varchar(255) not null,
    upload_id varchar(36),
    source_task_id varchar(36),
    version_index integer,
    title varchar(255),
    summary text,
    raw_text text,
    optimized_text text,
    markdown_content text,
    raw_word_count integer,
    optimized_word_count integer,
    hotword_hit_count integer,
    estimated_cost numeric(12, 6),
    recognition_duration_ms bigint,
    optimization_duration_ms bigint,
    total_duration_ms bigint,
    saved_to_history boolean not null default false,
    deleted boolean not null default false,
    model_config_snapshot text,
    hotword_matches_json text,
    error_message text,
    completed_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table if not exists hotword_category (
    id bigserial primary key,
    code varchar(64) not null unique,
    name varchar(128) not null,
    icon varchar(32),
    sort_order integer not null,
    enabled boolean not null default true
);

create table if not exists hotword (
    id varchar(36) primary key,
    recognized_term varchar(255) not null,
    standard_term varchar(255) not null,
    category_id bigint not null references hotword_category(id),
    scene_codes varchar(255) not null,
    enabled boolean not null default true,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table if not exists hotword_sample (
    id bigserial primary key,
    hotword_id varchar(36) not null references hotword(id) on delete cascade,
    sample_before text not null,
    sample_after text not null
);

create table if not exists app_config (
    id varchar(64) primary key,
    recognition_model varchar(128) not null,
    language_type varchar(64) not null,
    domain_model varchar(64) not null,
    output_format varchar(64) not null,
    stability_mode varchar(64) not null,
    optimization_model varchar(128) not null,
    optimization_goal varchar(128) not null,
    tone_style varchar(64) not null,
    length_preference varchar(64) not null,
    hotword_enabled boolean not null default true,
    cost_mode varchar(64) not null
);

create table if not exists config_template (
    id varchar(36) primary key,
    name varchar(255) not null,
    description varchar(512),
    recognition_model varchar(128) not null,
    language_type varchar(64) not null,
    domain_model varchar(64) not null,
    output_format varchar(64) not null,
    stability_mode varchar(64) not null,
    optimization_model varchar(128) not null,
    optimization_goal varchar(128) not null,
    tone_style varchar(64) not null,
    length_preference varchar(64) not null,
    hotword_enabled boolean not null default true,
    cost_mode varchar(64) not null,
    is_default_template boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table if not exists export_record (
    id varchar(36) primary key,
    task_id varchar(36) not null,
    file_name varchar(255) not null,
    object_key varchar(512) not null,
    export_type varchar(64) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

insert into hotword_category(code, name, icon, sort_order, enabled)
values
    ('ALL', '全部热词', 'grid', 0, true),
    ('TECH', '技术术语', 'braces', 1, true),
    ('PRODUCT', '产品名称', 'briefcase', 2, true),
    ('ABBREVIATION', '英文缩写', 'badge', 3, true),
    ('COMMON_MISS', '常见误识别', 'alert', 4, true),
    ('CUSTOM', '自定义词库', 'database', 5, true)
on conflict (code) do nothing;

insert into app_config(
    id, recognition_model, language_type, domain_model, output_format,
    stability_mode, optimization_model, optimization_goal, tone_style,
    length_preference, hotword_enabled, cost_mode
)
values (
    'active',
    '通用语音识别 v3',
    '中文（普通话）',
    '通用领域',
    '纯文本',
    '平衡',
    '文本优化增强 v2',
    '会议纪要优化',
    '专业客观',
    '适中',
    true,
    '成本优先'
)
on conflict (id) do nothing;
