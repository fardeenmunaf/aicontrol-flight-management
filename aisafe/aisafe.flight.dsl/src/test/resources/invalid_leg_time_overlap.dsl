// leg 2 parte antes de leg 1 ter chegado

flight TP5678 {
    type:  regular;
    aircraft:  A320;
        pilot:     atcc1@tap.pt;
    route: TP001 from OPO to BCN;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 13:00; }
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

    leg {
        departure { airport: MAD; date: 2026-05-05; time: 11:00; }
        arrival   { airport: BCN; date: 2026-05-05; time: 13:30; }
        fuel { quantity: 38000 kg; }
        profile {
            climb   [ altitude: 610 m,  speed: 210 kts, rate: 10 m/s ];
            climb   [ altitude: 9249 m, speed: 300 kts, rate: 10 m/s ];
            cruise  [ speed: 460 kts ];
            descend [ altitude: 9249 m, speed: 300 kts, rate: -10 m/s ];
            descend [ altitude: 4 m,    speed: 140 kts, rate: -5 m/s  ];
        }
        segment {
            from: (40.4895, -3.5643, altitude: 610 m);
            to:   (41.0, -4.3, altitude: 9249 m);
            altslot [ min: 0 m, max: 9249 m, width: 50 m ];
            wind { direction: 90; speed: 12 m/s; }
            mode: climb;
        }
        segment {
            from: (41.0, -4.3, altitude: 9249 m);
            to:   (40.5, 0.0, altitude: 9249 m);
            altslot [ min: 9000 m, max: 9500 m, width: 50 m ];
            wind { direction: 90; speed: 15 m/s; }
            mode: cruise;
        }
        segment {
            from: (40.5, 0.0, altitude: 9249 m);
            to:   (41.2974, 2.0833, altitude: 4 m);
            altslot [ min: 0 m, max: 9249 m, width: 50 m ];
            wind { direction: 90; speed: 10 m/s; }
            mode: descend;
        }
    }
}
