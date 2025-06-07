package com.example.friends.and.chats.module.model.dto.message;

import com.example.friends.and.chats.module.model.enumeration.MessageReact;
import com.example.friends.and.chats.module.model.enumeration.MessageStatus;
import com.example.friends.and.chats.module.validator.ValidEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMessageReactDTO {
    @NotNull
    @ValidEnum(enumClass = MessageReact.class)
    private MessageReact messageReact;
}