ALTER TABLE group_enrollments ENABLE ROW LEVEL SECURITY;
ALTER TABLE group_enrollments FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON group_enrollments
 USING (center_id = NULLIF(current_setting('app.current_center_id', true), '')::BIGINT)
 WITH CHECK (center_id = NULLIF(current_setting('app.current_center_id', true), '')::BIGINT);
