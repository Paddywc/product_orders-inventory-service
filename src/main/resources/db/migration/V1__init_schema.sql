
CREATE TABLE `product`
(
    `available_quantity` int(11) NOT NULL,
    `version`            bigint(20) DEFAULT NULL,
    `product_id`         binary(16) NOT NULL,
    PRIMARY KEY (`product_id`)
);

CREATE TABLE `inventory_reservation` (
                                         `quantity` int(11) NOT NULL,
                                         `created_at` datetime(6) NOT NULL,
                                         `updated_at` datetime(6) DEFAULT NULL,
                                         `order_id` binary(16) NOT NULL,
                                         `product_id` binary(16) NOT NULL,
                                         `reservation_id` binary(16) NOT NULL,
                                         `reservation_status` enum('CONFIRMED','RELEASED','RESERVED') NOT NULL,
                                         PRIMARY KEY (`reservation_id`),
                                         UNIQUE KEY `uk_inventory_reservation_order_product` (`order_id`,`product_id`)
);


CREATE TABLE `processed_order_event` (
                                         `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                         `processed_at` datetime(6) NOT NULL,
                                         `event_id` binary(16) NOT NULL,
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `p_order_e_uk_event_id` (`event_id`)
);

CREATE TABLE `processed_product_event` (
                                           `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                           `processed_at` datetime(6) NOT NULL,
                                           `event_id` binary(16) NOT NULL,
                                           PRIMARY KEY (`id`),
                                           UNIQUE KEY `p_product_e_uk_event_id` (`event_id`)
);