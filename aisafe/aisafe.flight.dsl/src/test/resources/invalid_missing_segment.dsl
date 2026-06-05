// ERROR: leg must have at least one segment

flight TP1234 {
    type:  regular;
    aircraft:  A320;
        pilot:     atcc1@tap.pt;
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
        // No segment blocks here
    }
}