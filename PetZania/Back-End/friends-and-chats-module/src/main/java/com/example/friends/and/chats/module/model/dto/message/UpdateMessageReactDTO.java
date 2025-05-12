package com.example.friends.and.chats.module.model.dto.message;

import com.example.friends.and.chats.module.model.enumeration.MessageReact;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMessageReactDTO {
    private MessageReact messageReact;
}