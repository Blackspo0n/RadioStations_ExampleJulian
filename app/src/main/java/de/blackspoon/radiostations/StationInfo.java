package de.blackspoon.radiostations;

import java.io.Serializable;
import java.net.URI;
import java.net.URL;



public class StationInfo implements Serializable {
    public String name;
    public URI host;
    public URI stationImageURL;
    public URI stationWebsite;

    @Override
    public String toString() {
        return "Sender: " + name + "\n" + "Host:\n" + host.toString() + "\nWebsite:\n" + stationWebsite;

    }
}
