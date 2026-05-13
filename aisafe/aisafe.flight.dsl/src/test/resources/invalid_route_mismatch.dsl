// TC08: route origin (LIS) does not match first leg departure (OPO)
//       route destination (BCN) does not match last leg arrival (MAD)

flight TP7777 {
    type:  regular;
    route: RP001 from LIS to BCN;

    leg {
        departure {
            airport: OPO;
            date:    2026-05-05;
            time:    08:00;
        }
        arrival {
            airport: MAD;
            date:    2026-05-05;
            time:    09:30;
        }
        fuel {
            quantity: 40200 kg;
        }
        segment {
            mode: cruise;
            from: (41.26, -8.68, altitude: 69 m);
            to:   (40.49, -3.56, altitude: 610 m);
        }
    }
}
