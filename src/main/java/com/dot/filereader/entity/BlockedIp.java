package com.dot.filereader.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "BLOCKED_IP_TABLE")
public class BlockedIp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String ip;

    @Column(name = "request_number")
    private int requestNumber;

    @NotNull
    private String comment;
}
