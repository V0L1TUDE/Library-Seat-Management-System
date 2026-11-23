package com.library.seatmgmt.service;

import com.library.seatmgmt.model.Seat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/**
 * Smart Rule-Based Seat Recommendation Engine.
 * Evaluates available seats against student criteria and ranks them by match score.
 * Demonstrates Collections, Comparators, and Java Streams.
 */
public class RecommendationService {

    private final SeatService seatService;

    public RecommendationService() {
        this(new SeatService());
    }

    public RecommendationService(SeatService seatService) {
        this.seatService = seatService;
    }

    /**
     * Evaluates all operational seats in the zone for the time slot and returns them ranked by score.
     */
    public List<Seat> recommendSeats(int zoneId, LocalDate date, LocalTime start, LocalTime end,
                                      boolean preferPower, boolean preferWindow, boolean preferPc) {

        List<Seat> availableSeats = seatService.getAvailableSeatsForSlot(zoneId, date, start, end);

        // Score each seat based on student preferences
        return availableSeats.stream()
                .sorted(Comparator.comparingInt((Seat seat) -> calculateScore(seat, preferPower, preferWindow, preferPc)).reversed())
                .toList();
    }

    private int calculateScore(Seat seat, boolean preferPower, boolean preferWindow, boolean preferPc) {
        int score = 0;
        if (preferPower && seat.isHasPower()) score += 10;
        if (preferWindow && seat.isNearWindow()) score += 8;
        if (preferPc && seat.isHasPc()) score += 12;

        // Base bonus if seat has power socket regardless
        if (seat.isHasPower()) score += 2;

        return score;
    }
}
