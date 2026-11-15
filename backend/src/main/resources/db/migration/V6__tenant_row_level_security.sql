-- Defense in depth: application services set app.current_center_id with SET LOCAL
-- inside each transaction. Missing context intentionally matches no tenant rows.
DO $$
DECLARE table_name TEXT;
BEGIN
 FOREACH table_name IN ARRAY ARRAY[
  'students','teachers','courses','study_groups','lessons','attendance_records','grade_records'
 ] LOOP
  EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', table_name);
  EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', table_name);
  EXECUTE format(
   'CREATE POLICY tenant_isolation ON %I USING (center_id = NULLIF(current_setting(''app.current_center_id'', true), '''')::BIGINT) WITH CHECK (center_id = NULLIF(current_setting(''app.current_center_id'', true), '''')::BIGINT)',
   table_name
  );
 END LOOP;
END $$;
