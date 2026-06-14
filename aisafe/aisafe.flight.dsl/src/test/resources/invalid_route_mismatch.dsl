// TC08: origem da rota (LIS) não corresponde ao aeroporto de partida (OPO)
//       destino da rota (BCN) não corresponde ao aeroporto de chegada (MAD)

flight TP7777 {
    type:  regular;
    aircraft:  A320;
        pilot:     atcc1@tap.pt;
    route: RP001 from LIS to BCN;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 09:30; }
        fuel { quantity: 40200 kg; }
        profile {
            climb   [ altitude: 69 m,   speed: 210 kts, rate: 10 m/s ];
            climb   [ altitude: 9249 m, speed: 300 kts, rate: 10 m/s ];
            cruise  [ speed: 460 kts ];
            descend [ altitude: 9249 m, speed: 300 kts, rate: -10 m/s ];
            descend [ altitude: 610 m,  speed: 140 kts, rate: -5 m/s  ];
        }
        segment {
            from: (41.262891, -8.68522, altitude: 69 m);
            to:   (42.0, -8.01, altitude: 9249 m);
            altslot [ min: 0 m, max: 9249 m, width: 50 m ];
            wind { direction: 270; speed: 15 m/s; }
            mode: climb;
        }
        segment {
            from: (42.0, -8.01, altitude: 9249 m);
            to:   (41.0, -4.3, altitude: 9249 m);
            altslot [ min: 9000 m, max: 9500 m, width: 50 m ];
            wind { direction: 270; speed: 20 m/s; }
            mode: cruise;
        }
        segment {
            from: (41.0, -4.3, altitude: 9249 m);
            to:   (40.4895, -3.5643, altitude: 610 m);
            altslot [ min: 0 m, max: 9249 m, width: 50 m ];
            wind { direction: 270; speed: 10 m/s; }
            mode: descend;
        }
    }
}
