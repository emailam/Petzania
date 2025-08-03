package com.example.friends.and.chats.module.model.entity;


import com.example.friends.and.chats.module.model.enumeration.MessageReact;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne
    @JoinColumn(name = "message_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Message message;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 20)
    private MessageReact reactionType;
}