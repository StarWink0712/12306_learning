package toolkit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserInfoDTO
 *  用户信息数据传输对象
 *  用户信息实体类
 * @author 执笔画棠
 * @date 2025/11/08 20:35
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDTO {

    private String userId;
    private String username;
    private String realName;
}
