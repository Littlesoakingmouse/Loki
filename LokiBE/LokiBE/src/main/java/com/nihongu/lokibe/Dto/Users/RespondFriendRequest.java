package com.nihongu.lokibe.Dto.Users;

import com.nihongu.lokibe.Enum.Status;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespondFriendRequest {
    @NotNull(message = "ID của lời mời kết bạn không được để trống")
    private Long friendshipId;

    @NotNull(message = "Trạng thái phản hồi không được để trống")
    private Status status;
}
