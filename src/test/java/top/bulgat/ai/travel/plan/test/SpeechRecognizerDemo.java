package top.bulgat.ai.travel.plan.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.alibaba.nls.client.AccessToken;
import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizer;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizerListener;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 此示例演示了：
 *      ASR一句话识别API调用。
 *      动态获取Token。获取Token具体操作，请参见：https://help.aliyun.com/document_detail/450514.html
 *      通过本地文件模拟实时流发送。
 *      识别耗时计算。
 */
public class SpeechRecognizerDemo {
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognizerDemo.class);
    private String appKey;
    NlsClient client;
    public SpeechRecognizerDemo(String appKey, String id, String secret, String url) {
        this.appKey = appKey;
        //应用全局创建一个NlsClient实例，默认服务地址为阿里云线上服务地址。
        //获取Token，实际使用时注意在accessToken.getExpireTime()过期前再次获取。
        AccessToken accessToken = new AccessToken(id, secret);
        try {
            accessToken.apply();
            System.out.println("get token: " + accessToken.getToken() + ", expire time: " + accessToken.getExpireTime());
            if(url.isEmpty()) {
                client = new NlsClient(accessToken.getToken());
            }else {
                client = new NlsClient(url, accessToken.getToken());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static SpeechRecognizerListener getRecognizerListener(int myOrder, String userParam) {
        SpeechRecognizerListener listener = new SpeechRecognizerListener() {
            //识别出中间结果。仅当setEnableIntermediateResult为true时，才会返回该消息。
            @Override
            public void onRecognitionResultChanged(SpeechRecognizerResponse response) {
                //getName是获取事件名称，getStatus是获取状态码，getRecognizedText是语音识别文本。
                System.out.println("name: " + response.getName() + ", status: " + response.getStatus() + ", result: " + response.getRecognizedText());
            }
            //识别完毕
            @Override
            public void onRecognitionCompleted(SpeechRecognizerResponse response) {
                //getName是获取事件名称，getStatus是获取状态码，getRecognizedText是语音识别文本。
                System.out.println("name: " + response.getName() + ", status: " + response.getStatus() + ", result: " + response.getRecognizedText());
            }
            @Override
            public void onStarted(SpeechRecognizerResponse response) {
                System.out.println("myOrder: " + myOrder + "; myParam: " + userParam + "; task_id: " + response.getTaskId());
            }
            @Override
            public void onFail(SpeechRecognizerResponse response) {
                //task_id是调用方和服务端通信的唯一标识，当遇到问题时，需要提供此task_id。
                System.out.println("task_id: " + response.getTaskId() + ", status: " + response.getStatus() + ", status_text: " + response.getStatusText());
            }
        };
        return listener;
    }
    //根据二进制数据大小计算对应的同等语音长度
    //sampleRate仅支持8000或16000。
    public static int getSleepDelta(int dataSize, int sampleRate) {
        // 仅支持16位采样。
        int sampleBytes = 16;
        // 仅支持单通道。
        int soundChannel = 1;
        return (dataSize * 10 * 8000) / (160 * sampleRate);
    }
    public void process(String filepath, int sampleRate) {
        SpeechRecognizer recognizer = null;
        try {
            //传递用户自定义参数
            String myParam = "user-param";
            int myOrder = 1234;
            SpeechRecognizerListener listener = getRecognizerListener(myOrder, myParam);
            recognizer = new SpeechRecognizer(client, listener);
            recognizer.setAppKey(appKey);
            //设置音频编码格式。如果是OPUS文件，请设置为InputFormatEnum.OPUS。
            recognizer.setFormat(InputFormatEnum.PCM);
            //设置音频采样率
            if(sampleRate == 16000) {
                recognizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
            } else if(sampleRate == 8000) {
                recognizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_8K);
            }
            //设置是否返回中间识别结果
            recognizer.setEnableIntermediateResult(true);
            //设置是否打开语音检测（即vad）
   recognizer.addCustomedParam("enable_voice_detection",true);
            //此方法将以上参数设置序列化为JSON发送给服务端，并等待服务端确认。
            long now = System.currentTimeMillis();
            recognizer.start();
            logger.info("ASR start latency : " + (System.currentTimeMillis() - now) + " ms");
            File file = new File(filepath);
            FileInputStream fis = new FileInputStream(file);
            byte[] b = new byte[3200];
            int len;
            while ((len = fis.read(b)) > 0) {
                logger.info("send data pack length: " + len);
                recognizer.send(b, len);
                //本案例用读取本地文件的形式模拟实时获取语音流，因为读取速度较快，这里需要设置sleep时长。
                // 如果实时获取语音则无需设置sleep时长，如果是8k采样率语音第二个参数设置为8000。
                int deltaSleep = getSleepDelta(len, sampleRate);
                Thread.sleep(deltaSleep);
            }
            //通知服务端语音数据发送完毕，等待服务端处理完成。
            now = System.currentTimeMillis();
            //计算实际延迟，调用stop返回之后一般即是识别结果返回时间。
            logger.info("ASR wait for complete");
            recognizer.stop();
            logger.info("ASR stop latency : " + (System.currentTimeMillis() - now) + " ms");
            fis.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            //关闭连接
            if (null != recognizer) {
                recognizer.close();
            }
        }
    }
    public void shutdown() {
        client.shutdown();
    }
    public static void main(String[] args) throws Exception {
        String appKey = System.getenv().get("NLS_APP_KEY");
        String id = System.getenv().get("ALIYUN_AK_ID");
        String secret = System.getenv().get("ALIYUN_AK_SECRET");
        String url = System.getenv().getOrDefault("NLS_GATEWAY_URL", "wss://nls-gateway-cn-shanghai.aliyuncs.com/ws/v1");
        SpeechRecognizerDemo demo = new SpeechRecognizerDemo(appKey, id, secret, url);
        //本案例使用本地文件模拟发送实时流数据。
        demo.process("./nls-sample-16k.wav", 16000);
        //demo.process("./nls-sample.opus", 16000);
        demo.shutdown();
    }
}