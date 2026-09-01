package com.anytypeview.infra.gateway;

import com.anytypeview.core.dto.CreateStudyPauseNoteDTO;
import com.anytypeview.core.dto.StudyPauseNoteDTO;
import com.anytypeview.core.gateway.StudyPauseNoteGateway;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StudyPauseNoteGatewayImpl implements StudyPauseNoteGateway {

    private final JdbcTemplate jdbcTemplate;

    public StudyPauseNoteGatewayImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<StudyPauseNoteDTO> list() {
        return jdbcTemplate.query("""
            select id, start_date, end_date, reason
            from study_pause_note
            order by start_date desc, created_at desc
            """, (resultSet, rowNum) -> new StudyPauseNoteDTO(
                resultSet.getString("id"),
                resultSet.getString("start_date"),
                resultSet.getString("end_date"),
                resultSet.getString("reason")
            ));
    }

    @Override
    public StudyPauseNoteDTO create(CreateStudyPauseNoteDTO note) {
        validateDates(note);
        String reason = note.reason().trim();
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
            insert into study_pause_note (id, start_date, end_date, reason, created_at)
            values (?, ?, ?, ?, ?)
            """, id, note.startDate().toString(), note.endDate().toString(), reason, OffsetDateTime.now().toString());
        return new StudyPauseNoteDTO(id, note.startDate().toString(), note.endDate().toString(), reason);
    }

    @Override
    public StudyPauseNoteDTO update(String id, CreateStudyPauseNoteDTO note) {
        validateDates(note);
        String reason = note.reason().trim();
        int updated = jdbcTemplate.update("""
            update study_pause_note
            set start_date = ?, end_date = ?, reason = ?
            where id = ?
            """, note.startDate().toString(), note.endDate().toString(), reason, id);
        if (updated == 0) {
            throw new IllegalArgumentException("Anotação de pausa não encontrada.");
        }
        return new StudyPauseNoteDTO(id, note.startDate().toString(), note.endDate().toString(), reason);
    }

    private void validateDates(CreateStudyPauseNoteDTO note) {
        if (note.endDate().isBefore(note.startDate())) {
            throw new IllegalArgumentException("A data final deve ser igual ou posterior à data inicial.");
        }
    }

    @Override
    public void delete(String id) {
        jdbcTemplate.update("delete from study_pause_note where id = ?", id);
    }
}
