package top.bulgat.ai.travel.plan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.bulgat.ai.travel.plan.model.Expense;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExpenseMapper extends BaseMapper<Expense> {
}