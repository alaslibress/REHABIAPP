package com.rehabiapp.data.domain.repository;

import com.rehabiapp.data.domain.model.PatientProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PatientProgressRepository extends MongoRepository<PatientProgress, String> {

    Optional<PatientProgress> findByPatientDniAndGameIdAndProgressionLevelAndPeriod(
            String patientDni, String gameId, Integer progressionLevel, String period);

    List<PatientProgress> findByPatientDniOrderByPeriodAsc(String patientDni);

    List<PatientProgress> findByPatientDni(String patientDni);
}
