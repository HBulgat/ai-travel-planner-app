package top.bulgat.ai.travel.plan.service;

import reactor.core.publisher.Flux;
import top.bulgat.ai.travel.plan.model.dto.TravelPlan;

public interface IAIRecognitionService {
    String generateTravelPlan(String userInput);
    TravelPlan analyzeBudget(String budgetDetails);
    String recognizeSpeech(String audioData);

    /**
     * AI 基础对话（支持多轮对话记忆）
     *
     * @param message
     * @param chatId
     * @return
     */
    String doChat(String message, String chatId);

    Flux<String> doChatByStream(String message, String chatId);
}