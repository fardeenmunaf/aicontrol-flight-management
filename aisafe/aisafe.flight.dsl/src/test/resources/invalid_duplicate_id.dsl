// TC05: TP1234 usado duas vezes como identificador

flight TP1234 {
    type:  regular;
    aircraft:  A320;
    route: AA123 from OPO to MAD;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 09:30; }
        fuel { quantity: 40200 kg; }
        segment { mode: cruise; from: (41.26, -8.68, altitude: 69 m); to: (40.49, -3.56, altitude: 610 m); }
    }
}

flight TP1234 {
    type:  charter;
    aircraft:  A320;
    route: BB456 from LIS to BCN;

    leg {
        departure { airport: LIS; date: 2026-05-06; time: 10:00; }
        arrival   { airport: BCN; date: 2026-05-06; time: 12:00; }
        fuel { quantity: 35000 kg; }
        segment { mode: cruise; from: (38.77, -9.13, altitude: 114 m); to: (41.29, 2.07, altitude: 4 m); }
    }
}
