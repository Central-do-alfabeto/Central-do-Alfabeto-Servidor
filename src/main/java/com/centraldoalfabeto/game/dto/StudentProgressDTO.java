package com.centraldoalfabeto.game.dto;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StudentProgressDTO {
    private UUID studentId;

    private String studentName;

    private String studentEmail;

    /** Próxima fase que o aluno deve jogar. */
    private Integer currentPhaseIndex;

    /** Última fase concluída pelo aluno (ou null se ainda não completou nenhuma). */
    private Integer lastCompletedPhaseIndex;

    /** Soma total de erros registrados em todas as fases. */
    private Long errorsDataJson;

    /** Soma total de reproduções de áudio em todas as fases. */
    private Long soundRepeatsDataJson;

    /** Lista com o histórico completo de snapshots de progresso. */
    private List<PlayerDataSnapshotDTO> snapshots = Collections.emptyList();
}
