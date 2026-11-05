# ShopLite architecture

A monolithic Spring Boot application with a layered architecture and MVC in the presentation layer (details in the Lab 2 report).

```
Browser ──HTTP──▶ Spring Security ──▶ Controllers (MVC, REST /api/**)
                                          │
                                          ▼
                              Services (business logic, interfaces)
                                          │
                                          ▼
                              Repositories (Spring Data JPA)
                                          │
                                          ▼
                                 PostgreSQL / H2 (Flyway)
```

## Modules

| Package | Responsibility | Requirements |
|---|---|---|
| `user` | registration, login, profile | FR-01, FR-02, FR-11 |
| `catalog` | categories, products, search | FR-03 – FR-05, FR-12, FR-13 |
| `cart` | customer cart | FR-06 |
| `order` | checkout, statuses, order history | FR-07 – FR-10, FR-14 |
| `stats` | sales statistics | FR-15 |
| `config` | security | NFR-02, NFR-03 |
| `common` | exceptions and error handling | NFR-05 |

## Key decisions

- **Transactional checkout** (`OrderServiceImpl.placeOrder`): products are read with a row lock
  (`SELECT … FOR UPDATE`); if any product is out of stock, the whole transaction is rolled back (NFR-04).
- **Price at purchase** is stored in `OrderItem.priceAtPurchase`, so later price changes do not affect past orders.
- **Products are never deleted**, only deactivated with the `active` flag.
- **Status transitions** are encapsulated in `OrderStatus.canChangeTo()` (order state diagram).

## Database schema

Tables: `users`, `categories`, `products`, `carts`, `cart_items`, `orders`, `order_items`
(`src/main/resources/db/migration/V1__init_schema.sql`).
