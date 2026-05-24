update app_config
set recognition_model = '通用语音识别 v3'
where recognition_model like '%?%'
   or recognition_model not in ('通用语音识别 v3', '快速语音识别 v3', '高精度语音识别 v3');

update app_config
set language_type = '中文（普通话）'
where language_type like '%?%'
   or language_type not in ('中文（普通话）', '中英混合', '英文');

update app_config
set domain_model = '通用领域'
where domain_model like '%?%'
   or domain_model not in ('通用领域', '会议场景', '技术表达', '客服沟通');

update app_config
set output_format = '纯文本'
where output_format like '%?%'
   or output_format not in ('纯文本', '结构化文本', 'Markdown');

update app_config
set stability_mode = '平衡'
where stability_mode like '%?%'
   or stability_mode not in ('更快', '平衡', '更准确');

update app_config
set optimization_model = '文本优化增强 v2'
where optimization_model like '%?%'
   or optimization_model not in ('文本优化增强 v2', '结构化整理 v2', '正式表达增强 v2');

update app_config
set optimization_goal = '会议纪要优化'
where optimization_goal like '%?%'
   or optimization_goal not in ('会议纪要优化', '工作汇报优化', '正式表达优化', 'Markdown 笔记优化', '代码注释优化', '聊天回复优化');

update app_config
set tone_style = '专业客观'
where tone_style like '%?%'
   or tone_style not in ('专业客观', '简洁直接', '正式严谨', '自然友好');

update app_config
set length_preference = '适中'
where length_preference like '%?%'
   or length_preference not in ('精简', '适中', '详细');

update app_config
set cost_mode = '成本优先'
where cost_mode like '%?%'
   or cost_mode not in ('成本优先', '质量优先');

update config_template
set recognition_model = '通用语音识别 v3'
where recognition_model like '%?%'
   or recognition_model not in ('通用语音识别 v3', '快速语音识别 v3', '高精度语音识别 v3');

update config_template
set language_type = '中文（普通话）'
where language_type like '%?%'
   or language_type not in ('中文（普通话）', '中英混合', '英文');

update config_template
set domain_model = '通用领域'
where domain_model like '%?%'
   or domain_model not in ('通用领域', '会议场景', '技术表达', '客服沟通');

update config_template
set output_format = '纯文本'
where output_format like '%?%'
   or output_format not in ('纯文本', '结构化文本', 'Markdown');

update config_template
set stability_mode = '平衡'
where stability_mode like '%?%'
   or stability_mode not in ('更快', '平衡', '更准确');

update config_template
set optimization_model = '文本优化增强 v2'
where optimization_model like '%?%'
   or optimization_model not in ('文本优化增强 v2', '结构化整理 v2', '正式表达增强 v2');

update config_template
set optimization_goal = '会议纪要优化'
where optimization_goal like '%?%'
   or optimization_goal not in ('会议纪要优化', '工作汇报优化', '正式表达优化', 'Markdown 笔记优化', '代码注释优化', '聊天回复优化');

update config_template
set tone_style = '专业客观'
where tone_style like '%?%'
   or tone_style not in ('专业客观', '简洁直接', '正式严谨', '自然友好');

update config_template
set length_preference = '适中'
where length_preference like '%?%'
   or length_preference not in ('精简', '适中', '详细');

update config_template
set cost_mode = '成本优先'
where cost_mode like '%?%'
   or cost_mode not in ('成本优先', '质量优先');
