package top.bulgat.ai.travel.plan.controller;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.bulgat.ai.travel.plan.annotation.AuthRequired;
import top.bulgat.ai.travel.plan.model.dto.*;
import top.bulgat.ai.travel.plan.service.ITravelPlanService;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import top.bulgat.ai.travel.plan.service.IUserService;
import top.bulgat.ai.travel.plan.util.JwtUtil;
import top.bulgat.ai.travel.plan.model.User;

@Slf4j
@RestController
@RequestMapping("/travel-plan")
public class TravelPlanController {

    @Resource
    private ITravelPlanService travelPlanService;

    @Resource
    private IUserService userService;

    @Resource
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    public Resp<String> createTravelPlan(@RequestBody CreateTravelPlanRequest request) {
        String planId = travelPlanService.createTravelPlan(request);
        return Resp.<String>builder().code(200).message("Success").data(planId).build();
    }

    @GetMapping("/{planId}")
    public Resp<TravelPlanResponse> getTravelPlanById(@PathVariable String planId) {
        TravelPlanResponse travelPlanResponse = travelPlanService.getTravelPlanById(planId);
        return Resp.<TravelPlanResponse>builder().code(200).message("Success").data(travelPlanResponse).build();
    }


    @DeleteMapping("/{planId}")
    public Resp<Boolean> deleteTravelPlan(@PathVariable String planId) {
        boolean success = travelPlanService.deleteTravelPlan(planId);
        return Resp.<Boolean>builder().code(200).message("Success").data(success).build();
    }


    @AuthRequired
    @GetMapping
    public Resp<List<TravelPlanResponse>> getAllTravelPlans() {
        List<TravelPlanResponse> travelPlans = travelPlanService.getAllTravelPlans();
        return Resp.<List<TravelPlanResponse>>builder().code(200).message("Success").data(travelPlans).build();
    }

