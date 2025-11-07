package top.bulgat.ai.travel.plan.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("travel_plan")
public class TravelPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String planName;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private Integer travelers;
    private String preferences;
    private String details;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}