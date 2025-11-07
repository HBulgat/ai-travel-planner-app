package top.bulgat.ai.travel.plan.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import top.bulgat.ai.travel.plan.annotation.AuthRequired;
import top.bulgat.ai.travel.plan.service.IAIRecognitionService;
import top.bulgat.ai.travel.plan.model.dto.Resp;
import top.bulgat.ai.travel.plan.model.dto.ChatRequest;

import jakarta.annotation.Resource;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/ai")
public class AIRecognitionController {

    @Resource
    private IAIRecognitionService aiRecognitionService;

    @AuthRequired
    @PostMapping("/travel-plan")
    public Resp<String> getTravelPlan(@RequestBody String userInput) {
        log.info("getTravelPlan:{}", userInput);
        String travelPlan = aiRecognitionService.generateTravelPlan(userInput);
        return Resp.<String>builder().code(200).message("Success").data(travelPlan).build();
    }

    @AuthRequired
    @PostMapping("/recognize-speech")
    public Resp<String> recognizeSpeech(@RequestBody String audioData) {
        String recognizedText = aiRecognitionService.recognizeSpeech(audioData);
        return Resp.<String>builder().code(200).message("Success").data(recognizedText).build();
    }

    @AuthRequired
    @PostMapping("/chat")
    public Resp<String> doChat(@RequestBody ChatRequest chatRequest) {
        String response = aiRecognitionService.doChat(chatRequest.getMessage(), chatRequest.getChatId());
        return Resp.<String>builder().code(200).message("Success").data(response).build();
    }

    @AuthRequired
    @PostMapping(value = "/chat/sse-emitter")
    public SseEmitter doChatWithSseEmitter(@RequestBody ChatRequest request) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(180000L); // 3 分钟超时
        // 获取 Flux 响应式数据流并且直接通过订阅推送给 SseEmitter
        aiRecognitionService.doChatByStream(request.getMessage(), request.getChatId())
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        // 返回
        return sseEmitter;
    }


}