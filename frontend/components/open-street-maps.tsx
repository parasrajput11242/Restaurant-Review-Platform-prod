"use client";

import { useEffect, useRef } from "react";
import type { GeoLocation } from "@/domain/domain";

/** Read-only map with one marker. Leaflet is loaded in the browser only. */
export default function OpenStreetMap({ location }: { location: GeoLocation }) {
  const mapRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const element = mapRef.current;
    if (!element) return;

    let map: import("leaflet").Map | undefined;
    let cancelled = false;

    (async () => {
      const L = (await import("leaflet")).default;
      await import("leaflet/dist/leaflet.css");
      if (cancelled) return;

      const position: [number, number] = [location.latitude, location.longitude];
      map = L.map(element, {
        zoomControl: false,
        dragging: false,
        touchZoom: false,
        scrollWheelZoom: false,
        doubleClickZoom: false,
        boxZoom: false,
        keyboard: false,
      }).setView(position, 16);

      L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution:
          '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      }).addTo(map);

      // Leaflet's default marker images are not bundled by Next, so draw the pin with CSS instead
      const icon = L.divIcon({
        className: "",
        html: '<div style="width:18px;height:18px;border-radius:50%;background:#111;border:3px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,.5)"></div>',
        iconSize: [18, 18],
        iconAnchor: [9, 9],
      });
      L.marker(position, { icon }).addTo(map);
    })();

    return () => {
      cancelled = true;
      map?.remove();
    };
  }, [location.latitude, location.longitude]);

  return <div ref={mapRef} className="w-full h-60 rounded-lg z-10" />;
}
