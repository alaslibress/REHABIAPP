package com.rehabiapp.data.domain.repository;

import com.rehabiapp.data.domain.document.PatientProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientProgressRepository extends MongoRepository<PatientProgress, String> {

    List<PatientProgress> findByPatientDni(String patientDni);

    void deleteByPatientDniAndGameIdAndPeriod(String patientDni, String gameId, String period);
}
