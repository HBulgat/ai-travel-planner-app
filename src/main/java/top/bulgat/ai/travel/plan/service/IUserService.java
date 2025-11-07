package top.bulgat.ai.travel.plan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.bulgat.ai.travel.plan.model.User;

public interface IUserService extends IService<User> {
    boolean registerUser(User user);
    String login(String username, String password);
    User getUserByUsername(String username);
}