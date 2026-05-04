SET NAMES utf8mb4;

-- 地区种子数据
INSERT INTO region (name, category, sort_order) VALUES
('国考', 'national', 0),
('北京', 'provincial', 1),
('上海', 'provincial', 2),
('广东', 'provincial', 3),
('浙江', 'provincial', 4),
('江苏', 'provincial', 5),
('山东', 'provincial', 6),
('河南', 'provincial', 7),
('四川', 'provincial', 8),
('湖北', 'provincial', 9),
('湖南', 'provincial', 10),
('福建', 'provincial', 11),
('安徽', 'provincial', 12),
('河北', 'provincial', 13),
('江西', 'provincial', 14),
('山西', 'provincial', 15),
('陕西', 'provincial', 16),
('重庆', 'provincial', 17),
('天津', 'provincial', 18),
('辽宁', 'provincial', 19),
('吉林', 'provincial', 20),
('黑龙江', 'provincial', 21),
('广西', 'provincial', 22),
('云南', 'provincial', 23),
('贵州', 'provincial', 24),
('甘肃', 'provincial', 25),
('内蒙古', 'provincial', 26),
('新疆', 'provincial', 27),
('宁夏', 'provincial', 28),
('青海', 'provincial', 29),
('海南', 'provincial', 30),
('西藏', 'provincial', 31);

-- 站点配置种子
INSERT INTO site_config (config_key, config_value) VALUES
('must_read', '# 进站必读\n\n欢迎来到 BALA 公考！\n\n## 使用指南\n\n1. 注册账号后即可使用全部功能\n2. 行测题库收录近6年真题\n3. 申论支持AI智能批改\n4. 每日打卡记录学习进度');
