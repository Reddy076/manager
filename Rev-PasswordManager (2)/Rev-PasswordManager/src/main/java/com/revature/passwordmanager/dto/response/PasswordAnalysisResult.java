package com.revature.passwordmanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordAnalysisResult {
    private String strength; // VERY_WEAK, WEAK, MODERATE, STRONG, VERY_STRONG
    private List<String> vulnerabilities;
    private List<String> suggestions;
}
