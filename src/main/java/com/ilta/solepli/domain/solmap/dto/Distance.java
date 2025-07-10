package com.ilta.solepli.domain.solmap.dto;

public record Distance(Number value, String unit) {
  public static Distance fromMeter(double meter) {
    if (meter < 1000) {
      return new Distance(Math.round(meter), "m");
    } else {
      double km = meter / 1000.0;
      double roundedKm = Math.round(km * 10.0) / 10.0;
      return new Distance(roundedKm, "km");
    }
  }
}
