update hotword_category
set name = case code
    when 'ALL' then '全部热词'
    when 'TECH' then '技术术语'
    when 'PRODUCT' then '产品名称'
    when 'ABBREVIATION' then '英文缩写'
    when 'COMMON_MISS' then '常见误识别'
    when 'CUSTOM' then '自定义词库'
    else name
end
where code in ('ALL', 'TECH', 'PRODUCT', 'ABBREVIATION', 'COMMON_MISS', 'CUSTOM');

update app_config
set recognition_model = '通用语音识别 v3',
    language_type = '中文（普通话）',
    domain_model = '通用领域',
    output_format = '纯文本',
    stability_mode = '平衡',
    optimization_model = '文本优化增强 v2',
    optimization_goal = '会议纪要优化',
    tone_style = '专业客观',
    length_preference = '适中',
    cost_mode = '成本优先'
where id = 'active';
