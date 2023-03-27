package com.dot.filereader.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "USER_ACCESS_LOG")
public class UserAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "date_time")
    private Timestamp date;

    @NotNull
    @Column(name = "ip_address")
    private String ipAddress;

    @NotNull
    private String request;

    @NotNull
    private String status;

    @NotNull
    @Column(name = "user_agent")
    private String userAgent;
}
