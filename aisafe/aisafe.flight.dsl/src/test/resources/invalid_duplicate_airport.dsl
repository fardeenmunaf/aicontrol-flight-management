// ERROR: OPO appears twice in the same flight

flight TP9999 {
    type:  regular;
    aircraft:  A320;
    route: XX001 from OPO to OPO;

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
            from: (41.26, -8.68, altitude: 69 m);
            to:   (40.49, -3.56, altitude: 610 m);
        }
    }

    leg {
        departure {
            airport: MAD;
            date:    2026-05-05;
            time:    11:00;
        }
        arrival {
            airport: OPO;
            date:    2026-05-05;
            time:    12:30;
        }
        fuel {
            quantity: 38000 kg;
        }
        segment {
            mode: cruise;
            from: (40.49, -3.56, altitude: 610 m);
            to:   (41.26, -8.68, altitude: 69 m);
        }
    }
}