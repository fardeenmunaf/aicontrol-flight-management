flight TP1234 {
    type:  regular;
    aircraft:  A320;
    route: AA123 from OPO to MAD;

    leg {
        departure {
            airport: OPO;
            date:    05-05-2026;
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
}