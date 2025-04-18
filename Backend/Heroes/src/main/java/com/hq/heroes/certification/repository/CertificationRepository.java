package com.hq.heroes.certification.repository;

import com.hq.heroes.certification.entity.Certification;
import com.hq.heroes.employee.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CertificationRepository extends JpaRepository<Certification, Long> {

    // 부서로 자격증 조회
    List<Certification> findByDepartment(Department department);
}
