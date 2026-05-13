// TC: segment with out-of-bounds latitude (95 > 90) and longitude (200 > 180)

flight TP1234 {
    type:  regular;
    route: AA123 from OPO to MAD;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 09:30; }
        fuel { quantity: 40200 kg; }
        segment {
            mode: cruise;
            from: (95.0, 200.0, altitude: 69 m);
            to:   (40.49, -3.56, altitude: 610 m);
        }
    }
}
