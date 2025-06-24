package com.example.adoption_and_breeding_module.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BlockEvent {
    @org.hibernate.validator.constraints.UUID
    private UUID blockId;

    @org.hibernate.validator.constraints.UUID
    private UUID blockerId;

    @org.hibernate.validator.constraints.UUID
    private UUID blockedId;

    @org.hibernate.validator.constraints.UUID
    private Timestamp createdAt;
}
