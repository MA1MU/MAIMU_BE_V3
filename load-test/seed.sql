SET FOREIGN_KEY_CHECKS=0; TRUNCATE maimus; TRUNCATE lockers; TRUNCATE members; SET FOREIGN_KEY_CHECKS=1;
-- 회원 50명, 회원마다 사물함 1개 (group_id = member_id)
INSERT INTO members (member_id, member_email, member_name, member_nickname, member_provider, member_provider_id, member_role, member_maimu_profile, created_at, modified_at)
WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n < 50)
SELECT n, CONCAT('u', n, '@bench.test'), CONCAT('user', n), CONCAT('nick', n), 'kakao', CONCAT('pid', n), 'MEMBER', '유자', NOW(6), NOW(6) FROM seq;
INSERT INTO lockers (group_id, member_id, group_name, group_color, created_at, modified_at)
SELECT member_id, member_id, '1학년 3반', '핑크', NOW(6), NOW(6) FROM members;
-- 인기 사물함(group 1)에 기존 마이무 300개
INSERT INTO maimus (group_id, maimu_title, maimu_message, maimu_color, maimu_writer_name, is_anonymous, maimu_sugar_content, is_favorite, is_read, is_notified, created_at, modified_at)
WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n < 300)
SELECT 1, CONCAT('첫인상 ', n), REPEAT('처음 봤을 때 정말 밝아 보였어! ', 10), 'RED', '익명', 1, 70, 0, 1, 1, NOW(6), NOW(6) FROM seq;
