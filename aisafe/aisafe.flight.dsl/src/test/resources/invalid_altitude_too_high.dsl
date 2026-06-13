flight TP1234 {
    type:      regular;
    aircraft:  A320;
        pilot:     atcc1@tap.pt;
    route:     AA123 from OPO to MAD;

    leg {
        departure { airport: OPO; date: 2027-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2027-05-05; time: 09:30; }
        fuel { quantity: 40200 kg; }
        profile {
            climb   [ altitude: 69 m,    speed: 210 kts, rate: 10 m/s ];
            climb   [ altitude: 99999 m, speed: 300 kts, rate: 10 m/s ];
            cruise  [ speed: 460 kts ];
            descend [ altitude: 99999 m, speed: 300 kts, rate: -10 m/s ];
            descend [ altitude: 610 m,   speed: 140 kts, rate: -5 m/s  ];
        }
        segment {
            from: (41.262891, -8.68522, altitude: 69 m);
            to:   (42.0, -8.01, altitude: 99999 m);
            altslot [ min: 0 m, max: 99999 m, width: 50 m ];
            wind { direction: 270; speed: 15 m/s; }
            mode: climb;
        }
        segment {
            from: (42.0, -8.01, altitude: 99999 m);
            to:   (41.0, -4.3, altitude: 99999 m);
            altslot [ min: 99000 m, max: 99999 m, width: 50 m ];
            wind { direction: 260; speed: 20 m/s; }
            mode: cruise;
        }
        segment {
            from: (41.0, -4.3, altitude: 99999 m);
            to:   (40.4895, -3.5643, altitude: 610 m);
            altslot [ min: 0 m, max: 99999 m, width: 50 m ];
            wind { direction: 250; speed: 10 m/s; }
            mode: descend;
        }
    }
}
