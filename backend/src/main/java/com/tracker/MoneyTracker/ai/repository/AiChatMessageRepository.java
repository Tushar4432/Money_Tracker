package com.tracker.MoneyTracker.ai.repository;

import com.tracker.MoneyTracker.ai.entity.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Persists AI chat messages so conversation history survives restarts.
 */
@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, String> {

    /**
     * Returns all messages for a user, oldest first.
     */
    List<AiChatMessage> findByUserIdOrderByCreatedAtAsc(String userId);

    /**
     * Returns the most recent N messages for a user, oldest first.
     * Useful for providing conversation context to the LLM.
     */
    List<AiChatMessage> findTop20ByUserIdOrderByCreatedAtDesc(String userId);
}
