package com.example.friendsAndChatsModule.model.entity;

import com.example.friendsAndChatsModule.model.enumeration.MessageReact;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "message_reactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "message_id"}))
public class MessageReaction {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "message_reaction_id", nullable = false)
    private UUID messageReactionId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false)
    private MessageReact reactionType;
}