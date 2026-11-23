package com.library.seatmgmt.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Built-in HTTP server providing an interactive browser preview at http://localhost:8080
 * Allows reviewing the visual design, interactive seat map, and themes directly in any browser.
 */
public class WebPreviewServer {

    private static final int PORT = 8080;
    private static HttpServer server;

    public static void startServer() {
        try {
            if (server != null) return;

            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/", new DashboardHandler());
            server.setExecutor(null);
            server.start();

            System.out.println("==================================================================");
            System.out.println("🌐 Web Preview Server LIVE at: http://localhost:" + PORT);
            System.out.println("Open your browser and navigate to http://localhost:" + PORT);
            System.out.println("==================================================================");
        } catch (IOException e) {
            System.err.println("Could not start Web Preview Server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        startServer();
    }

    static class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Smart Library Seat Management System - Interactive Preview</title>
    <style>
        :root {
            --color-primary: #1E3A2F;
            --color-primary-hover: #162E25;
            --color-primary-light: #E8F0EC;
            --color-secondary: #8A9A86;
            --color-bg-app: #F8F9F6;
            --color-surface: #FFFFFF;
            --color-border: #E2E8E4;
            --color-text-main: #1C2520;
            --color-text-muted: #6B7A72;
            --color-available-bg: #EBF7EE;
            --color-available-border: #2D7A4C;
            --color-available-text: #1B5233;
            --color-reserved-bg: #FEF6E9;
            --color-reserved-border: #D9822B;
            --color-reserved-text: #935316;
            --color-occupied-bg: #FDEEE9;
            --color-occupied-border: #C94A4A;
            --color-occupied-text: #8A2A2A;
            --color-selected-bg: #E9F2FE;
            --color-selected-border: #2563EB;
            --color-selected-text: #1E40AF;
        }

        body.dark-mode {
            --color-primary: #2D5A49;
            --color-primary-hover: #386E5A;
            --color-primary-light: #1E382E;
            --color-secondary: #748A7D;
            --color-bg-app: #121815;
            --color-surface: #1B2420;
            --color-border: #2A3831;
            --color-text-main: #ECF1EE;
            --color-text-muted: #8E9E96;
            --color-available-bg: #153322;
            --color-available-border: #2D7A4C;
            --color-available-text: #9EE0B7;
            --color-reserved-bg: #3B2811;
            --color-reserved-border: #D9822B;
            --color-reserved-text: #FAD19A;
            --color-occupied-bg: #3B1616;
            --color-occupied-border: #C94A4A;
            --color-occupied-text: #FBB8B8;
            --color-selected-bg: #142E4F;
            --color-selected-border: #3B82F6;
            --color-selected-text: #93C5FD;
        }

        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { background-color: var(--color-bg-app); color: var(--color-text-main); height: 100vh; display: flex; overflow: hidden; }

        /* Sidebar */
        .sidebar {
            width: 250px; background-color: #142820; padding: 24px 16px; display: flex; flex-direction: column; gap: 8px; flex-shrink: 0;
        }
        body.dark-mode .sidebar { background-color: #0E1311; }
        .sidebar-brand { display: flex; align-items: center; gap: 10px; padding-bottom: 16px; border-bottom: 1px solid #233A30; }
        .sidebar-brand h2 { font-size: 16px; color: #FFFFFF; }
        .sidebar-brand p { font-size: 11px; color: #A3B8AD; }
        .nav-heading { font-size: 10px; color: #748A7D; font-weight: bold; margin-top: 14px; padding-left: 8px; }
        .nav-btn {
            background: transparent; border: none; color: #C2D1C9; font-size: 13px; font-weight: 500;
            padding: 10px 14px; text-align: left; border-radius: 6px; cursor: pointer; display: flex; align-items: center; gap: 10px;
        }
        .nav-btn:hover { background-color: #1E3A2F; color: #FFFFFF; }
        .nav-btn.active { background-color: #26483B; color: #FFFFFF; font-weight: bold; }

        /* Main Workspace */
        .main-wrapper { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
        .topbar {
            background-color: var(--color-surface); border-bottom: 1px solid var(--color-border); padding: 14px 28px;
            display: flex; justify-content: space-between; align-items: center;
        }
        .topbar-user { display: flex; align-items: center; gap: 12px; }
        .user-pill {
            background-color: var(--color-primary-light); color: var(--color-primary); padding: 6px 14px; border-radius: 20px;
            font-size: 12px; font-weight: bold; display: flex; align-items: center; gap: 8px;
        }

        .content-area { flex: 1; padding: 24px; overflow-y: auto; display: flex; gap: 20px; }
        .left-col { flex: 1; display: flex; flex-direction: column; gap: 18px; }
        .drawer-col { width: 310px; flex-shrink: 0; }

        .card {
            background-color: var(--color-surface); border: 1px solid var(--color-border); border-radius: 8px; padding: 20px;
        }

        /* KPI Row */
        .kpi-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 6px; }
        .stat-card {
            background-color: var(--color-surface); border: 1px solid var(--color-border); border-radius: 8px; padding: 16px 20px;
        }
        .stat-label { font-size: 11px; font-weight: bold; color: var(--color-text-muted); text-transform: uppercase; }
        .stat-value { font-size: 26px; font-weight: bold; margin: 4px 0; }

        /* Filter Controls */
        .filter-row { display: flex; gap: 14px; align-items: center; flex-wrap: wrap; }
        .filter-group { display: flex; flex-direction: column; gap: 4px; }
        .filter-group label { font-size: 11px; font-weight: bold; }
        select, input[type="date"] {
            background-color: var(--color-surface); color: var(--color-text-main); border: 1px solid var(--color-border);
            padding: 8px 12px; border-radius: 6px; font-size: 13px; outline: none;
        }

        /* Seat Grid */
        .seat-grid {
            display: grid; grid-template-columns: repeat(auto-fill, minmax(90px, 1fr)); gap: 16px; margin-top: 14px;
        }
        .seat-tile {
            height: 82px; border-radius: 8px; border: 1.5px solid transparent; display: flex; flex-direction: column;
            align-items: center; justify-content: center; cursor: pointer; transition: all 0.15s ease;
        }
        .seat-tile:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
        .seat-tile .number { font-size: 14px; font-weight: bold; }
        .seat-tile .features { font-size: 10px; margin-top: 4px; }

        .seat-available { background-color: var(--color-available-bg); border-color: var(--color-available-border); color: var(--color-available-text); }
        .seat-reserved { background-color: var(--color-reserved-bg); border-color: var(--color-reserved-border); color: var(--color-reserved-text); cursor: not-allowed; }
        .seat-occupied { background-color: var(--color-occupied-bg); border-color: var(--color-occupied-border); color: var(--color-occupied-text); cursor: not-allowed; }
        .seat-selected { background-color: var(--color-selected-bg); border-color: var(--color-selected-border); border-width: 2.5px; color: var(--color-selected-text); }

        /* Legend */
        .legend-row { display: flex; gap: 14px; font-size: 11px; align-items: center; }
        .legend-item { display: flex; align-items: center; gap: 6px; }
        .legend-box { width: 12px; height: 12px; border-radius: 3px; border: 1.5px solid; }

        .btn-primary {
            background-color: var(--color-primary); color: #FFFFFF; border: none; padding: 10px 18px; border-radius: 6px;
            font-size: 13px; font-weight: bold; cursor: pointer; transition: background-color 0.2s;
        }
        .btn-primary:hover { background-color: var(--color-primary-hover); }

        .btn-theme {
            background: transparent; border: 1px solid #233A30; color: #C2D1C9; padding: 8px 12px; border-radius: 6px;
            font-size: 12px; cursor: pointer; text-align: left;
        }
        .btn-theme:hover { background: #1E3A2F; color: #FFF; }
    </style>
</head>
<body>

    <!-- Sidebar -->
    <div class="sidebar">
        <div class="sidebar-brand">
            <span style="font-size: 24px;">🏛️</span>
            <div>
                <h2>Smart Library</h2>
                <p>Seat Management</p>
            </div>
        </div>

        <div class="nav-heading">NAVIGATION</div>
        <button class="nav-btn" onclick="switchTab('dashboard')">📊 Dashboard</button>
        <button class="nav-btn active" onclick="switchTab('seatmap')">🪑 Find a Seat</button>
        <button class="nav-btn" onclick="switchTab('bookings')">📅 My Bookings</button>
        <button class="nav-btn" onclick="switchTab('reports')">📈 Reports & Analytics</button>
        <button class="nav-btn" onclick="switchTab('admin')">⚙️ Admin Control</button>

        <div style="flex: 1;"></div>

        <button class="btn-theme" onclick="toggleTheme()" id="btnThemeToggle">🌙 Toggle Dark Mode</button>
        <div style="font-size: 11px; color: #748A7D; text-align: center; margin-top: 8px;">Desktop JavaFX + MySQL</div>
    </div>

    <!-- Main Wrapper -->
    <div class="main-wrapper">
        <!-- Top Bar -->
        <div class="topbar">
            <div>
                <h1 style="font-size: 18px; font-weight: bold;" id="topbarTitle">Find a Seat</h1>
                <p style="font-size: 12px; color: var(--color-text-muted);" id="topbarSubtitle">Select a zone, pick your time slot, and reserve your desk pod.</p>
            </div>
            <div class="topbar-user">
                <span style="font-size: 12px; color: var(--color-text-muted);">Monday, Nov 23, 2026</span>
                <div class="user-pill">
                    <span>Alex Rivera</span>
                    <span style="background-color: var(--color-primary); color: #FFF; padding: 2px 6px; border-radius: 10px; font-size: 10px;">STUDENT</span>
                </div>
            </div>
        </div>

        <!-- Content Area -->
        <div class="content-area">
            <div class="left-col">
                <!-- KPI Stat Row -->
                <div class="kpi-row">
                    <div class="stat-card">
                        <div class="stat-label">Total Seats</div>
                        <div class="stat-value">15</div>
                        <div style="font-size: 11px; color: var(--color-text-muted);">4 active zones</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-label">Available Now</div>
                        <div class="stat-value" style="color: var(--color-available-border);">12</div>
                        <div style="font-size: 11px; color: var(--color-text-muted);">Ready to book</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-label">Occupied</div>
                        <div class="stat-value" style="color: var(--color-occupied-border);">2</div>
                        <div style="font-size: 11px; color: var(--color-text-muted);">Checked in</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-label">My Active Bookings</div>
                        <div class="stat-value" style="color: var(--color-primary);">1</div>
                        <div style="font-size: 11px; color: var(--color-text-muted);">Seat A-01 at 10 AM</div>
                    </div>
                </div>

                <!-- Filter Card -->
                <div class="card">
                    <div class="filter-row">
                        <div class="filter-group">
                            <label>Library Zone</label>
                            <select id="selZone" onchange="renderSeats()">
                                <option value="1">Silent Study Gallery (Zone A)</option>
                                <option value="2">Digital Research Lab (Zone B)</option>
                                <option value="3">Discussion Hub (Zone C)</option>
                                <option value="4">Main Reading Hall (Zone D)</option>
                            </select>
                        </div>
                        <div class="filter-group">
                            <label>Date</label>
                            <input type="date" value="2026-11-23">
                        </div>
                        <div class="filter-group">
                            <label>Start Time</label>
                            <select id="selStart">
                                <option>10:00 AM</option>
                                <option>11:00 AM</option>
                                <option>12:00 PM</option>
                                <option>01:00 PM</option>
                                <option>02:00 PM</option>
                            </select>
                        </div>
                        <div class="filter-group">
                            <label>End Time</label>
                            <select id="selEnd">
                                <option>12:00 PM</option>
                                <option>01:00 PM</option>
                                <option>02:00 PM</option>
                                <option>03:00 PM</option>
                            </select>
                        </div>
                        <div style="flex: 1;"></div>
                        <button class="btn-primary" onclick="alert('Filters applied. Real-time availability refreshed.')">Refresh Grid</button>
                    </div>
                </div>

                <!-- Seat Layout Floor Grid -->
                <div class="card" style="flex: 1;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                        <div>
                            <h3 style="font-size: 15px;" id="zoneTitle">Silent Study Gallery Floor Plan</h3>
                            <p style="font-size: 11px; color: var(--color-text-muted);">Click on any available green seat tile to select.</p>
                        </div>
                        <div class="legend-row">
                            <div class="legend-item"><div class="legend-box" style="background: var(--color-available-bg); border-color: var(--color-available-border);"></div> Available</div>
                            <div class="legend-item"><div class="legend-box" style="background: var(--color-reserved-bg); border-color: var(--color-reserved-border);"></div> Reserved</div>
                            <div class="legend-item"><div class="legend-box" style="background: var(--color-occupied-bg); border-color: var(--color-occupied-border);"></div> Occupied</div>
                        </div>
                    </div>

                    <div class="seat-grid" id="seatContainer">
                        <!-- Seat tiles rendered by JS -->
                    </div>
                </div>
            </div>

            <!-- Right Drawer -->
            <div class="drawer-col">
                <div class="card" style="height: 100%; display: flex; flex-direction: column;">
                    <h3 style="font-size: 15px; margin-bottom: 14px;">Booking Summary</h3>

                    <div id="drawerEmpty" style="flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; color: var(--color-text-muted);">
                        <span style="font-size: 40px; margin-bottom: 10px; opacity: 0.6;">🪑</span>
                        <p style="font-weight: bold;">No Seat Selected</p>
                        <p style="font-size: 11px; margin-top: 6px;">Select any green seat pod from the floor plan to view reservation details.</p>
                    </div>

                    <div id="drawerSelected" style="display: none; flex-direction: column; gap: 14px; flex: 1;">
                        <div style="background-color: var(--color-primary-light); padding: 14px; border-radius: 6px;">
                            <div style="font-size: 10px; font-weight: bold; color: var(--color-primary);">SELECTED SEAT</div>
                            <div style="font-size: 26px; font-weight: bold; color: var(--color-primary);" id="drawSeat">A-01</div>
                            <div style="font-size: 12px; color: var(--color-text-muted);" id="drawZone">Silent Study Gallery</div>
                        </div>

                        <div style="border-top: 1px solid var(--color-border); border-bottom: 1px solid var(--color-border); padding: 12px 0; font-size: 12px; display: flex; flex-direction: column; gap: 8px;">
                            <div style="display: flex; justify-content: space-between;"><span style="color: var(--color-text-muted);">Date:</span> <strong>Today (2026-11-23)</strong></div>
                            <div style="display: flex; justify-content: space-between;"><span style="color: var(--color-text-muted);">Time Slot:</span> <strong id="drawTime">10:00 AM - 12:00 PM</strong></div>
                            <div style="display: flex; justify-content: space-between;"><span style="color: var(--color-text-muted);">Duration:</span> <strong>2 Hours</strong></div>
                            <div style="display: flex; justify-content: space-between;"><span style="color: var(--color-text-muted);">Facilities:</span> <span id="drawFac">🔌 Power, 🪟 Window</span></div>
                        </div>

                        <div style="font-size: 11px; color: var(--color-reserved-border); background: var(--color-reserved-bg); padding: 8px; border-radius: 6px;">
                            <strong>Grace Period:</strong> You must check in within 15 minutes of slot start time or seat will auto-release.
                        </div>

                        <div style="flex: 1;"></div>

                        <button class="btn-primary" style="width: 100%; padding: 12px;" onclick="confirmBooking()">Confirm Reservation</button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script>
        const zoneData = {
            "1": {
                name: "Silent Study Gallery",
                seats: [
                    { num: "A-01", status: "available", fac: "🔌 🪟" },
                    { num: "A-02", status: "available", fac: "🔌 🪟" },
                    { num: "A-03", status: "occupied", fac: "🔌" },
                    { num: "A-04", status: "available", fac: "🔌" },
                    { num: "A-05", status: "available", fac: "🔌 🪟" }
                ]
            },
            "2": {
                name: "Digital Research Lab",
                seats: [
                    { num: "B-01", status: "available", fac: "🔌 🖥️" },
                    { num: "B-02", status: "reserved", fac: "🔌 🖥️" },
                    { num: "B-03", status: "available", fac: "🔌 🖥️" },
                    { num: "B-04", status: "available", fac: "🔌 🪟 🖥️" }
                ]
            },
            "3": {
                name: "Discussion & Group Hub",
                seats: [
                    { num: "C-01", status: "available", fac: "🔌 🪟" },
                    { num: "C-02", status: "available", fac: "🔌" },
                    { num: "C-03", status: "available", fac: "Standard" }
                ]
            },
            "4": {
                name: "Main Reading Hall",
                seats: [
                    { num: "D-01", status: "available", fac: "Standard" },
                    { num: "D-02", status: "available", fac: "Standard" },
                    { num: "D-03", status: "available", fac: "🔌 🪟" }
                ]
            }
        };

        let selectedSeatNum = null;

        function renderSeats() {
            const zoneId = document.getElementById('selZone').value;
            const data = zoneData[zoneId];
            document.getElementById('zoneTitle').innerText = data.name + " Floor Plan";
            const container = document.getElementById('seatContainer');
            container.innerHTML = '';

            data.seats.forEach(seat => {
                const tile = document.createElement('div');
                const isSelected = (selectedSeatNum === seat.num);
                tile.className = 'seat-tile seat-' + (isSelected ? 'selected' : seat.status);
                tile.innerHTML = `<span class="number">${seat.num}</span><span class="features">${seat.fac}</span>`;

                if (seat.status === 'available') {
                    tile.onclick = () => selectSeat(seat, data.name);
                }
                container.appendChild(tile);
            });
        }

        function selectSeat(seat, zoneName) {
            selectedSeatNum = seat.num;
            document.getElementById('drawerEmpty').style.display = 'none';
            document.getElementById('drawerSelected').style.display = 'flex';
            document.getElementById('drawSeat').innerText = seat.num;
            document.getElementById('drawZone').innerText = zoneName;
            document.getElementById('drawFac').innerText = seat.fac;
            renderSeats();
        }

        function confirmBooking() {
            alert('🎉 Reservation Confirmed for Seat ' + selectedSeatNum + '!\\nBooking ID #104 placed successfully. Check in opens 15 mins prior to slot start.');
            selectedSeatNum = null;
            document.getElementById('drawerEmpty').style.display = 'flex';
            document.getElementById('drawerSelected').style.display = 'none';
            renderSeats();
        }

        function toggleTheme() {
            document.body.classList.toggle('dark-mode');
            const isDark = document.body.classList.contains('dark-mode');
            document.getElementById('btnThemeToggle').innerText = isDark ? '☀️ Toggle Light Mode' : '🌙 Toggle Dark Mode';
        }

        function switchTab(tab) {
            document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));
            event.target.classList.add('active');
            if (tab === 'dashboard') {
                document.getElementById('topbarTitle').innerText = 'Student Dashboard';
                document.getElementById('topbarSubtitle').innerText = 'Overview of library traffic and your active study bookings.';
            } else if (tab === 'seatmap') {
                document.getElementById('topbarTitle').innerText = 'Find a Seat';
                document.getElementById('topbarSubtitle').innerText = 'Select a zone, pick your time slot, and reserve your desk pod.';
            } else if (tab === 'bookings') {
                document.getElementById('topbarTitle').innerText = 'My Bookings';
                document.getElementById('topbarSubtitle').innerText = 'Upcoming reservations, check-in, check-out, and cancellation.';
            } else if (tab === 'reports') {
                document.getElementById('topbarTitle').innerText = 'Reports & Analytics';
                document.getElementById('topbarSubtitle').innerText = 'Library utilization metrics and CSV export.';
            } else if (tab === 'admin') {
                document.getElementById('topbarTitle').innerText = 'Administrative Management';
                document.getElementById('topbarSubtitle').innerText = 'Manage physical desk inventory and zones.';
            }
        }

        renderSeats();
    </script>
</body>
</html>
""";
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
