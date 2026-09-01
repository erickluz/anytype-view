package com.anytypeview.inbound.http;

import com.anytypeview.core.dto.CreateStudyPauseNoteDTO;
import com.anytypeview.core.dto.StudyPauseNoteDTO;
import com.anytypeview.core.gateway.StudyPauseNoteGateway;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/study-pause-notes")
public class StudyPauseNoteController {

    private final StudyPauseNoteGateway studyPauseNoteGateway;

    public StudyPauseNoteController(StudyPauseNoteGateway studyPauseNoteGateway) {
        this.studyPauseNoteGateway = studyPauseNoteGateway;
    }

    @GetMapping
    public List<StudyPauseNoteDTO> list() {
        return studyPauseNoteGateway.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudyPauseNoteDTO create(@Valid @RequestBody CreateStudyPauseNoteDTO note) {
        return studyPauseNoteGateway.create(note);
    }

    @PutMapping("/{id}")
    public StudyPauseNoteDTO update(@PathVariable String id, @Valid @RequestBody CreateStudyPauseNoteDTO note) {
        return studyPauseNoteGateway.update(id, note);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        studyPauseNoteGateway.delete(id);
    }
}
