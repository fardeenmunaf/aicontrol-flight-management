// ERROR: leg must have at least one segment

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
        // No segment blocks here
    }
}