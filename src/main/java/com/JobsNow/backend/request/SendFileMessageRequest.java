package com.JobsNow.backend.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendFileMessageRequest {
    private Integer conversationId;
    private Integer senderId;
    private String content;
    private String fileName;
    private String fileType;
    private String filePath;
}
