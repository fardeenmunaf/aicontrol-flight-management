// velocidade 0 no perfil climb deve ser rejeitada

flight TP1234 {
    type:  regular;
    aircraft:  A320;
    route: AA123 from OPO to MAD;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 09:30; }
        fuel { quantity: 40200 kg; }
        profile { climb [ altitude: 1000 m, speed: 0 kts ]; }
        segment { mode: climb;   from: (41.262891, -8.68522, altitude: 69 m);  to: (42.0, -8.01, altitude: 9249 m); }
        segment { mode: cruise;  from: (42.0, -8.01, altitude: 9249 m); to: (41.0, -4.3, altitude: 9249 m); }
        segment { mode: descend; from: (41.0, -4.3,  altitude: 9249 m); to: (40.4895, -3.5643, altitude: 610 m); }
}
}
