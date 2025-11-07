package top.bulgat.ai.travel.plan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.bulgat.ai.travel.plan.model.TravelPlan;
import top.bulgat.ai.travel.plan.model.dto.CreateTravelPlanRequest;
import top.bulgat.ai.travel.plan.model.dto.UpdateTravelPlanRequest;
import top.bulgat.ai.travel.plan.model.dto.TravelPlanResponse;

import java.util.List;

public interface ITravelPlanService extends IService<TravelPlan> {

    TravelPlanResponse generatePersonalizedTravelPlan(String userInput);

    String createTravelPlan(CreateTravelPlanRequest request);

    TravelPlanResponse getTravelPlanById(String planId);

    boolean updateTravelPlan(UpdateTravelPlanRequest request);

    boolean deleteTravelPlan(String planId);

    List<TravelPlanResponse> getAllTravelPlans();
    /**
     * 根据 chatId 调用 AI 服务生成行程计划 JSON
     */
    String generatePlanByChatId(String chatId);
    /**
     * 根据行程计划ID和AI提示词生成预算分析JSON
     */
    String generateBudgetAnalysis(String planId, String prompt);

    /**
     * 保存预算分析结果到数据库
     */
    void saveBudgetAnalysis(String planId, String budgetJson, String userId);
}