package com.dot.filereader.repository;

import com.dot.filereader.entity.BlockedIp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedIpTableRepository extends JpaRepository<BlockedIp, Long> {
}
