package top.bulgat.ai.travel.plan.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.bulgat.ai.travel.plan.mapper.ExpenseMapper;
import top.bulgat.ai.travel.plan.mapper.TravelPlanMapper;
import top.bulgat.ai.travel.plan.model.Expense;
import top.bulgat.ai.travel.plan.model.TravelPlan;
import top.bulgat.ai.travel.plan.model.dto.CreateTravelPlanRequest;
import top.bulgat.ai.travel.plan.model.dto.UpdateTravelPlanRequest;
import top.bulgat.ai.travel.plan.model.dto.TravelPlanResponse;
import top.bulgat.ai.travel.plan.service.IAIRecognitionService;
import top.bulgat.ai.travel.plan.service.ITravelPlanService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@Slf4j
@Service
public class TravelPlanServiceImpl extends ServiceImpl<TravelPlanMapper, TravelPlan> implements ITravelPlanService {

    @Resource
    private IAIRecognitionService aiRecognitionService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private ChatClient chatClient;

    @Override
    public TravelPlanResponse generatePersonalizedTravelPlan(String userInput) {
        // 调用AI服务生成个性化行程计划
        String aiResponseJson = aiRecognitionService.generateTravelPlan(userInput);
        try {
            CreateTravelPlanRequest request = objectMapper.readValue(aiResponseJson, CreateTravelPlanRequest.class);
            // 假设userId从当前登录用户获取，这里暂时硬编码或从其他地方获取
            // TODO: 获取当前登录用户的userId
//            request.setUserId("1"); // 示例：硬编码用户ID

            // 将CreateTravelPlanRequest转换为TravelPlanResponse返回给前端
            TravelPlanResponse response = new TravelPlanResponse();
            response.setPlanName(request.getPlanName());
            response.setDestination(request.getDestination());
            response.setStartDate(request.getStartDate());
            response.setEndDate(request.getEndDate());
            response.setBudget(request.getBudget());
            response.setTravelers(request.getTravelers());
            response.setActivities(request.getDetails());
            response.setNotes(request.getNotes());
            // 其他字段如id, userId, createTime, updateTime等在实际保存时生成或从数据库获取
            response.setUserId(request.getUserId());

            return response;
        } catch (JsonProcessingException e) {
            // 处理JSON解析异常
            e.printStackTrace();
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage());
        }
    }

    @Override
    public String createTravelPlan(CreateTravelPlanRequest request) {
        TravelPlan travelPlan = new TravelPlan();
        travelPlan.setUserId(Long.valueOf(request.getUserId()));
        travelPlan.setPlanName(request.getPlanName());
        travelPlan.setDestination(request.getDestination());
        travelPlan.setStartDate(request.getStartDate());
        travelPlan.setEndDate(request.getEndDate());
        travelPlan.setBudget(request.getBudget());
        travelPlan.setTravelers(request.getTravelers());
        travelPlan.setPreferences(request.getPreferences());
        travelPlan.setDetails(JSON.toJSONString(request.getDetails()));
        travelPlan.setCreateTime(LocalDateTime.now());
        travelPlan.setUpdateTime(LocalDateTime.now());
        save(travelPlan);
        return travelPlan.getId().toString();
    }

    @Override
    public TravelPlanResponse getTravelPlanById(String planId) {
        TravelPlan travelPlan = getById(Long.valueOf(planId));
        if (travelPlan == null) {
            return null;
        }
        TravelPlanResponse response = new TravelPlanResponse();
        response.setId(travelPlan.getId().toString());
        response.setUserId(travelPlan.getUserId());
        response.setPlanName(travelPlan.getPlanName());
        response.setDestination(travelPlan.getDestination());
        response.setStartDate(travelPlan.getStartDate());
        response.setEndDate(travelPlan.getEndDate());
        response.setBudget(travelPlan.getBudget());
        response.setTravelers(travelPlan.getTravelers());
        response.setActivities(travelPlan.getPreferences() == null || travelPlan.getPreferences().isEmpty()
                ? List.of()
                : List.of(travelPlan.getPreferences().split(",")));
        response.setNotes(travelPlan.getDetails());
        return response;
    }

    @Override
    public boolean updateTravelPlan(UpdateTravelPlanRequest request) {
        TravelPlan travelPlan = getById(Long.valueOf(request.getId()));
        if (travelPlan == null) {
            return false;
        }
        travelPlan.setUserId(Long.valueOf(request.getUserId()));
        travelPlan.setPlanName(request.getPlanName());
        travelPlan.setDestination(request.getDestination());
        travelPlan.setStartDate(request.getStartDate());
        travelPlan.setEndDate(request.getEndDate());
        travelPlan.setBudget(request.getBudget());
        travelPlan.setTravelers(request.getTravelers());
        travelPlan.setPreferences(String.join(",", request.getActivities()));
        travelPlan.setDetails(request.getNotes());
        travelPlan.setUpdateTime(LocalDateTime.now());
        return updateById(travelPlan);
    }

    @Override
    public boolean deleteTravelPlan(String planId) {
        return removeById(Long.valueOf(planId));
    }

    @Override
    public List<TravelPlanResponse> getAllTravelPlans() {
        List<TravelPlan> travelPlans = list();
        return travelPlans.stream().map(travelPlan -> {
            TravelPlanResponse response = new TravelPlanResponse();
            response.setId(travelPlan.getId().toString());
            response.setUserId(travelPlan.getUserId());
            response.setPlanName(travelPlan.getPlanName());
            response.setDestination(travelPlan.getDestination());
            response.setStartDate(travelPlan.getStartDate());
            response.setEndDate(travelPlan.getEndDate());
            response.setBudget(travelPlan.getBudget());
            response.setTravelers(travelPlan.getTravelers());
            response.setActivities(travelPlan.getPreferences() == null || travelPlan.getPreferences().isEmpty()
                    ? List.of()
                    : List.of(travelPlan.getPreferences().split(",")));
            response.setNotes(travelPlan.getDetails());
            return response;
        }).collect(Collectors.toList());
    }

    @Resource
    private ChatModel dashscopeChatModel;
    /**
     * 根据 chatId 调用 AI 服务生成行程计划 JSON
     */
    public String generatePlanByChatId(String chatId) {
        // 明确告知 AI 输出的 JSON 字段和格式要求，并直接解析为实体类
        String prompt = "请根据当前对话内容生成行程计划";
        CreateTravelPlanRequest plan = chatClient
                .prompt()
                .user(u -> u.text(prompt))
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(CreateTravelPlanRequest.class);
        // 可根据需要将 plan 转为 JSON 字符串返回
        try {
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.writeValueAsString(plan);
        } catch (Exception e) {
            throw new RuntimeException("AI返回内容解析失败: " + e.getMessage());
        }
    }
    /**
     * 根据行程计划ID和AI提示词生成预算分析JSON
     */
    public String generateBudgetAnalysis(String planId, String prompt) {
        // 这里调用AI服务生成预算分析JSON，直接返回AI结果
        top.bulgat.ai.travel.plan.model.dto.TravelPlan plan = aiRecognitionService.analyzeBudget(prompt);
        try {
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.writeValueAsString(plan);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    @Resource
    private ExpenseMapper expenseMapper;

    /**
     * 保存预算分析结果到数据库
     */
    public void saveBudgetAnalysis(String planId, String budgetJson, String userId) {
        log.info("budgetJson:{}", budgetJson);
        try {
            objectMapper.registerModule(new JavaTimeModule());
            top.bulgat.ai.travel.plan.model.dto.TravelPlan plan=objectMapper.readValue(budgetJson, top.bulgat.ai.travel.plan.model.dto.TravelPlan.class);
            Expense expense=new Expense();
            expense.setTravelPlanId(Long.valueOf(planId));
            expense.setUserId(Long.valueOf(userId));
            expense.setExpenseTime(plan.getStartDate().atStartOfDay());
            expense.setDetails(budgetJson);
            expense.setAmount(BigDecimal.valueOf(plan.getBudget()));
            expenseMapper.insert(expense);
        } catch (Exception e) {
            log.error("save error",e);
            throw new RuntimeException("预算分析保存失败: " + e.getMessage());
        }
    }
}