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
        segment { mode: climb;   from: (41.262891, -8.68522, altitude: 69 m);  to: (42.0, -8.01, altitude: 9249 m); }
        segment { mode: cruise;  from: (42.0, -8.01, altitude: 9249 m); to: (41.0, -4.3, altitude: 9249 m); }
        segment { mode: descend; from: (41.0, -4.3,  altitude: 9249 m); to: (40.4895, -3.5643, altitude: 610 m); }
}
}
