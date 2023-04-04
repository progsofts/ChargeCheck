package com.progsoft.ChargeCheck;

public class DataItem {
    private String address, LL, distance;
    private String record;

    public DataItem(String address, String LL, String distance) {
        this.address = address;
        this.LL = LL;
        this.distance = distance;
        this.record = "";
    }

    public DataItem(String s) {
        this.record = s;
    }

    public String getRecord() {
        return record;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLL() {
        return LL;
    }

    public void setLL(String LL) {
        this.LL = LL;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }
}
