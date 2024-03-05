package com.example.smilestock.service;

import io.github.flashvayne.chatgpt.service.ChatgptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private final ChatgptService chatgptService;

    @Autowired
    public ChatService(ChatgptService chatgptService) {
        this.chatgptService = chatgptService;
    }

    public String getChatResponse(String prompt) {
        // ChatGPT 에게 질문을 던집니다.
        return chatgptService.sendMessage(prompt);
    }
}