    /**
     * 确认保存：接收前端从 /ai/chat 获取到的 JSON 字符串（字段参照行程计划表），解析并保存。
     * 从 Authorization 解析当前登录用户并关联到 travel_plan.user_id。
     */
    @PostMapping("/confirm")
    public Resp<String> confirmAndSavePlan(
            @RequestHeader(value = "Authorization", required = false) String authorization,
                                           @RequestBody String planJson) {
        try {
            // 从 Authorization 获取当前用户
            if (authorization == null || authorization.isEmpty()) {
                return Resp.<String>builder().code(401).message("Missing Authorization header").build();
            }
            String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
            String username = jwtUtil.extractUsername(token);
            User user = userService.getUserByUsername(username);
            if (user == null) {
                return Resp.<String>builder().code(401).message("Invalid user").build();
            }

            JsonNode root = objectMapper.readTree(planJson);

            CreateTravelPlanRequest req = new CreateTravelPlanRequest();
            req.setUserId(user.getId());
            req.setPlanName(getText(root, "planName"));
            req.setDestination(getText(root, "destination"));
            // 解析日期，支持字符串或 [yyyy,MM,dd] 数组格式
            LocalDate startDate = parseDate(root.get("startDate"));
            LocalDate endDate = parseDate(root.get("endDate"));
            req.setStartDate(startDate);
            req.setEndDate(endDate);

            if (root.has("budget") && !root.get("budget").isNull()) {
                req.setBudget(new BigDecimal(root.get("budget").asText()));
            }
            if (root.has("travelers") && !root.get("travelers").isNull()) {
                req.setTravelers(root.get("travelers").asInt());
            }

            // activities or preferences -> activities
            List<String> activities = new ArrayList<>();
            JsonNode actNode = root.has("activities") ? root.get("activities") : root.get("preferences");
            if (actNode != null && !actNode.isNull()) {
                if (actNode.isArray()) {
                    for (JsonNode n : actNode) {
                        String v = n.asText();
                        if (v != null && !v.isEmpty()) activities.add(v);
                    }
                } else {
                    String v = actNode.asText();
                    activities = Arrays.stream(v.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
                }
            }
            req.setDetails(activities);

            // details or notes
            if (root.has("details")) {
                JsonNode details = root.get("details");
                req.setNotes(details.isContainerNode() ? objectMapper.writeValueAsString(details) : details.asText());
            } else if (root.has("notes")) {
                req.setNotes(root.get("notes").asText());
            }

            String planId = travelPlanService.createTravelPlan(req);
            return Resp.<String>builder().code(200).message("Success").data(planId).build();

        } catch (Exception e) {
            return Resp.<String>builder().code(500).message("Failed to save plan: " + e.getMessage()).build();
        }
    }

    private String getText(JsonNode root, String field) {
        return (root.has(field) && !root.get(field).isNull()) ? root.get(field).asText() : null;
    }
    /**
     * 新增：生成行程计划接口，接收 chatId，由后端调用 AI 服务生成 JSON 并返回
     */
    @AuthRequired
    @PostMapping("/generate")
    public Resp<String> generatePlanByChatId(
            @RequestHeader(value = "Authorization", required = false) String authorization,
                                             @RequestBody(required = true) GeneratePlanRequest request) {
        try {
            // 校验用户身份（可选）
            if (authorization == null || authorization.isEmpty()) {
                return Resp.<String>builder().code(401).message("Missing Authorization header").build();
            }
            String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
            String username = jwtUtil.extractUsername(token);
            User user = userService.getUserByUsername(username);
            if (user == null) {
                return Resp.<String>builder().code(401).message("Invalid user").build();
            }
            // 调用 AI 服务生成行程计划 JSON
            String aiJson = travelPlanService.generatePlanByChatId(request.getChatId());
            return Resp.<String>builder().code(200).message("Success").data(aiJson).build();
        } catch (Exception e) {
            return Resp.<String>builder().code(500).message("Failed to generate plan: " + e.getMessage()).build();
        }
    }
    /**
     * 生成预算分析接口：根据行程计划ID，调用AI服务生成预算分析JSON
     */
    @AuthRequired
    @PostMapping("/{planId}/budget/generate")
    public Resp<String> generateBudgetAnalysis(@RequestHeader(value = "Authorization", required = false) String authorization,
                                               @PathVariable String planId) {
        try {
            // 获取行程计划详情
            TravelPlanResponse plan = travelPlanService.getTravelPlanById(planId);
            if (plan == null) {
                return Resp.<String>builder().code(404).message("Travel plan not found").build();
            }
            objectMapper.registerModule(new JavaTimeModule());
            // 构造预算分析AI提示词，要求输出Expense.java所有字段，类型严格对应：
            String prompt = "请对以下行程计划进行详细预算分析，输出JSON，行程详情：" + objectMapper.writeValueAsString(plan);
            // 调用AI服务生成预算分析JSON
            String budgetJson = travelPlanService.generateBudgetAnalysis(planId, prompt);
            return Resp.<String>builder().code(200).message("Success").data(budgetJson).build();
        } catch (Exception e) {
            log.error("generateBudgetAnalysis error", e);
            return Resp.<String>builder().code(500).message("Failed to generate budget analysis: " + e.getMessage()).build();
        }
    }

    /**
     * 保存预算分析结果接口
     */
    @AuthRequired
    @PostMapping("/{planId}/budget")
    public Resp<String> saveBudgetAnalysis(
            @RequestHeader(value = "Authorization", required = false) String authorization,
                                           @PathVariable String planId,
                                           @RequestBody String budgetJson) {
        try {
            if (authorization == null || authorization.isEmpty()) {
                return Resp.<String>builder().code(401).message("Missing Authorization header").build();
            }
            String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
            String username = jwtUtil.extractUsername(token);
            User user = userService.getUserByUsername(username);
            if (user == null) {
                return Resp.<String>builder().code(401).message("Invalid user").build();
            }
            // 保存预算分析到数据库
            Long userId=user.getId();
            travelPlanService.saveBudgetAnalysis(planId, budgetJson, String.valueOf(userId));
            return Resp.<String>builder().code(200).message("预算分析已保存").build();
        } catch (Exception e) {
            log.error("saveBudgetAnalysis error", e);
            return Resp.<String>builder().code(500).message("Failed to save budget analysis: " + e.getMessage()).build();
        }
    }

    private LocalDate parseDate(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray() && node.size() == 3) {
            int y = node.get(0).asInt();
            int m = node.get(1).asInt();
            int d = node.get(2).asInt();
            return LocalDate.of(y, m, d);
        }
        if (node.isTextual()) {
            String txt = node.asText();
            if (!txt.isEmpty()) {
                return LocalDate.parse(txt);
            }
        }
        return null;
    }
}