package com.library.seatmgmt;

import com.library.seatmgmt.model.Seat;
import com.library.seatmgmt.model.Zone;
import com.library.seatmgmt.model.enums.SeatStatus;
import com.library.seatmgmt.model.enums.ZoneType;
import com.library.seatmgmt.service.RecommendationService;
import com.library.seatmgmt.service.SeatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test validating the Rule-Based Smart Seat Recommendation Engine.
 * Tests facility weighting (Power, Window, PC) and sorting.
 */
public class RecommendationServiceTest {

    @Test
    @DisplayName("Verify Smart Recommendation Ranking by Preference Weighting")
    void testRecommendationScoring() {
        // Mock SeatService with predefined seats in a zone
        Seat seat1 = new Seat(1, 1, "S-01", true, false, false, SeatStatus.AVAILABLE, null); // Power only
        Seat seat2 = new Seat(2, 1, "S-02", true, true, false, SeatStatus.AVAILABLE, null);  // Power + Window
        Seat seat3 = new Seat(3, 1, "S-03", true, true, true, SeatStatus.AVAILABLE, null);   // Power + Window + PC
        Seat seat4 = new Seat(4, 1, "S-04", false, false, false, SeatStatus.AVAILABLE, null);// Standard desk

        SeatService mockSeatService = new SeatService() {
            @Override
            public List<Seat> getAvailableSeatsForSlot(int zoneId, LocalDate date, LocalTime start, LocalTime end) {
                return List.of(seat1, seat2, seat3, seat4);
            }
        };

        RecommendationService service = new RecommendationService(mockSeatService);

        // Case 1: Student wants Window and Power
        List<Seat> recs1 = service.recommendSeats(1, LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(12, 0),
                true, true, false);

        assertFalse(recs1.isEmpty());
        // Both seat2 and seat3 have Power + Window, so they should be ranked at the top before seat1 and seat4
        assertTrue(recs1.get(0).isNearWindow() && recs1.get(0).isHasPower());
        assertEquals("S-04", recs1.get(recs1.size() - 1).getSeatNumber(), "Standard desk should rank last");

        // Case 2: Student specifically needs a PC
        List<Seat> recs2 = service.recommendSeats(1, LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(12, 0),
                false, false, true);

        assertEquals("S-03", recs2.get(0).getSeatNumber(), "Seat with PC terminal must be ranked highest");
    }
}
