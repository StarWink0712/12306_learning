package core;

/**
 * UserContext
 *
 * @author 执笔画棠
 * @date 2025/11/04 21:48
 **/

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Optional;

/**
 * 用户上下文
 * 用户上下文类用于在应用程序中存储和管理用户相关的上下文信息。
 * 它使用 TransmittableThreadLocal 来确保在异步任务中传递用户上下文。
 */
public class UserContext {

    private static final ThreadLocal<UserInfoDTO> USER_THREAD_LOCAL=new TransmittableThreadLocal<>();

    //设置用户至上下文
    public static void setUser(UserInfoDTO userInfoDTO){
        USER_THREAD_LOCAL.set(userInfoDTO);
    }

    //获取上下文中的用户id
    public static String getUserId(){
        UserInfoDTO userInfoDTO = USER_THREAD_LOCAL.get();
        // 从上下文中获取用户 ID，如果不存在则返回 null
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getUserId).orElse(null);
    }

    /**
     * 获取上下文中用户名称
     *
     * @return 用户名称
     */
    public static String getUsername() {
        UserInfoDTO userInfoDTO = USER_THREAD_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getUsername).orElse(null);
    }

    /**
     * 获取上下文中用户真实姓名
     *
     * @return 用户真实姓名
     */
    public static String getRealName() {
        UserInfoDTO userInfoDTO = USER_THREAD_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getRealName).orElse(null);
    }

    /**
     * 获取上下文中用户 Token
     *
     * @return 用户 Token
     */
    public static String getToken() {
        UserInfoDTO userInfoDTO = USER_THREAD_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getToken).orElse(null);
    }

    /**
     * 清理用户上下文
     */
    public static void removeUser() {
        USER_THREAD_LOCAL.remove();
    }
}
