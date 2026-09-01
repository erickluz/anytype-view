create table if not exists study_pause_note (
    id text not null,
    start_date text not null,
    end_date text not null,
    reason text not null,
    created_at text not null,
    constraint study_pause_note_pk primary key (id),
    constraint study_pause_note_dates_ck check (end_date >= start_date)
);

create index if not exists study_pause_note_start_date_idx on study_pause_note(start_date);
