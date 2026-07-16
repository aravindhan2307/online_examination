package com.online.exam.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which candidate+category pairs currently have an active exam session.
 * Uses a thread-safe in-memory set — no database persistence needed since
 * a session is temporary and tied to the lifetime of the server process.
 *
 * Session key format: "candidateName::category" (both lower-cased)
 */
@Service
public class SessionService {

    private final Set<String> activeSessions = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private String buildKey(String candidateName, String category) {
        return candidateName.trim().toLowerCase() + "::" + category.trim().toLowerCase();
    }

    /**
     * Attempts to register a new active session.
     * @return true if the session was successfully started (no existing session),
     *         false if this candidate+category pair already has an active session.
     */
    public boolean startSession(String candidateName, String category) {
        String key = buildKey(candidateName, category);
        return activeSessions.add(key); // add() returns false if already present
    }

    /**
     * Releases the active session for a candidate+category pair.
     * Called on exam submission or timeout.
     */
    public void endSession(String candidateName, String category) {
        activeSessions.remove(buildKey(candidateName, category));
    }

    /**
     * Checks whether the candidate+category pair currently has an active session.
     */
    public boolean isActive(String candidateName, String category) {
        return activeSessions.contains(buildKey(candidateName, category));
    }
}
