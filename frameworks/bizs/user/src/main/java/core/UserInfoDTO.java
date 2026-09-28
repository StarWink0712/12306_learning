package core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserInfoDTO 用户信息实体类
 *
 * @author 执笔画棠
 * @date 2025/11/04 21:15
 **/
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UserInfoDTO {
    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 用户 Token
     */
    private String token;
}
