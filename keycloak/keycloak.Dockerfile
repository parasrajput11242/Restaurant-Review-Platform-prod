# Keycloak with the realm baked in, for hosts that cannot mount files (Render, etc.)
FROM quay.io/keycloak/keycloak:26.7.2
COPY restaurant-review-realm.json /opt/keycloak/data/import/restaurant-review-realm.json
ENV KC_DB=postgres
ENV KC_HTTP_ENABLED=true
ENV KC_PROXY_HEADERS=xforwarded
ENV KC_HTTP_PORT=10000
EXPOSE 10000
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--import-realm"]
