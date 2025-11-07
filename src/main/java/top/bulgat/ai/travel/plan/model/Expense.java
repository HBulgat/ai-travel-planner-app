package top.bulgat.ai.travel.plan.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("expense")
public class Expense {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long travelPlanId;
    private BigDecimal amount;
    private String description;
    private LocalDateTime expenseTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String details;
}