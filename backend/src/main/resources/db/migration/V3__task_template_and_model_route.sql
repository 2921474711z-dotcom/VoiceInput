alter table app_config
    add column if not exists asr_model_route varchar(128);

alter table app_config
    add column if not exists llm_model_route varchar(128);

alter table app_config
    add column if not exists default_template_id varchar(36);

update app_config
set asr_model_route = coalesce(asr_model_route, 'mimo-v2.5'),
    llm_model_route = coalesce(llm_model_route, 'mimo-v2.5-pro')
where id = 'active';

alter table app_config
    alter column asr_model_route set not null;

alter table app_config
    alter column llm_model_route set not null;

alter table config_template
    add column if not exists asr_model_route varchar(128);

alter table config_template
    add column if not exists llm_model_route varchar(128);

update config_template
set asr_model_route = coalesce(asr_model_route, 'mimo-v2.5'),
    llm_model_route = coalesce(llm_model_route, 'mimo-v2.5-pro');

alter table config_template
    alter column asr_model_route set not null;

alter table config_template
    alter column llm_model_route set not null;

alter table processing_task
    add column if not exists template_id varchar(36);

alter table processing_task
    add column if not exists template_name varchar(255);

update processing_task
set template_name = coalesce(template_name, '未命名模板')
where template_name is null;
