DO $$
DECLARE table_name TEXT;
BEGIN
 FOREACH table_name IN ARRAY ARRAY['parent_student_links','notifications','audit_logs'] LOOP
  EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',table_name);
  EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',table_name);
  EXECUTE format(
   'CREATE POLICY tenant_isolation ON %I USING (center_id = NULLIF(current_setting(''app.current_center_id'', true), '''')::BIGINT) WITH CHECK (center_id = NULLIF(current_setting(''app.current_center_id'', true), '''')::BIGINT)',
   table_name
  );
 END LOOP;
END $$;
