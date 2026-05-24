alter table export_record
    add column if not exists task_title varchar(255),
    add column if not exists content_type varchar(128),
    add column if not exists content_source varchar(32),
    add column if not exists size_bytes bigint,
    add column if not exists status varchar(32) not null default 'SUCCESS',
    add column if not exists error_message text;

alter table processing_task
    add column if not exists proofread_raw_text text,
    add column if not exists proofread_optimized_text text,
    add column if not exists proofread_markdown_content text,
    add column if not exists proofread_revision_id varchar(36),
    add column if not exists proofread_at timestamp;

create table if not exists proofread_revision (
    id varchar(36) primary key,
    task_id varchar(36) not null,
    before_raw_text text,
    before_optimized_text text,
    before_markdown_content text,
    after_raw_text text not null,
    after_optimized_text text not null,
    after_markdown_content text,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table if not exists model_connection_test (
    id varchar(36) primary key,
    target varchar(16) not null,
    provider varchar(128),
    base_url varchar(512),
    model_name varchar(128),
    status varchar(32) not null,
    message text,
    duration_ms bigint,
    upload_id varchar(36),
    created_at timestamp not null,
    updated_at timestamp not null
);
