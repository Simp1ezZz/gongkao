SET NAMES utf8mb4;

-- 默认管理员账号 (密码: admin123)
INSERT INTO user (email, role, password_hash, nickname) VALUES
('admin@admin.com', 'admin', '$2b$12$9hD6Qn7TeU9v2bZltYkKx.952wImwE3qEQC9Hlqp9DhTxEXkL.Blm', '管理员');

-- 站点配置种子
INSERT INTO site_config (config_key, config_value) VALUES
('must_read', '# 进站必读\n\n欢迎来到 BALA 公考！\n\n## 使用指南\n\n1. 注册账号后即可使用全部功能\n2. 行测题库收录近6年真题\n3. 申论支持AI智能批改\n4. 每日打卡记录学习进度');
