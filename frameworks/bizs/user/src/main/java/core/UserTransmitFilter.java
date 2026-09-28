package core;

import com.guoxu.constant.UserConstant;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLDecoder;
import static java.nio.charset.StandardCharsets.UTF_8;
/**
 * UserTransmitFilter
 *
 * @author 执笔画棠
 * @date 2025/11/04 21:46
 **/
/**
 * 用户信息传输过滤器
 * 用户信息传输过滤器类用于在应用程序中传递用户相关的上下文信息。
 * 它主要负责从 HTTP 请求头中提取用户 ID、用户名、真实姓名和用户 Token 等信息，并将其设置至用户上下文。
 */
public class UserTransmitFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        // 从 HTTP 请求头中提取用户 ID、用户名、真实姓名和用户 Token 等信息
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String userId = httpServletRequest.getHeader(UserConstant.USER_ID_KEY);

        // 如果用户 ID 存在，则从请求头中提取用户名、真实姓名和用户 Token 等信息
        if (StringUtils.hasText(userId)) {
            // 对用户名和真实姓名进行 URL 解码
            String userName = httpServletRequest.getHeader(UserConstant.USER_NAME_KEY);
            String realName = httpServletRequest.getHeader(UserConstant.REAL_NAME_KEY);

            // hastext方法是判断字符串是否为空或只包含空格
            if (StringUtils.hasText(userName)) {
                // 如果用户名存在，则对其进行 URL 解码
                userName = URLDecoder.decode(userName, UTF_8);
            }
            if (StringUtils.hasText(realName)) {
                // 如果真实姓名存在，则对其进行 URL 解码
                realName = URLDecoder.decode(realName, UTF_8);
            }
            // 如果用户 Token 存在，则从请求头中提取用户 Token 等信息
            String token = httpServletRequest.getHeader(UserConstant.USER_TOKEN_KEY);

            // 构建用户信息实体类
            UserInfoDTO userInfoDTO = UserInfoDTO.builder()
                    .userId(userId)
                    .username(userName)
                    .realName(realName)
                    .token(token)
                    .build();
            // 将用户信息实体类设置至用户上下文
            UserContext.setUser(userInfoDTO);
        }
        try {
            // 继续执行过滤器链
            // filterChain.doFilter(servletRequest, servletResponse);
            // 这行代码会将请求传递给过滤器链中的下一个过滤器，或者最终传递给处理该请求的业务代码（比如你的Controller）。
            // 在后续的整个处理流程中（包括Service层、DAO层），任何地方都可以通过UserContext.getUser()来获取当前登录用户的信息。
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            // 最后，从用户上下文移除用户信息
            UserContext.removeUser();
        }
    }
}