
UPDATE users u
SET group_id = first_group.group_id
FROM (
  SELECT DISTINCT ON (user_id) user_id, group_id
  FROM group_members
  ORDER BY user_id, joined_at
) first_group
WHERE u.id = first_group.user_id
  AND u.group_id IS NULL;

SELECT u.id, u.name, u.email
FROM users u
JOIN group_members gm ON gm.user_id = u.id
WHERE u.group_id IS NULL;
