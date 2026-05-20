package com.JobsNow.backend.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagesPageResponse {
    private List<MessageResponse> messages;
    private boolean hasMore;
    private Integer oldestMessageId;
}
