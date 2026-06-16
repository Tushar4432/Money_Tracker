package com.tracker.MoneyTracker.ai.controller;

import com.tracker.MoneyTracker.ai.dto.*;
import com.tracker.MoneyTracker.ai.service.AffordabilityService;
import com.tracker.MoneyTracker.ai.service.AiChatService;
import com.tracker.MoneyTracker.ai.service.HealthScoreService;
import com.tracker.MoneyTracker.ai.service.RecommendationService;
import com.tracker.MoneyTracker.error.ErrorCode;
import com.tracker.MoneyTracker.exception.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the AI Spend Coach feature.
 * <p>
 * Provides endpoints for:
 * <ul>
 *   <li>Chat with the AI financial coach</li>
 *   <li>Get personalized spending recommendations</li>
 *   <li>Get financial health score</li>
 *   <li>Affordability analysis for purchases</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiCoachController {

    private final AiChatService chatService;
    private final RecommendationService recommendationService;
    private final HealthScoreService healthScoreService;
    private final AffordabilityService affordabilityService;

    public AiCoachController(AiChatService chatService,
                             RecommendationService recommendationService,
                             HealthScoreService healthScoreService,
                             AffordabilityService affordabilityService) {
        this.chatService = chatService;
        this.recommendationService = recommendationService;
        this.healthScoreService = healthScoreService;
        this.affordabilityService = affordabilityService;
    }

    /**
     * Chat with the AI financial coach.
     * Sends the user's message along with their financial context to the LLM.
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request.userId() == null || request.userId().isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT, "userId is required");
        }
        if (request.message() == null || request.message().isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT, "message is required");
        }
        ChatResponse response = chatService.chat(request.userId(), request.message());
        return ResponseEntity.ok(response);
    }

    /**
     * Get personalized AI-generated spending recommendations.
     */
    @GetMapping("/recommendations")
    public ResponseEntity<RecommendationResponse> getRecommendations(@RequestParam("userId") String userId) {
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT, "userId is required");
        }
        RecommendationResponse response = recommendationService.generateRecommendations(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get the user's financial health score (0-100).
     */
    @GetMapping("/health-score")
    public ResponseEntity<HealthScoreResponse> getHealthScore(@RequestParam("userId") String userId) {
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT, "userId is required");
        }
        HealthScoreResponse response = healthScoreService.calculateScore(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Analyze whether the user can afford a specific purchase.
     */
    @PostMapping("/affordability")
    public ResponseEntity<AffordabilityResponse> analyzeAffordability(@RequestBody AffordabilityRequest request) {
        if (request.userId() == null || request.userId().isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT, "userId is required");
        }
        if (request.itemName() == null || request.itemName().isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT, "itemName is required");
        }
        if (request.cost() == null || request.cost().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT, "cost must be positive");
        }
        AffordabilityResponse response = affordabilityService.analyze(
                request.userId(), request.itemName(), request.cost());
        return ResponseEntity.ok(response);
    }
}
