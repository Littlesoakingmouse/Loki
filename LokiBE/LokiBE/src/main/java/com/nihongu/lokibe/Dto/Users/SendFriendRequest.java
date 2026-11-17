package com.nihongu.lokibe.Dto.Users;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class SendFriendRequest {

    @NotNull(message = "ID người nhận lời mời không được để trống")
    private Long addresseeId;
}
