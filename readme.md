# teslamate-query

## Goals

- reusable query services
- share cache
- unified authorization
- grafana-friendly APIs
- no sql in grafana


##

```
   Grafana -> RestAPI -> teslamate-query
                             | - Cache (In-Memory/Redis)
                             | - Auth
                             | - Repository  -> PostgreSQL <- Teslamate
```

## API Draft

```

GET /drives
GET /drives/{id}
GET /charges
GET /charges/{id}


```