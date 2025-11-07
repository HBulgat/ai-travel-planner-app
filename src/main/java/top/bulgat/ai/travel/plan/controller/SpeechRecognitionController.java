package top.bulgat.ai.travel.plan.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.bulgat.ai.travel.plan.annotation.AuthRequired;
import top.bulgat.ai.travel.plan.manager.SpeechRecognitionManager;
import top.bulgat.ai.travel.plan.model.dto.Resp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@RestController
@RequestMapping("/speech")
public class SpeechRecognitionController {
    
    @Resource
    private SpeechRecognitionManager speechRecognitionManager;

//    @AuthRequired
    @PostMapping("/recognize")
    public Resp<String> recognizeAudio(
            @RequestPart("audioFile") MultipartFile audioFile) {
        try {
            // 保存上传的临时文件
            Path tempFile = Files.createTempFile("speech_", "_audio");
            Files.copy(audioFile.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("tempFile:{}", tempFile.toAbsolutePath());
            int exitValue = Runtime.getRuntime().exec("ffmpeg -i " +tempFile.toAbsolutePath()+ " -ar 16000 -ac 1 -f s16le "+tempFile.toAbsolutePath()+".pcm").waitFor();
            if (exitValue != 0) {
                throw  new RuntimeException("exit value is " + exitValue);
            }
            try {
                // 调用识别服务
                SpeechRecognitionManager.RecognitionResult result = 
                    speechRecognitionManager.recognizeFile(tempFile +".pcm");
                
//                // 清理临时文件
//                Files.deleteIfExists(tempFile);
                
                return Resp.<String>builder().code(200).message("Success").data(result.getText()).build();
                
            } catch (Exception e) {
                Files.deleteIfExists(tempFile);
                throw e;
            }
            
        } catch (IOException e) {
            log.error("处理上传文件失败", e);
            return Resp.<String>builder().code(500).message("处理失败: " + e.getMessage()).build();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}