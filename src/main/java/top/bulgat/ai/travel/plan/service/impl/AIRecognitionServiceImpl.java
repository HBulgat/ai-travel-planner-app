package top.bulgat.ai.travel.plan.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import top.bulgat.ai.travel.plan.model.dto.TravelPlan;
import top.bulgat.ai.travel.plan.service.IAIRecognitionService;

import jakarta.annotation.Resource;

import org.springframework.ai.chat.memory.ChatMemory;

@Slf4j
@Service
public class AIRecognitionServiceImpl implements IAIRecognitionService {


    @Resource
    private ChatClient chatClient;

//    @Resource
//    private ToolCallbackProvider toolCallbackProvider;

    @Override
    public String generateTravelPlan(String userInput) {
        String prompt = String.format("Generate a travel plan in JSON format based on the following input: '%s'. The JSON should contain the following fields: 'planName' (String), 'destination' (String), 'startDate' (LocalDate in YYYY-MM-DD format), 'endDate' (LocalDate in YYYY-MM-DD format), 'budget' (BigDecimal), 'travelers' (Integer), 'preferences' (comma-separated String), 'details' (JSON String for detailed itinerary).", userInput);
        return chatClient.prompt()
                .user(prompt)
//                .toolCallbacks(toolCallbackProvider)
                .call()
                .content();
    }

    @Override
    public TravelPlan analyzeBudget(String budgetDetails) {
        return chatClient.prompt()
                .user(budgetDetails + ". Analyze the budget.")
//                .toolCallbacks(toolCallbackProvider)
                .call()
                .entity(TravelPlan.class);
    }

    @Override
    public String recognizeSpeech(String audioData) {
        return chatClient.prompt()
                .user("Recognize speech from audio data: " + audioData + ". Return the recognized text.")
//                .toolCallbacks(toolCallbackProvider)
                .call()
                .content();
    }

    @Override
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
//                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Override
    public Flux<String> doChatByStream(String message, String chatId) {
        log.info("message: {}, chatId: {}",message,chatId);
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
//                .toolCallbacks(toolCallbackProvider)
                .stream()
                .content();
    }
}