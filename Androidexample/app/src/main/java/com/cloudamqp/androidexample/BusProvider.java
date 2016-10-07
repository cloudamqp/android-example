package com.cloudamqp.androidexample;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

/**
 * Created by carlos on 10/7/16.
 */

public class BusProvider {
    private static Bus busInstance = new Bus(ThreadEnforcer.ANY);

    public static Bus getInstance() {
        return busInstance;
    }

    private BusProvider() {
    }
}
