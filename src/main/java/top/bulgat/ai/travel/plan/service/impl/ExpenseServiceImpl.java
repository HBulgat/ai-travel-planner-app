package top.bulgat.ai.travel.plan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import top.bulgat.ai.travel.plan.mapper.ExpenseMapper;
import top.bulgat.ai.travel.plan.model.Expense;
import top.bulgat.ai.travel.plan.model.dto.*;
import top.bulgat.ai.travel.plan.service.IAIRecognitionService;
import top.bulgat.ai.travel.plan.service.IExpenseService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExpenseServiceImpl extends ServiceImpl<ExpenseMapper, Expense> implements IExpenseService {

//    @Resource
//    private IAIRecognitionService aiRecognitionService;
//
//    @Override
//    public Long recordExpense(RecordExpenseRequest request) {
//        Expense expense = new Expense();
//        expense.setTravelPlanId(Long.valueOf(request.getTravelPlanId()));
////        expense.setCategory(request.getCategory());
//        expense.setAmount(request.getAmount());
////        expense.setExpenseDate(request.getExpenseDate());
//        expense.setDescription(request.getDescription());
//        // TODO: DTO to Entity conversion
//        save(expense);
//        return expense.getId();
//    }
//
//    @Override
//    public ExpenseResponse getExpenseById(String id) {
//        Expense expense = getById(id);
//        if (expense == null) {
//            return null;
//        }
//        ExpenseResponse response = new ExpenseResponse();
//        response.setId(expense.getId());
//        response.setTravelPlanId(expense.getTravelPlanId());
//        response.setCategory(expense.getCategory());
//        response.setAmount(expense.getAmount());
//        response.setExpenseDate(expense.getExpenseDate());
//        response.setDescription(expense.getDescription());
//        // TODO: Entity to DTO conversion
//        return response;
//    }
//
//    @Override
//    public boolean updateExpense(UpdateExpenseRequest request) {
//        Expense expense = getById(request.getId());
//        if (expense == null) {
//            return false;
//        }
//        expense.setTravelPlanId(request.getTravelPlanId());
//        expense.setCategory(request.getCategory());
//        expense.setAmount(request.getAmount());
//        expense.setExpenseDate(request.getExpenseDate());
//        expense.setDescription(request.getDescription());
//        // TODO: DTO to Entity conversion
//        return updateById(expense);
//    }
//
//    @Override
//    public boolean deleteExpense(String id) {
//        return removeById(id);
//    }
//
//    @Override
//    public List<ExpenseResponse> getAllExpenses() {
//        List<Expense> expenses = list();
//        return expenses.stream().map(expense -> {
//            ExpenseResponse response = new ExpenseResponse();
//            response.setId(expense.getId());
//            response.setTravelPlanId(expense.getTravelPlanId());
//            response.setCategory(expense.getCategory());
//            response.setAmount(expense.getAmount());
//            response.setExpenseDate(expense.getExpenseDate());
//            response.setDescription(expense.getDescription());
//            return response;
//        }).collect(Collectors.toList());
//    }
//
//    @Override
//    public ExpenseSummaryResponse summarizeExpenses(String travelPlanId) {
//        List<Expense> expenses = lambdaQuery().eq(Expense::getTravelPlanId, travelPlanId).list();
//
//        BigDecimal totalAmount = expenses.stream()
//                .map(Expense::getAmount)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        Map<String, BigDecimal> amountByCategory = expenses.stream()
//                .collect(Collectors.groupingBy(Expense::getCategory,
//                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));
//
//        Map<String, BigDecimal> amountByTravelPlan = expenses.stream()
//                .collect(Collectors.groupingBy(Expense::getTravelPlanId,
//                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));
//
//        ExpenseSummaryResponse summary = new ExpenseSummaryResponse();
//        summary.setTotalAmount(totalAmount);
//        summary.setAmountByCategory(amountByCategory);
//        summary.setAmountByTravelPlan(amountByTravelPlan);
//        return summary;
//    }
//
//    @Override
//    public AnalyzeBudgetResponse analyzeBudget(String travelPlanId) {
//        // TODO: Fetch actual budget details based on travelPlanId
//        String budgetDetails = "Budget details for travel plan " + travelPlanId;
//        String analysisResult = aiRecognitionService.analyzeBudget(budgetDetails);
//        AnalyzeBudgetResponse response = new AnalyzeBudgetResponse();
//        response.setAnalysisResult(analysisResult);
//        return response;
//    }
}