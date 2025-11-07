package top.bulgat.ai.travel.plan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.bulgat.ai.travel.plan.model.TravelPlan;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TravelPlanMapper extends BaseMapper<TravelPlan> {
}