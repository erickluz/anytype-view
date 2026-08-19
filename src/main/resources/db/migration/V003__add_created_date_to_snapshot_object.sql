alter table snapshot_object add column created_date text;

create index if not exists snapshot_object_created_date_idx on snapshot_object(created_date);
