CREATE TABLE IF NOT EXISTS users (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(120) NOT NULL, email VARCHAR(180) NOT NULL UNIQUE,
 password_hash VARCHAR(300) NOT NULL, role VARCHAR(20) NOT NULL CHECK (role IN ('LECTURER','STUDENT'))
);
CREATE TABLE IF NOT EXISTS classes (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, lecturer_id BIGINT NOT NULL, name VARCHAR(160) NOT NULL,
 join_code VARCHAR(12) NOT NULL UNIQUE, FOREIGN KEY (lecturer_id) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS class_members (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, class_id BIGINT NOT NULL, student_id BIGINT NOT NULL,
 archetype VARCHAR(30) NOT NULL DEFAULT 'Code Warrior', exp_total INT NOT NULL DEFAULT 0 CHECK (exp_total >= 0),
 UNIQUE (class_id, student_id), FOREIGN KEY (class_id) REFERENCES classes(id), FOREIGN KEY (student_id) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS learning_modules (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, class_id BIGINT NOT NULL, title VARCHAR(200) NOT NULL,
 file_path VARCHAR(1000), extracted_text LONGTEXT, content_json LONGTEXT NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', FOREIGN KEY (class_id) REFERENCES classes(id)
);
CREATE TABLE IF NOT EXISTS quests (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, module_id BIGINT NOT NULL, title VARCHAR(200) NOT NULL,
 description LONGTEXT NOT NULL, deadline TIMESTAMP NOT NULL, allowed_types VARCHAR(200) NOT NULL,
 exp_reward INT NOT NULL CHECK (exp_reward > 0), rubric_json LONGTEXT NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', FOREIGN KEY (module_id) REFERENCES learning_modules(id)
);
CREATE TABLE IF NOT EXISTS submissions (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, quest_id BIGINT NOT NULL, student_id BIGINT NOT NULL,
 version INT NOT NULL, file_path VARCHAR(1000) NOT NULL, submitted_at TIMESTAMP NOT NULL,
 status VARCHAR(30) NOT NULL, final_score INT CHECK (final_score BETWEEN 0 AND 100), final_feedback LONGTEXT,
 reviewed_by BIGINT, reviewed_at TIMESTAMP NULL,
 UNIQUE (quest_id, student_id, version), FOREIGN KEY (quest_id) REFERENCES quests(id),
 FOREIGN KEY (student_id) REFERENCES users(id), FOREIGN KEY (reviewed_by) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS ai_evaluations (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, submission_id BIGINT NOT NULL, status VARCHAR(20) NOT NULL,
 extracted_text LONGTEXT, score_draft INT CHECK (score_draft BETWEEN 0 AND 100), feedback LONGTEXT, error_message LONGTEXT,
 created_at TIMESTAMP NOT NULL, FOREIGN KEY (submission_id) REFERENCES submissions(id)
);
CREATE TABLE IF NOT EXISTS exp_transactions (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, member_id BIGINT NOT NULL, quest_id BIGINT NOT NULL,
 exp_amount INT NOT NULL, awarded_at TIMESTAMP NOT NULL,
 UNIQUE (member_id, quest_id), FOREIGN KEY (member_id) REFERENCES class_members(id), FOREIGN KEY (quest_id) REFERENCES quests(id)
);
CREATE TABLE IF NOT EXISTS achievements (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, member_id BIGINT NOT NULL, badge_code VARCHAR(50) NOT NULL,
 earned_at TIMESTAMP NOT NULL, UNIQUE (member_id, badge_code), FOREIGN KEY (member_id) REFERENCES class_members(id)
);
