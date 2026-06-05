// ERROR: segment missing mandatory altslot

flight TP1234 {
    type:  regular;
    aircraft:  A320;
        pilot:     atcc1@tap.pt;
    route: AA123 from OPO to MAD;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 09:30; }
        fuel { quantity: 40200 kg; }
        segment {
            from: (41.262891, -8.68522, altitude: 69 m);
            to:   (40.4895, -3.5643, altitude: 610 m);
            wind { direction: 270; speed: 15 m/s; }
            mode: climb;
        }
    }
}
