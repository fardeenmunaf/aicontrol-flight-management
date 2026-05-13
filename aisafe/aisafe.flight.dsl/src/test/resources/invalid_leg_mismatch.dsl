// ERROR: arrival of leg 1 (MAD) does not match departure of leg 2 (LIS)

flight TP5678 {
    type:  regular;
    route: TP001;

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
            airport: LIS;
            date:    2026-05-05;
            time:    11:00;
        }
        arrival {
            airport: BCN;
            date:    2026-05-05;
            time:    13:00;
        }
        fuel {
            quantity: 38000 kg;
        }
        segment {
            mode: cruise;
            from: (38.77, -9.13, altitude: 114 m);
            to:   (41.29, 2.07, altitude: 4 m);
        }
    }
}