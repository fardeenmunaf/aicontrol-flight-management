
flight TP1234 {
    type:  regular;
    route: AA123;

    leg {
        departure {
            airport: OPO;
            date:    2026-05-05;
            time:    08:00;
        }

        arrival {
            airport: MAD;
            date:    2026-05-05;
            time:    09:30;
        }

        fuel {
            quantity: 40200 kg;
        }

        profile {
            climb   [ altitude: 0 m,     speed: 210 kts ];
            climb   [ altitude: 1000 m,  speed: 210 kts ];
            descend [ altitude: 12000 m, speed: 300 kts, rate: -10 ];
            descend [ altitude: 0 m,     speed: 140 kts, rate: -5 ];
        }

        segment {
            mode: climb;
            from: (41.262891, -8.68522, altitude: 69 m);
            to:   (42.0, -8.01, altitude: 9249 m);
        }

        segment {
            mode: cruise;
            from: (42.0, -8.01, altitude: 9249 m);
            to:   (41.0, -4.3, altitude: 9249 m);
        }

        segment {
            mode: descend;
            from: (41.0, -4.3, altitude: 9249 m);
            to:   (40.4895, -3.5643, altitude: 610 m);
        }
    }
}