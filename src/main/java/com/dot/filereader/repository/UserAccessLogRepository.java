package com.dot.filereader.repository;

import com.dot.filereader.entity.UserAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface UserAccessLogRepository extends JpaRepository<UserAccessLog, Long> {

      @Query(value = "SELECT u.ipAddress, COUNT(u) \n" +
              "FROM USER_ACCESS_LOG u \n" +
              "WHERE u.date BETWEEN :startDateTime AND :endDateTime \n" +
              "GROUP BY u.ipAddress \n" +
              "HAVING COUNT(u) > :limit")
    List<Object[]> findByDateBetweenAndCountGreaterThan(Timestamp startDateTime, Timestamp endDateTime, int limit);
}
