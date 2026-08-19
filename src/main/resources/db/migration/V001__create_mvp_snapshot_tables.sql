create table if not exists sync_run (
    id text not null,
    started_at text not null,
    finished_at text,
    status text not null,
    message text,
    constraint sync_run_pk primary key (id)
);

create table if not exists daily_snapshot (
    id text not null,
    snapshot_date text not null,
    source text not null,
    sync_run_id text,
    created_at text not null,
    object_count integer not null default 0,
    checksum text,
    constraint daily_snapshot_pk primary key (id),
    constraint daily_snapshot_sync_run_fk foreign key (sync_run_id) references sync_run(id),
    constraint daily_snapshot_date_uk unique (snapshot_date)
);

create table if not exists snapshot_object (
    id text not null,
    daily_snapshot_id text not null,
    anytype_object_id text not null,
    anytype_type_id text,
    anytype_type_key text,
    object_name text,
    archived integer not null default 0,
    last_modified_date text,
    relevant_properties_hash text,
    relevant_properties_json text,
    constraint snapshot_object_pk primary key (id),
    constraint snapshot_object_daily_snapshot_fk foreign key (daily_snapshot_id) references daily_snapshot(id),
    constraint snapshot_object_uk unique (daily_snapshot_id, anytype_object_id)
);

create table if not exists activity_day (
    id text not null,
    activity_date text not null,
    source text not null,
    object_count integer not null default 0,
    created_at text not null,
    constraint activity_day_pk primary key (id),
    constraint activity_day_date_source_uk unique (activity_date, source)
);

create table if not exists observed_anytype_schema (
    id text not null,
    observed_at text not null,
    space_id text not null,
    space_name text not null,
    schema_json text not null,
    constraint observed_anytype_schema_pk primary key (id)
);

create index if not exists snapshot_object_anytype_object_idx on snapshot_object(anytype_object_id);
create index if not exists snapshot_object_type_key_idx on snapshot_object(anytype_type_key);
create index if not exists snapshot_object_last_modified_idx on snapshot_object(last_modified_date);
create index if not exists activity_day_activity_date_idx on activity_day(activity_date);
