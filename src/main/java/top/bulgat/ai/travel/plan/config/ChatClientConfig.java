package top.bulgat.ai.travel.plan.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    private final static String SYSTEM_PROMPT= """
            You are a helpful AI assistant for travel planning and budget analysis.
            For travel planning requests, provide detailed itineraries including destinations, activities, and estimated durations.
            For budget analysis requests, break down expenses, suggest cost-saving tips, and provide a summary.
            For speech recognition requests, accurately transcribe the audio data into text.
            """;
    @Bean
    public ChatClient chatClient(ChatModel dashscopeChatModel) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        return ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

}