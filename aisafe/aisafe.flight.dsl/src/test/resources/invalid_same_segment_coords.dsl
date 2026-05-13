// ERROR: segment start and end coordinates are identical

flight TP1234 {
    type:  regular;
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
        fuel {
            quantity: 40200 kg;
        }
        segment {
            mode: cruise;
            from: (41.26, -8.68, altitude: 9249 m);
            to:   (41.26, -8.68, altitude: 9249 m);
        }
    }
}