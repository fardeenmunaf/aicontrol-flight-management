// TC08: origem da rota (LIS) não corresponde ao aeroporto de partida (OPO)
//       destino da rota (BCN) não corresponde ao aeroporto de chegada (MAD)

flight TP7777 {
    type:  regular;
    aircraft:  A320;
    route: RP001 from LIS to BCN;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 09:30; }
        fuel { quantity: 40200 kg; }
        segment { mode: cruise; from: (41.26, -8.68, altitude: 69 m); to: (40.49, -3.56, altitude: 610 m); }
    }
}
