package com.smooth.smooth_backend_user.user.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraitsBulkResponse {
    private List<TraitData> data;
    private String generatedAtUtc;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TraitData {
        private String userId;
        private String character;
    }
}