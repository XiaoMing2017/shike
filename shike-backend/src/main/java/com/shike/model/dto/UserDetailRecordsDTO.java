package com.shike.model.dto;

import com.shike.model.entity.DietRecord;
import com.shike.model.entity.ExerciseRecord;
import com.shike.model.entity.TeamCheckin;
import com.shike.model.entity.User;
import com.shike.model.entity.WaterRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailRecordsDTO {
    private User user;
    private List<DietRecord> dietRecords;
    private List<ExerciseRecord> exerciseRecords;
    private List<WaterRecord> waterRecords;
    private List<TeamCheckin> teamCheckins;
}
