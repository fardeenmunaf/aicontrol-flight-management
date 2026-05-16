// ERROR: OPO appears twice in the same flight

flight TP9999 {
    type:  regular;
    aircraft:  A320;
    route: XX001 from OPO to OPO;

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
        segment { mode: climb;   from: (41.262891, -8.68522, altitude: 69 m);  to: (42.0, -8.01, altitude: 9249 m); }
        segment { mode: cruise;  from: (42.0, -8.01, altitude: 9249 m); to: (41.0, -4.3, altitude: 9249 m); }
        segment { mode: descend; from: (41.0, -4.3,  altitude: 9249 m); to: (40.4895, -3.5643, altitude: 610 m); }
}

    leg {
        departure {
            airport: MAD;
            date:    2026-05-05;
            time:    11:00;
        }
        arrival {
            airport: OPO;
            date:    2026-05-05;
            time:    12:30;
        }
        fuel {
            quantity: 38000 kg;
        }
        segment { mode: climb;   from: (40.4895, -3.5643, altitude: 610 m); to: (41.5, -6.0, altitude: 9249 m); }
        segment { mode: cruise;  from: (41.5, -6.0, altitude: 9249 m); to: (41.8, -8.0, altitude: 9249 m); }
        segment { mode: descend; from: (41.8, -8.0, altitude: 9249 m); to: (41.262891, -8.68522, altitude: 69 m); }
}
}