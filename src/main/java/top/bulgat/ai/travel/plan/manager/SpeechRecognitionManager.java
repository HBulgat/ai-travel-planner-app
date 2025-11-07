package top.bulgat.ai.travel.plan.manager;

import com.alibaba.nls.client.AccessToken;
import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriber;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberListener;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.bulgat.ai.travel.plan.config.AliyunNlsConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SpeechRecognitionManager {
    
    @Resource
    private AliyunNlsConfig nlsConfig;
    
    private NlsClient client;
    private volatile boolean initialized = false;
    
    // 识别结果封装类
    @Data
    public static class RecognitionResult {
        private boolean success;
        private String text;
        private String errorMessage;
        private String taskId;
        private Long processingTime;
        
        public static RecognitionResult success(String text, String taskId, Long processingTime) {
            RecognitionResult result = new RecognitionResult();
            result.setSuccess(true);
            result.setText(text);
            result.setTaskId(taskId);
            result.setProcessingTime(processingTime);
            return result;
        }
        
        public static RecognitionResult error(String errorMessage) {
            RecognitionResult result = new RecognitionResult();
            result.setSuccess(false);
            result.setErrorMessage(errorMessage);
            return result;
        }
    }
    
    @PostConstruct
    public void init() {
        if (!nlsConfig.isValid()) {
            log.warn("阿里云语音识别配置不完整，SpeechRecognitionManager将无法使用");
            return;
        }
        
        try {
            AccessToken accessToken = new AccessToken(nlsConfig.getAccessKeyId(), nlsConfig.getAccessKeySecret());
            accessToken.apply();
            log.info("阿里云语音识别Token获取成功，过期时间: {}", accessToken.getExpireTime());
            
            if (nlsConfig.getUrl() == null || nlsConfig.getUrl().isEmpty()) {
                client = new NlsClient(accessToken.getToken());
            } else {
                client = new NlsClient(nlsConfig.getUrl(), accessToken.getToken());
            }
            
            initialized = true;
            log.info("SpeechRecognitionManager初始化成功");
        } catch (Exception e) {
            log.error("SpeechRecognitionManager初始化失败", e);
        }
    }
    
    @PreDestroy
    public void destroy() {
        if (client != null) {
            try {
                client.shutdown();
                log.info("SpeechRecognitionManager资源已释放");
            } catch (Exception e) {
                log.error("关闭NlsClient时发生错误", e);
            }
        }
    }
    
    /**
     * 识别音频文件
     */
    public RecognitionResult recognizeFile(String filePath) {
        return recognizeFile(filePath, 16000);
    }
    
    /**
     * 识别音频文件（可指定采样率）
     */
    public RecognitionResult recognizeFile(String filePath, int sampleRate) {
        if (!initialized) {
            return RecognitionResult.error("语音识别服务未正确初始化");
        }
        
        long startTime = System.currentTimeMillis();
        SpeechTranscriber transcriber = null;
        FileInputStream fis = null;
        
        try {
            // 创建结果收集器
            RecognitionResultCollector collector = new RecognitionResultCollector();
            // 创建识别器
            transcriber = new SpeechTranscriber(client, collector);
            transcriber.setAppKey(nlsConfig.getAppKey());
            transcriber.setFormat(InputFormatEnum.PCM);
            transcriber.setSampleRate(sampleRate == 8000 ? 
                SampleRateEnum.SAMPLE_RATE_8K : SampleRateEnum.SAMPLE_RATE_16K);
            transcriber.setEnableIntermediateResult(false);
            transcriber.setEnablePunctuation(true);
            transcriber.setEnableITN(false);
            
            // 开始识别
            transcriber.start();
            
            // 读取并发送音频数据
            File file = new File(filePath);
            if (!file.exists()) {
                return RecognitionResult.error("音频文件不存在: " + filePath);
            }
            
            fis = new FileInputStream(file);
            byte[] buffer = new byte[3200];
            int len;
            
            while ((len = fis.read(buffer)) > 0) {
                transcriber.send(buffer, len);
                
                // 控制发送速率，模拟实时流
                int sleepTime = calculateSleepTime(len, sampleRate);
                Thread.sleep(sleepTime);
            }
            
            // 停止识别并等待结果
            transcriber.stop();
            
            // 等待识别完成（最多等待30秒）
            boolean completed = collector.completionLatch.await(30, TimeUnit.SECONDS);
            
            if (!completed) {
                return RecognitionResult.error("识别超时");
            }
            
            if (collector.errorMessage != null) {
                return RecognitionResult.error(collector.errorMessage);
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            return RecognitionResult.success(collector.fullText.toString(), 
                                           collector.taskId, processingTime);
            
        } catch (Exception e) {
            log.error("语音识别处理失败", e);
            return RecognitionResult.error("处理失败: " + e.getMessage());
        } finally {
            if (transcriber != null) {
                transcriber.close();
            }
            if (fis != null) {
                try { fis.close(); } catch (IOException e) { log.warn("关闭文件流失败", e); }
            }
        }
    }
    
    /**
     * 异步识别音频文件
     */
    public CompletableFuture<RecognitionResult> recognizeFileAsync(String filePath) {
        return CompletableFuture.supplyAsync(() -> recognizeFile(filePath));
    }
    
    /**
     * 识别字节数组格式的音频数据
     */
    public RecognitionResult recognizeAudioData(byte[] audioData, int sampleRate) {
        // 实现字节数组识别逻辑（类似文件识别）
        // 这里可以扩展支持直接从字节数组识别
        return RecognitionResult.error("字节数组识别功能待实现");
    }
    
    /**
     * 计算发送间隔时间
     */
    private int calculateSleepTime(int dataSize, int sampleRate) {
        return (dataSize * 10 * 8000) / (160 * sampleRate);
    }
    
    /**
     * 识别结果收集器（内部类）
     */
    private static class RecognitionResultCollector extends SpeechTranscriberListener {
        private final StringBuilder fullText = new StringBuilder();
        private final CountDownLatch completionLatch = new CountDownLatch(1);
        private String taskId;
        private String errorMessage;
        
        @Override
        public void onTranscriberStart(SpeechTranscriberResponse response) {
            this.taskId = response.getTaskId();
            log.info("识别任务开始: taskId={}", taskId);
        }
        
        @Override
        public void onSentenceBegin(SpeechTranscriberResponse response) {
            // 句子开始，可以记录开始时间等
        }
        
        @Override
        public void onSentenceEnd(SpeechTranscriberResponse response) {
            String sentenceText = response.getTransSentenceText();
            if (sentenceText != null && !sentenceText.trim().isEmpty()) {
                if (fullText.length() > 0) {
                    fullText.append("。");
                }
                fullText.append(sentenceText);
                log.info("识别到句子: {}", sentenceText);
            }
        }
        
        @Override
        public void onTranscriptionResultChange(SpeechTranscriberResponse response) {
            // 中间结果处理（如果需要可以启用）
        }
        
        @Override
        public void onTranscriptionComplete(SpeechTranscriberResponse response) {
            log.info("识别任务完成: taskId={}", taskId);
            completionLatch.countDown();
        }
        
        @Override
        public void onFail(SpeechTranscriberResponse response) {
            this.errorMessage = String.format("识别失败: status=%s, message=%s", 
                response.getStatus(), response.getStatusText());
            log.error("识别失败: taskId={}, error={}", taskId, errorMessage);
            completionLatch.countDown();
        }
    }
}