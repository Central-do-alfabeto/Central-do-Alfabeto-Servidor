package com.centraldoalfabeto.game.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerDataSnapshotDTO {
    private Integer phaseIndex;
    private Long totalErrors;
    private Long totalAudioReproductions;
}
