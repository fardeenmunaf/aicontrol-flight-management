// TC: leg 2 departs before leg 1 has arrived

flight TP5678 {
    type:  regular;
    route: TP001 from OPO to BCN;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 13:00; }
        fuel { quantity: 40200 kg; }
        segment {
            mode: cruise;
            from: (41.26, -8.68, altitude: 69 m);
            to:   (40.49, -3.56, altitude: 610 m);
        }
    }

    leg {
        departure { airport: MAD; date: 2026-05-05; time: 11:00; }
        arrival   { airport: BCN; date: 2026-05-05; time: 13:30; }
        fuel { quantity: 38000 kg; }
        segment {
            mode: cruise;
            from: (40.49, -3.56, altitude: 610 m);
            to:   (41.29, 2.07, altitude: 4 m);
        }
    }
}
