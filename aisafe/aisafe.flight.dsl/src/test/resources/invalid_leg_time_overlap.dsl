// leg 2 parte antes de leg 1 ter chegado

flight TP5678 {
    type:  regular;
    aircraft:  A320;
    route: TP001 from OPO to BCN;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 13:00; }
        fuel { quantity: 40200 kg; }
        segment { mode: climb;   from: (41.262891, -8.68522, altitude: 69 m);  to: (42.0, -8.01, altitude: 9249 m); }
        segment { mode: cruise;  from: (42.0, -8.01, altitude: 9249 m); to: (41.0, -4.3, altitude: 9249 m); }
        segment { mode: descend; from: (41.0, -4.3,  altitude: 9249 m); to: (40.4895, -3.5643, altitude: 610 m); }
}

    leg {
        departure { airport: MAD; date: 2026-05-05; time: 11:00; }
        arrival   { airport: BCN; date: 2026-05-05; time: 13:30; }
        fuel { quantity: 38000 kg; }
        segment { mode: climb;   from: (40.4895, -3.5643, altitude: 610 m); to: (41.0, 0.5, altitude: 9249 m); }
        segment { mode: cruise;  from: (41.0, 0.5, altitude: 9249 m); to: (41.2, 1.8, altitude: 9249 m); }
        segment { mode: descend; from: (41.2, 1.8, altitude: 9249 m); to: (41.2974, 2.0833, altitude: 4 m); }
}
}
