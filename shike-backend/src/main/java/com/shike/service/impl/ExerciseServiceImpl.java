package com.shike.service.impl;

import com.shike.model.entity.ExerciseRecord;
import com.shike.model.entity.User;
import com.shike.repository.ExerciseRecordRepository;
import com.shike.repository.UserRepository;
import com.shike.service.ExerciseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class ExerciseServiceImpl implements ExerciseService {

    @Autowired
    private ExerciseRecordRepository exerciseRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ExerciseRecord> getDailyRecords(Long userId, LocalDate date) {
        return exerciseRecordRepository.findByUserIdAndRecordDate(userId, date);
    }

    @Override
    @Transactional(readOnly = true)
    public double getDailyTotalCalories(Long userId, LocalDate date) {
        List<ExerciseRecord> records = exerciseRecordRepository.findByUserIdAndRecordDate(userId, date);
        return records.stream().mapToDouble(ExerciseRecord::getCaloriesBurned).sum();
    }

    @Override
    @Transactional
    public ExerciseRecord addExercise(Long userId, LocalDate date, String activityName, int durationMinutes, Double caloriesBurned) {
        log.info("Adding exercise: user={}, date={}, name={}, duration={}, inputCalories={}", 
                userId, date, activityName, durationMinutes, caloriesBurned);
        
        double finalCalories = 0.0;
        if (caloriesBurned != null && caloriesBurned > 0.0) {
            finalCalories = caloriesBurned;
        } else {
            // MET Calculation
            double met = getMETValue(activityName);
            double weight = 70.0; // Default weight
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getWeight() != null) {
                weight = user.getWeight().doubleValue();
            }
            // MET * Weight (kg) * Hours * 1.05
            finalCalories = met * weight * (durationMinutes / 60.0) * 1.05;
            // Round to 1 decimal place
            finalCalories = Math.round(finalCalories * 10) / 10.0;
        }

        ExerciseRecord record = ExerciseRecord.builder()
                .userId(userId)
                .recordDate(date)
                .activityName(activityName)
                .durationMinutes(durationMinutes)
                .caloriesBurned(finalCalories)
                .build();

        return exerciseRecordRepository.save(record);
    }

    @Override
    @Transactional
    public void deleteExercise(Long id) {
        log.info("Deleting exercise record: id={}", id);
        exerciseRecordRepository.deleteById(id);
    }

    private double getMETValue(String activityName) {
        if (activityName == null) return 4.0;
        String name = activityName.toLowerCase();
        if (name.contains("跑")) {
            return 8.0;
        } else if (name.contains("健走") || name.contains("快走")) {
            return 4.5;
        } else if (name.contains("步") || name.contains("走")) {
            return 3.0;
        } else if (name.contains("单车") || name.contains("骑") || name.contains("自行车")) {
            return 6.0;
        } else if (name.contains("游")) {
            return 7.0;
        } else if (name.contains("健身") || name.contains("力量") || name.contains("无氧") || name.contains("哑铃") || name.contains("撸铁")) {
            return 5.0;
        } else if (name.contains("瑜伽") || name.contains("普拉提") || name.contains("拉伸")) {
            return 2.5;
        } else if (name.contains("篮球") || name.contains("足球") || name.contains("羽毛球") || name.contains("网球") || name.contains("排球")) {
            return 6.0;
        } else if (name.contains("hiit") || name.contains("高强度") || name.contains("跳操") || name.contains("波比")) {
            return 8.0;
        }
        return 4.0; // Default MET
    }
}
