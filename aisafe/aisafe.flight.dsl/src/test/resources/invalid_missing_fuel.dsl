flight TP1234 {
    type:  regular;
    aircraft:  A320;
    route: AA123 from OPO to MAD;

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

        segment {
            mode: cruise;
            from: (42.0, -8.01, altitude: 9249 m);
            to:   (41.0, -4.3, altitude: 9249 m);
        }
    }
}