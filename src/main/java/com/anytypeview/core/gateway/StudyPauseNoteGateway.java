package com.anytypeview.core.gateway;

import com.anytypeview.core.dto.CreateStudyPauseNoteDTO;
import com.anytypeview.core.dto.StudyPauseNoteDTO;
import java.util.List;

public interface StudyPauseNoteGateway {

    List<StudyPauseNoteDTO> list();

    StudyPauseNoteDTO create(CreateStudyPauseNoteDTO note);

    StudyPauseNoteDTO update(String id, CreateStudyPauseNoteDTO note);

    void delete(String id);
}
